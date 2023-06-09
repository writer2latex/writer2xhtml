/************************************************************************
 *
 *  ImageConverter.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2022 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2022-06-07)
 *
 */

package writer2xhtml.base;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2xhtml.api.GraphicConverter;
import writer2xhtml.office.EmbeddedBinaryObject;
import writer2xhtml.office.EmbeddedObject;
import writer2xhtml.office.MIMETypes;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.SVMReader;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Misc;

/** This class extracts and converts images from an office document.
 *  The images are returned as <code>BinaryGraphicsDocument</code>.
 *  The image converter can be configured as destructive. In this case, the returned
 *  graphics documents will contain the only reference to the image (the original data
 *  will be removed).
 */
public final class ImageConverter {
	private OfficeReader ofr;
	private boolean bDestructive;
	private boolean bUseBase64 = false;

    // Data for file name generation
    private String sBaseFileName = "";
    private String sSubDirName = "";
    private int nImageCount = 0;
    private NumberFormat formatter;
	
    // should EPS be extracted from SVM?
    private boolean bExtractEPS;
	
    // Data for image conversion
    private GraphicConverter gcv = null;
    private boolean bAcceptOtherFormats = true;
    private String sDefaultFormat = null;
    private String sDefaultVectorFormat = null;
    private HashSet<String> acceptedFormats = new HashSet<String>();
    
    // In the package format, the same image file may be used more than once in the document
    // Hence we keep information of all documents for potential reuse
    private HashMap<String,BinaryGraphicsDocument> recycledImages = new HashMap<String,BinaryGraphicsDocument>();

    /** Construct a new <code>ImageConverter</code> referring to a specific document
     * 
     * @param ofr the office reader to use
     * @param bExtractEPS set true if EPS content should be extracted from SVM files
     */
    public ImageConverter(OfficeReader ofr, boolean bDestructive, boolean bExtractEPS) {
        this.ofr = ofr;
        this.bDestructive = bDestructive;
        this.bExtractEPS = bExtractEPS;
        this.formatter = new DecimalFormat("000");
    }
	
    /** Define the base file name to use for generating file names
     * 
     * @param sBaseFileName the base file name
     */
    public void setBaseFileName(String sBaseFileName) {
    	this.sBaseFileName = sBaseFileName;
    }
    
    /** Define the name of a sub directory to prepend to file names
     * 
     * @param sSubDirName the sub directory
     */
    public void setUseSubdir(String sSubDirName) {
    	this.sSubDirName = sSubDirName+"/";
    }
    
    /** Specify that the <code>ImageConverter</code> should return an image even if it was not possible
     *  to convert it to an acceptable format.
     * 
     * @param b true if other formats should be accepted
     */
    public void setAcceptOtherFormats(boolean b) {
    	bAcceptOtherFormats = b;
    }
	
    /** Define the default format for raster graphics
     * 
     * @param sMime the MIME type of the default raster format
     */
    public void setDefaultFormat(String sMime) {
        addAcceptedFormat(sMime);
        sDefaultFormat = sMime;
    }
	
    /** Define the default format for vector graphics
     * 
     * @param sMime the MIME type for the default vector format
     */
    public void setDefaultVectorFormat(String sMime) {
        addAcceptedFormat(sMime);
        sDefaultVectorFormat = sMime;
    }
	
    /** Define an accepted graphics format
     * 
     * @param sMime the MIME type of the format 
     */
    public void addAcceptedFormat(String sMime) {
    	acceptedFormats.add(sMime);
    }
	
    /** Is a given format accepted?
     * 
     * @param sMime the MIME type to query
     * @return true if this is an accepted format
     */
    private boolean isAcceptedFormat(String sMime) {
    	return acceptedFormats.contains(sMime);
    }
	
    /** Define the <code>GraphicConverter</code> to use for image conversion
     * 
     * @param gcv the graphics converter
     */
    public void setGraphicConverter(GraphicConverter gcv) {
    	this.gcv = gcv;
    }
    
    /** Define whether to use Base64 to represent binary data
     * 
     * @param b
     */
    public void setUseBase64(boolean b) {
    	this.bUseBase64 = b;
    }
    
