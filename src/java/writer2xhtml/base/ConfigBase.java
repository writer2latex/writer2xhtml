/************************************************************************
 *
 *  ConfigBase.java
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
 *  Version 1.7.1 (2023-08-01)
 *
 */

package writer2xhtml.base;

/** Base implementation of writer2xhtml.api.Config 
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import writer2xhtml.api.ComplexOption;
import writer2xhtml.util.Misc;

import org.w3c.dom.Element;
import org.w3c.dom.DOMImplementation;

public abstract class ConfigBase implements writer2xhtml.api.Config {
	
    protected abstract int getOptionCount();
    protected abstract String getDefaultConfigPath();
	
    protected Option[] options;
    protected Map<String,ComplexOption> optionGroups;
	
    public ConfigBase() {
        options = new Option[getOptionCount()];
        optionGroups = new HashMap<String,ComplexOption>();
    }
	
    public void setOption(String sName,String sValue) {
    	if (sName!=null && sValue!=null) {
    		for (int j=0; j<getOptionCount(); j++) {
    			if (sName.equals(options[j].getName())) {
    				options[j].setString(sValue);
    				break;
    			}
    		}
        }
    }
	
    public String getOption(String sName) {
    	if (sName!=null) {
    		for (int j=0; j<getOptionCount(); j++) {
    			if (sName.equals(options[j].getName())) {
    				return options[j].getString();
    			}
    		}
    	}
        return null;
    }
    
    public ComplexOption getComplexOption(String sGroup) {
   		return optionGroups.get(sGroup);
    }
    
	// The subclass may use this method to define option groups
	protected ComplexOption addComplexOption(String sGroup) {
		optionGroups.put(sGroup, new ComplexOption());
		return optionGroups.get(sGroup);
	}

    public void readDefaultConfig(String sName) throws IllegalArgumentException {
        InputStream is = this.getClass().getResourceAsStream(getDefaultConfigPath()+sName);
        if (is==null) {
            throw new IllegalArgumentException("The standard configuration '"+sName+ "' does not exist");
        }
        try {
            read(is);
        }
        catch (IOException e) {
            // This would imply a bug in the configuration file!
            throw new IllegalArgumentException("The standard configuration '"+sName+ "' is invalid");
        }
    }

	
    /** <p>Read configuration from a specified input stream</p>
     *  @param is the input stream to read the configuration from
     */
    public void read(InputStream is) throws IOException {
        DOMDocument doc = new DOMDocument("config",".xml");
        doc.read(is); // may throw an IOException
        Document dom = doc.getContentDOM();
        if (dom==null) {
            throw new IOException("Failed to parse configuration");
        }

        Node root = dom.getDocumentElement();
        String sExtends = Misc.getAttribute(root, "extends"); 
        if (sExtends!=null) {
        	try {
        		readDefaultConfig(sExtends);
        	}
        	catch (IllegalArgumentException e) {
        		throw new IOException("The standard configuration "+sExtends+" does not exist");
        	}
        }
        Node child = root.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                Element elm = (Element)child;
                if (elm.getTagName().equals("option")) {
                    String sName = elm.getAttribute("name");
                    String sValue = elm.getAttribute("value");
                    if (sName.length()>0) { setOption(sName,sValue); }
                }
                else {
                    readInner(elm);
                }
            }
            child = child.getNextSibling();
        }
    }
	
    public void read(File file) throws IOException {
    	read(new FileInputStream(file));
    }
    
    /** Read configuration information from an xml element.
     *  The subclass must define this to read richer configuration data
     */
    protected abstract void readInner(Element elm);

    public void write(OutputStream os) throws IOException {
        DOMDocument doc = new DOMDocument("config",".xml");
        Document dom = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            dom = domImpl.createDocument("","config",null);
    	} catch (ParserConfigurationException e) {
    		// This will not happen
            e.printStackTrace();
            return;
        }
        Element rootElement = dom.getDocumentElement();

        for (int i=0; i<getOptionCount(); i++) {
            Element optionNode = dom.createElement("option");
            optionNode.setAttribute("name",options[i].getName());
            optionNode.setAttribute("value",options[i].getString());
            rootElement.appendChild(optionNode);
        }

        writeInner(dom);
		
        doc.setContentDOM(dom);
        doc.write(os); // may throw an IOException
    }
	
    public void write(File file) throws IOException {
    	write(new FileOutputStream(file));
    }

    /** Write configuration information to an xml document.
     *  The subclass must define this to write richer configuration data
     */
    protected abstract void writeInner(Document dom);
	
}

