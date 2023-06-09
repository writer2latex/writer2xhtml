/************************************************************************
 *
 *	EndnoteConverter.java
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
 *  Copyright: 2002-2015 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6 (2015-06-12)
 *
 */
package writer2xhtml.xhtml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.office.OfficeReader;

class EndnoteConverter extends NoteConverter {
	
    EndnoteConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,ofr.getEndnotesConfiguration());
    }
    
    /** Insert all the endnotes
     * 
     * @param hnode a block HTML element to contain the endnotes
     */
    void insertEndnotes(Node hnode) {
        if (hasNotes()) {
        	if (config.getXhtmlSplitLevel()>0) { hnode = converter.nextOutFile(); }
        	Element section = createNoteSection(hnode, "rearnotes");
        	insertNoteHeading(section, config.getEndnotesHeading(), "endnotes");
        	flushNotes(section,"rearnote");
        }
    }
}
