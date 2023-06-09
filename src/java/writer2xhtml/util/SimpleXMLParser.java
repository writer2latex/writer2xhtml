/************************************************************************
 *
 *  SimpleSAXHandler.java
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
 *  Copyright: 2002-2012 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.4 (2012-04-01) 
 * 
 */

package writer2xhtml.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/** A simple SAX based XML parser which transforms the SAX events into a DOM tree
 *  (supporting element and text nodes only)
 */
public class SimpleXMLParser extends DefaultHandler {
	
	/** Static method to parse an XML input stream into a DOM tree
	 * 
	 * @param is the input stream to parse
	 * @return a DOM tree of the document
	 * @throws IOException if an error occurs reading the input stream
	 * @throws SAXException if an error occurs parsing the stream
	 */
	public static Document parse(InputStream is) throws IOException, SAXException {
		SAXParserFactory factory=SAXParserFactory.newInstance();
		
		factory.setValidating(false);
		SimpleXMLParser handler = new SimpleXMLParser();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(is,handler);
			return handler.getDOM();
		}
		catch (ParserConfigurationException e) {
			System.err.println("Oops - failed to get XML parser!?");
			e.printStackTrace();
		}
		return null;
	}

	private SimpleDOMBuilder builder = new SimpleDOMBuilder();
	
	public org.w3c.dom.Document getDOM() {
		return builder.getDOM();
	}
	
	// We don't need - and in fact should avoid - any external entities
	@Override public InputSource resolveEntity(String publicID, String systemID) throws SAXException {
		return new InputSource(new StringReader(""));
	}
	
	@Override public void startElement(String nameSpace, String localName, String qName, Attributes attributes){
		builder.startElement(qName);
		int nLen = attributes.getLength();
		for (int i=0;i<nLen;i++) {
			builder.setAttribute(attributes.getQName(i), attributes.getValue(i));
		}
	}
	
	@Override public void endElement(String nameSpace, String localName, String qName){
		builder.endElement();
	}

	@Override public void characters(char[] characters, int nStart, int nEnd) throws SAXException {
		builder.characters(new String(characters,nStart,nEnd));
		
	}

}
