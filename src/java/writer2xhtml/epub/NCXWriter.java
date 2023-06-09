/************************************************************************
 *
 *  NCXWriter.java
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

import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import writer2xhtml.api.ContentEntry;
import writer2xhtml.api.ConverterResult;
import writer2xhtml.base.DOMDocument;
import writer2xhtml.util.Misc;

/** This class creates the required NXC file for an EPUB document
 *  (see http://www.idpf.org/2007/opf/OPF_2.0_final_spec.html#Section2.4).
 */
public class NCXWriter extends DOMDocument {
	
	public NCXWriter(ConverterResult cr, String sUUID) {
		super("book", "ncx");
		
        // create DOM
        Document contentDOM = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            DocumentType doctype = domImpl.createDocumentType("ncx","",""); 
            contentDOM = domImpl.createDocument("http://www.daisy.org/z3986/2005/ncx/","ncx",doctype);
        }
        catch (ParserConfigurationException t) { // this should never happen
            throw new RuntimeException(t);
        }
        
        // Populate the DOM tree
        Element ncx = contentDOM.getDocumentElement();
        ncx.setAttribute("version", "2005-1");
        ncx.setAttribute("xml:lang", cr.getMetaData().getLanguage());
        ncx.setAttribute("xmlns","http://www.daisy.org/z3986/2005/ncx/");
        
        // The head has four required meta data items
        Element head = contentDOM.createElement("head");
        ncx.appendChild(head);
        
        Element uid = contentDOM.createElement("meta");
        uid.setAttribute("name","dtb:uid");
        uid.setAttribute("content", sUUID);
        head.appendChild(uid);
        
        Element depth = contentDOM.createElement("meta");
        depth.setAttribute("name","dtb:depth");
        // Setting the content attribute later
        head.appendChild(depth);
        
        Element totalPageCount = contentDOM.createElement("meta");
        totalPageCount.setAttribute("name","dtb:totalPageCount");
        totalPageCount.setAttribute("content", "0");
        head.appendChild(totalPageCount);

        Element maxPageNumber = contentDOM.createElement("meta");
        maxPageNumber.setAttribute("name","dtb:maxPageNumber");
        maxPageNumber.setAttribute("content", "0");
        head.appendChild(maxPageNumber);
        
        // The ncx must contain a docTitle element
        Element docTitle = contentDOM.createElement("docTitle");
        ncx.appendChild(docTitle);
        Element docTitleText = contentDOM.createElement("text");
        docTitle.appendChild(docTitleText);
        docTitleText.appendChild(contentDOM.createTextNode(cr.getMetaData().getTitle()));
        
        // Build the navMap from the content table in the converter result
        Element navMap = contentDOM.createElement("navMap");
        ncx.appendChild(navMap);
        
        Element currentContainer = ncx;
        int nCurrentLevel = 0;
        int nCurrentEntryLevel = 0; // This may differ from nCurrentLevel if the heading levels "jump" in then document
        int nDepth = 0;
        int nPlayOrder = 0;
        
        Iterator<ContentEntry> content = cr.getContent().iterator();
        while (content.hasNext()) {
        	ContentEntry entry = content.next();
        	int nEntryLevel = Math.max(entry.getLevel(), 1);
        	
        	if (nEntryLevel<nCurrentLevel) {
        		// Return to higher level
        		for (int i=nEntryLevel; i<nCurrentLevel; i++) {
        			currentContainer = (Element) currentContainer.getParentNode();
        		}
        		nCurrentLevel = nEntryLevel;
        	}
        	else if (nEntryLevel>nCurrentEntryLevel) {
        		// To lower level (always one step; a jump from e.g. heading 1 to heading 3 in the document
        		// is considered an error)
        		currentContainer = (Element) currentContainer.getLastChild();
        		nCurrentLevel++;
        	}
        	
        	nCurrentEntryLevel = nEntryLevel;
        	
        	Element navPoint = contentDOM.createElement("navPoint");
        	navPoint.setAttribute("playOrder", Integer.toString(++nPlayOrder));
        	navPoint.setAttribute("id", "text"+nPlayOrder);
        	currentContainer.appendChild(navPoint);

        	Element navLabel = contentDOM.createElement("navLabel");
        	navPoint.appendChild(navLabel);
        	Element navLabelText = contentDOM.createElement("text");
        	navLabel.appendChild(navLabelText);
        	navLabelText.appendChild(contentDOM.createTextNode(entry.getTitle()));

        	Element navPointContent = contentDOM.createElement("content");
        	String sHref = Misc.makeHref(entry.getFile().getFileName());
        	if (entry.getTarget()!=null) { sHref+="#"+entry.getTarget(); }
        	navPointContent.setAttribute("src", sHref);
        	navPoint.appendChild(navPointContent);
        	
        	nDepth = Math.max(nDepth, nCurrentLevel);
        }
        
        if (nDepth==0) {
        	// TODO: We're in trouble: The document has no headings
        }
        
        depth.setAttribute("content", Integer.toString(nDepth));
        
        setContentDOM(contentDOM);
        
	}
}
