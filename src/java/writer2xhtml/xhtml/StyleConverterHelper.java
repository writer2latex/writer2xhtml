/************************************************************************
 *
 *  StyleConverterHelper.java
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
 *  Version 1.7 (2022-06-14)
 *
 */

package writer2xhtml.xhtml;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyleFamily;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.ExportNameCollection;

/**
 * <p>This is an abstract base class to convert an OpenDocument style family to
 * CSS2 styles.</p>
 */
public abstract class StyleConverterHelper extends ConverterHelper {

    // Translation of OpenDocument style names to CSS class names
    protected ExportNameCollection styleNames = new ExportNameCollection(true);
	
    // Style map to use
    protected XhtmlStyleMap styleMap;

    // Should we convert styles resp. hard formatting?
    protected boolean bConvertStyles = true;
    protected boolean bConvertHard = true;
	
    // The type of xhtml document
    protected int nType;
	
    /** Create a new <code>StyleConverterHelper</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public StyleConverterHelper(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter);
        this.nType = nType;
    }

    /** Apply the writing direction (ltr or rtl) attribute from a style
     *  @param style the OpenDocument style to use
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    protected static void applyDirection(StyleWithProperties style, StyleInfo info) {
        String sDir = style.getProperty(XMLString.STYLE_WRITING_MODE);
        if ("lr-tb".equals(sDir)) { info.sDir="ltr"; }
        else if ("rl-tb".equals(sDir)) { info.sDir="rtl"; }
    }

    /** Apply language+country from a style
     *  @param style the OpenDocument style to use
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    protected static void applyLang(StyleWithProperties style, StyleInfo info) {
        String sLang = style.getProperty(XMLString.FO_LANGUAGE);
        String sCountry = style.getProperty(XMLString.FO_COUNTRY);
        if (sLang!=null) {
            if (sCountry==null || sCountry.equals("none")) { info.sLang = sLang; }
            else { info.sLang = sLang+"-"+sCountry; }
        }
    }

    /** Get the OpenDocument style family associated with this
     *  StyleConverterHelper
     *  @return the style family
     */
    public abstract OfficeStyleFamily getStyles();

    /** <p>Convert style information for used styles</p>
     *  @param sIndent a String of spaces to add before each line
     */
    public abstract String getStyleDeclarations(String sIndent);	

}
