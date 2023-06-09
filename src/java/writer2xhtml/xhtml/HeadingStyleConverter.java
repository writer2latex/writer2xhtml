/************************************************************************
 *
 *  HeadingStyleConverter.java
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
 *  Copyright: 2002-2018 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6.1 (2018-08-23)
 *
 */
package writer2xhtml.xhtml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyleFamily;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.util.CSVList;

public class HeadingStyleConverter extends StyleConverterHelper {
	
	// Sets of additional styles (other than the main heading style for the level)
	private List<Set<String>> otherLevelStyles;

	public HeadingStyleConverter(OfficeReader ofr, XhtmlConfig config,
			Converter converter, int nType) {
		super(ofr, config, converter, nType);
        this.styleMap = config.getXHeadingStyleMap();
        this.bConvertStyles = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES;
        this.otherLevelStyles = new ArrayList<Set<String>>();
        for (int i=0; i<=6; i++) {
        	otherLevelStyles.add(new HashSet<String>());
        }
	}

	@Override
	public String getStyleDeclarations(String sIndent) {
        if (bConvertStyles) {
        	StringBuilder buf = new StringBuilder();
        	for (int i=1; i<=6; i++) {
        		// Convert main style for this level
        		if (ofr.getHeadingStyle(i)!=null) {
        			CSVList props = new CSVList(";");
        			getParSc().applyProperties(ofr.getHeadingStyle(i),props,true);
        			props.addValue("clear","left");
        			buf.append(sIndent).append("h").append(i)
        			.append(" {").append(props.toString()).append("}").append(config.prettyPrint() ? "\n" : " ");
        		}
        		// Convert other styles for this level
        		for (String sDisplayName : otherLevelStyles.get(i)) {
                    StyleWithProperties style = (StyleWithProperties)
                            getStyles().getStyleByDisplayName(sDisplayName);
                    CSVList props = new CSVList(";");
                    getParSc().applyProperties(style,props,true);
        			props.addValue("clear","left");
        			buf.append(sIndent).append("h").append(i).append(".").append(styleNames.getExportName(sDisplayName))
        			.append(" {").append(props.toString()).append("}").append(config.prettyPrint() ? "\n" : " ");
        		}
            }
            return buf.toString();
        }
        return "";
	}

	@Override
	public OfficeStyleFamily getStyles() {
		return ofr.getParStyles();
	}
	
	/** Apply a style on a heading
	 * 
	 * @param nLevel the heading level
	 * @param sStyleName the style name
	 * @param info add style information to this StyleInfo
	 */
	public void applyStyle(int nLevel, String sStyleName, StyleInfo info) {
        StyleWithProperties style = (StyleWithProperties) getStyles().getStyle(sStyleName);
        if (style!=null) {
            if (config.multilingual()) {
            	applyLang(style,info);
                applyDirection(style,info);
            }
            if (style.isAutomatic()) {
                // Apply parent style + hard formatting
                applyStyle(nLevel, style.getParentName(),info);
                if (bConvertHard) { getParSc().applyProperties(style,info.props,false); }
            }
            else {
                String sDisplayName = style.getDisplayName();
                if (styleMap.contains(sDisplayName)) {
                    // Apply attributes as specified in style map from user
                	XhtmlStyleMapItem map = styleMap.get(sDisplayName);
                    info.sTagName = map.sBlockElement;
                    if (!"(none)".equals(map.sBlockCss)) {
                        info.sClass = map.sBlockCss;
                    }
                }
                else if (style!=ofr.getHeadingStyle(nLevel)) {
                	// This is not the main style for this level, add class and remember
                    info.sClass = styleNames.getExportName(sDisplayName);
                    if (1<=nLevel && nLevel<=6) {
                    	otherLevelStyles.get(nLevel).add(sDisplayName);
                    }
                }
            }
        }
	}

	/** Apply an inner style on a heading. The inner style surrounds the text content, excluding the numbering label.
	 *  Inner styles are not an OpenDocument feature, but is provided as an additional style hook for own style sheets.
	 *  An inner style is only applied if there is an explicit style map for the style.
	 * 
	 * @param nLevel the heading level
	 * @param sStyleName the style name
	 * @param info add style information to this StyleInfo
	 */
	public void applyInnerStyle(int nLevel, String sStyleName, StyleInfo info) {
        StyleWithProperties style = (StyleWithProperties) getStyles().getStyle(sStyleName);
        if (style!=null) {
            if (style.isAutomatic()) {
                // Apply parent style
                applyInnerStyle(nLevel, style.getParentName(), info);
            }
            else {
                String sDisplayName = style.getDisplayName();
                if (styleMap.contains(sDisplayName)) {
                    // Apply attributes as specified in style map from user
                	XhtmlStyleMapItem map = styleMap.get(sDisplayName);
                    info.sTagName = map.sElement;
                    if (!"(none)".equals(map.sCss)) {
                        info.sClass = map.sCss;
                    }
                }
            }
        }
	}

}
