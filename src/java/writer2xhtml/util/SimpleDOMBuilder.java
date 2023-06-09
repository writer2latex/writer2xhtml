/************************************************************************
 *
 *  SimpleDOMBuilder.java
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
 *  Copyright: 2002-2014 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.4 (2014-09-16)
 *
 */

package writer2xhtml.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

/** This class provides a simple way to create and populate a DOM tree in logical order
 */
public class SimpleDOMBuilder {
	private Document dom=null;
	private Element currentElement=null;
	private StringBuilder charBuffer=new StringBuilder();
	
	/**
	 * Append an element to the current element and set this new element to be the current element.
	 * If there is no current element, a new DOM tree will be created (discarding the current DOM tree if any)
	 * with the new element as the document element.
	 * 
	 * @param sTagName
	 * @return true on success
	 */
	public boolean startElement(String sTagName) {
		if (currentElement!=null) {
			flushCharacters();
			currentElement = (Element) currentElement.appendChild(dom.createElement(sTagName));
		}
		else {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
		        DocumentBuilder builder = factory.newDocumentBuilder();
		        DOMImplementation domImpl = builder.getDOMImplementation();
		        DocumentType doctype = domImpl.createDocumentType(sTagName, "", ""); 
		        dom = domImpl.createDocument("",sTagName,doctype);
		        currentElement = dom.getDocumentElement();
			} catch (ParserConfigurationException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Set the current element to the parent of the current element
	 * @return true on success, false if there is no current element to end
	 */
	public boolean endElement() {
		if (currentElement!=null) {
			flushCharacters();
			if (currentElement!=dom.getDocumentElement()) {
				currentElement=(Element) currentElement.getParentNode();
			}
			else { // Back at document element: Finished populating the DOM tree
				currentElement=null;
			}
			return true;
		}
		return false;		
	}
	
	/**
	 * Set an attribute of the current element
	 * @param sName
	 * @param sValue
	 * @return true on success, false if there is no current element
	 */
	public boolean setAttribute(String sName,String sValue) {
		if (currentElement!=null) {
			currentElement.setAttribute(sName, sValue);
			return true;
		}
		return false;
	}
	
	/**
	 * Add characters to the currentElement. The actual writing of characters to the DOM is delayed until the
	 * <code>startElement</code> or <code>endElement</code> methods are invoked
	 * @param sText
	 * @return true on success, false if there is no current element
	 */
	public boolean characters(String sText) {
		if (currentElement!=null) {
			charBuffer.append(sText);
			return true;
		}
		return false;
	}
	
	private void flushCharacters() {
		if (charBuffer.length()>0) {
			currentElement.appendChild(dom.createTextNode(charBuffer.toString()));
			charBuffer.setLength(0);
		}
	}
	
	/**
	 * Get the DOM tree
	 * 
	 * @return the DOM tree, or null if none has been created
	 */
	public Document getDOM() {
		return dom;
	}
}
