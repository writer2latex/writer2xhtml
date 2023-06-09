/************************************************************************
 *
 *  ListStyle.java
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
 *  Version 1.7 (2023-05-25)
 *
 */

package writer2xhtml.office;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.util.Misc;

/** <p> Class representing a list style (including outline numbering) in OOo Writer</p>
*/
public class ListStyle extends OfficeStyle {
    // the file format doesn't specify a maximum nesting level, but OOo
    // currently supports 10
    private static final int MAX_LEVEL = 10; 
    private PropertySet[] level;
    private PropertySet[] levelStyle;
    private PropertySet[] levelStyleText;
    private Element[] levelImage;

    public ListStyle() {
        level = new PropertySet[MAX_LEVEL+1];
        levelStyle = new PropertySet[MAX_LEVEL+1];
        levelStyleText = new PropertySet[MAX_LEVEL+1];
        levelImage = new Element[MAX_LEVEL+1];
        for (int i=1; i<=MAX_LEVEL; i++) {
            level[i] = new PropertySet();
            levelStyle[i] = new PropertySet();
            levelStyleText[i] = new PropertySet();
            levelImage[i] = null;
        }
    }
	
    public String getLevelType(int i) {
        if (i>=1 && i<=MAX_LEVEL) {
            return level[i].getName();
        }
        else {
            return null;
        }
    }
	
    public boolean isNumber(int i) {
        return XMLString.TEXT_LIST_LEVEL_STYLE_NUMBER.equals(level[i].getName()) ||
        XMLString.TEXT_OUTLINE_LEVEL_STYLE.equals(level[i].getName());
    }
	
    public boolean isBullet(int i) {
        return XMLString.TEXT_LIST_LEVEL_STYLE_BULLET.equals(level[i].getName());
    }

    public boolean isImage(int i) {
        return XMLString.TEXT_LIST_LEVEL_STYLE_IMAGE.equals(level[i].getName());
    }
    
    // Return image for this level or null if the level is not text:list-level-style-image
    public Element getImage(int i) {
    	return levelImage[i];
    }
    
    // Return true if this level is using the new list formatting of ODT 1.2
    public boolean isNewType(int i) {
    	return "label-alignment".equals(getLevelStyleProperty(i,XMLString.TEXT_LIST_LEVEL_POSITION_AND_SPACE_MODE));
    }

    public String getLevelProperty(int i, String sName) {
        if (i>=1 && i<=MAX_LEVEL) {
            return level[i].getProperty(sName);
        }
        else {
            return null;
        }
    }
	
    public String getLevelStyleProperty(int i, String sName) {
        if (i>=1 && i<=MAX_LEVEL) {
            return levelStyle[i].getProperty(sName);
        }
        else {
            return null;
        }
    }

    public String getLevelStyleTextProperty(int i, String sName) {
        if (i>=1 && i<=MAX_LEVEL) {
            return levelStyleText[i].getProperty(sName);
        }
        else {
            return null;
        }
    }

    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        // Collect level information from child elements (text:list-level-style-*):
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE){
                String sLevel = Misc.getAttribute(child,XMLString.TEXT_LEVEL);
                if (sLevel!=null) {
                    int nLevel = Misc.getPosInteger(sLevel,1);
                    if (nLevel>=1 && nLevel<=MAX_LEVEL) {
                    	loadLevelPropertiesFromDOM(nLevel,child);
                    	if (XMLString.TEXT_LIST_LEVEL_STYLE_IMAGE.equals(child.getNodeName())) {
                    		levelImage[nLevel] = (Element) child;
                    	}
                    }
                }
            }
            child = child.getNextSibling();
        }
    }
    
    private void loadLevelPropertiesFromDOM(int nLevel, Node node) {
    	// Load the attributes
        level[nLevel].loadFromDOM(node);
        // Also include style:properties
        Node child = node.getFirstChild();
        while (child!=null) {
        	if (child.getNodeType()==Node.ELEMENT_NODE){
        		if (child.getNodeName().equals(XMLString.STYLE_PROPERTIES)) {
        			levelStyle[nLevel].loadFromDOM(child);
        			loadLevelLabelPropertiesFromDOM(nLevel,node);
                } 
        		else if (child.getNodeName().equals(XMLString.STYLE_LIST_LEVEL_PROPERTIES)) { // oasis
                    levelStyle[nLevel].loadFromDOM(child);
        			loadLevelLabelPropertiesFromDOM(nLevel,child);
                }                                
        		else if (child.getNodeName().equals(XMLString.STYLE_TEXT_PROPERTIES)) {
                    levelStyleText[nLevel].loadFromDOM(child);
                }                               
            }                                   
            child = child.getNextSibling();
        }
    }
    
    private void loadLevelLabelPropertiesFromDOM(int nLevel, Node node) {
    	// Merge the properties from style:list-level-label-alignment
        Node child = node.getFirstChild();
        while (child!=null) {
        	if (child.getNodeType()==Node.ELEMENT_NODE){
        		if (child.getNodeName().equals(XMLString.STYLE_LIST_LEVEL_LABEL_ALIGNMENT)) {
        			levelStyle[nLevel].loadFromDOM(child);
                }
            }                                   
            child = child.getNextSibling();
        }    	
    }
	
}