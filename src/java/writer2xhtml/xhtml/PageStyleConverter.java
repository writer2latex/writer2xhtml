/************************************************************************
 *
 *  PageStyleConverter.java
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
 *  Version 1.7 (2022-08-03)
 *
 */

package writer2xhtml.xhtml;

import java.util.Enumeration;

import writer2xhtml.office.MasterPage;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyleFamily;
import writer2xhtml.office.PageLayout;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.CSVList;
import writer2xhtml.util.Calc;

/**
 * This class converts OpenDocument page styles to CSS2 styles.
 * A page style in a presentation is represented through the master page,
 * which links to a page layout defining the geometry and optionally a drawing
 * page defining the drawing background.
 * 
 * In a presentation document we export the full page style.
 * In a text document we export the writing direction, background color and footnote rule for the first master page only
 */
public class PageStyleConverter extends StyleConverterHelper {
	
	private boolean bHasFootnoteRules = false;

	/** Create a new <code>PageStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public PageStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.bConvertStyles = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD;
    }
    
    /** Get the text width of the first master page (page width minus left and right margin)
     * 
     *  @return the text width
     */
    public String getTextWidth(MasterPage masterPage) {
        if (masterPage!=null) {
            PageLayout pageLayout = ofr.getPageLayout(masterPage.getPageLayoutName());
            if (pageLayout!=null) {
                String sWidth = pageLayout.getProperty(XMLString.FO_PAGE_WIDTH);
                if (sWidth!=null) {
                	String sMarginLeft = pageLayout.getProperty(XMLString.FO_MARGIN_LEFT);
                	if (sMarginLeft!=null) {
                		sWidth = Calc.sub(sWidth, sMarginLeft);
                	}
                	String sMarginRight = pageLayout.getProperty(XMLString.FO_MARGIN_RIGHT);
                	if (sMarginRight!=null) {
                		sWidth = Calc.sub(sWidth, sMarginRight);
                	}
                	return sWidth;
                }
            }
        }
        return "17cm"; // Default in Writer, hence usable as a fallback value
    }
    
    /** Apply footnote rule formatting (based on first master page)
     * 
     * @param info then StyleInfo to which style information should be attached
     */
    public void applyFootnoteRuleStyle(StyleInfo info) {
    	bHasFootnoteRules = true;
    	info.sClass="footnoterule";
    }
    
    /** Apply default writing direction (based on first master page)
     * 
     * @param info then StyleInfo to which style information should be attached
     */
    public void applyDefaultWritingDirection(StyleInfo info) {
        MasterPage masterPage = ofr.getFirstMasterPage();
        if (masterPage!=null) {
            PageLayout pageLayout = ofr.getPageLayout(masterPage.getPageLayoutName());
            if (pageLayout!=null) {
                applyDirection(pageLayout,info);
            }
        }    	
    }

    /** Apply a master page style - currently only for presentations
     * 
     * @param sStyleName The name of the master page
     * @param info the StyleInfo to which style information should be attached
     */
    public void applyStyle(String sStyleName, StyleInfo info) {
        MasterPage masterPage = ofr.getMasterPage(sStyleName);
        if (masterPage!=null) {
            String sDisplayName = masterPage.getDisplayName();
            if (ofr.isPresentation()) {
                // Always generates class name
                info.sClass="masterpage"+styleNames.getExportName(sDisplayName);
            }
        }
    }

    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        StringBuilder buf = new StringBuilder();

        // This will be master pages for presentations only
        Enumeration<String> names = styleNames.keys();
        while (names.hasMoreElements()) {
            String sDisplayName = names.nextElement();
            MasterPage style = (MasterPage)
                getStyles().getStyleByDisplayName(sDisplayName);
            StyleInfo info = new StyleInfo();
            // First apply page layout (size)
            PageLayout pageLayout = ofr.getPageLayout(style.getPageLayoutName());
            if (pageLayout!=null) {
                applyDirection(pageLayout,info);
                cssPageSize(pageLayout,info.props);
                getFrameSc().cssBackground(pageLayout,info.props,true);
            }
            // Next apply drawing-page style (draw background)
            StyleWithProperties drawingPage = ofr.getDrawingPageStyle(style.getProperty(XMLString.DRAW_STYLE_NAME));
            if (drawingPage!=null) {
                cssDrawBackground(drawingPage,info.props,true);
            }
            // Then export the results
            buf.append(sIndent)
               .append(".masterpage").append(styleNames.getExportName(sDisplayName))
               .append(" {").append(info.props.toString()).append("}")
               .append(config.prettyPrint() ? "\n" : " ");
        }
        
