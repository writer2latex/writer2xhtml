/************************************************************************
 *
 *  TextStyleConverter.java
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
 *  Version 1.7.1 (2023-06-29)
 *
 */

package writer2xhtml.xhtml;

import java.util.HashMap;
import java.util.Map;

import writer2xhtml.office.FontDeclaration;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyleFamily;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.CSVList;
import writer2xhtml.util.Calc;
import writer2xhtml.util.ExportNameCollection;

/**
* This class converts OpenDocument text styles to CSS styles. Text styles defines the various settings for
* fonts, font effects, font decorations and colors.
* These properties are contained in the text-properties element.
* Text properties are contained in a number of other styles too, however:
* Paragraph styles, table cell styles and frame styles.
* Thus this class provides methods to convert text properties for other styles.
* Note that language and country information is handled by {@link StyleConverterHelper}, and hidden text is
* handled by the content converters. Currently CJK and CTL is not supported.
*/

public class TextStyleConverter extends StyleWithPropertiesConverterHelper {

    // OpenDocument does *not* define the style for links without style name,
    // but LO uses these styles, and so do we if they are available
    // (Caveat: LO does not export "Visited Internet Link" until a link is actually clicked)
    private static final String DEFAULT_LINK_STYLE = "Internet link"; // Not "Link"!
    private static final String DEFAULT_VISITED_LINK_STYLE = "Visited Internet Link";

    // Bookkeeping for anchors
    private ExportNameCollection anchorStyleNames = new ExportNameCollection(true);
    private ExportNameCollection anchorVisitedStyleNames = new ExportNameCollection(true);
    private Map<String, String> anchorCombinedStyleNames = new HashMap<>();
    private Map<String, String> orgAnchorStyleNames = new HashMap<>();
    private Map<String, String> orgAnchorVisitedStyleNames = new HashMap<>();
    
    // Export font sizes as percentages? (Only for EPUB)
    private boolean bRelativeFontSize = false;
    private String sFontScaling = "100%";
    private String sBaseFontSize = "12pt";
    
    // Use default font? (Mainly for EPUB)
    private boolean bConvertFont = false;

