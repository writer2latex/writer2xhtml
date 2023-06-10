/************************************************************************
 *
 *  BibliographyGenerator.java
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
 *  Version 1.7 (2023-06-10)
 *
 */

package writer2xhtml.xhtml;

import org.w3c.dom.Element;

import writer2xhtml.base.BibliographyGenerator;
import writer2xhtml.office.OfficeReader;

class XhtmlBibliographyGenerator extends BibliographyGenerator {
	
	private Converter converter;
	private Element ul; // The container element
	private Element currentPar; // The paragraph of the current item
	
	XhtmlBibliographyGenerator(OfficeReader ofr, Converter converter) {
		super(ofr,false);
		this.converter = converter;
	}
	
	/** Populate the bibliography
	 * 
	 * @param bibliography a text:bibliography element
	 * @param ul an XHTML list element to contain the code
	 */
	void populateBibliography(Element bibliography, Element ul) {
		this.ul = ul;
		generateBibliography(bibliography);
	}

	@Override protected void insertBibliographyItem(String sStyleName, String sKey) {
		Element li = converter.createElement("li");
		converter.addTarget(li, "bib"+sKey);
		converter.addEpubType(li, "biblioentry");
		ul.appendChild(li);
		currentPar = converter.getTextCv().createParagraph(li, sStyleName, false);
	}
	
	@Override protected void insertBibliographyItemElement(String sStyleName, String sText) {
		if (sStyleName!=null) {
			converter.getTextCv().createInline(currentPar, sStyleName).appendChild(converter.createTextNode(sText));
		}
		else {
			currentPar.appendChild(converter.createTextNode(sText));			
		}
	}

}
