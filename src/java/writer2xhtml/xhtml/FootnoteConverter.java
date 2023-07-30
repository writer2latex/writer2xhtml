/************************************************************************
 *
 *	FootnoteConverter.java
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
 *  Version 1.7.1 (2023-07-30)
 *
 */
package writer2xhtml.xhtml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.PropertySet;
import writer2xhtml.office.XMLString;
import writer2xhtml.xhtml.l10n.L10n;

class FootnoteConverter extends NoteConverter {
	
    // Footnote position (can be page or document)
    private boolean bFootnotesAtPage = true;

    FootnoteConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,ofr.getFootnotesConfiguration());
        PropertySet configuration=ofr.getFootnotesConfiguration();
        if (configuration!=null) {
        	bFootnotesAtPage = !"document".equals(configuration.getProperty(XMLString.TEXT_FOOTNOTES_POSITION));
        }
    }
    
    /** Insert the footnotes gathered so far. Export will only happen if the source document configures footnotes
     *  per page, or if this is the final call of the method.
     * 
     * @param hnode a block HTML element to contain the footnotes
     * @param bFinal true if this is the final call
     */
    void insertFootnotes(Node hnode, boolean bFinal) {
        if (hasNotes()) {
        	if (bFootnotesAtPage) {
        		Element section = createNoteSection(hnode, "footnotes");

        		// Add footnote rule
        		Element rule = converter.createElement("hr");
        		StyleInfo info = new StyleInfo();
        		getPageSc().applyFootnoteRuleStyle(info);
        		getPageSc().applyStyle(info, rule);
        		section.appendChild(rule);

        		flushNotes(section,"footnote");
        	}
        	else if (bFinal) {
        		// New page if required for footnotes as endnotes
            	String sFileTitle = config.getFootnotesHeading().length()>0 ? config.getFootnotesHeading() : converter.getL10n().get(L10n.FOOTNOTES); 
            	if (config.getXhtmlSplitLevel()>0) { hnode = converter.nextOutFile("",sFileTitle,1); }
        		Element section = createNoteSection(hnode, "footnotes");
        		insertNoteHeading(section, config.getFootnotesHeading(), "footnotes");        	
        		flushNotes(section,"footnote");
        	}
        }
    }

}