    /** Create a new <code>TextStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     *  @param nType the type of XHTML to use
     */
    public TextStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, int nType) {
        super(ofr,config,converter,nType);
        this.styleMap = config.getXTextStyleMap();
        this.bConvertStyles = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES;
        this.bRelativeFontSize = converter.isOPS() && config.relativeFontSize();
        this.sFontScaling = config.fontScaling();
        StyleWithProperties defaultStyle = ofr.getDefaultParStyle();
        if (defaultStyle!=null) {
        	String sFontSize = defaultStyle.getTextProperty(XMLString.FO_FONT_SIZE,false);
        	if (sFontSize!=null) {
        		sBaseFontSize = sFontSize;
        	}
        }
        this.bConvertFont = !config.useDefaultFont();
    }

    /** Apply a link style, using a combination of two text styles
     *  @param sStyleName name of the OpenDocument style
     *  @param sVisitedStyleName name of the OpenDocument style for visited links
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    public void applyAnchorStyle(String sStyleName, String sVisitedStyleName,
        StyleInfo info) {
        if (sStyleName==null || sVisitedStyleName==null) { return; }
        if (sStyleName.length()==0 || sVisitedStyleName.length()==0) { return; }
        // Look for a style map
        String sDisplayName = ofr.getTextStyles().getDisplayName(sStyleName);
        if (styleMap.contains(sDisplayName)) { // class name from config
        	XhtmlStyleMapItem map = styleMap.get(sDisplayName);
            if (!"(none)".equals(map.sCss)) {
                info.sClass = map.sCss;
            }
            return;
        }

        String sName = sStyleName+sVisitedStyleName;
        if (!anchorCombinedStyleNames.containsKey(sName)) {
            String sExportName;
            // This combination is not seen before, but the base style may be known
            // In that case, use the visited style name as well
            if (anchorStyleNames.containsName(sStyleName)) {
                sExportName = anchorStyleNames.getExportName(sStyleName)
                              +anchorVisitedStyleNames.getExportName(sVisitedStyleName);
            }
            else {
                sExportName = anchorStyleNames.getExportName(sStyleName);
            }
            anchorCombinedStyleNames.put(sName,sExportName);
            orgAnchorStyleNames.put(sExportName,sStyleName);
            orgAnchorVisitedStyleNames.put(sExportName,sVisitedStyleName);
        }
        info.sClass = anchorCombinedStyleNames.get(sName);
    }
	
    /** <p>Convert style information for used styles</p>
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        StringBuilder buf = new StringBuilder();
        buf.append(super.getStyleDeclarations(sIndent));
        if (bConvertStyles) {
            // Export anchor styles
            // Default is always the styles "Internet link" and "Visited Internet Link"(?) 
            StyleWithProperties defaultLinkStyle = (StyleWithProperties)
                getStyles().getStyleByDisplayName(DEFAULT_LINK_STYLE);
            if (defaultLinkStyle!=null) {
                CSVList props = new CSVList(";");
                cssText(defaultLinkStyle,props,true);
                cssHyperlink(defaultLinkStyle,props);
                buf.append(sIndent)
                   .append("a:link {").append(props.toString()).append("}\n");
            }
		
            defaultLinkStyle = (StyleWithProperties)
                getStyles().getStyleByDisplayName(DEFAULT_VISITED_LINK_STYLE);
            if (defaultLinkStyle!=null) {
                CSVList props = new CSVList(";");
                cssText(defaultLinkStyle,props,true);
                cssHyperlink(defaultLinkStyle,props);
                buf.append(sIndent)
                   .append("a:visited {").append(props.toString()).append("}\n");
            }

            // Remaining link styles...
            for (String sExportName : anchorCombinedStyleNames.values()) {
                String sStyleName = orgAnchorStyleNames.get(sExportName);
                String sVisitedStyleName = orgAnchorVisitedStyleNames.get(sExportName);

                StyleWithProperties style = ofr.getTextStyle(sStyleName);

                if (style!=null) {
                    CSVList props = new CSVList(";");
                    cssText(style,props,true);
                    cssHyperlink(style,props);
                    buf.append(sIndent).append("a.").append(sExportName)
                       .append(":link {").append(props.toString()).append("}\n");
                }
			
                style = ofr.getTextStyle(sVisitedStyleName);
                if (style!=null) {
                    CSVList props = new CSVList(";");
                    cssText(style,props,true);
                    cssHyperlink(style,props);
                    buf.append(sIndent).append("a.").append(sExportName)
                       .append(":visited {").append(props.toString()).append("}\n");
                }
            }
        }
        return buf.toString();

    }

    /** Get the family of text (character) styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getTextStyles();
    }
	
    /** Create default tag name to represent a text
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "span";
    }
	
    /** Convert formatting properties for a specific text style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssText(style,props,bInherit);
    }
	
    ////////////////////////////////////////////////////////////////////////////
    // OpenDocument text properties
    // Text properties can be applied to text, paragraph, cell, graphic and
    // presentation styles.
    // Language and country attributes are handled elsewhere
    // All attributes for CJK and CTL text are currently ignored:
    //   style:font-name-*, style:font-family-*, style:font-family-generic-*,
    //   style:font-style-name-*, style:font-pitch-*, style:font-charset-*,
    //   style:font-size-*, style:font-size-rel-*, style:script-type-* 
    //   style:text-combine-*
    //   style:text-emphasis

    /** Convert all text properties that can be applied to a block element
     * 
     * @param style the Office style from which to read the properties
     * @param props the property list to which CSS properties are added
     * @param bInherit true if properties should be inherited from parent style(s)
     */
    public void cssTextBlock(StyleWithProperties style, CSVList props, boolean bInherit) {
    	cssTextCommon(style, props, bInherit);
    }
    
    /** Convert all text properties that can be applied to an inline element
     * 
     * @param style the Office style from which to read the properties
     * @param props the property list to which CSS properties are added
     * @param bInherit true if properties should be inherited from parent style(s)
     */
    public void cssText(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssTextCommon(style,props,bInherit);
        cssTextBackground(style,props,bInherit);
    }
	
    private void cssTextCommon(StyleWithProperties style, CSVList props, boolean bInherit) {
    	// Font
        cssFontFamily(style,props,bInherit);
        cssFontStyle(style,props,bInherit);
        cssFontWeight(style,props,bInherit);
        cssFontSizeAndPosition(style,props,bInherit);
        // Font effects
        cssColor(style,props,bInherit);
        cssTextDecoration(style,props,bInherit);
        cssTextTransform(style,props,bInherit);
        cssFontVariant(style,props,bInherit);
        // Position
        cssTextRotation(style, props, bInherit, true);
        cssTextShadow(style,props,bInherit);
        cssLetterSpacing(style,props,bInherit);
        cssLetterKerning(style,props,bInherit);
        // Hyphenation
        cssHyphenate(style,props,bInherit);        
    }
    
    // ************ Convert individual style properties to CSS (ordered by tab pages in LO)
    
    // ---- PART 1. Font
        
    // Font family. The attribute style:font-name specifies to font for western text.
    // In addition, style:font-name-asian and style:font-name-complex specifies font for CJK and CTL text.
    // Currently we only support the former, which maps to CSS
    // style:font-charset (other encoding) is currently not supported
    private void cssFontFamily(StyleWithProperties style, CSVList props, boolean bInherit) {
	    if (bConvertFont && (bInherit || style.getTextProperty(XMLString.STYLE_FONT_NAME,false)!=null)) {
	        CSVList val = new CSVList(","); // multivalue property!
	        // Get font family information from font declaration
	        String sName = style.getTextProperty(XMLString.STYLE_FONT_NAME,true);
	        if (sName!=null) {
	            FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(sName);
	            if (fd!=null) {
	                String s1 = fd.getFontFamily();
	                String s2 = fd.getFontFamilyGeneric();
	                String s3 = fd.getFontPitch();
	    	        // Add the western font family (CJK and CTL is more complicated)
	    	        if (s1!=null) { val.addValue(s1); }
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
	    }
    }
    
    // Font style. The attribute fo:font-style can have the values normal, italic or oblique
    // This fits with CSS.
    // In addition the attributes style:font-style-asian and style:font-style-complex supports
    // the same formatting for CJK and CTL text. This is currently not supported
    private void cssFontStyle(StyleWithProperties style, CSVList props, boolean bInherit) {
	    String s = style.getTextProperty(XMLString.FO_FONT_STYLE,bInherit);
	    if (s!=null) { props.addValue("font-style",s); }
    }
    
    // Font weight. The attribute fo:font-weight can have the values normal, bold, 100, 200,..., 900
    // This fits with CSS.
    // In addition the attributes style:font-weight-asian and style:font-weight-complex supports
    // the same formatting for CJK and CTL text. This is currently not supported
    private void cssFontWeight(StyleWithProperties style, CSVList props, boolean bInherit) {
	    String s = style.getTextProperty(XMLString.FO_FONT_WEIGHT,bInherit);
	    if (s!=null) { props.addValue("font-weight",s); }
    }
    
    // Font size: Absolute values of this property fit with css2
    // this is handled together with sub- and superscripts (style:text-position)
    // First value: sub, super or percentage (raise/lower relative to font height)
    // Second value (optional): percentage (relative size);
    private void cssFontSizeAndPosition(StyleWithProperties style, CSVList props, boolean bInherit) {
        String s=null,s2=null,s3=null,s4=null;
        if (bInherit || style.getTextProperty(XMLString.FO_FONT_SIZE,false)!=null 
        			 || style.getTextProperty(XMLString.STYLE_FONT_SIZE_REL,false)!=null
        			 || style.getTextProperty(XMLString.STYLE_TEXT_POSITION,false)!=null) {
            s = style.getAbsoluteFontSize();
            s2 = style.getTextProperty(XMLString.STYLE_TEXT_POSITION,true); // TODO: Why do we use true instead of bInherit?
	        if (s2!=null) {
                s2 = s2.trim();
                int i = s2.indexOf(" ");
                if (i>0) { // two values
                    s3 = s2.substring(0,i);
                    s4 = s2.substring(i+1);
                } 		
                else { // one value
                    s3 = s2; s4="100%";
                }
                if (s!=null) {
                	if (bRelativeFontSize) {
                		String sFontSize = Calc.divide(Calc.multiply(sFontScaling, Calc.multiply(s4,s)), sBaseFontSize);
                		if (!"100%".equals(sFontSize)) props.addValue("font-size", sFontSize);
                	}
                	else {
                		props.addValue("font-size",Calc.multiply(s4,scale(s)));
                	}
                }
                else {
                	props.addValue("font-size",s4);
                }
                if (!"0%".equals(s3)) {
                	props.addValue("vertical-align",s3);
                }
            }
            else if (s!=null) {
            	if (bRelativeFontSize) {
            		String sFontSize = Calc.divide(Calc.multiply(sFontScaling, s),sBaseFontSize);
            		if (!"100%".equals(sFontSize)) props.addValue("font-size", sFontSize);
            	}
            	else {
            		props.addValue("font-size",scale(s));
            	}
            }
        }
    }
    
    // ---- PART 2. Font effects
    
    // Color. The attribute fo:color fits with CSS
    // The attribute style:use-window-font-color can have the values true or false
    // In the former case, fo:color is not used, but an automatic foreground color
    // This is currently not supported
    private void cssColor(StyleWithProperties style, CSVList props, boolean bInherit) {
    	// TODO: Transparency
	    String s = style.getTextProperty(XMLString.FO_COLOR,bInherit);
	    if (s!=null) { props.addValue("color",s); }
    }

    // Text decoration. This is supported by the attributes style:text-underline-*,
    // style:text-overline-* and style: text-line-through-*, where * can be style, color, mode,
    // type, width. In addition the attributes style:text-line-through-text and
    // style:text-line-through-text-style specifies text used to strike out text.
    // LO does not support style:text-line-through-color, though.
    // In this case three different properties all maps to a single CSS property
    // This implies that style and color cannot be set independently (without creating
    // an additional inline element, which we don't want to do)
    // Also in CSS text-decoration cannot be turned of on child elements
    // We cannot support style:text-*-width, style:text-line-through-text and
    // style:text-line-through-text-style in CSS.
    // Also only a limited number of ODF line styles are supported in CSS
    // Finally currently Safari is the only browser supporting text-decoration-skip
    private void cssTextDecoration(StyleWithProperties style, CSVList props, boolean bInherit) {
    	String sThrough = style.getTextProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE,bInherit);
    	String sUnder = style.getTextProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,bInherit);
    	String sOver = style.getTextProperty(XMLString.STYLE_TEXT_OVERLINE_STYLE,bInherit);
        if (active(sThrough) || active(sUnder) || active(sOver)) {
        	// At least one decoration is active (not none)
            CSVList val = new CSVList(" ");

            // Select color from one of the decorations 
            String sColor = null;
            if (active(sThrough)) {
            	sColor = style.getTextProperty(XMLString.STYLE_TEXT_LINE_THROUGH_COLOR, bInherit);
            }
            if (sColor==null && active(sUnder)) {
            	sColor = style.getTextProperty(XMLString.STYLE_TEXT_UNDERLINE_COLOR, bInherit);
            }
            if (sColor==null && active(sOver)) {
                sColor = style.getTextProperty(XMLString.STYLE_TEXT_OVERLINE_COLOR, bInherit);            		
            }
            if (sColor!=null && !"font-color".equals(sColor)) {
            	val.addValue(sColor);
            }
            
            // Select style from one of the decorations
            String sStyle = null;
            String sType = null;
            if (active(sThrough)) {
            	sStyle = sThrough;
            	sType = style.getTextProperty(XMLString.STYLE_TEXT_LINE_THROUGH_TYPE, bInherit);
            }
            else if (active(sUnder)) {
            	sStyle = sUnder;
            	sType = style.getTextProperty(XMLString.STYLE_TEXT_UNDERLINE_TYPE, bInherit);
            }
            else if (active(sOver)) {
            	sStyle = sOver;
                sType = style.getTextProperty(XMLString.STYLE_TEXT_OVERLINE_TYPE, bInherit);            		
            }
            if (sStyle!=null) {
            	switch (sStyle) {
            	case "wave":
            		val.addValue("wavy"); break;
            	case "dash":
            	case "long-dash":
            	case "dot-dash":
            		val.addValue("dashed"); break;
            	case "dot-dot-dash":
            	case "dotted":
            		val.addValue("dotted"); break;
            	case "solid":
            	default:
            		if ("double".equals(sType)) {
            			val.addValue("double");
            		}
            		else {
            			val.addValue("solid");
            		}
            	}
            }
            
            // Select the required decorations  
            if (active(sThrough)) { val.addValue("line-through"); }
            if (active(sUnder)) { val.addValue("underline"); }
            if (active(sOver)) { val.addValue("overline"); }
            if (!val.isEmpty()) { props.addValue("text-decoration",val.toString()); }

            // Select mode
            String sMode = null;
            if (active(sThrough)) {
            	sMode = style.getTextProperty(XMLString.STYLE_TEXT_LINE_THROUGH_MODE, bInherit);
            }
            if (sMode==null && active(sUnder)) {
            	sMode = style.getTextProperty(XMLString.STYLE_TEXT_UNDERLINE_MODE, bInherit);
            }
            if (sMode==null && active(sOver)) {
                	sMode = style.getTextProperty(XMLString.STYLE_TEXT_OVERLINE_MODE, bInherit);            		
            }
            if (sMode!=null) {
            	if (sMode.equals("skip-white-space")) {
            		props.addValue("text-decoration-skip", "spaces");
            	}
            	else if (sMode.equals("continuous")) {
            		props.addValue("text-decoration-skip", "none");            		
            	}
            }
        }
        else if (sThrough!=null || sUnder!=null || sOver!=null) {
        	// At least one decoration set to none
        	props.addValue("text-decoration", "none");
        }
    }
    
    // Test whether a given decoration is active (set and not none)
    private boolean active(String sDecorationStyle) {
    	return sDecorationStyle!=null && !"none".equals(sDecorationStyle);
    }

    // Text transform. The attribute fo:text-transform can have the values none, lowercase,
    // uppercase or capitalize. This fits with CSS. Note: In ODF, fo:font-variant and
    // fo:text-transform are mutually exclusive, this is not the case in CSS 
    private void cssTextTransform(StyleWithProperties style, CSVList props, boolean bInherit) {
        String s = style.getTextProperty(XMLString.FO_TEXT_TRANSFORM,bInherit);
        if (s!=null) { props.addValue("text-transform",s); }
    }

    // Font variant. The attribute fo:font-variant can have the values small-caps or normal
    // This fits with CSS
    private void cssFontVariant(StyleWithProperties style, CSVList props, boolean bInherit) {
        String s = style.getTextProperty(XMLString.FO_FONT_VARIANT,bInherit);
        if (s!=null) { props.addValue("font-variant",s); }    	
    }
    
    // Font relief. The attribute style:font-relief can have the values none, embossed, engraved
    // TODO: This can be simulated in CSS with a text shadow (if the font color is known)
    
    // Outline. The attribute style:text-outline can have the values true, false
    // TODO: This can be simulated in CSS with text shadows (if the font color is known)

    // Display. The attribute text:display can have the values true, none, condition.
    // In the latter case, the attribute text:condition gives the condition to hide the text.
    // This is currently handled by the content converters (condition is not supported)

    // Shadow: The attribute fo:text-shadow can have the same values as in CSS.
    // (Currently LO has only one shadow style, which is saved as 1pt 1pt)
    // TODO: Actually LO selects an automatic color based on the font color
    // gray if the color is black, and black otherwise, it seems
    private void cssTextShadow(StyleWithProperties style, CSVList props, boolean bInherit) {
	    String s = style.getTextProperty(XMLString.FO_TEXT_SHADOW,bInherit);
	    if (s!=null) { props.addValue("text-shadow",multiscale(s)); }
    }
    
    private String multiscale(String sValue) {
    	CSVList newValue = new CSVList(" ");
    	String[] sValues = sValue.split(" ");
    	int nLen = sValues.length;
    	for (int i=0; i<nLen; i++) {
    		if (sValues[i].length()>0) {
    			if (Character.isDigit(sValues[i].charAt(0))) {
    	            // If it's a number it must be a unit -> convert it
                    newValue.addValue(scale(sValues[i]));
    			}
    			else {
    				newValue.addValue(sValues[i]);
    			}
    		}
    	}
    	return newValue.toString();
    }
    
    // ---- Part 3. Placement
    
    // Superscripts and subscripts are handled together with font
    
    // Rotation. The property style:text-rotation-angle defines a rotation angle for a text span
    // The value can be an integer, optionally followed by a unit (deg, grad og rad)
    // Currently we only support 90deg and 270deg (which is all that LO supports)
    // Other angles would require a CSS transform, which is sub optimal.
    // The associated property style:text-rotation-scale can have the values fixed or line-height
    // This is currently not supported as we would need to know the line-height to convert to CSS
    private void cssTextRotation(StyleWithProperties style, CSVList props, boolean bInherit, boolean bInline) {
    	String s = style.getTextProperty(XMLString.STYLE_TEXT_ROTATION_ANGLE,bInherit);
    	if (s!=null) {
    		String sMode = null;
    		if (s.equals("90deg") || s.equals("90")) { sMode = "sideways-lr"; }
    		else if (s.equals("270deg") || s.equals("270")) { sMode = "vertical-rl"; }
    		if (sMode!=null) {
    			if (bInline) { props.addValue("vertical-align","middle"); }
    			props.addValue("writing-mode",sMode);    			
    		}
    	}	
    }

    // Text scale. The property style:text-scale can have a percentage as value.
    // This could be represented in CSS with transform:scaleX(...). However the original unscaled box is
    // still used for layout, which leads to bad results.
    // An alternative is to use font-stretch, but this depends on the font and cannot set arbitrary sizes
    // Thus for now the property is left unsupported

    
    // Letter spacing: The property fo:letter-spacing defines can have the values normal or a length
    // This fits with CSS
    private void cssLetterSpacing(StyleWithProperties style, CSVList props, boolean bInherit) {
	    String s = style.getTextProperty(XMLString.FO_LETTER_SPACING,bInherit);
	    if (s!=null) { props.addValue("letter-spacing",scale(s)); }
    }
    
    // Kerning. The property style:letter-kerning can have the values true or false.
    // This fits with CSS.
    private void cssLetterKerning(StyleWithProperties style, CSVList props, boolean bInherit) {
    	String s = style.getTextProperty(XMLString.STYLE_LETTER_KERNING, bInherit);
    	if (s!=null) {
    		if (s.equals("false")) { props.addValue("font-kerning", "none"); }
    		else if (s.equals("true")) { props.addValue("font-kerning", "normal"); }
    	}
    }
    
    // ---- Part 4. Hyperlink
    
    private void cssHyperlink(StyleWithProperties style, CSVList props) {
        // For hyperlinks, export text-decoration:none even if nothing is defined in source (to override browser default)
        if (style.getTextProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,true)==null) {
            props.addValue("text-decoration","none");
        }
    }
    
    // ---- Part 5. Highlighting 
    
    /** Convert text background color. This should be applied to an inline element.
     * 
     * @param style the Office style from which to read the properties
     * @param props the property list to which CSS properties are added
     * @param bInherit true if properties should be inherited from parent style(s)
     */
    public void cssTextBackground(StyleWithProperties style, CSVList props, boolean bInherit) {
        // Background color. The property fo:background-color can have the values transparent or a color
        // This fits with CSS when applied to inline text
        String s = style.getTextProperty(XMLString.FO_BACKGROUND_COLOR,bInherit);
	    if (s!=null && !s.equals("transparent")) { props.addValue("background-color",s); }
    }
    
    // ---- Part 6. Border
    
    // TODO
    
    // ---- Other

    // Hyphenate. The property fo:hyphenate can have the values true or false.
    // This fits with CSS, but we cannot support the additional attributes fo:hyphnation-*
    // Note: In LO this is a paragraph property
    private void cssHyphenate(StyleWithProperties style, CSVList props, boolean bInherit) {
    	String s = style.getTextProperty(XMLString.FO_HYPHENATE, bInherit);
    	if (s!=null) {
    		if (s.equals("false")) { props.addValue("hyphens", "manual"); }
    		else if (s.equals("true")) { props.addValue("hyphens", "auto"); }
    	}
    }
}
