/************************************************************************
 *
 *	BibliographyConverter.java
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
 *  Version 1.6.1 (2018-08-07)
 *
 */
package writer2xhtml.xhtml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Misc;

/** This class handles the export of the bibliography. Most of the work is delegated to the
 *  {@link XhtmlBibliographyGenerator} 
 */
class BibliographyConverter extends IndexConverterHelper {
	
	private XhtmlBibliographyGenerator bibGenerator;

    BibliographyConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,XMLString.TEXT_BIBLIOGRAPHY_SOURCE);
        bibGenerator = new XhtmlBibliographyGenerator(ofr,converter);
    }
    
    void handleBibliographyMark(Node onode, Node hnode) {
    	String sKey = Misc.getAttribute(onode, XMLString.TEXT_IDENTIFIER);
    	if (sKey!=null) {
	        Element anchor = converter.createLink("bib"+sKey);
	        hnode.appendChild(anchor);
	        anchor.appendChild(converter.createTextNode(bibGenerator.generateCitation(sKey)));
    	}
    }
    
    @Override void generate(IndexData data) {
    	bibGenerator.populateBibliography(data.onode, data.hnode);
    }

}
