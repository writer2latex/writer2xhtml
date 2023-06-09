/************************************************************************
 *
 *  StyleConverter.java
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

package writer2xhtml.xhtml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.office.*;
import writer2xhtml.util.*;

/** This class converts OpenDocument styles to CSS2 styles.
 * Note that some elements in OpenDocument has attributes that also maps to CSS2 properties.
 * Example: the width of a text box.
 * Also note, that some OpenDocument style properties cannot be mapped to CSS2 without creating an additional inline element.
 * The class uses one helper class per OpenDocument style family (paragraph, frame etc.)
 */
class StyleConverter extends ConverterHelper {

    // Helpers for text styles
    private TextStyleConverter textSc;
    private ParStyleConverter parSc;
    private HeadingStyleConverter headingSc;
    private ListStyleConverter listSc;
    private SectionStyleConverter sectionSc;

    // Helpers for table styles
    private TableStyleConverter tableSc;
    private RowStyleConverter rowSc;
    private CellStyleConverter cellSc;

    // Helpers for drawing styles
    private FrameStyleConverter frameSc;
    private PresentationStyleConverter presentationSc;
	
    // Helper for page styles
    private PageStyleConverter pageSc;
    
    /** Create a new <code>StyleConverter</code>
     * 
     * @param ofr the office reader used to access the source document
     * @param config the configuration to use
     * @param converter the main converter
     * @param nType the XHTML type
     */
    StyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter);
        // Create the helpers
        textSc = new TextStyleConverter(ofr,config,converter,nType);
        parSc = new ParStyleConverter(ofr,config,converter,nType);
        headingSc = new HeadingStyleConverter(ofr,config,converter,nType);
        listSc = new ListStyleConverter(ofr,config,converter,nType);
        sectionSc = new SectionStyleConverter(ofr,config,converter,nType);
        tableSc = new TableStyleConverter(ofr,config,converter,nType);
        rowSc = new RowStyleConverter(ofr,config,converter,nType);
        cellSc = new CellStyleConverter(ofr,config,converter,nType);
        frameSc = new FrameStyleConverter(ofr,config,converter,nType);
        presentationSc = new PresentationStyleConverter(ofr,config,converter,nType);
        pageSc = new PageStyleConverter(ofr,config,converter,nType);
    }
	
    // Accessor methods for helpers: We need to override the style helper accessors
    
    TextStyleConverter getTextSc() { return textSc; }

    ParStyleConverter getParSc() { return parSc; }

    HeadingStyleConverter getHeadingSc() { return headingSc; }

    ListStyleConverter getListSc() { return listSc; }

    SectionStyleConverter getSectionSc() { return sectionSc; }

    TableStyleConverter getTableSc() { return tableSc; }

    RowStyleConverter getRowSc() { return rowSc; }

    CellStyleConverter getCellSc() { return cellSc; }

    FrameStyleConverter getFrameSc() { return frameSc; }

    PresentationStyleConverter getPresentationSc() { return presentationSc; }

    PageStyleConverter getPageSc() { return pageSc; }
	
    /** Apply the default language of the source document on an XHTML element
     * 
     * @param node the XHTML element
     */
    void applyDefaultLanguage(Element node) {
        StyleWithProperties style = getDefaultStyle();
        if (style!=null) {
            StyleInfo info = new StyleInfo();
            StyleConverterHelper.applyLang(style,info);
            applyStyle(info,node);
        }
    }
    
    /** Export style information as a string of plain CSS code
     * 
     * @param bIndent true if the CSS code should be indented
     * @return the CSS code
     */
    String exportStyles(boolean bIndent) {
    	String sIndent = bIndent ? "      " : "";
        StringBuilder buf = new StringBuilder();
        
        exportDefaultStyle(buf,sIndent);
		
        // Export declarations from helpers
        // For OpenDocument documents created with OOo only some will generate content:
        //   Text documents: text, par, list, frame
        //   Spreadsheet documents: cell
        //   Presentation documents: frame, presentation, page
        buf.append(getTextSc().getStyleDeclarations(sIndent));
        buf.append(getParSc().getStyleDeclarations(sIndent));
        buf.append(getHeadingSc().getStyleDeclarations(sIndent));
        buf.append(getListSc().getStyleDeclarations(sIndent));
        buf.append(getSectionSc().getStyleDeclarations(sIndent));
        buf.append(getCellSc().getStyleDeclarations(sIndent));
        buf.append(getTableSc().getStyleDeclarations(sIndent));
        buf.append(getRowSc().getStyleDeclarations(sIndent));
        buf.append(getFrameSc().getStyleDeclarations(sIndent));
        buf.append(getPresentationSc().getStyleDeclarations(sIndent));
        buf.append(getPageSc().getStyleDeclarations(sIndent));
        return buf.toString();
    }
    
    /** Export style information as an XHTML style element
     * 
     * @param htmlDOM the XHTML DOM to which the generated element belongs
     * @return the style element
     */
    Node exportStyles(Document htmlDOM) {
        String sStyles = exportStyles(config.prettyPrint());
		
        // Create node
        if (sStyles.length()>0) {
            Element htmlStyle = htmlDOM.createElement("style");
            htmlStyle.setAttribute("media","all");
            if (!converter.isHTML5()) { htmlStyle.setAttribute("type","text/css"); }
            htmlStyle.appendChild(htmlDOM.createTextNode(config.prettyPrint() ? "\n" : " "));
            htmlStyle.appendChild(htmlDOM.createTextNode(sStyles));
            if (config.prettyPrint()) { htmlStyle.appendChild(htmlDOM.createTextNode("    ")); }
            return htmlStyle;
        }
        else {
            return null;
        }
    }
    
    // Private helper methods
    
    private void exportDefaultStyle(StringBuilder buf, String sIndent) {
        // Export default style
        if (config.xhtmlCustomStylesheet().length()==0 &&
            (config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL ||
            config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD)) {
            CSVList props = new CSVList(";");
        
            // Default paragraph/cell/frame style is applied to the body element
            StyleWithProperties defaultStyle = getDefaultStyle();
            if (defaultStyle!=null) {
                // text properties only!
                getTextSc().cssTextCommon(defaultStyle,props,true);
                if (config.useDefaultFont() && config.defaultFontName().length()>0) {
                	props.addValue("font-family", "'"+config.defaultFontName()+"'");
                }
            }
            
            // For text documents (XHTML only), also set maximum width
            if (ofr.isText() && !converter.isOPS()) {
            	String sMaxWidth = config.getMaxWidth().trim();
            	if (sMaxWidth.length()>0) {
		            props.addValue("max-width", sMaxWidth);
		            props.addValue("margin-left","auto");
		            props.addValue("margin-right","auto");
            	}
            }
            
            // Apply properties to body
            if (!props.isEmpty()) {
	            buf.append(sIndent)
	               .append("body {").append(props.toString()).append("}").append(config.prettyPrint() ? "\n" : " ");
            }
        }
    }
	
    private StyleWithProperties getDefaultStyle() {
        if (ofr.isSpreadsheet()) return ofr.getDefaultCellStyle();
        else if (ofr.isPresentation()) return ofr.getDefaultFrameStyle();
        else return ofr.getDefaultParStyle();
    }

}