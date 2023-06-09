/************************************************************************
 *
 *  PropertySet.java
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
 *  Version 1.6.1 (2018-08-10)
 *
 */

package writer2xhtml.office;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import java.util.Enumeration;
import java.util.Hashtable;

/** <p> Class representing a set of style properties in OOo (actually this
    is simply the set of attributes of an element). </p> 
  */
public class PropertySet {
    private Hashtable<String, String> properties = new Hashtable<String, String>();
    private String sName;

    public PropertySet() {
        properties = new Hashtable<String, String>();
        sName="";
    }
    
    public int getSize() {
    	return properties.size();
    }
	
    public String getProperty(String sPropName) {
        if (sPropName!=null) {
            String sValue = properties.get(sPropName);
            if (sValue!=null && sValue.endsWith("inch")) {
                // Cut of inch to in
                return sValue.substring(0,sValue.length()-2);
            }
            else {
                return sValue;
            }
        }
        else {
            return null;
        }
    }
	
    public String getName() { return sName; }

    public void loadFromDOM(Node node) {
        // read the attributes of the node, if any
        if (node!=null) {
            sName = node.getNodeName();
            NamedNodeMap attrNodes = node.getAttributes();
            if (attrNodes!=null) {    
                int nLen = attrNodes.getLength();
                for (int i=0; i<nLen; i++){
                    Node attr = attrNodes.item(i);
                    properties.put(attr.getNodeName(),attr.getNodeValue());
                }
            }
        }
    }
	
    public boolean containsProperty(String sProperty) {
        return sProperty!=null && properties.containsKey(sProperty);
    }
	
    public void setProperty(String sProperty, String sValue){
        properties.put(sProperty,sValue);
    }
	
    public String toString() {
        String s="";
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String sKey = keys.nextElement();
            String sValue = properties.get(sKey);
            s += sKey+"="+sValue+" ";
        }
        return s;
    }

}