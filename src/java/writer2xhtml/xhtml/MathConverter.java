/************************************************************************
 *
 *  MathConverter.java
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
 *  Version 1.7 (2022-06-10)
 *
 */

package writer2xhtml.xhtml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2xhtml.base.BinaryGraphicsDocument;
import writer2xhtml.office.*;
import writer2xhtml.util.Misc;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/** This class converts formulas: Either as LaTeX (TexMaths), MathML, as an image or as plain text (StarMath format)
 */
public class MathConverter extends ConverterHelper {
	
    private boolean bSupportMathML;
    private boolean bUseImage;
	
    /** Create a new <code>MathConverter</code>
     * 
     * @param ofr the OfficeReader to query about the document 
     * @param config the configuration determining the type of export
     * @param converter the converter instance
     * @param bSupportMathML true if the formula should be exported as MathML
     */
    public MathConverter(OfficeReader ofr, XhtmlConfig config, Converter converter,
        boolean bSupportMathML) {

        super(ofr,config,converter);
        this.bSupportMathML = bSupportMathML;
        this.bUseImage = config.formulas()==XhtmlConfig.IMAGE_STARMATH;
    }
	
    /** Convert a formula
     * 
     * @param image image version of the formula (or null if no image is available)
     * @param onode the math node
     * @param hnode the xhtml node to which content should be added
     */
    public void convert(Element image, Element onode, Node hnode, boolean bAllowDisplayStyle) {
        if (bSupportMathML) {
            convertAsMathML(onode,hnode,bAllowDisplayStyle);
        }
        else {
        	convertAsImageOrText(image,onode,hnode);
        }
    }
    
    public boolean convertTexMathsEquation(Element onode, Element hnodeBlock, Element hnodeInline, int nMode) {
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

    	String sLaTeX = null;
    	Element equation = converter.getTexMathsEquation(onode);
    	if (equation!=null) {
    		sLaTeX = Misc.getPCDATA(equation);
    	}
    	else { // Try OOoLaTeX
    		// The LaTeX code is embedded in a custom style attribute:
    		StyleWithProperties style = ofr.getFrameStyle(Misc.getAttribute(onode, XMLString.DRAW_STYLE_NAME));
    		if (style!=null) {
    			sLaTeX = style.getProperty("OOoLatexArgs");    		
    		}
    	}
    	if (sLaTeX!=null) {
    		// Format is <point size>X<mode>X<TeX code>X<format>X<resolution>X<transparency>
    		// where X is a paragraph sign
    		String sMathJax;
    		if (config.useMathJax() && bSupportMathML) {
    			switch (converter.getTexMathsStyle(sLaTeX)) {
    			case inline:
    				sMathJax = "\\("+converter.getTexMathsEquation(sLaTeX)+"\\)";
    				break;
    			case display:
    				// TODO: For non-text paragraphs display would be OK
    				//sMathJax = "\\["+converter.getTexMathsEquation(sLaTeX)+"\\]";
    				sMathJax = "\\("+converter.getTexMathsEquation(sLaTeX)+"\\)";
    				break;
    			case latex:
    			default: // Arbitrary LaTeX; this is the tricky bit	
    				sMathJax = "\\("+converter.getTexMathsEquation(sLaTeX)+"\\)";
    			}
    		}
    		else {
    			sMathJax = " "+converter.getTexMathsEquation(sLaTeX)+" ";
    		}
    		hnode.appendChild(converter.createTextNode(sMathJax));
    		converter.setContainsMath();
    		return true;
    	}
    	return false;
    }

    
    // For plain xhtml: Convert the formula as an image or as plain text
    private void convertAsImageOrText(Element image, Node onode, Node hnode) {
    	NodeList annotationList = ((Element) onode).getElementsByTagName(XMLString.ANNOTATION); // Since OOo 3.2
    	if (annotationList.getLength()==0) {
    		annotationList = ((Element) onode).getElementsByTagName(XMLString.MATH_ANNOTATION);
    	}
    	if (annotationList.getLength()>0 && annotationList.item(0).hasChildNodes()) {
    		// First create the StarMath annotation
    		String sAnnotation = "";
    		Node child = annotationList.item(0).getFirstChild();
    		while (child!=null) {
    			sAnnotation+=child.getNodeValue();
    			child = child.getNextSibling();
    		}
    		
    		// Next insert the image if required and available
    		if (bUseImage) {
    			// Get the image from the ImageLoader
    			String sHref = Misc.getAttribute(onode,XMLString.XLINK_HREF);
    			if (sHref==null || sHref.length()==0 || ofr.isInPackage(sHref)) {
    				BinaryGraphicsDocument bgd = converter.getImageCv().getImage(image);
    				if (bgd!=null) {
    					String sMIME = bgd.getMIMEType();
    					if (MIMETypes.PNG.equals(sMIME) || MIMETypes.JPEG.equals(sMIME) || MIMETypes.GIF.equals(sMIME)) {
    						converter.addDocument(bgd);
    	    				// Create the image and add the StarMath formula as alternative text
    	    				Element img = converter.createElement("img");
    	    				if (bgd.isLinked() || !config.embedImg()) {
    	    					img.setAttribute("src",bgd.getFileName());
    	    				}
    	    				else {
    	                		img.setAttribute("src",
    	                				new StringBuilder().append("data:").append(bgd.getMIMEType()).append(";base64,").append(bgd.getBase64()).toString());    	    					
    	    				}
    	    				img.setAttribute("class", "formula");
    	    				img.setAttribute("alt",sAnnotation);

    	    				hnode.appendChild(img);
    	    				
    	    				return;
    					}
    				}
    			}
    		}

    		// Otherwise insert the StarMath annotation as a kbd element
    		Element kbd = converter.createElement("kbd");
    		kbd.setAttribute("class", "formula");
    		hnode.appendChild(kbd);
    		kbd.appendChild(converter.createTextNode(sAnnotation));
    	}
    	else {
    		hnode.appendChild(converter.createTextNode("[Warning: formula ignored]"));
    	}
    }
    
