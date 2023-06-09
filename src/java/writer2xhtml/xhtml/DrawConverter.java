/************************************************************************
 *
 *  DrawConverter.java
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
 *  Version 1.7 (2022-06-23)
 *
 */
 
 /* TODO (impress2xhtml)
  * Support master page content!
  * New option: xhtml_draw_scaling: scale all draw objects this percentage
  * (applies to applySize in this class + page size in PageStyleConverter)
  * Certain options should have a fixed value for impress2xhtml:
  *   original_image_size: always false
  *   xhtml_formatting: always "convert_all"
  *   xhtml_frame_formatting: always "convert_all"
  *   xhtml_use_list_hack: always "true" (until list merge is fixed..)
  * apply hard draw page background (see below)
  * apply z-order for draw objects (frames)
  * export notes (part of draw-page)
  * export list-style-image for image bullets!
  */
package writer2xhtml.xhtml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

import writer2xhtml.base.BinaryGraphicsDocument;
import writer2xhtml.office.ControlReader;
import writer2xhtml.office.EmbeddedObject;
import writer2xhtml.office.EmbeddedXMLObject;
import writer2xhtml.office.FormReader;
import writer2xhtml.office.MIMETypes;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.CSVList;
import writer2xhtml.util.Calc;
import writer2xhtml.util.Misc;
import writer2xhtml.util.SimpleInputBuffer;
import writer2xhtml.util.SimpleXMLParser;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

public class DrawConverter extends ConverterHelper {

    /** Identifies objects that should be displayed inline.
     */
    public static final int INLINE = 0;

    /** Identifies objects that should be displayed as floats, either alone
     *  or with text wrap (using the css attribute float:left or float:right)
     */
    public static final int FLOATING = 1; 

    /** Identifies objects that should be positioned absolute (using the css
     *  attribute postion:absolute)
     */
    public static final int ABSOLUTE = 2;
	
    /** Identifies objects that should be placed centered */
    public static final int CENTERED = 3;
    
    /** Identifies objects that should fill the entire screen */
    public static final int FULL_SCREEN = 4;
    
	
    private FormReader form = null;
    private String sScale;
    private int nImageSize;
    private String sImageSplit;
    private boolean bCoverImage;
    private boolean bEmbedSVG;
    private boolean bEmbedImg;

    // Embedded SVG images for recycling are collected here
    private HashMap<String,Element> svgImages = new HashMap<String,Element>();
    
    // Frames in spreadsheet documents are collected here
    private Vector<Element> frames = new Vector<Element>();
    // This flag determines whether to collect frames or insert them immediately
    private boolean bCollectFrames = false;
    
    // Large images (for full screen) in EPUB export are collected here
    private Vector<Element> fullscreenFrames = new Vector<Element>();
    // This flag determines whether to collect full screen images or insert them immediately
    private boolean bCollectFullscreenFrames = true;

