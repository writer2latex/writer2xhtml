/************************************************************************
 *
 *  ContainerWriter.java
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
 *  Copyright: 2001-2014 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  version 1.4 (2014-08-26)
 *
 */

package writer2xhtml.epub;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import writer2xhtml.base.DOMDocument;

/** This class creates the required META-INF/container.xml file for an EPUB package 
 *  (see http://www.idpf.org/ocf/ocf1.0/download/ocf10.htm).
 */
public class ContainerWriter extends DOMDocument {
	
	public ContainerWriter() {
		super("container", "xml");
		
        // create DOM
        Document contentDOM = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            DocumentType doctype = domImpl.createDocumentType("container","",""); 
            contentDOM = domImpl.createDocument("urn:oasis:names:tc:opendocument:xmlns:container","container",doctype);
        }
        catch (ParserConfigurationException t) { // this should never happen
            throw new RuntimeException(t);
        }
        
        // Populate the DOM tree
        Element container = contentDOM.getDocumentElement();
        container.setAttribute("version", "1.0");
        container.setAttribute("xmlns","urn:oasis:names:tc:opendocument:xmlns:container");
        
        Element rootfiles = contentDOM.createElement("rootfiles");
        container.appendChild(rootfiles);
        
        Element rootfile = contentDOM.createElement("rootfile");
        rootfile.setAttribute("full-path", "OEBPS/book.opf");
        rootfile.setAttribute("media-type", "application/oebps-package+xml");
        rootfiles.appendChild(rootfile);
        
        setContentDOM(contentDOM);
	}

}