    /** Get an image from a <code>draw:image</code> or <code>text:list-level-style-image</code>element.
     *  If the converter is destructive, the returned <code>BinaryGraphicsDocument</code> will hold the
     *  only reference to the image data (the original data will be removed).
     * 
     * @param node the image element
     * @return a document containing the (converted) image, or null if it was not possible to read the image
     * or convert it to an accepted format
     */
    public BinaryGraphicsDocument getImage(Element node) {
        String sName = sSubDirName+sBaseFileName+formatter.format(++nImageCount);
        BinaryGraphicsDocument bgd = getImage(node,sName);
        if (bgd!=null) {
        	if (!bgd.isAcceptedFormat() || (sDefaultVectorFormat!=null && !sDefaultVectorFormat.equals(bgd.getMIMEType()))) {
        		// We may have better luck with an alternative image
	        	Element sibling = getAlternativeImage(node);
	        	if (sibling!=null) {
	        		BinaryGraphicsDocument altBgd = getImage(sibling,sName);
	        		if (altBgd!=null && altBgd.isAcceptedFormat()) {
	        			if (!bgd.isAcceptedFormat() ||
	        				(sDefaultVectorFormat!=null && !sDefaultVectorFormat.equals(bgd.getMIMEType()) &&
	        								sDefaultVectorFormat.equals(altBgd.getMIMEType()))) {
		        			bgd = altBgd;
	        			}
	        		}
	        	}
        	}        	
        }
    	if (bgd==null || bgd.isLinked() || bgd.isRecycled()) {
    		 // The file name was not used
    		nImageCount--;
    	}
    	return bgd;
    }
    
