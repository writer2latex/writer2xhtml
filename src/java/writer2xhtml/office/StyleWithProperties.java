/************************************************************************
 *
 *  StyleWithProperties.java
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
 *  Version 1.7.1 (2023-06-26)
 */
 
package writer2xhtml.office;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import writer2xhtml.util.Calc;
import writer2xhtml.util.Misc;

/** Class representing an ODF-style which contains a style:properties element 
  */
public class StyleWithProperties extends OfficeStyle {
    public final static int OLDPROPS = 0;
    public final static int TEXT = 1;
    public final static int PAR = 2;
    public final static int SECTION = 3;
    public final static int TABLE = 4;
    public final static int COLUMN = 5;
    public final static int ROW = 6;
    public final static int CELL = 7;
    public final static int GRAPHIC = 8;
    public final static int PAGE = 9;
    public final static int COUNT = 10;
	
    private boolean bEmpty; // Flag to indicate that this style does not contain any properties except rsid
	
    private PropertySet[] properties = new PropertySet[COUNT];
    private boolean bIsOldProps = false;

    private PropertySet backgroundImageProperties = new PropertySet();

    private int nColCount = 0;

    private boolean bHasFootnoteSep = false;
    private PropertySet footnoteSep = new PropertySet();
    private List<String> tabStops = new ArrayList<>();

