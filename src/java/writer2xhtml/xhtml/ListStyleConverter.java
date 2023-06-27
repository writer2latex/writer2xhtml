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
 *  Version 1.7.1 (2023-06-25)
 *
 */

package writer2xhtml.xhtml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import writer2xhtml.util.StringComparator;

/**
 * This class converts OpenDocument list styles to CSS styles.
 * Note that unlike other style converters automatic styles are also exported as CSS classes.
 */
public class ListStyleConverter extends StyleConverterHelper {
	
	// Value of the configuration option list_formatting (ignore_all, convert_labels, convert_all)
	private int nListFormatting;
	// Flag to indicate that we need a special li.no_label-class
	private boolean bHasListHeaders=false;
	// Maps and sets to keep track of the CSS variants of the class style we need
    private Map<String,Integer> listDepth = new HashMap<>(); // Maximum depth for a given list
    private Set<String> listStyles = new HashSet<>(); // List styles (used if formatting=convert_labels)
    private Map<String,Set<String>> listParStyles = new HashMap<>(); // List styles->Paragraph styles (if formatting=convert_all)
	
    /** Create a new <code>ListStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of xhtml to use
     */
    public ListStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.styleMap = config.getXListStyleMap();
        this.nListFormatting = config.listFormatting();
        this.bConvertHard = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES;
    }

    public void applyStyle(int nLevel, String sStyleName, String sCounterReset, String sParStyleName, StyleInfo info) {
        updateLevel(nLevel,sStyleName);
    	ListStyle style = ofr.getListStyle(sStyleName);
        if (style!=null) {
        	if (nListFormatting!=XhtmlConfig.IGNORE_ALL) {
        		applyExplicitCounterReset(style, nLevel, sCounterReset,info);
        	}
            if (styleMap.contains(style.getDisplayName())) {
            	applyStyleMap(style,info);
            }
            else if (nListFormatting==XhtmlConfig.CONVERT_ALL) {
            	StyleWithProperties parStyle = ofr.getParStyle(sParStyleName);
            	if (parStyle!=null && parStyle.isAutomatic() &&
            			(!bConvertHard ||
            					(parStyle.getParProperty(XMLString.FO_MARGIN_LEFT, false)==null &&
            					parStyle.getParProperty(XMLString.FO_TEXT_INDENT, false)==null &&
            					parStyle.getTabStops(false).isEmpty())
            			)) { // Don't use automatic styles unless we have to
            		parStyle = (StyleWithProperties) parStyle.getParentStyle();
            	}
            	info.sClass = getExtendedClassName(style,nLevel,parStyle);
            	listParStyles.computeIfAbsent(style.getDisplayName(), key -> new HashSet<>()).add(parStyle.getDisplayName());
            }
            else if (nListFormatting==XhtmlConfig.CONVERT_LABELS || !style.isAutomatic()) { // Always apply class names for soft styles
				info.sClass = getClassName(style,nLevel);
				listStyles.add(sStyleName);
            }
        }
    }
    
    private void applyExplicitCounterReset(ListStyle style, int nLevel, String sCounterReset, StyleInfo info) {
    	if ("none".equals(sCounterReset)) { // Continue list numbering from previous list
           	info.props.addValue("counter-reset","none");
    	} else if (sCounterReset!=null) { // Restart list numbering TODO: Compare to the list style; may be redundant?
           	info.props.addValue("counter-reset",getClassName(style,nLevel)+" "+Integer.toString(Misc.getPosInteger(sCounterReset, 1)-1));        		        		
    	}    	
    }
    
    private void applyStyleMap(ListStyle style, StyleInfo info) {
    	XhtmlStyleMapItem map = styleMap.get(style.getDisplayName());
    	if (map.sElement.length()>0) {
    		info.sTagName = map.sElement;
    	}
        if (!"(none)".equals(map.sCss)) {
            info.sClass = map.sCss;
        }    	
    }
    
    public void applyUnnumberedItemStyle(String sStyleName, StyleInfo info) {
    	ListStyle style = ofr.getListStyle(sStyleName);
        if (style!=null && nListFormatting!=XhtmlConfig.IGNORE_ALL) {
        	info.sClass = "no_label";
        	bHasListHeaders = true;
        }
    }
    
    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
    	// TODO: What if the first paragraph is a heading?
		if (nListFormatting==XhtmlConfig.CONVERT_ALL) {
			return getFullStyleDeclarations(sIndent);
		}
		else if (nListFormatting==XhtmlConfig.CONVERT_LABELS) {
			return getLabelStyleDeclarations(sIndent);
		}
		else {
			return "";
		}
    }
    
    // Used if formatting=convert_all
    private String getFullStyleDeclarations(String sIndent) {
    	StringBuilder buf = new StringBuilder();
    	for (String sDisplayName : listParStyles.keySet()) {
            ListStyle listStyle = (ListStyle) getStyles().getStyleByDisplayName(sDisplayName);
    		for (String sParDisplayName : listParStyles.get(sDisplayName)) {
                StyleWithProperties parStyle = (StyleWithProperties) getParSc().getStyles().getStyleByDisplayName(sParDisplayName);
                if (listStyle!=null && parStyle!=null) {
    	        	int nDepth = getListDepth(listStyle.getName());
    	            for (int nLevel=1; nLevel<=nDepth; nLevel++) {
    	    		    String sSelector = "."+getExtendedClassName(listStyle,nLevel,parStyle);
						// .liststyle_level_parstyle (we format lists as numbered paragraphs, hence no indentation on list)
						CSVList props = new CSVList(";");
						props.addValue("margin","0");
						props.addValue("padding","0");
						props.addValue("clear:left");
						cssCounterReset(listStyle,nLevel,props);
						addStyleDeclaration(sSelector,props,sIndent,buf);
						
						// .liststyle_level_parstyle > li::marker (for the same reason, the marker is empty)
						props = new CSVList(";");
						props.addValue("content", "''");
						addStyleDeclaration(sSelector+" > li::marker",props,sIndent,buf);
						
						// TODO: Potential optimization; if we override text-indent and it happens to be zero, we can do with
						// only one .list_n_par > li > p rule instead of three
						
	        		    // .liststyle_level > li > p (the list style may override the margin-left)
						if (cssOverride(listStyle,parStyle,XMLString.FO_MARGIN_LEFT)) {
		        		    props = new CSVList(";");
		        	        cssListMarginLeft(listStyle,nLevel,props);
		        		    addStyleDeclaration(sSelector+" > li > p",props,sIndent,buf);
						}

	        	    	// .liststyle_level > li > p:not(:first-of-type) (text-indent of additional paragraphs in item is always 0)
        		        props = new CSVList(";");
	        	        props.addValue("text-indent", "0");
	        		    addStyleDeclaration(sSelector+" > li > p:not(:first-of-type)",props,sIndent,buf);
	        		    
	        	    	// .liststyle_level > li > p:first-of-type (the list style may override the text-indent)
						if (cssOverride(listStyle,parStyle,XMLString.FO_TEXT_INDENT)) {
	        		        props = new CSVList(";");
		        	        cssListTextIndent(listStyle,nLevel,props);
		        		    addStyleDeclaration(sSelector+" > li > p:first-of-type",props,sIndent,buf);
						}
	        		    
						// .liststyle_level_parstyle > li > p:first-of-type::before (this is where we put the label)
						props = new CSVList(";");
						cssCounterIncrement(listStyle,nLevel,props);
						cssCounterContent(listStyle,nLevel,props);
						cssLabelTextStyle(listStyle,nLevel,props);    	
						cssLabelSize(listStyle,nLevel,parStyle,props);
						addStyleDeclaration(sSelector+" > li > p:first-of-type::before",props,sIndent,buf);
    	            }
    		    }
    		}
    	}
        if (bHasListHeaders) {
        	// li.no_label p:first-of-type::before
        	CSVList props = new CSVList(";");
    		props.addValue("content", "''");
    		props.addValue("counter-increment","none");
    		addStyleDeclaration("li.no_label p:first-of-type::before",props,sIndent,buf);	
        }
        return buf.toString();    	
    }
    
    // Used if formatting=convert_labels
    private String getLabelStyleDeclarations(String sIndent) {
    	StringBuilder buf = new StringBuilder();
    	for (String sDisplayName : listStyles) {
            ListStyle style = (ListStyle) getStyles().getStyleByDisplayName(sDisplayName);
            if (style!=null) {
	        	int nDepth = getListDepth(style.getName());
	            for (int nLevel=1; nLevel<=nDepth; nLevel++) {
	        	    String sSelector = "."+getClassName(style,nLevel);
           	    	// .liststyle_level (reset counter)
           		    CSVList props = new CSVList(";");
            		cssCounterReset(style, nLevel, props);
        		    addStyleDeclaration(sSelector,props,sIndent,buf);

           	    	// .liststyle_level > li (incrementation of counter)
           		    props = new CSVList(";");
           			cssCounterIncrement(style,nLevel,props);
        		    addStyleDeclaration(sSelector+" > li",props,sIndent,buf);
           		    	           		    
           		    // .liststyle_level > li::marker (the label)
           		    props = new CSVList(";");
           			cssCounterContent(style,nLevel,props);
           	    	cssLabelTextStyle(style,nLevel,props);    	
        		    addStyleDeclaration(sSelector+" > li::marker",props,sIndent,buf);
	            }
	        }
    	}
	    if (bHasListHeaders) {
	    	// li.no_label
        	CSVList props = new CSVList(";");
    		props.addValue("counter-increment","none");
    		addStyleDeclaration("li.no_label",props,sIndent,buf);
    		
    		// li.no_label::marker
    		props = new CSVList(";");
    		props.addValue("content", "''");
    		addStyleDeclaration("li.no_label::marker",props,sIndent,buf);	
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
    
    // Check if the list style overrides a paragraph property: Track the paragraph style backwards in the hierarchy
    private boolean cssOverride(ListStyle listStyle, StyleWithProperties parStyle, String sXML) {
    	// The list style will override if it is not referred to at all
    	boolean bRefersStyle = false;
    	StyleWithProperties currentStyle = parStyle;
    	while (currentStyle!=null) {
    		bRefersStyle |= listStyle.getName().equals(currentStyle.getListStyleName());
    		currentStyle = (StyleWithProperties) currentStyle.getParentStyle();
    	}
    	if (!bRefersStyle) { return true; }
    	// The list style will also override if the property is applied above the list style
    	currentStyle = parStyle;
    	while (currentStyle!=null) {
    		if (currentStyle.getParProperty(sXML, false)!=null) {
    			// Found the property above or at the same level as the list style
    			return false;
    		}
    		bRefersStyle = listStyle.getName().equals(currentStyle.getListStyleName());
    		currentStyle = (StyleWithProperties) currentStyle.getParentStyle();
    		if (bRefersStyle && currentStyle!=null && !listStyle.getName().equals(currentStyle.getListStyleName())) {
    			// The previous level is before the list style is applied
    			return true;
    		}
    	}
    	// The property is not applied at all
    	return true;
    }
    
    // Create CSS property for left margin of list 
    private void cssListMarginLeft(ListStyle style, int nLevel, CSVList props) {
	    String sMarginLeft = getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_MARGIN_LEFT));
		props.addValue("margin-left",scale(sMarginLeft));
    }
    
    // Create CSS property for text-indent of list
    private void cssListTextIndent(ListStyle style, int nLevel, CSVList props) {
    	props.addValue("text-indent", scale(getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_TEXT_INDENT))));
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
    // If the parStyle is nonzero, left margin and text indent is taken from the here, otherwise from the list style
    private void cssLabelSize(ListStyle style, int nLevel, StyleWithProperties parStyle, CSVList props) {
    	if ("listtab".equals(style.getLevelStyleProperty(nLevel, XMLString.TEXT_LABEL_FOLLOWED_BY))) {
    		// First collect and sort tab stops
    		List<String> tabStops = new ArrayList<>();

    		// The list style provides an additional tab stop
    		String sListTabStop = getLength(style.getLevelStyleProperty(nLevel, XMLString.TEXT_LIST_TAB_STOP_POSITION));
    		tabStops.add(sListTabStop);
    		
    		// The left margin is used as an implicit tabstop
    		String sMarginLeft = cssOverride(style,parStyle,XMLString.FO_MARGIN_LEFT)?
    				getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_MARGIN_LEFT)):getLength(parStyle.getProperty(XMLString.FO_MARGIN_LEFT));
    		tabStops.add(sMarginLeft);
    		for (String sTabStop : parStyle.getTabStops(true)) {
    			tabStops.add(Calc.add(sMarginLeft, sTabStop)); // Tabstops are relative to the left margin
    		}
    		
    		// Sort the tab stops
    		Comparator<String> comparator = new StringComparator<String>("en","US") {
    			public int compare(String a, String b) {
    				if (Calc.isZero(Calc.sub(a, b))) { return 0; }
    				else if (Calc.isLessThan(a, b)) { return -1; }
    				else { return 1; }
    			}
    		};
    		Collections.sort(tabStops, comparator);
    		
    		// Next calculate label width
    		
    		// This formula is arbitrary (should be width of the actual content)... 
    		int nDisplayLevels = Misc.getPosInteger(getString(style.getLevelProperty(nLevel, XMLString.TEXT_DISPLAY_LEVELS)), 1);
    		String sWidthEstimate = (1.5+2.5*nDisplayLevels)+"mm";

    		// Label width is calculated as the difference between a tab stop and the absolute value of the text-indent
    		String sTextIndent = cssOverride(style,parStyle,XMLString.FO_TEXT_INDENT)?
    				getLength(style.getLevelStyleProperty(nLevel, XMLString.FO_TEXT_INDENT)):getLength(parStyle.getProperty(XMLString.FO_TEXT_INDENT));
    		String sAbsoluteTextIndent = Calc.add(sMarginLeft,sTextIndent);
    		
    		// First try the collected tab stops
    		String sWidth = null;
    		for (String sTabStop : tabStops) {
    			sWidth = Calc.sub(sTabStop,sAbsoluteTextIndent);
    			if (Calc.isLessThan(sWidth, sWidthEstimate)) {
    				sWidth = null;
    			}
    			else {
    				break;
    			}
    		}

    		// If that fails, try implicit tab stops every 0.5in
    		if (sWidth==null) {
    			sWidth = Calc.multiply("-100%", sAbsoluteTextIndent);
    			while (Calc.isLessThan(sWidth, sWidthEstimate)) {
    				sWidth = Calc.add(sWidth, "0.5in");
    			}
    		}

    		props.addValue("display","inline-block");
			props.addValue("width",scale(sWidth));
			props.addValue("text-indent","0"); // No indentation inside box
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
        		String sCounter = getCounterFormat(style,nLevel-i+1);
        		label.append(sCounter);
        		if (i>1 && sCounter.length()>0) { label.append(" '.' "); }
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
    	if (sNumFormat.length()>0) {
	    	String sCSSNumFormat = "decimal";
	    	if ("i".equals(sNumFormat)) { sCSSNumFormat = "lower-roman"; }
	    	else if ("I".equals(sNumFormat)) { sCSSNumFormat = "upper-roman"; }
	    	else if ("a".equals(sNumFormat)) { sCSSNumFormat = "lower-alpha"; }
	    	else if ("A".equals(sNumFormat)) { sCSSNumFormat = "upper-alpha"; }
			return "counter("+getClassName(style,nLevel)+","+sCSSNumFormat+")";
    	}
    	else { // No numbering at this level
    		return "";
    	}
    }
	
    // ================= Helpers ===================

    // Create class name for a specific level
    private String getClassName(ListStyle style, int nLevel) {
    	if (style!=null) {
    		return styleNames.getExportName(style.getDisplayName())+"_"+nLevel;
    	}
    	return "list-item";
    }
    
    // Create paragraph class name
    private String getExtendedClassName(ListStyle listStyle, int nLevel, StyleWithProperties parStyle) {
    	if (parStyle!=null) {
    		return getClassName(listStyle,nLevel)+"_"+getParSc().styleNames.getExportName(parStyle.getDisplayName());	
    	}
    	return getClassName(listStyle,nLevel);
    }
    
    private String getString(String s) {
    	return s!=null ? s : "";
    }
    
    private String getLength(String s) {
    	return s!=null? s : "0cm";
    }
    
}
