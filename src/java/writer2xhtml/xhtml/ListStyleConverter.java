/************************************************************************
 *
 *  ListStyleConverter.java
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
 *  Copyright: 2002-2023 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2023-06-08)
 *
 */

package writer2xhtml.xhtml;

import java.util.Enumeration;
import java.util.HashMap;

import org.w3c.dom.Element;

import writer2xhtml.base.BinaryGraphicsDocument;
import writer2xhtml.office.FontDeclaration;
import writer2xhtml.office.ListStyle;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyleFamily;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.CSVList;
import writer2xhtml.util.Calc;
import writer2xhtml.util.Misc;

/**
 * This class converts OpenDocument list styles to CSS styles.
 * Note that unlike other style converters automatic styles are also exported as CSS classes.
 */
public class ListStyleConverter extends StyleConverterHelper {
	
	private int nFormatting;
	private boolean bHasListHeaders=false;
    private HashMap<String,Integer> listDepth = new HashMap<>();
	
    /** Create a new <code>ListStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public ListStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.styleMap = config.getXListStyleMap();
        this.nFormatting = config.listFormatting();
    }

    public void applyStyle(int nLevel, String sStyleName, StyleInfo info, String sCounterReset) {
        updateLevel(nLevel,sStyleName);
    	ListStyle style = ofr.getListStyle(sStyleName);
        if (style!=null) {
        	if (nFormatting!=XhtmlConfig.IGNORE_ALL) {
        		// In addition to the style, we may have an explicit counter-reset
	        	if ("none".equals(sCounterReset)) { // Continue list numbering from previous list
	               	info.props.addValue("counter-reset","none");
	        	} else if (sCounterReset!=null) { // Restart list numbering TODO: Compare to the list style; may be redundant?
	               	info.props.addValue("counter-reset",getClassName(style,nLevel)+" "+Integer.toString(Misc.getPosInteger(sCounterReset, 1)-1));        		        		
	        	}
        	}
            if (nFormatting!=XhtmlConfig.IGNORE_ALL || !style.isAutomatic()) {
            	// Apply class name if requested, but always for soft styles
                String sDisplayName = style.getDisplayName();
                if (styleMap.contains(sDisplayName)) {
                	XhtmlStyleMapItem map = styleMap.get(sDisplayName);
                	if (map.sElement.length()>0) {
                		info.sTagName = map.sElement;
                	}
                    if (!"(none)".equals(map.sCss)) {
                        info.sClass = map.sCss;
                    }
                }
                else {
                	info.sClass = getClassName(style,nLevel);
                }
            }
        }
    }
    
    public void applyUnnumberedItemStyle(int nLevel, String sStyleName, StyleInfo info) {
    	ListStyle style = ofr.getListStyle(sStyleName);
        if (style!=null && nFormatting!=XhtmlConfig.IGNORE_ALL) {
        	info.sClass = "no_label";
        	bHasListHeaders = true;
        }
    }
    
    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
    	StringBuilder buf = new StringBuilder();
        Enumeration<String> names = styleNames.keys();
        while (names.hasMoreElements()) {
            String sDisplayName = names.nextElement();
            ListStyle style = (ListStyle) getStyles().getStyleByDisplayName(sDisplayName);
            if (style!=null) {
	        	int nDepth = getListDepth(style.getName());
	            for (int nLevel=1; nLevel<=nDepth; nLevel++) {
	        	    String sSelector = "."+getClassName(style,nLevel);
	        	    if (nFormatting==XhtmlConfig.CONVERT_ALL) {
	        	    	// The list style itself gets the margins and initialization of the counter
	        		    CSVList props = new CSVList(";");
	        	        cssListMargins(style,nLevel,props);
	        	        cssCounterReset(style,nLevel,props);
	        		    props.addValue("clear:left");
	        		    addStyleDeclaration(sSelector,props,sIndent,buf);

	        		    // The marker is empty
        	            props = new CSVList(";");
        	   			props.addValue("content", "''");
	        		    addStyleDeclaration(sSelector+" > li::marker",props,sIndent,buf);

	        	    	// The first paragraph gets a new text-indent
        		        props = new CSVList(";");
	        	        cssListTextIndent(style,nLevel,props);
	        		    addStyleDeclaration(sSelector+" > li > p:first-of-type",props,sIndent,buf);

	        	    	// The first paragraph also gets the label
        		        // TODO: What if the first paragraph is a heading?
	        	        props = new CSVList(";");
	        			cssCounterIncrement(style,nLevel,props);
	        			cssCounterContent(style,nLevel,props);
	        	    	cssLabelTextStyle(style,nLevel,props);    	
	        	        cssLabelSize(style,nLevel,props);
	        		    addStyleDeclaration(sSelector+" > li > p:first-of-type::before",props,sIndent,buf);
	           	    }
	        	    else if (nFormatting==XhtmlConfig.CONVERT_LABELS) {
	           	    	// The list style itself gets the initialization of the counter
	           		    CSVList props = new CSVList(";");
	            		cssCounterReset(style, nLevel, props);
	        		    addStyleDeclaration(sSelector,props,sIndent,buf);

	           		    // The list item gets the incrementation of the counter
	           		    props = new CSVList(";");
	           			cssCounterIncrement(style,nLevel,props);
	        		    addStyleDeclaration(sSelector+" > li",props,sIndent,buf);
	           		    	           		    
	           		    // The list item also gets the label content
	           		    props = new CSVList(";");
	           			cssCounterContent(style,nLevel,props);
	           	    	cssLabelTextStyle(style,nLevel,props);    	
	        		    addStyleDeclaration(sSelector+" > li::marker",props,sIndent,buf);
	           	    }
	            }
	        }
        }
        if (bHasListHeaders) {
        	// Override counter-increment and content; location depends on config
        	if (nFormatting==XhtmlConfig.CONVERT_ALL) {
            	CSVList props = new CSVList(";");
        		props.addValue("content", "''");
        		props.addValue("counter-increment","none");
        		addStyleDeclaration("li.no_label p:first-of-type::before",props,sIndent,buf);	
        	}
        	else if (nFormatting==XhtmlConfig.CONVERT_LABELS) {
            	CSVList props = new CSVList(";");
        		props.addValue("counter-increment","none");
        		addStyleDeclaration("li.no_label",props,sIndent,buf);
        		props = new CSVList(";");
        		props.addValue("content", "''");
        		addStyleDeclaration("li.no_label::marker",props,sIndent,buf);	
        	}
        }
        return buf.toString();
    }
    
    // TODO: Would this fit better in StyleConverterHelper?
    private void addStyleDeclaration(String sSelector, CSVList props, String sIndent, StringBuilder buf) {
    	if (!props.isEmpty()) { // TODO: Is this a good idea? Perhaps only for some rules?
    		buf.append(sIndent).append(sSelector).append(" {").append(props.toString()).append("}").append(config.prettyPrint() ? "\n" : " ");
    	}
    }
	
    /** Get the family of list styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getListStyles();
    }
    
    // ================= Keep track of maximum depth ===================
    // This is used to avoid CSS code for unused levels

    private void updateLevel(int nLevel, String sStyleName) {
    	if (sStyleName!=null) {
    		if (listDepth.containsKey(sStyleName)) {
    			if (listDepth.get(sStyleName)<nLevel) {
    				listDepth.put(sStyleName, listDepth.get(sStyleName)+1);
    			}
    		} else {
    			listDepth.put(sStyleName, nLevel);
    		}
    	}
    }
    
    private int getListDepth(String sStyleName) {
    	return listDepth.containsKey(sStyleName) ? listDepth.get(sStyleName) : 0;
    }
        
    // ================= List formatting ===================
    
    // Create CSS properties for margins of list 
    private void cssListMargins(ListStyle style, int nLevel, CSVList props) {
	    props.addValue("margin-top","0");
	    props.addValue("margin-bottom","0");
	    String sMarginLeft = getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_MARGIN_LEFT));
		if (nLevel>1) { // The ODF value is from page margin; we need it to be relative to the previous level
			sMarginLeft = Calc.sub(sMarginLeft, getLength(style.getLevelStyleProperty(nLevel-1, XMLString.FO_MARGIN_LEFT)));
		}
		props.addValue("margin-left","0");
		props.addValue("padding-left",sMarginLeft);
    }
    
    // Create CSS property for text-indent
    private void cssListTextIndent(ListStyle style, int nLevel, CSVList props) {
    	props.addValue("text-indent", getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_TEXT_INDENT)));
    }

    // ================= Label formatting ===================
    
    // Create CSS properties for the text formatting of the label (text style + explicit font)
    private void cssLabelTextStyle(ListStyle style, int nLevel, CSVList props) {
    	// Create character formatting attributes for label
    	String sCharStyle = style.getLevelProperty(nLevel, XMLString.TEXT_STYLE_NAME);
    	if (sCharStyle!=null) {
    		StyleWithProperties charStyle = (StyleWithProperties) ofr.getTextStyles().getStyle(sCharStyle);
    		if (charStyle!=null) {
    			converter.getStyleCv().getTextSc().applyProperties(charStyle, props, true);
    		}
    	}
    	// The list style may override the font
        // TODO: Fix this! Code is currently duplicated from TextStyleConverter (because code there needs a StyleWithProperties)
        if (!config.useDefaultFont()) {
            String s=null,s2=null,s3=null;
            CSVList val = new CSVList(",");
            s = style.getLevelStyleTextProperty(nLevel,XMLString.STYLE_FONT_NAME);
            if (s!=null) {
                FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(s);
                if (fd!=null) {
                    s = fd.getFontFamily();
                    s2 = fd.getFontFamilyGeneric();
                    s3 = fd.getFontPitch();
                }
            }
            else {            
                s = style.getLevelStyleTextProperty(nLevel,XMLString.FO_FONT_FAMILY);
                s2 = style.getLevelStyleTextProperty(nLevel,XMLString.STYLE_FONT_FAMILY_GENERIC);
                s3 = style.getLevelStyleTextProperty(nLevel,XMLString.STYLE_FONT_PITCH);
            }
   		
            if (s!=null) { val.addValue(s); }
            // Add generic font family
            if ("fixed".equals(s3)) { val.addValue("monospace"); }
            else if ("roman".equals(s2)) { val.addValue("serif"); }
            else if ("swiss".equals(s2)) { val.addValue("sans-serif"); }
            else if ("modern".equals(s2)) { val.addValue("monospace"); }
            else if ("decorative".equals(s2)) { val.addValue("fantasy"); }
            else if ("script".equals(s2)) { val.addValue("cursive"); }
            else if ("system".equals(s2)) { val.addValue("serif"); } // System default font
            if (!val.isEmpty()) { props.addValue("font-family",val.toString()); }
        }
    }
    
    // Create CSS properties to define the size of the label for a specific level
    private void cssLabelSize(ListStyle style, int nLevel, CSVList props) {
    	if ("listtab".equals(style.getLevelStyleProperty(nLevel, XMLString.TEXT_LABEL_FOLLOWED_BY))) {
    		String sMarginLeft = Calc.add(getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_MARGIN_LEFT)),
    				getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_TEXT_INDENT)));
    		String sWidth = Calc.sub(getLength(style.getLevelStyleProperty(nLevel, XMLString.TEXT_LIST_TAB_STOP_POSITION)),sMarginLeft);
    		// This formula is arbitrary (should be width of the actual content)... 
    		String sWidthEstimate = (3+3*Misc.getPosInteger(getString(style.getLevelProperty(nLevel, XMLString.TEXT_DISPLAY_LEVELS)), 1))+"mm";
    		// ...but disregarding that, this seems to be the algorithm folllowed by LO
    		if (Calc.isLessThan(sWidth, sWidthEstimate)) { // Too narrow space, use left margin as alternative tab stop
        		sWidth = Calc.sub(getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_MARGIN_LEFT)),sMarginLeft);
        		if (Calc.isLessThan(sWidth, sWidthEstimate)) { // Still too narrow, use implicit tab stops every 0.5in
        			sWidth = Calc.sub("0.5in", sMarginLeft);
        			while (Calc.isLessThan(sWidth, sWidthEstimate)) { // Continue until sufficient space
        				sWidth = Calc.add(sWidth, "0.5in");
        			}
        		}
    		}
			props.addValue("display","inline-block");
			props.addValue("width",sWidth);
			props.addValue("text-indent","0");
    	}
    }
     
    // ================= Counters and bullets ===================
    
    // Create CSS property to reset (and implicitly define) counter for a specific level (if the level is numbered)
    private void cssCounterReset(ListStyle style, int nLevel, CSVList props) {
    	if (XMLString.TEXT_LIST_LEVEL_STYLE_NUMBER.equals(style.getLevelType(nLevel))) {
			int nStartValue = Misc.getPosInteger(style.getLevelProperty(nLevel,XMLString.TEXT_START_VALUE),1);
			props.addValue("counter-reset",getClassName(style,nLevel)+" "+(nStartValue-1));
    	}
    }
    
    // Create CSS property to increment counter for a specific level (if the level is numbered)
    private void cssCounterIncrement(ListStyle style, int nLevel, CSVList props) {
    	if (XMLString.TEXT_LIST_LEVEL_STYLE_NUMBER.equals(style.getLevelType(nLevel))) {
			String sCounterName = getClassName(style,nLevel);
			props.addValue("counter-increment",sCounterName);
    	}
    }
    
    // Create CSS property to display counter/bullet for a specific level
    private void cssCounterContent(ListStyle style, int nLevel, CSVList props) {
    	String sLevelType = style.getLevelType(nLevel);
    	String sFormat = style.getLevelStyleProperty(nLevel, XMLString.TEXT_LABEL_FOLLOWED_BY);
    	if (XMLString.TEXT_LIST_LEVEL_STYLE_NUMBER.equals(sLevelType)) {
        	// Get the prefix and suffix (the latter possibly followed by a space)
        	String sPrefix = getString(style.getLevelProperty(nLevel, XMLString.STYLE_NUM_PREFIX));
        	if (sPrefix.length()>0) { sPrefix = "'"+sPrefix+"' "; }
        	String sSuffix = getString(style.getLevelProperty(nLevel, XMLString.STYLE_NUM_SUFFIX));
        	if ("space".equals(sFormat)||"listtab".equals(sFormat)) { sSuffix+=" "; }
        	if (sSuffix.length()>0) { sSuffix = " '"+sSuffix+"'"; } 
        	// Create label
        	int nLevels = Misc.getPosInteger(style.getLevelProperty(nLevel, XMLString.TEXT_DISPLAY_LEVELS),1);
        	StringBuilder label = new StringBuilder();
        	label.append(sPrefix);
        	for (int i=nLevels; i>0; i--) {
        		label.append(getCounterFormat(style,nLevel-i+1));
        		if (i>1) { label.append(" '.' "); }
        	}
        	label.append(sSuffix);
        	// Assign the content property
        	props.addValue("content",label.toString());    	
    	}
    	else if (XMLString.TEXT_LIST_LEVEL_STYLE_BULLET.equals(sLevelType)) {
    		// The label is a single character possibly followed by a space character
    		String sChar = style.getLevelProperty(nLevel, XMLString.TEXT_BULLET_CHAR);
	    	String sSpace = ("space".equals(sFormat)||"listtab".equals(sFormat))?" ":"";
    		props.addValue("content", "'"+sChar+sSpace+"'");
    	}
    	else if (XMLString.TEXT_LIST_LEVEL_STYLE_IMAGE.equals(sLevelType)) {
    		// The label is an image, which is linked or embedded
    		String sURL = null;
    		Element image = style.getImage(nLevel);
    		if (image!=null) {
    			BinaryGraphicsDocument bgd = converter.getImageCv().getImage(image);
    			if (bgd!=null) {
    				sURL = bgd.getFileName();
    				if (config.embedImg() && !bgd.isLinked()) {
    					StringBuilder sb = new StringBuilder();
    	        		sb.append("data:").append(bgd.getMIMEType()).append(";base64,").append(bgd.getBase64());
    	        		sURL = sb.toString();
    				}
    				else if (!bgd.isRecycled() && !bgd.isLinked()) {
    	        		converter.addDocument(bgd);
    	        	}
    			}
    		}
    		if (sURL!=null) { props.addValue("content", "url("+sURL+") ' '"); }
    	}
    }
    
    // Translate the number format for a numbered style to a CSS counter format
    private String getCounterFormat(ListStyle style, int nLevel) {
    	String sNumFormat = style.getLevelProperty(nLevel,XMLString.STYLE_NUM_FORMAT);
    	String sCSSNumFormat = "decimal";
    	if ("i".equals(sNumFormat)) { sCSSNumFormat = "lower-roman"; }
    	else if ("I".equals(sNumFormat)) { sCSSNumFormat = "upper-roman"; }
    	else if ("a".equals(sNumFormat)) { sCSSNumFormat = "lower-alpha"; }
    	else if ("A".equals(sNumFormat)) { sCSSNumFormat = "upper-alpha"; }
		return "counter("+getClassName(style,nLevel)+","+sCSSNumFormat+")";
    }
	
    // ================= Helpers ===================

    // Create class name for a specific level
    private String getClassName(ListStyle style, int nLevel) {
    	if (style!=null) {
    		return styleNames.getExportName(style.getDisplayName())+"_"+nLevel;
    	}
    	return "list-item";
    }
    
    private String getString(String s) {
    	return s!=null ? s : "";
    }
    
    private String getLength(String s) {
    	return s!=null? s : "0cm";
    }
    
}