    public StyleWithProperties() {
        for (int i=0; i<COUNT; i++) {
            properties[i] = new PropertySet();
        }
    }

    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        // read the properties of the style, if any
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                String sName = child.getNodeName();
                if (XMLString.STYLE_PROPERTIES.equals(sName)) {
                    bIsOldProps = true; // style:properties identifies old format
                    loadPropertiesFromDOM(OLDPROPS,child);
                }
                else if (XMLString.STYLE_TEXT_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(TEXT,child);
                }
                else if (XMLString.STYLE_PARAGRAPH_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(PAR,child);
                }
                else if (XMLString.STYLE_SECTION_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(SECTION,child);
                }
                else if (XMLString.STYLE_TABLE_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(TABLE,child);
                }
                else if (XMLString.STYLE_TABLE_COLUMN_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(COLUMN,child);
                }
                else if (XMLString.STYLE_TABLE_ROW_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(ROW,child);
                }
                else if (XMLString.STYLE_TABLE_CELL_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(CELL,child);
                }
                else if (XMLString.STYLE_GRAPHIC_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(GRAPHIC,child);
                }
                else if (XMLString.STYLE_PAGE_LAYOUT_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(PAGE,child);
                }
                else if (XMLString.STYLE_DRAWING_PAGE_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(PAGE,child);
                }
            }
            child = child.getNextSibling();
        }
        // Test whether this style contains any real properties
        int nSize = 0;
        for (int i=0; i<COUNT; i++) {
        	nSize+=properties[i].getSize();
        }
        bEmpty = (nSize==1 && properties[TEXT].containsProperty(XMLString.OFFICEOOO_RSID));
    }
	
    private void loadPropertiesFromDOM(int nIndex,Node node) {
        properties[nIndex].loadFromDOM(node);
        // Several property sets may contain these complex properties, but only one per style:
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {        
                String sName = child.getNodeName();
                if (XMLString.STYLE_BACKGROUND_IMAGE.equals(sName)) {    
                    backgroundImageProperties.loadFromDOM(child);
                }
                else if (XMLString.STYLE_COLUMNS.equals(sName)) {    
                    nColCount = Misc.getPosInteger(Misc.getAttribute(child,
                                XMLString.FO_COLUMN_COUNT),1);
                    // TODO: read individual columns
                }
                else if (XMLString.STYLE_FOOTNOTE_SEP.equals(sName)) {
                    bHasFootnoteSep = true; 
                    footnoteSep.loadFromDOM(child);
                }
                else if (XMLString.STYLE_TAB_STOPS.equals(sName)) {
                	loadTabStops(child);
                }
            }
            child = child.getNextSibling();
        }
    }
    
    private void loadTabStops(Node node) {
    	Node child = node.getFirstChild();
    	while (child!=null) {
    		if (Misc.isElement(child, XMLString.STYLE_TAB_STOP)) {
    			tabStops.add(Misc.getAttribute(child, XMLString.STYLE_POSITION));
    		}
    		child = child.getNextSibling();
    	}
    }
    
    public boolean isEmpty() {
    	return bEmpty;
    }
	
    protected String getProperty(int nIndex, String sName, boolean bInherit) {
        int nRealIndex = bIsOldProps ? OLDPROPS : nIndex;
        if (properties[nRealIndex].containsProperty(sName)) {
            String sValue = properties[nRealIndex].getProperty(sName);
            return Calc.truncateLength(sValue);
        }
        else if (bInherit && getParentName()!=null) {
            StyleWithProperties parentStyle = (StyleWithProperties) family.getStyle(getParentName());
            if (parentStyle!=null) {
                return parentStyle.getProperty(nIndex,sName,bInherit);
            }
        }
        return null; // no value
    }
	
    public String getTextProperty(String sName, boolean bInherit) {
        return getProperty(TEXT,sName,bInherit);
    }

    public String getParProperty(String sName, boolean bInherit) {
        return getProperty(PAR,sName,bInherit);
    }

    public String getSectionProperty(String sName, boolean bInherit) {
        return getProperty(SECTION,sName,bInherit);
    }

    public String getTableProperty(String sName, boolean bInherit) {
        return getProperty(TABLE,sName,bInherit);
    }

    public String getColumnProperty(String sName, boolean bInherit) {
        return getProperty(COLUMN,sName,bInherit);
    }

    public String getRowProperty(String sName, boolean bInherit) {
        return getProperty(ROW,sName,bInherit);
    }

    public String getCellProperty(String sName, boolean bInherit) {
        return getProperty(CELL,sName,bInherit);
    }

    public String getGraphicProperty(String sName, boolean bInherit) {
        return getProperty(GRAPHIC,sName,bInherit);
    }

    // TODO: Remove this method
    public String getProperty(String sProperty, boolean bInherit){
        String sValue;
        for (int i=0; i<COUNT; i++) {
            sValue = getProperty(i,sProperty,bInherit);
            if (sValue!=null) { return sValue; }
        }
        return null; // no value
    }

    // TODO: Remove this method
    public String getProperty(String sProperty){
        return getProperty(sProperty,true); 
    }
	
    protected String getAbsoluteProperty(int nIndex, String sProperty){
        int nRealIndex = bIsOldProps ? OLDPROPS : nIndex;
        if (properties[nRealIndex].containsProperty(sProperty)){
            String sValue=(String) properties[nRealIndex].getProperty(sProperty);
            if (sValue.endsWith("%")) {
                StyleWithProperties parentStyle 
	                = (StyleWithProperties) family.getStyle(getParentName());
                if (parentStyle!=null) {
                    String sParentValue = parentStyle.getAbsoluteProperty(nIndex,sProperty);
                    if (sParentValue!=null) { return Calc.multiply(sValue,sParentValue); }
                }
                else if (getFamily()!=null && getFamily().getDefaultStyle()!=null) {
                    StyleWithProperties style = (StyleWithProperties) getFamily().getDefaultStyle();
                    String sDefaultValue=(String) style.getProperty(nIndex,sProperty,false);
                    if (sValue !=null) { return Calc.multiply(sValue,sDefaultValue); }
                }
            }
            else {
                return Calc.truncateLength(sValue);
            }
        }
        else if (getParentName()!=null){
            StyleWithProperties parentStyle 
                = (StyleWithProperties) family.getStyle(getParentName());
            if (parentStyle!=null) {
                return parentStyle.getAbsoluteProperty(nIndex,sProperty);
            }
        }
        else if (getFamily()!=null && getFamily().getDefaultStyle()!=null) {
            StyleWithProperties style = (StyleWithProperties) getFamily().getDefaultStyle();
            String sValue=(String) style.getProperty(nIndex,sProperty,false);
            if (sValue !=null) { return sValue; }
        }
        // no value!
        return null;
    }
	
    public String getAbsoluteTextProperty(String sName) {
        return getAbsoluteProperty(TEXT,sName);
    }
	
    public String getAbsoluteParProperty(String sName) {
        return getAbsoluteProperty(PAR,sName);
    }
	
    public String getAbsoluteSectionProperty(String sName) {
        return getAbsoluteProperty(SECTION,sName);
    }
	
    public String getAbsoluteTableProperty(String sName) {
        return getAbsoluteProperty(TABLE,sName);
    }
	
    public String getAbsoluteColumnProperty(String sName) {
        return getAbsoluteProperty(COLUMN,sName);
    }
	
    public String getAbsoluteRowProperty(String sName) {
        return getAbsoluteProperty(ROW,sName);
    }
	
    public String getAbsoluteCellProperty(String sName) {
        return getAbsoluteProperty(CELL,sName);
    }
	
    public String getAbsoluteGraphicProperty(String sName) {
        return getAbsoluteProperty(GRAPHIC,sName);
    }

    // TODO: Remove this method
    public String getAbsoluteProperty(String sProperty){
        String sValue;
        for (int i=0; i<COUNT; i++) {
            sValue = getAbsoluteProperty(i,sProperty);
            if (sValue!=null) { return sValue; }
        }
        return null; // no value
    }
    
    /** Get the font size. This is a special case which combines the two properties
     *  <code>fo:font-size</code> and <code>style:font-size-rel</code>.
     *  This method resolves the font size to an absolute size
     * 
     * @return the absolute value, or null if the property is not set
     */
    public String getAbsoluteFontSize(){
        if (properties[TEXT].containsProperty(XMLString.STYLE_FONT_SIZE_REL)) {
        	// Size specified as e.g. +3pt or -2pt
        	String sValue= properties[TEXT].getProperty(XMLString.STYLE_FONT_SIZE_REL);
        	String sParentValue = getAbsoluteParentFontSize();
       		if (sParentValue!=null) {
       			return Calc.add(Calc.truncateLength(sValue),sParentValue);
       		}
        }
        else if (properties[TEXT].containsProperty(XMLString.FO_FONT_SIZE)) {
          	String sValue=(String) properties[TEXT].getProperty(XMLString.FO_FONT_SIZE);
            if (sValue.endsWith("%")) {
            	// Size specified as a percentage
                String sParentValue = getAbsoluteParentFontSize();
                if (sParentValue!=null) {
                	return Calc.multiply(sValue,sParentValue);
                }
            }
            else {
            	// Absolute size
                return Calc.truncateLength(sValue);
            }
        }
        // If we failed, return the parent size
        return getAbsoluteParentFontSize();
    }

    private String getAbsoluteParentFontSize() {
        StyleWithProperties parentStyle = (StyleWithProperties) family.getStyle(getParentName());
        if (parentStyle!=null) {
            return parentStyle.getAbsoluteFontSize();
        }
        else if (getFamily()!=null && getFamily().getDefaultStyle()!=null) {
            StyleWithProperties style = (StyleWithProperties) getFamily().getDefaultStyle();
            return Calc.truncateLength(style.getProperty(TEXT,XMLString.FO_FONT_SIZE,false));
        }
        return null;
    }
	
    // Get a length property that defaults to 0cm
    public String getAbsoluteLength(String sProperty) {
        String s = getAbsoluteProperty(sProperty);
        if (s==null) { return "0cm"; }
        else { return s; }
    }
	
    public String getBackgroundImageProperty(String sName) {
        return backgroundImageProperties.getProperty(sName);
    }
	
    public int getColCount() { return nColCount; }
	
    public boolean hasFootnoteSep() { return bHasFootnoteSep; }

    public String getFootnoteProperty(String sPropName) {
        return footnoteSep.getProperty(sPropName);
    }
    
    public List<String> getTabStops(boolean bInherit) {
    	if (!tabStops.isEmpty() || !bInherit) {
    		return Collections.unmodifiableList(tabStops);
    	}
    	StyleWithProperties parentStyle = (StyleWithProperties) family.getStyle(getParentName());
    	if (parentStyle!=null) {
    		return parentStyle.getTabStops(bInherit);
    	}
    	// Found no tab stops, return the empty list
    	return Collections.unmodifiableList(tabStops);
    }

}