    public DrawConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        // We can only handle one form; pick an arbitrary one.
        // Also we cannot split a form over several files.
        Iterator<FormReader> formsIterator = ofr.getForms().getFormsIterator();
        if (formsIterator.hasNext() && config.getXhtmlSplitLevel()==0) {
            form = formsIterator.next();
        }
        bCollectFrames = ofr.isSpreadsheet();
        bCollectFullscreenFrames = true;
        sScale = config.getXhtmlScaling();
        nImageSize = config.imageSize();
        sImageSplit = config.imageSplit();
        bCoverImage = config.coverImage();
        bEmbedSVG = config.embedSVG();
        bEmbedImg = config.embedImg();
    }
    
    ///////////////////////////////////////////////////////////////////////
    // Complete Draw documents/presentations

    public void convertDrawContent(Element onode) {
        if (!onode.hasChildNodes()) { return; }
        NodeList nList = onode.getChildNodes();
        int nLen = nList.getLength();
        for (int i=0; i<nLen; i++) {
            Node child = nList.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sNodeName = child.getNodeName();
                if (sNodeName.equals(XMLString.DRAW_PAGE)) {
                    handleDrawPage((Element)child,converter.nextOutFile());
                }
            }
        }
    }

    private void handleDrawPage(Element onode, Element hnode) {
        Element div = converter.createElement("div");
        hnode.appendChild(div);
		
        // Style it (TODO: Apply hard drawing-page (background) style)
        StyleInfo info = new StyleInfo();
        getPageSc().applyStyle(onode.getAttribute(XMLString.DRAW_MASTER_PAGE_NAME),info);
        info.props.addValue("top","40px"); // Somewhat arbitrary
        info.props.addValue("left","0");
        info.props.addValue("position","absolute");		
        applyStyle(info,div);
		
        // Traverse the draw:page
        if (!onode.hasChildNodes()) { return; }
        NodeList nList = onode.getChildNodes();
        int nLen = nList.getLength();
        for (int i=0; i<nLen; i++) {
            Node child = nList.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                handleDrawElement((Element)child,div,div,ABSOLUTE);
            }
        }
    }
	
    /////////////////////////////////////////////////////////////////////////
    // Form document

    /** <p>Create form, if there is a form in this document</p>
     *  @return the form element, or null if there are no forms in the document
     */
    public Element createForm() {
        if (form==null) return null;
        Element htmlForm = converter.createElement("form");
        htmlForm.setAttribute("name", form.getAttribute(XMLString.FORM_NAME));
        htmlForm.setAttribute("action", form.getAttribute(XMLString.XLINK_HREF));
        String sMethod = form.getAttribute(XMLString.FORM_METHOD);
        htmlForm.setAttribute("method", sMethod!=null ? sMethod : "get");
        return htmlForm;
    }
	
    /////////////////////////////////////////////////////////////////////////
    // DRAW ELEMENTS
	
    /** <p> A draw element with a hyperlink is represented as two elements,
     *  eg. <code>&lt;draw:a&gt;&lt;draw:image/&gt;&lt;/draw:a&gt;</code>.
     *  We thus need methods to switch between the two elements.</p>
     *  <p> This method takes a <code>draw</code>-element.
     *  If this element is a hyperlink, the child element is returned.
     *  Otherwise the argument is returned unchanged.</p>
     *  @param onode the <code>draw:a</code> element
     *  @return the corresponding element
     */
    public Element getRealDrawElement(Element onode) {
        if (XMLString.DRAW_A.equals(onode.getTagName())) {
            Node child = onode.getFirstChild();
            while (child!=null) {
                if (OfficeReader.isDrawElement(child)) { return (Element) child; }
                child = child.getNextSibling();
            }
            return null; // empty anchor
        }
        return onode;
    }
	
    /** <p> A draw element with a hyperlink is represented as two elements,
     *  eg. <code>&lt;draw:a&gt;&lt;draw:image/&gt;&lt;/draw:a&gt;</code>.
     *  We thus need methods to switch between the two elements.</p>
     *  <p> This method takes a <code>draw</code>-element.
     *  If this element is contained in a hyperlink, the hyperlink is returned.
     *  Otherwise null is returned.</p>
     *  @param onode the <code>draw:a</code> element
     *  @return the hyperlink element, if any
     */
    public Element getDrawAnchor(Element onode) {
        Element parent = (Element) onode.getParentNode();
        // in oasis format, we need to skip the frame as well
        if (XMLString.DRAW_FRAME.equals(parent.getTagName())) {
            parent = (Element) parent.getParentNode();
        }
        if (XMLString.DRAW_A.equals(parent.getTagName())) { return parent; }
        return null;
    }
	
    private Element getFrame(Element onode) {
        if (ofr.isOpenDocument()) return (Element) onode.getParentNode();
        else return onode;
    }
    
    // Add cover image (the first image in the document is taken out of context)
    public Element insertCoverImage(Element hnode) {
    	Element currentNode = hnode;
    	if (bCoverImage) {
    		Element cover = ofr.getFirstImage();
    		if (cover!=null) {
    			converter.setCoverFile(null);
    			bCollectFullscreenFrames = false;
    			handleDrawElement(cover,currentNode,null,FULL_SCREEN);
    			bCollectFullscreenFrames = true;
    			// Add margin:0 to body
    			Element head = Misc.getChildByTagName(hnode.getOwnerDocument().getDocumentElement(),"head");
    			if (head!=null) {
    				Element style = converter.createElement("style");
    				head.appendChild(style);
    				style.setAttribute("type", "text/css");
    				style.appendChild(converter.createTextNode("body { margin:0 }"));
    			}
    			currentNode = getTextCv().doMaybeSplit(hnode, 0);    	
    		}
    	}
    	return currentNode;
    }
    
    // Flush all full screen images, returning the new document node
    public Element flushFullscreenFrames(Element hnode) {
    	Element currentNode = hnode;
    	if (converter.isTopLevel() && !fullscreenFrames.isEmpty()) {
    		bCollectFullscreenFrames = false;
    		currentNode = getTextCv().doMaybeSplit(hnode, 0);
    		for (Element image : fullscreenFrames) {
    			handleDrawElement(image,currentNode,null,FULL_SCREEN);
    			currentNode = getTextCv().doMaybeSplit(hnode, 0);
    		}
    		fullscreenFrames.clear();
    		bCollectFullscreenFrames = true;
    	}
    	return currentNode;
    }
	
    public void flushFrames(Element hnode) {
        bCollectFrames = false;
        int nCount = frames.size();
        for (int i=0; i<nCount; i++) {
            handleDrawElement(frames.get(i),hnode,null,CENTERED);
        }
        frames.clear();
        bCollectFrames = true;
    }
	
    /** <p>Convert a draw element to xhtml. The presentation depends on the
     *  parameter <code>nMode</code>:</p>
     *  <ul><li><code>DrawConverter.INLINE</code>: Presented inline. The hnode
     *  must accept inline content. An inline container <em>must</em> be
     *  provided.</li>
     *  <li><code>DrawConverter.FLOAT</code>: Presented as a float. The hnode
     *  must accept block/flow content. A block container <em>must</em> be
     *  provided.</li>
     *  <li><code>DrawConverter.ABSOLUTE</code>: Presented at an absolute
     *  position. A block container <em>must</em> be provided.</li>
     *  </ul>
     *  <p>Containers for block and inline elements should be supplied.
     *  The containers may be identical (flow container).</p>
     *  <p>Note: A draw:text-box will be ignored in inline mode.</p>
     *  @param onode the draw element
     *  @param hnodeBlock the xhtml element to attach the converted element to if it's a block element
     *  @param hnodeInline the xhtml element to attach the converted element to if it's an inline element
     *  @param nMode identifies how the element should be presented 
     */
    public void handleDrawElement(Element onode, Element hnodeBlock, Element hnodeInline, int nMode) {
        if (bCollectFrames) {
            frames.add(onode);
            return;
        }
        String sName = onode.getNodeName();
        if (sName.equals(XMLString.DRAW_OBJECT)) {
            handleDrawObject(onode,hnodeBlock,hnodeInline,nMode);
        }		
        else if (sName.equals(XMLString.DRAW_OBJECT_OLE)) {
            handleDrawObject(onode,hnodeBlock,hnodeInline,nMode);
        }		
        else if (sName.equals(XMLString.DRAW_IMAGE)) {
            handleDrawImage(onode,hnodeBlock,hnodeInline,nMode);
        }		
        else if (sName.equals(XMLString.DRAW_TEXT_BOX)) {
            handleDrawTextBox(onode,hnodeBlock,hnodeInline,nMode);
        }		
        else if (sName.equals(XMLString.DRAW_A)) {
            Element elm = getRealDrawElement(onode);
            if (elm!=null) {
                handleDrawElement(elm,hnodeBlock,hnodeInline,nMode);
            }
        }
        else if (sName.equals(XMLString.DRAW_FRAME)) {
        	// First check for TexMaths equation
        	if (!getMathCv().convertTexMathsEquation(onode, hnodeBlock, hnodeInline, nMode)) {
        		// OpenDocument embeds the draw element in a frame element
        		handleDrawElement(Misc.getFirstChildElement(onode),hnodeBlock,hnodeInline,nMode);
        	}
        }
        else if (sName.equals(XMLString.DRAW_G)) {
        	// First check for TexMaths equation
        	if (!getMathCv().convertTexMathsEquation(onode, hnodeBlock, hnodeInline, nMode)) {
        		handleDrawGroup(onode,hnodeBlock,hnodeInline,nMode);
        	}
        }		
        else if (sName.equals(XMLString.DRAW_CONTROL)) {
            handleDrawControl(onode,hnodeBlock,hnodeInline,nMode);
        }		
    }    

    private void handleDrawObject(Element onode, Element hnodeBlock, Element hnodeInline, int nMode) {
        // TODO: Placement if not inline
        // If possible, add the object inline. In pure block context, add a div.
        Element hnode;
        if (hnodeInline!=null) {
            hnode = hnodeInline;
        }
        else {
            Element div = converter.createElement("div");
            hnodeBlock.appendChild(div);
            hnode = div;
        }

        boolean bNoTextPar = true;
        Node par = OfficeReader.getParagraph(onode);
        if (par!=null) {
        	bNoTextPar = OfficeReader.isNoTextPar(par);
        }

    	String sHref = Misc.getAttribute(onode, XMLString.XLINK_HREF);
    	if (sHref!=null) { // Embedded object in package or linked object
            if (ofr.isInPackage(sHref)) { // Embedded object in package
                if (sHref.startsWith("#")) { sHref=sHref.substring(1); }
                if (sHref.startsWith("./")) { sHref=sHref.substring(2); }
                EmbeddedObject object = converter.getEmbeddedObject(sHref); 
                if (MIMETypes.MATH.equals(object.getType()) || MIMETypes.ODF.equals(object.getType())) { // Formula!
                    EmbeddedXMLObject xmlObject = (EmbeddedXMLObject) object;
                    // Document settings = object.getSettingsDOM();
                    Element replacementImage = null;
                    if (ofr.isOpenDocument()) { // look for replacement image
                        replacementImage = Misc.getChildByTagName(getFrame(onode),XMLString.DRAW_IMAGE);
                    }
                    try {
                        hnode.appendChild(converter.createTextNode(" "));
                        getMathCv().convert(replacementImage,xmlObject.getContentDOM().getDocumentElement(),hnode,bNoTextPar);
                        hnode.appendChild(converter.createTextNode(" "));
                    }
                    catch (SAXException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else { // unsupported object
                    boolean bIgnore = true;
                    if (ofr.isOpenDocument()) { // look for replacement image
                        Element replacementImage = Misc.getChildByTagName(getFrame(onode),XMLString.DRAW_IMAGE);
                        if (replacementImage!=null) {
                            handleDrawImage(replacementImage,hnodeBlock,hnodeInline,nMode);
                            bIgnore = false;
                        }
                    }
                    if (bIgnore) { 
                        hnode.appendChild( converter.createTextNode("[Warning: object ignored]"));
                    }
                }
            }
            else { // TODO: Linked object
                hnode.appendChild( converter.createTextNode("[Warning: Linked object ignored]"));
            }
        }
        else { // flat xml format
            Element formula = Misc.getChildByTagName(onode,XMLString.MATH); // Since OOo 3.2
            if (formula==null) {
            	formula = Misc.getChildByTagName(onode,XMLString.MATH_MATH);
            }
            if (formula != null) {
            	Element replacementImage = null;
                if (ofr.isOpenDocument()) { // look for replacement image
                    replacementImage = Misc.getChildByTagName(getFrame(onode),XMLString.DRAW_IMAGE);
                }
                hnode.appendChild(converter.createTextNode(" "));
                getMathCv().convert(replacementImage,formula,hnode,bNoTextPar);
                hnode.appendChild(converter.createTextNode(" "));
            }
            else { // unsupported object
                boolean bIgnore = true;
                if (ofr.isOpenDocument()) { // look for replacement image
                    Element replacementImage = Misc.getChildByTagName(getFrame(onode),XMLString.DRAW_IMAGE);
                    if (replacementImage!=null) {
                        handleDrawImage(replacementImage,hnodeBlock,hnodeInline,nMode);
                        bIgnore = false;
                    }
                }
                if (bIgnore) { 
                    hnode.appendChild( converter.createTextNode("[Warning: object ignored]"));
                }
            }
        }
    }
	
    private void handleDrawImage(Element onode, Element hnodeBlock, Element hnodeInline, int nMode) {
        Element frame = getFrame(onode);
        
    	// For EPUB document some images require special treatment: Cover image and full screen images
        if (bCollectFullscreenFrames && converter.isOPS()) {
            // First check whether this is the cover image
        	if (bCoverImage && onode==ofr.getFirstImage()) {
        		return;
        	}
        	// Next check to see if we should treat this image as a "full screen" image
        	// (Currently only images are handled like this, hence the code is here rather than in handleDrawElement)
        	else if (!"none".equals(sImageSplit) && converter.isTopLevel()) {
        		StyleWithProperties style = ofr.getFrameStyle(frame.getAttribute(XMLString.DRAW_STYLE_NAME));
        		String sWidth = getFrameWidth(frame, style);
        		String sHeight = getFrameHeight(frame, style);
        		// It is if the image width exceeds a certain percentage of the current text width and the height is
        		// greater than 1.33*the width (recommended by Michel "Coolmicro")
        		if (sWidth!=null && sHeight!=null &&
        				Calc.sub(Calc.multiply("133%",sWidth), sHeight).startsWith("-") &&
        				Calc.sub(Calc.multiply(sImageSplit,converter.getContentWidth()),
        						Calc.multiply(sScale,Calc.truncateLength(sWidth))).startsWith("-")) {
        			fullscreenFrames.add(onode);
        			return;
        		}
        	}
        }
        
        // Get the image from the ImageLoader
        BinaryGraphicsDocument bgd = null;
        String sFileName = null;
        bgd = converter.getImageCv().getImage(onode);
        if (bgd!=null) {
        	if (!bgd.isLinked()) { // embedded image
                sFileName = bgd.getFileName();
                // If this is the cover image, add it to the converter result
            	if (bCoverImage && onode==ofr.getFirstImage()) {
            		converter.setCoverImageFile(bgd,null);
            	}        		
        	}
        	else { // linked image
            	if (!converter.isOPS()) { // Cannot have linked images in EPUB, ignore the image
            		sFileName = bgd.getFileName();
            	}
        	}
        }        
        
        if (sFileName==null) { return; } // TODO: Add warning?
        
        // Create the image (sFileName contains the file name)
        Element imageElement = null;
        if (converter.isHTML5() && bEmbedSVG && bgd!=null && MIMETypes.SVG.equals(bgd.getMIMEType())) {
        	// In HTML5 we may embed SVG images directly in the document
        	if (bgd.isRecycled()) {
        		// Get from the cache (actually we are certain it is there)
        		if (svgImages.containsKey(bgd.getFileName())) {
					Element elm = hnodeInline!=null ? hnodeInline : hnodeBlock;
					imageElement = (Element) elm.getOwnerDocument().importNode(svgImages.get(bgd.getFileName()), true);
        		}
        	}
        	else {
        		// Parse the data
	        	byte[] blob = bgd.getData();
	        	try {
					Document dom = SimpleXMLParser.parse(new ByteArrayInputStream(blob));
					if (dom!=null) {
						Element elm = hnodeInline!=null ? hnodeInline : hnodeBlock;
						imageElement = (Element) elm.getOwnerDocument().importNode(dom.getDocumentElement(), true);
						// Store in cache in case we need to recycle
						svgImages.put(bgd.getFileName(), imageElement);
					}
					// Else fail silently
				} catch (IOException e) {
					// Will not happen with a byte array
				} catch (SAXException e) {
					// Fail silently
				}
        	}
        }
        else {
        	 // In all other cases, create an img element
        	if (bgd!=null && !bgd.isLinked() && !bgd.isRecycled() && (!bEmbedImg || MIMETypes.SVG.equals(bgd.getMIMEType()))) { converter.addDocument(bgd); }
        	Element image = converter.createElement("img");
        	String sName = Misc.getAttribute(getFrame(onode),XMLString.DRAW_NAME);
        	converter.addTarget(image,sName+"|graphic");
        	if (!bEmbedImg || MIMETypes.SVG.equals(bgd.getMIMEType()) || bgd.isLinked()) {
        		image.setAttribute("src",sFileName);
        	}
        	else {
        		StringBuilder buf = new StringBuilder();
        		buf.append("data:").append(bgd.getMIMEType()).append(";base64,").append(bgd.getBase64());
        		image.setAttribute("src", buf.toString());
        	}
        	// Add alternative text, using either alt.text, name or file name
        	Element desc = Misc.getChildByTagName(frame,XMLString.SVG_DESC);
        	if (desc==null) {
        		desc = Misc.getChildByTagName(frame,XMLString.SVG_TITLE);
        	}
        	String sAltText = desc!=null ? Misc.getPCDATA(desc) : (sName!=null ? sName : sFileName);
        	image.setAttribute("alt",sAltText);
        	imageElement = image; 
        }
        
        if (imageElement!=null) {
        	// Now style it
        	StyleInfo info = new StyleInfo();
        	String sStyleName = Misc.getAttribute(frame, XMLString.DRAW_STYLE_NAME);
        	if (nMode!=FULL_SCREEN) { getFrameSc().applyStyle(sStyleName,info); }
        	applyImageSize(frame,info.props,nMode,false);

        	// Apply placement
        	applyPlacement(frame, hnodeBlock, hnodeInline, nMode, imageElement, info);

        	applyStyle(info,imageElement);
        	addLink(onode,imageElement);
        }
    }

    private void handleDrawTextBox(Element onode, Element hnodeBlock, Element hnodeInline, int nMode) {
        // Create the div with id=name
        Element textbox = converter.createElement("div");
        if (hnodeBlock!=null) {
            hnodeBlock.appendChild(textbox);
        }
        else { // cannot include the div inline, ignore
            return;
        }
        // Add name, if defined
        String sName = Misc.getAttribute(getFrame(onode),XMLString.DRAW_NAME);
        if (sName!=null) { converter.addTarget(textbox,sName+"|frame"); }
        
        // Now style it
        Element frame = getFrame(onode);
        StyleInfo info = new StyleInfo();
        // Draw frame style
        String sStyleName = Misc.getAttribute(frame, XMLString.DRAW_STYLE_NAME);
        if (sStyleName!=null) {
            getFrameSc().applyStyle(sStyleName,info);
        }
        // Presentation frame style
        String sPresentationStyleName = Misc.getAttribute(frame, XMLString.PRESENTATION_STYLE_NAME);
        if (sPresentationStyleName!=null) {
            if ("outline".equals(Misc.getAttribute(frame, XMLString.PRESENTATION_CLASS))) {
                getPresentationSc().enterOutline(sPresentationStyleName);
            }
            getPresentationSc().applyStyle(sPresentationStyleName,info);
        }
        // Additional text formatting
        String sTextStyleName = Misc.getAttribute(frame, XMLString.DRAW_TEXT_STYLE_NAME);
        if (sTextStyleName!=null) {
            //getStyleCv().applyParStyle(sTextStyleName,info);
        }
		
        // Apply placement
        String sContentWidth = null;
        
        switch (nMode) {
        case INLINE:
        	break;
        case ABSOLUTE:
        	sContentWidth = applyImageSize(frame,info.props,nMode,false);
        	info.props.addValue("margin-left","auto");
        	info.props.addValue("margin-right","auto");
        	applyPosition(frame,info.props);
        	break;
        case CENTERED:
        	info.props.addValue("margin-top","2px");
        	info.props.addValue("margin-bottom","2px");
        	info.props.addValue("margin-left","auto");
        	info.props.addValue("margin-right","auto");
        	sContentWidth = applyImageSize(frame,info.props,nMode,true);
        	break;
        case FLOATING:
        	sContentWidth = applyImageSize(frame,info.props,nMode,true);
        	StyleWithProperties style = ofr.getFrameStyle(sStyleName);
        	if (style!=null) {
        		String sPos = style.getProperty(XMLString.STYLE_HORIZONTAL_POS);
        		String sWrap = style.getProperty(XMLString.STYLE_WRAP);
        		if (isLeft(sPos)) {
        			if (mayWrapRight(sWrap)) {
        				info.props.addValue("float","left");
        			}
        			else {
        				// TODO: Remove the margin-right attribute from the existing properties
        				// and likewise for the other two cases below (requires new CSVList)
        				info.props.addValue("margin-right", "auto");
        			}
        		}
        		else if (isRight(sPos)) {
        			if (mayWrapLeft(sWrap)) {
        				info.props.addValue("float","right");
        			}
        			else {
        				info.props.addValue("margin-left", "auto");
        			}
        		}
        		else if (isCenter(sPos)) {
    				info.props.addValue("margin-left", "auto");
    				info.props.addValue("margin-right", "auto");        				        			
        		}
        		else if (isFromLeft(sPos)) {
        			if (mayWrapRight(sWrap)) {
        				info.props.addValue("float","left");
        			}
        			String sX = frame.getAttribute(XMLString.SVG_X);
        			if (sX!=null && sX.length()>0) {
        				info.props.addValue("margin-left",scale(Calc.truncateLength(sX)));
        			}
        		}
        	}
        }

        //Finish
        applyStyle(info,textbox);
        if (sContentWidth!=null) {
        	converter.pushContentWidth(sContentWidth);
        }
        getTextCv().traverseBlockText(onode,textbox);
        if (sContentWidth!=null) {
        	converter.popContentWidth();
        }
        getPresentationSc().exitOutline();
    }
	
    private void handleDrawGroup(Element onode, Element hnodeBlock, Element hnodeInline, int nMode) {
        // TODO: style-name and z-index should be transferred to children
        Node child = onode.getFirstChild();
        while (child!=null) {
            if (OfficeReader.isDrawElement(child)) {
                handleDrawElement((Element) child, hnodeBlock, hnodeInline, nMode);
            }
            child = child.getNextSibling();
        }
    }
	
    //////////////////////////////////////////////////////////////////////////
    // Forms

    private void handleDrawControl(Element onode, Element hnodeBlock, Element hnodeInline, int nMode) {
        // Get the control, if possible
        if (form==null) { return; }
        ControlReader control = ofr.isOpenDocument() ?
            ofr.getForms().getControl(Misc.getAttribute(onode,XMLString.DRAW_CONTROL)) :
            ofr.getForms().getControl(Misc.getAttribute(onode,XMLString.FORM_ID));
        if (control==null || control.getOwnerForm()!=form) { return; }

        // Create the control element
        Element hcontrol = null;
        String sType = control.getControlType();
		
        if (XMLString.FORM_TEXT.equals(sType)) {
            hcontrol = createInputText(control,false);
        }
        else if (XMLString.FORM_PASSWORD.equals(sType)) {
            hcontrol = createInputText(control,true);
        }
        else if (XMLString.FORM_FILE.equals(sType)) {
            hcontrol = createInputFile(control);
        }
        else if (XMLString.FORM_IMAGE.equals(sType)) {
            hcontrol = createInput(control,"image");
        }
        else if (XMLString.FORM_HIDDEN.equals(sType)) {
            hcontrol = createInput(control,"hidden");
        }
        else if (XMLString.FORM_CHECKBOX.equals(sType)) {
            hcontrol = createInputCheck(control,false);
        }
        else if (XMLString.FORM_RADIO.equals(sType)) {
            hcontrol = createInputCheck(control,true);
        }
        else if (XMLString.FORM_BUTTON.equals(sType)) {
            hcontrol = createInputButton(control);
        }
        else if (XMLString.FORM_FIXED_TEXT.equals(sType)) {
            hcontrol = createLabel(control);
        }
        else if (XMLString.FORM_TEXTAREA.equals(sType)) {
            hcontrol = createTextarea(control);
        }
        else if (XMLString.FORM_LISTBOX.equals(sType)) {
            hcontrol = createSelect(control);
        }
        // ignore other controls
		
        if (hcontrol!=null) {
            Element frame = onode; // controls are *not* contained in a draw:frame!
            StyleInfo info = new StyleInfo();
            getFrameSc().applyStyle(frame.getAttribute(XMLString.DRAW_STYLE_NAME),info);
            applySize(frame,info.props,false);
            applyPlacement(frame,hnodeBlock,hnodeInline,nMode,hcontrol,info);
            applyStyle(info,hcontrol);
        }        

    }
	
    private Element createInput(ControlReader control, String sType) {
        // Create the element
        Element input = converter.createElement("input");
        input.setAttribute("type",sType);
        return input;
    }
	
    private Element createInputFile(ControlReader control) {
        Element input = converter.createElement("input");
        input.setAttribute("type","file");
        setCommonAttributes(control,input);
        setDisabled(control,input);
        setReadonly(control,input);
        setValue(control,input);
        return input;
    }
	
    private Element createInputText(ControlReader control, boolean bPassword) {
        Element input = converter.createElement("input");
        input.setAttribute("type",bPassword ? "password" : "text");
        setCommonAttributes(control,input);
        setName(control,input,true);
        setValue(control,input);
        setMaxLength(control,input);
        setDisabled(control,input);
        setReadonly(control,input);
        return input;
    }
	
    private Element createInputCheck(ControlReader control, boolean bRadio) {
        Element input = converter.createElement("input");
        input.setAttribute("type",bRadio ? "radio" : "checkbox");
        setCommonAttributes(control,input);
        setName(control,input,true);
        setValue(control,input);
        setChecked(control,input);
        setDisabled(control,input);
        setReadonly(control,input);
        // Add a label for the check/radio
        Element label = converter.createElement("label");
        setFor(control,label);
        label.appendChild(input);
        label.appendChild(converter.createTextNode(control.getTypeAttribute(XMLString.FORM_LABEL)));
        return label;
    }
	
    private Element createInputButton(ControlReader control) {
        Element input = converter.createElement("input");
        String sButtonType = control.getTypeAttribute(XMLString.FORM_BUTTON_TYPE);
        if ("submit".equals(sButtonType)) {
            input.setAttribute("type","submit");
        }
        else if ("reset".equals(sButtonType)) {
            input.setAttribute("type","reset");
        }
        else { // TODO: url button (using javascript)
            input.setAttribute("type","button");
        }
        setCommonAttributes(control,input);
        setName(control,input,true);
        input.setAttribute("value",control.getTypeAttribute(XMLString.FORM_LABEL));
        setDisabled(control,input);
        return input;
    }
	
    private Element createLabel(ControlReader control) {
        Element label = converter.createElement("label");
        setCommonAttributes(control,label);
        setFor(control,label);
        label.setAttribute("value",control.getTypeAttribute(XMLString.FORM_LABEL));
        label.appendChild(converter.createTextNode(control.getTypeAttribute(XMLString.FORM_LABEL)));
        return label;
    }

    private Element createTextarea(ControlReader control) {
        Element textarea = converter.createElement("textarea");
        setCommonAttributes(control,textarea);
        setName(control,textarea,true);
        setDisabled(control,textarea);
        setReadonly(control,textarea);
		// rows & cols are required - but css will override them!
        textarea.setAttribute("rows","10");
        textarea.setAttribute("cols","5");
        // The value attribute should be used as content
        String s = control.getTypeAttribute(XMLString.FORM_VALUE);
        if (s!=null) {
            textarea.appendChild(converter.createTextNode(s));
        }
        return textarea;
    }

    private Element createSelect(ControlReader control) {
        Element select = converter.createElement("select");
        setCommonAttributes(control,select);
        setName(control,select,false);
        setSize(control,select);
        setMultiple(control,select);
        setDisabled(control,select);
        // Add options
        int nCount = control.getItemCount();
        for (int i=0; i<nCount; i++) {
            String sLabel = control.getItemAttribute(i,XMLString.FORM_LABEL);
            boolean bSelected = "true".equals(control.getItemAttribute(i,XMLString.FORM_SELECTED));
            Element option = converter.createElement("option");
            select.appendChild(option);
            if (bSelected) { option.setAttribute("selected","selected"); }
            option.appendChild(converter.createTextNode(sLabel));
        }
        return select;
    }
	
    // form helpers
	
    private void setCommonAttributes(ControlReader control, Element hnode) {
        setId(control,hnode);
        setTitle(control,hnode);
        setTabIndex(control,hnode);
    }
	
    private void setId(ControlReader control, Element hnode) {
        String s = control.getId();
        if (s!=null) { hnode.setAttribute("id",s); }
    }
	
    private void setName(ControlReader control, Element hnode, boolean bRequired) {
        String s = control.getAttribute(XMLString.FORM_NAME);
        if (s!=null) { hnode.setAttribute("name",s); }
        else if (bRequired) { hnode.setAttribute("name","unknown"); }
    }
	
    private void setValue(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_VALUE); // TODO: Correct??
        if (s!=null) { hnode.setAttribute("value",s); }
    }
	
    private void setTitle(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_TITLE);
        if (s!=null) { hnode.setAttribute("title",s); }
    }

    private void setTabIndex(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_TAB_INDEX);
        if (s!=null) { hnode.setAttribute("tabindex",s); }
    }

    private void setMaxLength(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_MAX_LENGTH);
        if (s!=null) { hnode.setAttribute("maxlength",s); }
    }
	
    private void setSize(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_SIZE);
        if (s!=null) { hnode.setAttribute("size",s); }
    }
	
    private void setChecked(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_SELECTED);
        if ("true".equals(s)) { hnode.setAttribute("checked","checked"); }
    }

    private void setMultiple(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_MULTIPLE);
        if ("true".equals(s)) { hnode.setAttribute("multiple","multiple"); }
    }
	
    private void setDisabled(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_DISABLED);
        if ("true".equals(s)) { hnode.setAttribute("disabled","disabled"); }
    }

    private void setReadonly(ControlReader control, Element hnode) {
        String s = control.getTypeAttribute(XMLString.FORM_READONLY);
        if ("true".equals(s)) { hnode.setAttribute("readonly","readonly"); }
    }
	
    private void setFor(ControlReader control, Element hnode) {
        hnode.setAttribute("for",control.getId());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utility methods
	
    // Add link to a draw element
    private void addLink(Element onode, Element hnode) {
        Element oanchor = getDrawAnchor(onode);
        if (oanchor!=null) {
            // If xlink:href is empty, there's no point in creating the anchor:
            String sHref = oanchor.getAttribute(XMLString.XLINK_HREF);
            if (sHref!=null && sHref.length()>0) {
                Element hanchor = converter.createLink(oanchor);
                hnode.getParentNode().replaceChild(hanchor,hnode);
                hanchor.appendChild(hnode);
            }
        }
    }
    
	// The width attribute in CSS refers to the content width, excluding borders and padding
	// We thus have to subtract the borders and padding to get the correct width
    // This method handles this
    private String getFrameWidth(Element node, StyleWithProperties style) {
    	String sWidth = node.getAttribute(XMLString.SVG_WIDTH);
    	if (sWidth.length()>0) {
    		if (style!=null) {
    			// Subtract padding
    			String s = style.getProperty(XMLString.FO_PADDING_LEFT);
    			if (s!=null) sWidth = Calc.sub(sWidth, s);
    			s = style.getProperty(XMLString.FO_PADDING_RIGHT);
    			if (s!=null) sWidth = Calc.sub(sWidth, s);
    			s = style.getProperty(XMLString.FO_PADDING);
    			if (s!=null) sWidth = Calc.sub(sWidth, Calc.multiply("200%", s));
    			// Subtract border
    			s = style.getProperty(XMLString.FO_BORDER_LEFT);
    			if (s!=null) sWidth = Calc.sub(sWidth, borderWidth(s));
    			s = style.getProperty(XMLString.FO_BORDER_RIGHT);
    			if (s!=null) sWidth = Calc.sub(sWidth, borderWidth(s));
    			s = style.getProperty(XMLString.FO_BORDER);
    			if (s!=null) sWidth = Calc.sub(sWidth, Calc.multiply("200%", borderWidth(s)));
    		}
			return sWidth;
    	}
        return null;
    }
    
    // Same for height
    private String getFrameHeight(Element node, StyleWithProperties style) {
    	String sHeight = node.getAttribute(XMLString.SVG_HEIGHT);
    	if (sHeight.length()>0) {
        	if (style!=null) {
        		// Subtract padding
                String s = style.getProperty(XMLString.FO_PADDING_TOP);
                if (s!=null) sHeight = Calc.sub(sHeight, s);
                s = style.getProperty(XMLString.FO_PADDING_BOTTOM);
                if (s!=null) sHeight = Calc.sub(sHeight, s);
                s = style.getProperty(XMLString.FO_PADDING);
                if (s!=null) sHeight = Calc.sub(sHeight, Calc.multiply("200%", s));
                // Subtract border
                s = style.getProperty(XMLString.FO_BORDER_TOP);
                if (s!=null) sHeight = Calc.sub(sHeight, borderWidth(s));
                s = style.getProperty(XMLString.FO_BORDER_BOTTOM);
                if (s!=null) sHeight = Calc.sub(sHeight, borderWidth(s));
                s = style.getProperty(XMLString.FO_BORDER);
                if (s!=null) sHeight = Calc.sub(sHeight, Calc.multiply("200%", borderWidth(s)));
            }
        	return sHeight;
        }
    	return null;
    }
	
    // Return the (unscaled) content width, or null if it's unknown
    private String applySize(Element node, CSVList props, boolean bOnlyWidth) {
    	StyleWithProperties style = ofr.getFrameStyle(node.getAttribute(XMLString.DRAW_STYLE_NAME));

    	String sWidth = getFrameWidth(node, style);
    	if (sWidth!=null) {
    		props.addValue("width",scale(Calc.truncateLength(sWidth)));
    	}

    	if (!bOnlyWidth) {
    		String sHeight = getFrameHeight(node,style);
    		if (sHeight!=null) {
    			props.addValue("height",scale(Calc.truncateLength(sHeight)));
    		}
    	}
    	
    	return sWidth;
    }
    
    // TODO: For absolute placement, only absolute size makes sense
    // TODO: How to handle NONE in case of text boxes? (currently using browser default, usually 100% width)
    private String applyImageSize(Element node, CSVList props, int nMode, boolean bOnlyWidth) {
    	switch (nImageSize) {
    	case XhtmlConfig.ABSOLUTE:
    		return applySize(node, props, bOnlyWidth);
    	case XhtmlConfig.RELATIVE:
    		if (nMode==FULL_SCREEN) {
    			props.addValue("max-width","100%");
    			props.addValue("height","100%");
    		}
    		else {
    			String sWidth = getFrameWidth(node, ofr.getFrameStyle(node.getAttribute(XMLString.DRAW_STYLE_NAME)));			
    			if (sWidth!=null) {    
    				props.addValue("width", Calc.divide(Calc.multiply(sScale,Calc.truncateLength(sWidth)),converter.getContentWidth()));
    			}
    			return sWidth;
    		}
    	case XhtmlConfig.NONE:
    		// Nothing to do :-)
    		return getFrameWidth(node, ofr.getFrameStyle(node.getAttribute(XMLString.DRAW_STYLE_NAME)));
    	}
		return null;
    }
	
    private void applyPosition(Element node, CSVList props) {
    	// The left and top attributes in css refers to the entire box, including margins
    	// We thus have to subtract the margins to get correct placement
    	String sX = node.getAttribute(XMLString.SVG_X);
        if (sX.length()==0) sX="0";
        String sY = node.getAttribute(XMLString.SVG_Y);
        if (sY.length()==0) sY="0";
    	StyleWithProperties style = ofr.getFrameStyle(node.getAttribute(XMLString.DRAW_STYLE_NAME));
    	if (style!=null) {
    		String s = style.getProperty(XMLString.FO_MARGIN_TOP);
    		if (s!=null) sX=Calc.sub(sX,s);
    		s = style.getProperty(XMLString.FO_MARGIN_LEFT);
    		if (s!=null) sY=Calc.sub(sY,s);
    	}
    	
        props.addValue("position","absolute");
        if (sX!=null && sX.length()>0) { props.addValue("left",scale(Calc.truncateLength(sX))); }
        if (sY!=null && sY.length()>0) { props.addValue("top",scale(Calc.truncateLength(sY))); }
        
    }
	
    private void applyPlacement(Element onode, Element hnodeBlock,
        Element hnodeInline, int nMode, Element object, StyleInfo info) {
        switch (nMode) {
            case INLINE :
                hnodeInline.appendChild(object);
                break;
            case ABSOLUTE:
                applyPosition(onode,info.props);
                if (hnodeInline!=null) {
                    hnodeInline.appendChild(object);
                }
                else {
                    Element div = converter.createElement("div");
                    hnodeBlock.appendChild(div);
                    div.appendChild(object);
                }
                break;
            case CENTERED:
                Element centerdiv = converter.createElement("div");
                centerdiv.setAttribute("style","margin:2px 0px 2px 0px");
                hnodeBlock.appendChild(centerdiv);
                centerdiv.appendChild(object);
                break;
            case FULL_SCREEN:
            	if (hnodeBlock!=null) {
            		Element div = converter.createElement("div");
            		div.setAttribute("style", "text-align:center");
            		hnodeBlock.appendChild(div);
            		div.appendChild(object);
            	}
            	break;
            case FLOATING:
                boolean bWrap = false;
                String sAlign = "center";
                String sX = null;
                String sStyleName = Misc.getAttribute(onode, XMLString.DRAW_STYLE_NAME);
                StyleWithProperties style = ofr.getFrameStyle(sStyleName);
                if (style!=null) {
                    String sPos = style.getProperty(XMLString.STYLE_HORIZONTAL_POS);
                    String sWrap = style.getProperty(XMLString.STYLE_WRAP);
                    if (isLeft(sPos)) {
                        bWrap = mayWrapRight(sWrap);
                        sAlign = "left";
                    }
                    else if (isRight(sPos)) {
                        bWrap = mayWrapLeft(sWrap);
                        sAlign = "right";
                    }
                    else if (isFromLeft(sPos)) {
                        bWrap = mayWrapRight(sWrap);
                        sAlign = "left";
                        sX = onode.getAttribute(XMLString.SVG_X);
                    }
                }
				
                if (bWrap) {
                    info.props.addValue("float",sAlign);
                    if (sX!=null && sX.length()>0) { info.props.addValue("margin-left",sX); }
                    if (hnodeInline!=null) {
                        hnodeInline.appendChild(object);
                    }
                    else {
                        Element div = converter.createElement("div");
                        hnodeBlock.appendChild(div);
                        div.appendChild(object);
                    }
                }
                else {
                    Element div = converter.createElement("div");
                    hnodeBlock.appendChild(div);
                    div.appendChild(object);
                    CSVList props = new CSVList(";");
					props.addValue("text-align",sAlign);
                    if (sX!=null && sX.length()>0) { props.addValue("margin-left",sX); }
                    div.setAttribute("style",props.toString());
                }
        }
        
    }
	
    private boolean isLeft(String sHPos) {
        return "left".equals(sHPos) || "inside".equals(sHPos);
    }

    private boolean isCenter(String sHPos) {
        return "center".equals(sHPos);
    }

    private boolean isRight(String sHPos) {
        return "right".equals(sHPos) || "outside".equals(sHPos);
    }

    private boolean isFromLeft(String sHPos) {
        return "from-left".equals(sHPos) || "from-inside".equals(sHPos);
    }
	
    private boolean mayWrapLeft(String sWrap) {
        return "left".equals(sWrap) || "parallel".equals(sWrap) || "biggest".equals(sWrap) ||
               "dynamic".equals(sWrap) || "run-through".equals(sWrap);
    }
	
    private boolean mayWrapRight(String sWrap) {
        return "right".equals(sWrap) || "parallel".equals(sWrap) || "biggest".equals(sWrap) ||
               "dynamic".equals(sWrap) || "run-through".equals(sWrap);
    }
	    
    private String borderWidth(String sBorder) {
        if (sBorder==null || sBorder.equals("none")) { return "0"; }
        SimpleInputBuffer in = new SimpleInputBuffer(sBorder);
        while (in.peekChar()!='\0') {
            // Skip spaces
            while(in.peekChar()==' ') { in.getChar(); }
            // If it's a number it must be a unit -> get it
            if ('0'<=in.peekChar() && in.peekChar()<='9') {
                return in.getNumber()+in.getIdentifier();
            }
            // skip other characters
            while (in.peekChar()!=' ' && in.peekChar()!='\0') { in.getChar(); } 
        }
        return "0";
    }

	

}