    // For xhtml+mathml: Insert the mathml, removing the namespace (if any) and the annotation
    private void convertAsMathML(Element onode, Node hnode, boolean bAllowDisplay) {
    	Element math = converter.createElement("math");
    	if (onode.hasAttribute("xmlns:math")) {
    		math.setAttribute("xmlns", onode.getAttribute("xmlns:math"));
    	}
    	else if (onode.hasAttribute("xmlns") && (!converter.isHTML5() || converter.isOPS())) {
    		// Don't include xmlns attribute in HTML5, unless we are creating EPUB 3
    		math.setAttribute("xmlns", onode.getAttribute("xmlns"));
    	}
    	if (bAllowDisplay && onode.hasAttribute("display")) {
    		// Starting with version 4.2, LO exports display="block" for display equations
    		// This is a good thing, but in XHTML we can unfortunately only allow this for
    		// paragraphs with no other text content
    		math.setAttribute("display", onode.getAttribute("display"));
    	}
    	hnode.appendChild(math);
    	if (converter.isOPS() && converter.isHTML5()) {
    		// Grab the StarMath annotation for alttext attribute
    		Element semantics = Misc.getChildByTagName(onode, XMLString.SEMANTICS);
    		if (semantics!=null) {
	    		Element annotation = Misc.getChildByTagName(semantics, XMLString.ANNOTATION);
	    		if (annotation!=null) {
	    			math.setAttribute("alttext", Misc.getPCDATA(annotation));
	    		}
    		}
    	}
    	convertMathMLNodeList(onode.getChildNodes(), math);
		converter.setContainsMath();
    }
    
    private void convertElementAsMathML(Node onode, Node hnode) {
        if (onode.getNodeType()==Node.ELEMENT_NODE) {
            if (onode.getNodeName().equals(XMLString.SEMANTICS)) { // Since OOo 3.2
                // ignore this construction
                convertMathMLNodeList(onode.getChildNodes(),hnode);
            }
            else if (onode.getNodeName().equals(XMLString.MATH_SEMANTICS)) {
                // ignore this construction
                convertMathMLNodeList(onode.getChildNodes(),hnode);
            }
            else if (onode.getNodeName().equals(XMLString.ANNOTATION)) { // Since OOo 3.2
                // ignore the annotation (StarMath) completely
                // (mozilla renders it for some reason)
            }
            else if (onode.getNodeName().equals(XMLString.MATH_ANNOTATION)) {
                // ignore the annotation (StarMath) completely
                // (mozilla renders it for some reason)
            }
            else {
                String sElementName = stripNamespace(onode.getNodeName());
                Element newNode = converter.createElement(sElementName);
                hnode.appendChild(newNode);
                if (onode.hasAttributes()) {
                    NamedNodeMap attr = onode.getAttributes();
                    int nLen = attr.getLength();
                    for (int i=0; i<nLen; i++) {
                        String sName = stripNamespace(attr.item(i).getNodeName());
                        String sValue = attr.item(i).getNodeValue();
                        newNode.setAttribute(sName,replacePrivateChars(sValue));
                    }
                }            
                convertMathMLNodeList(onode.getChildNodes(),newNode);
            }
        }
        else if (onode.getNodeType()==Node.TEXT_NODE) {
            String s = replacePrivateChars(onode.getNodeValue());
            hnode.appendChild(hnode.getOwnerDocument().createTextNode(s));
        }
    }
	
    private void convertMathMLNodeList(NodeList list, Node hnode) {
        if (list==null) { return; }
        int nLen = list.getLength();
        for (int i=0; i<nLen; i++) {
            convertElementAsMathML(list.item(i),hnode);
        }
    }
	
    private String stripNamespace(String s) {
        int nPos = s.indexOf(':');
        if (nPos>-1) { return s.substring(nPos+1); }
        else { return s; }
    }
	