        if (ofr.isText()) {
        	// Export page formatting for first master page in text documents
        	MasterPage masterPage = ofr.getFirstMasterPage();
        	if (masterPage!=null) {
        		PageLayout pageLayout = ofr.getPageLayout(masterPage.getPageLayoutName());
        		if (pageLayout!=null) {
        			if (bConvertStyles) {
        				// Background color
        				StyleInfo pageInfo = new StyleInfo();
        				getFrameSc().cssBackground(pageLayout,pageInfo.props,true);
        				/*if (converter.isOPS()) { // Use zero margin for EPUB and default margins for XHTML
        					pageInfo.props.addValue("margin", "0");
        				}*/
        				if (pageInfo.hasAttributes()) {
        					buf.append(sIndent).append("body {").append(pageInfo.props.toString()).append("}")
        					.append(config.prettyPrint() ? "\n" : " ");
        				}
        				
        				// Footnote rule
        				if (bHasFootnoteRules) {
        					StyleInfo ruleInfo = new StyleInfo();
        					cssFootnoteRule(pageLayout,ruleInfo.props);
        					buf.append(sIndent).append("hr.footnoterule {").append(ruleInfo.props.toString()).append("}")
        					.append(config.prettyPrint() ? "\n" : " ");
        				}
        			}
        		}
        	}
        }
        return buf.toString();
    }
	
    /** Get the family of page styles (master pages)
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getMasterPages();
    }

    // Background properties in draw: Color, gradient, hatching or bitmap
    private void cssDrawBackground(StyleWithProperties style, CSVList props, boolean bInherit){
        // Fill color: Same as in css
        String s = style.getProperty(XMLString.DRAW_FILL_COLOR,bInherit);
        if (s!=null) { props.addValue("background-color",s); }
    }

	
    private void cssPageSize(PageLayout style, CSVList props) {
        String sWidth = style.getProperty(XMLString.FO_PAGE_WIDTH);
        if (sWidth!=null) { props.addValue("width",scale(sWidth)); }
        String sHeight = style.getProperty(XMLString.FO_PAGE_HEIGHT);
        if (sHeight!=null) { props.addValue("height",scale(sHeight)); }
    }
    
	// Footnote rule
    private void cssFootnoteRule(PageLayout style, CSVList props) {
    	String sBefore = style.getFootnoteProperty(XMLString.STYLE_DISTANCE_BEFORE_SEP);
    	if (sBefore!=null) { props.addValue("margin-top",scale(sBefore)); }
    	String sAfter = style.getFootnoteProperty(XMLString.STYLE_DISTANCE_AFTER_SEP);
    	if (sAfter!=null) { props.addValue("margin-bottom", scale(sAfter)); }
    	String sHeight = style.getFootnoteProperty(XMLString.STYLE_WIDTH);
    	if (sHeight!=null) { props.addValue("height", scale(sHeight)); }
    	String sWidth = style.getFootnoteProperty(XMLString.STYLE_REL_WIDTH);
    	if (sWidth!=null) { props.addValue("width", sWidth); }
    	
    	String sColor = style.getFootnoteProperty(XMLString.STYLE_COLOR);
    	if (sColor!=null) { // To get the expected result in all browsers we must set both
    		props.addValue("color", sColor);
    		props.addValue("background-color", sColor);
    	}

    	String sAdjustment = style.getFootnoteProperty(XMLString.STYLE_ADJUSTMENT);
    	if ("right".equals(sAdjustment)) {
    		props.addValue("margin-left", "auto");
    		props.addValue("margin-right", "0");
    	}
    	else if ("center".equals(sAdjustment)) {
    		props.addValue("margin-left", "auto");
    		props.addValue("margin-right", "auto");
    	}
    	else { // default left
    		props.addValue("margin-left", "0");
    		props.addValue("margin-right", "auto");
    	}
    }


	
	
}
