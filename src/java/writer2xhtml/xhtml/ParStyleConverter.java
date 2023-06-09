/************************************************************************
 *
 *  ParStyleConverter.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2010-05-13)
 *
 */

package writer2xhtml.xhtml;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyleFamily;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.CSVList;

    /*
    TODO: drop caps (contained in a child of the style:properties element)
    The CSS attributes should be applied to the :first-letter
    pseudo-element or to an additional inline element.
    */

/**
 * This class converts OpenDocument paragraph styles to CSS2 styles for
 * use in ordinary paragraphs.
 * This also includes conversion of paragraph properties in other styles
 * (heading styles, cell styles).
 */
public class ParStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>ParStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of XHTML to use
     */
    public ParStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.styleMap = config.getXParStyleMap();
        this.bConvertStyles = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES;
    }
	
    /** Get the family of paragraph styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getParStyles();
    }
	
    /** Create default tag name to represent a paragraph
     *  @param style to use
     *  @return the tag name. 
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "p";
    }
	
    /** Convert formatting properties for a specific Par style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
    	cssPageBreak(style,props,bInherit);
        getFrameSc().cssMargins(style,props,bInherit);
        getFrameSc().cssBorder(style,props,bInherit);
        getFrameSc().cssPadding(style,props,bInherit);
        getFrameSc().cssBackgroundCommon(style,props,bInherit);
        cssPar(style,props,bInherit);
        getTextSc().cssTextCommon(style,props,bInherit);
    }
	
    public String getTextBackground(String sStyleName) {
        CSVList props = new CSVList(";");
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style!=null) {
            getTextSc().cssTextBackground(style,props,true);
        }
        return props.toString();
    }
	
    // TODO: get rid of this
    public String getRealParStyleName(String sStyleName) {
        if (sStyleName==null) { return sStyleName; }
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style==null || !style.isAutomatic()) { return sStyleName; }
        return style.getParentName();
    }
    
    public void cssPageBreak(StyleWithProperties style, CSVList props, boolean bInherit) {
    	if ("page".equals(style.getProperty(XMLString.FO_BREAK_BEFORE, bInherit))) {
    		props.addValue("page-break-before", "always");
    	}
    	else if ("page".equals(style.getProperty(XMLString.FO_BREAK_AFTER, bInherit))) {
    		props.addValue("page-break-after", "always");
    	}
    }
	
    public void cssPar(StyleWithProperties style, CSVList props, boolean bInherit){
        String s;

        // translates paragraph style properties.
        // The following properties are not supported by CSS2:
        // style:justify-single-word and style:text-align-last

/* problem: 120% times normal makes no sense...
        s = style.getProperty(XMLString.FO_LINE_HEIGHT);
	    if (s!=null && s.equals("normal")) {
            props.addValue("line-height:normal;"; 
        }
        else { // length or percentage
            s = style.getAbsoluteProperty(XMLString.FO_LINE_HEIGHT);
            if (s!=null) { props.addValue("line-height",s); }
        }
		*/
        // TODO: style:line-height-at-least and stype:line-spacing
		
        // Background color fits with css (note: Paragraph property!)
        s = style.getParProperty(XMLString.FO_BACKGROUND_COLOR,bInherit);
        if (s!=null) { props.addValue("background-color",s); }

        // Indentation: Absolute values of this property fit with css...
        if (bInherit || style.getProperty(XMLString.FO_TEXT_INDENT,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_TEXT_INDENT);
	        if (s!=null) { 
                props.addValue("text-indent",scale(s));
            }
            else { // ... but css doesn't have this one
                s = style.getProperty(XMLString.STYLE_AUTO_TEXT_INDENT);
                if ("true".equals(s)) { props.addValue("text-indent","2em"); }
            }
        }
		
        // Alignment: This property fit with css, but two values have different names		
        s = style.getProperty(XMLString.FO_TEXT_ALIGN,bInherit);
        if (s!=null) { // rename two property values:
            if (s.equals("start")) { s="left"; }
            else if (s.equals("end")) { s="right"; }
            props.addValue("text-align",s);
        }
		
        // Wrap (only in table cells, only in spreadsheets):
        if (ofr.isSpreadsheet()) {
            s = style.getProperty(XMLString.FO_WRAP_OPTION,bInherit);
            if ("no-wrap".equals(s)) props.addValue("white-space","nowrap");
            else if ("wrap".equals(s)) props.addValue("white-space","normal");
        }
    }
		

}