    private BinaryGraphicsDocument getImage(Element node, String sName) {
    	assert(XMLString.DRAW_IMAGE.equals(node.getTagName()) || XMLString.TEXT_LIST_LEVEL_STYLE_IMAGE.equals(node.getTagName()));

    	// Image data
        String sExt = null;
    	String sMIME = null;
    	byte[] blob = null;
    	String sId = null;
    	
    	// First try to extract the image using the xlink:href attribute
    	if (node.hasAttribute(XMLString.XLINK_HREF)) {
    		String sHref = node.getAttribute(XMLString.XLINK_HREF);
    		if (sHref.length()>0) {
    			// We may have seen this image before, return the recycled version
    			if (recycledImages.containsKey(sHref)) {
    				return recycledImages.get(sHref);
    			}
	    		// Image may be embedded in package:
    			String sPath = sHref;
	            if (sPath.startsWith("#")) { sPath = sPath.substring(1); }
	            if (sPath.startsWith("./")) { sPath = sPath.substring(2); }
	            EmbeddedObject obj = ofr.getEmbeddedObject(sPath);
	            if (obj!=null && obj instanceof EmbeddedBinaryObject) {
	                EmbeddedBinaryObject object = (EmbeddedBinaryObject) obj;
	                blob = object.getBinaryData();
	                sMIME = object.getType();
	                if (sMIME.length()==0) {
	                    // If the manifest provides a media type, trust that
	                    // Otherwise determine it by byte inspection
	                	sMIME = MIMETypes.getMagicMIMEType(blob);
	                }
	            	sExt = MIMETypes.getFileExtension(sMIME);
	            	if (bDestructive) {
	            		object.dispose();
	            	}
	            	// We got an image, define ID for recycling
	            	sId = sHref;
	            }
	            else {
	                // This is a linked image
	                // TODO: Add option to download image from the URL?
	            	String sFileName = ofr.fixRelativeLink(sHref);
	            	BinaryGraphicsDocument bgd
	            		= new BinaryGraphicsDocument(sFileName,null);
	            	return bgd;
	            }
    		}
    	}
        
    	// If there is no suitable xlink:href attribute, the image must be contained in an office:binary-element as base64
    	if (blob==null) {
	        Node obd = Misc.getChildByTagName(node,XMLString.OFFICE_BINARY_DATA);
	        if (obd!=null) {
	            StringBuilder buf = new StringBuilder();
	            NodeList nl = obd.getChildNodes();
	            int nLen = nl.getLength();
	            for (int i=0; i<nLen; i++) {
	                if (nl.item(i).getNodeType()==Node.TEXT_NODE) {
	                    buf.append(nl.item(i).getNodeValue());
	                }
	            }
	            //blob = Base64.decode(buf.toString());
	            //blob = DatatypeConverter.parseBase64Binary(buf.toString());
	            blob = Base64.getDecoder().decode(buf.toString());
    			// We may have seen this image before, return the recycled version
	            String sId1 = createId(blob);
    			if (recycledImages.containsKey(sId1)) {
    				return recycledImages.get(sId1);
    			}
    			sMIME = MIMETypes.getMagicMIMEType(blob);
	            sExt = MIMETypes.getFileExtension(sMIME);
	            if (bDestructive) {
	            	node.removeChild(obd);
	            }
	            // We got an image, define ID for recycling
	            sId = sId1;
	        }
	        else {
	        	// There is no image data
	        	return null;
	        }
    	}
        
        // Is this an EPS file embedded in an SVM file?
        // (This case is obsolete, but kept for the sake of old documents)
        if (bExtractEPS && MIMETypes.SVM.equals(sMIME)) {
            // Look for postscript:
            int[] offlen = new int[2];
            if (SVMReader.readSVM(blob,offlen)) {
            	String sFileName = sName+MIMETypes.EPS_EXT;
                BinaryGraphicsDocument bgd
                	= new BinaryGraphicsDocument(sFileName, MIMETypes.EPS);
                bgd.setData(blob,offlen[0],offlen[1],true);
                return bgd;
             }
        }
        
        // We have an embedded image.
     
        // If we have a converter AND a default format AND this image
        // is not in an accepted format AND the converter knows how to
        // convert it - try to convert...
        if (gcv!=null && !isAcceptedFormat(sMIME) && sDefaultFormat!=null) {
        	byte[] newBlob = null;
            String sTargetMIME = null;

            if (MIMETypes.isVectorFormat(sMIME) && sDefaultVectorFormat!=null &&
                gcv.supportsConversion(sMIME,sDefaultVectorFormat,false,false)) {
            	// Try vector format first
                newBlob = gcv.convert(blob, sMIME, sTargetMIME=sDefaultVectorFormat);
            }
            if (newBlob==null && gcv.supportsConversion(sMIME,sDefaultFormat,false,false)) {
            	// Then try bitmap format
                newBlob = gcv.convert(blob,sMIME,sTargetMIME=sDefaultFormat);
            }

            if (newBlob!=null) {
            	// Conversion successful - create new data
            	blob = newBlob;
            	sMIME = sTargetMIME;
            	sExt = MIMETypes.getFileExtension(sMIME);
            }
        }
        
        // Create the result
        if (isAcceptedFormat(sMIME) || bAcceptOtherFormats) {
        	String sFileName = sName+sExt;
            BinaryGraphicsDocument bgd = new BinaryGraphicsDocument(sFileName,sMIME);
            bgd.setData(blob,isAcceptedFormat(sMIME));
            if (bUseBase64 && !MIMETypes.SVG.equals(sMIME)) { bgd.convertToBase64(); }
            if (sId!=null) {
        		recycledImages.put(sId, new BinaryGraphicsDocument(bgd));
            }
            return bgd;
        }
        else {
        	return null;
        }
    }
    
    private Element getAlternativeImage(Element node) {
    	Node sibling = node.getNextSibling();
    	if (sibling!=null && Misc.isElement(sibling, XMLString.DRAW_IMAGE)) {
    		return (Element) sibling;
    	}
    	return null;
    }
    
    // Create a fingerprint of a blob. The fingerprint concatenates the MD5 hash with the first 10 bytes of the blob.
    private String createId(byte[] blob) {
    	MessageDigest md;
    	try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// This would be surprising
			return null;
		}
    	return Base64.getEncoder().encodeToString(md.digest(blob))
    			+Base64.getEncoder().encodeToString(Arrays.copyOf(blob, 10));
    	// return DatatypeConverter.printHexBinary(md.digest(blob))
    	//		+DatatypeConverter.printHexBinary(Arrays.copyOf(blob, 10));
    }
    
}