    // OOo exports some characters (from the OpenSymbol/StarSymbol font)
    // in the private use area of unicode. These should be replaced
    // with real unicode positions.
    private String replacePrivateChars(String s) {        
        int nLen = s.length();
        StringBuilder buf = new StringBuilder(nLen);
        for (int i=0; i<nLen; i++) {
            buf.append(replacePrivateChar(s.charAt(i)));
        }
        return buf.toString();
    }

    // This method maps {Open|Star}Symbol private use area to real unicode
    // positions. This is the same table as in w2l/latex/style/symbols.xml.
    // The list is contributed by Bruno Mascret
    private char replacePrivateChar(char c) {
        switch (c) {
            case '\uE002': return '\u2666';
            case '\uE003': return '\u25C6';
            case '\uE005': return '\u274D';
            case '\uE006': return '\u2794';
            case '\uE007': return '\u2713';
            case '\uE008': return '\u25CF';
            case '\uE009': return '\u274D';
            case '\uE00A': return '\u25FC';
            case '\uE00B': return '\u2752';
            case '\uE00D': return '\u2756';
            case '\uE013': return '\u2742';
            case '\uE01B': return '\u270D';
            case '\uE01E': return '\u2022';
            case '\uE021': return '\u00A9';
            case '\uE024': return '\u00AE';
            case '\uE025': return '\u21E8';
            case '\uE026': return '\u21E9';
            case '\uE027': return '\u21E6';
            case '\uE028': return '\u21E7';
            case '\uE02B': return '\u279E';
            case '\uE032': return '\u2741';
            case '\uE036': return '\u0028';
            case '\uE037': return '\u0029';
            case '\uE03A': return '\u20AC';
            case '\uE080': return '\u2030';
            case '\uE081': return '\uFE38'; // underbrace
            case '\uE082': return '\uFE37'; // overbrace
            case '\uE083': return '\u002B';
            case '\uE084': return '\u003C';
            case '\uE085': return '\u003E';
            case '\uE086': return '\u2264';
            case '\uE087': return '\u2265';
            case '\uE089': return '\u2208';
            case '\uE08B': return '\u2026';
            case '\uE08C': return '\u2192';
            case '\uE090': return '\u2225';
            case '\uE091': return '\u005E';
            case '\uE092': return '\u02C7';
            case '\uE093': return '\u02D8';
            case '\uE094': return '\u00B4';
            case '\uE095': return '\u0060';
            case '\uE096': return '\u02DC'; // or 007E
            case '\uE097': return '\u00AF';
            case '\uE098': return '\u2192'; // or 21E1
            case '\uE09B': return '\u20DB'; // triple dot, neither MathPlayer nor Mozilla understands this glyph
            case '\uE09E': return '\u0028';
            case '\uE09F': return '\u0029';
            case '\uE0A0': return '\u2221';
            case '\uE0AA': return '\u2751';
            case '\uE0AC': return '\u0393';
            case '\uE0AD': return '\u0394';
            case '\uE0AE': return '\u0398';
            case '\uE0AF': return '\u039B';
            case '\uE0B0': return '\u039E';
            case '\uE0B1': return '\u03A0';
            case '\uE0B2': return '\u03A3';
            case '\uE0B3': return '\u03A5';
            case '\uE0B4': return '\u03A6';
            case '\uE0B5': return '\u03A8';
            case '\uE0B6': return '\u03A9';
            case '\uE0B7': return '\u03B1';
            case '\uE0B8': return '\u03B2';
            case '\uE0B9': return '\u03B3';
            case '\uE0BA': return '\u03B4';
            case '\uE0BB': return '\u03F5';
            case '\uE0BC': return '\u03B6';
            case '\uE0BD': return '\u03B7';
            case '\uE0BE': return '\u03B8';
            case '\uE0BF': return '\u03B9';
            case '\uE0C0': return '\u03BA';
            case '\uE0C1': return '\u03BB';
            case '\uE0C2': return '\u03BC';
            case '\uE0C3': return '\u03BD';
            case '\uE0C4': return '\u03BE';
            case '\uE0C5': return '\u03BF';
            case '\uE0C6': return '\u03C0';
            case '\uE0C7': return '\u03C1';
            case '\uE0C8': return '\u03C3';
            case '\uE0C9': return '\u03C4';
            case '\uE0CA': return '\u03C5';
            case '\uE0CB': return '\u03D5';
            case '\uE0CC': return '\u03C7';
            case '\uE0CD': return '\u03C8';
            case '\uE0CE': return '\u03C9';
            case '\uE0CF': return '\u03B5';
            case '\uE0D0': return '\u03D1';
            case '\uE0D1': return '\u03D6';
            case '\uE0D3': return '\u03C2';
            case '\uE0D4': return '\u03C6';
            case '\uE0D5': return '\u2202';
            case '\uE0D9': return '\u22A4';
            case '\uE0DB': return '\u2190';
            case '\uE0DC': return '\u2191';
            case '\uE0DD': return '\u2193';
            default: 
                return c;
        }
    }

}