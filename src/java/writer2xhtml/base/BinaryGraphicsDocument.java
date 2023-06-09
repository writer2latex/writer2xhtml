/************************************************************************
 *
 *  BinaryGraphicsDocument.java
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

import java.io.OutputStream;
import java.util.Base64;

import writer2xhtml.api.OutputFile;

import java.io.IOException;


/** This class is used to represent a binary graphics document to be included in the converter result.
 *  It may also represent to further types of images,which should <em>not</em> be included (and will produce an empty file
 *  if it is): 
 *  An image with the data encoded as base64, and a linked image whose data are not included 
 */
public class BinaryGraphicsDocument implements OutputFile {

    private String sFileName;
    private String sMimeType;
    
    private boolean bAcceptedFormat;
    
    private boolean bRecycled = false;
    
    // Data for an embedded image
    private byte[] blob = null;
    private int nOff = 0;
    private int nLen = 0;
    
    // Alternative data for base64 encoded data
    private String sBase64 = null;
    
    /**Constructs a new graphics document.
     * Until data is added using the <code>read</code> methods, the document is considered a link to
     * the image given by the file name.
     *
     * @param sFileName The name or URL of the <code>GraphicsDocument</code>.
     * @param sMimeType the MIME type of the document
     */
    public BinaryGraphicsDocument(String sFileName, String sMimeType) {
        this.sFileName = sFileName; 
        this.sMimeType = sMimeType;
        bAcceptedFormat = false; // or rather "don't know"
    }
    
    /** Construct a new graphics document which is a recycled version of the supplied one.
     *  This implies that all information is identical, but the recycled version does not contain any data blob.
     *  If the data is given by a base64 encoded string, the recycled versions does however contain the data.
     *  This is for images that are used more than once in the document.
     * 
     * @param bgd the source document
     */
    public BinaryGraphicsDocument(BinaryGraphicsDocument bgd) {
    	this.sFileName = bgd.getFileName();
    	this.sMimeType = bgd.getMIMEType();
    	this.bAcceptedFormat = bgd.isAcceptedFormat();
    	this.bRecycled = true;
    	this.sBase64 = bgd.getBase64();
    }
    
    /** Is this graphics document recycled?
     * 
     * @return true if this is the case
     */
    public boolean isRecycled() {
    	return bRecycled;
    }

    /** Set image contents to a byte array
     * 
     * @param data the image data
     * @param bIsAcceptedFormat flag to indicate that the format of the image is acceptable for the converter
     */
    public void setData(byte[] data, boolean bIsAcceptedFormat) {
        setData(data,0,data.length,bIsAcceptedFormat);
    }
    
    /** Set image contents to part of a byte array
     * 
     * @param data the image data
     * @param nOff the offset into the byte array
     * @param nLen the number of bytes to use
     * @param bIsAcceptedFormat flag to indicate that the format of the image is acceptable for the converter
     */
    public void setData(byte[] data, int nOff, int nLen, boolean bIsAcceptedFormat) {
        this.blob = data;
        this.nOff = nOff;
        this.nLen = nLen;
        this.bAcceptedFormat = bIsAcceptedFormat;
    }
    
    /** Convert image contents to a base64 encoded string
     * 
     * @param sBase64 the data
     */
    public void convertToBase64() {
    	sBase64 = Base64.getEncoder().encodeToString(this.blob);
    	this.blob = null;
    }
    
    /** Does this <code>BinaryGraphicsDocument</code> represent a linked image?
     * 
     * @return true if so
     */
    public boolean isLinked() {
    	return blob==null && sBase64==null && !bRecycled;
    }
    
    /** Is this image in an acceptable format for the converter?
     * 
     * @return true if so (always returns false for linked images)
     */
    public boolean isAcceptedFormat() {
    	return bAcceptedFormat;
    }
    
    /** Get the data of the image
     * 
     * @return the image data as a byte array - or null if this is a linked image
     */
    public byte[] getData() {
    	return blob;
    }
    
    /** Get the base64 encoded data of the image
     * 
     * @return
     */
    public String getBase64() {
    	return sBase64;
    }
    
    // Implement OutputFile
    
    /** Writes out the content to the specified <code>OutputStream</code>.
     *  Linked images will not write any data.
     *
     * @param  os  <code>OutputStream</code> to write out the  content.
     *
     * @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {
    	if (blob!=null) {
    		os.write(blob, nOff, nLen);
    	}
    }
    
    /** Get the document name or URL</p>
    *
    * @return  The document name or URL
    */
   public String getFileName() {
       return sFileName;
   }
   
    /** Get the MIME type of the document.
     *
     * @return  The MIME type or null if this is unknown
     */
	public String getMIMEType() {
		return sMimeType;
	}
	
    /** Is this document a master document?
     * 
     *  @return false - a graphics file is never a master document
     */
    public boolean isMasterDocument() {
		return false;
	}
    
    /** Does this document contain formulas?
     * 
     *  @return false - a graphics file does not contain formulas
     */
    public boolean containsMath() {
    	return false;
    }
}