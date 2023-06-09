/************************************************************************
 *
 *  TableStyleConverter.java
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
 *  Version 1.7 (2022-06-16)
 *
 */

package writer2xhtml.xhtml;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyleFamily;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.CSVList;

/**
 * This class converts OpenDocument table styles to CSS2 styles.
 * Table formatting includes <em>background</em>, <em>alignment</em>,
 * <em>margins</em>, and also <em>width</em>, which is considered elsewhere.
 */
public class TableStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>TableStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public TableStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        // Style maps for tables are currently not supported.
        this.styleMap = new XhtmlStyleMap();
        this.bConvertStyles = config.xhtmlTableFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlTableFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlTableFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlTableFormatting()==XhtmlConfig.IGNORE_STYLES;
    }

    /** Get the family of table styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getTableStyles();
    }
	
    /** Create default tag name to represent a table object
     *  @param style to use
     *  @return the tag name
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "table";
    }
	
    /** Convert formatting properties for a specific table style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
    	// Page break
    	getParSc().cssPageBreak(style, props, bInherit);
        // Apply background
        getFrameSc().cssBackground(style,props,bInherit);
        // Table-specific properties
        cssTable(style,props,bInherit);
    }
	
    private void cssTable(StyleWithProperties style, CSVList props, boolean bInherit){
        // Top and bottom margins
        String sMarginTop = style.getAbsoluteProperty(XMLString.FO_MARGIN_TOP);
        if (sMarginTop!=null) { props.addValue("margin-top",scale(sMarginTop)); }
        else { props.addValue("margin-top","0"); }

        String sMarginBottom = style.getAbsoluteProperty(XMLString.FO_MARGIN_BOTTOM);
	    if (sMarginBottom!=null) { props.addValue("margin-bottom",scale(sMarginBottom)); }
        else { props.addValue("margin-bottom","0"); }

        // Left and right margins and horizontal alignment
        String sAlign = style.getProperty(XMLString.TABLE_ALIGN);
        String sMarginLeft = style.getAbsoluteProperty(XMLString.FO_MARGIN_LEFT);
        if (sMarginLeft!=null) { sMarginLeft = scale(sMarginLeft); }
        String sMarginRight = style.getAbsoluteProperty(XMLString.FO_MARGIN_RIGHT);
        if (sMarginRight!=null) { sMarginRight = scale(sMarginRight); }

        if ("center".equals(sAlign)) {
		    sMarginLeft = "auto"; sMarginRight = "auto";
        }
        else if ("right".equals(sAlign)) {
		    sMarginLeft = "auto";
        }
        else if ("left".equals(sAlign)) {
		    sMarginRight = "auto";
        }

        if (sMarginLeft!=null) { props.addValue("margin-left",sMarginLeft); }		
        if (sMarginRight!=null) { props.addValue("margin-right",sMarginRight); }
        
        // Border model
        String sBorderModel = style.getProperty(XMLString.TABLE_BORDER_MODEL);
        if ("separating".equals(sBorderModel)) {
            props.addValue("border-collapse", "separate");
            props.addValue("border-spacing", "0"); // Not supported by ODF
        }
        else { // collapsing is the default (at least in LO)
            props.addValue("border-collapse", "collapse");        	
        }

    }
	
}
