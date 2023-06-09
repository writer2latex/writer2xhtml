/************************************************************************
 *
 *  BibMark.java
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
 *  Version 1.6 (2014-11-24) 
 *
 */

package writer2xhtml.office;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

import writer2xhtml.util.Misc;

/** This class represents a single bibliography-mark in an ODF document
 */
public final class BibMark {
	
    /** Entry types in an ODF bibliography marks. These are more or less modeled on BibTeX with
     *  the following exceptions: organizations is organization in BibTeX, report_type is report in BibTeX,
     *  the BibTeX fields crossref and key are missing in ODF, the ODF fields
     *  url, custom1, custom2, custom3, custom4, custom5, isbn are not standard in BibTeX 
     * 
     */
    public enum EntryType { address, annote, author, booktitle, chapter, 
		edition, editor, howpublished, institution, journal, month,
		note, number, organizations, pages, publisher, school, series,
		title, report_type, volume, year,
		url, custom1, custom2, custom3, custom4, custom5, isbn
	}
	
    // The data of the bibliography mark
    private String sIdentifier;
    private String sEntryType;
    private Map<EntryType,String> fields = new HashMap<EntryType,String>();
	
    /** Create a new BibMark from scratch.
     *  @param sIdentifier the unique identifier for this BibMark
     *  @param sEntryType the type of entry such as book or article
     */
    public BibMark(String sIdentifier, String sEntryType) {
        this.sIdentifier = sIdentifier;
        this.sEntryType = sEntryType;
    }

    /** Create a new <code>BibMark</code> from a text:bibliography-mark node.
     */
    public BibMark(Node node) {
        sIdentifier = Misc.getAttribute(node,XMLString.TEXT_IDENTIFIER);
        sEntryType = Misc.getAttribute(node,XMLString.TEXT_BIBLIOGRAPHY_TYPE);
        if (sEntryType==null) { // bug in OOo 1.0!
            sEntryType = Misc.getAttribute(node,XMLString.TEXT_BIBILIOGRAPHIC_TYPE);
        }
        fields.put(EntryType.address, Misc.getAttribute(node,XMLString.TEXT_ADDRESS));  
        fields.put(EntryType.annote, Misc.getAttribute(node,XMLString.TEXT_ANNOTE));  
        fields.put(EntryType.author, Misc.getAttribute(node,XMLString.TEXT_AUTHOR));
        fields.put(EntryType.booktitle, Misc.getAttribute(node,XMLString.TEXT_BOOKTITLE));
        fields.put(EntryType.chapter, Misc.getAttribute(node,XMLString.TEXT_CHAPTER));
        fields.put(EntryType.edition, Misc.getAttribute(node,XMLString.TEXT_EDITION));
        fields.put(EntryType.editor, Misc.getAttribute(node,XMLString.TEXT_EDITOR));
        fields.put(EntryType.howpublished, Misc.getAttribute(node,XMLString.TEXT_HOWPUBLISHED));
        fields.put(EntryType.institution, Misc.getAttribute(node,XMLString.TEXT_INSTITUTION));
        fields.put(EntryType.journal, Misc.getAttribute(node,XMLString.TEXT_JOURNAL));
        fields.put(EntryType.month, Misc.getAttribute(node,XMLString.TEXT_MONTH));
        fields.put(EntryType.note, Misc.getAttribute(node,XMLString.TEXT_NOTE));
        fields.put(EntryType.number, Misc.getAttribute(node,XMLString.TEXT_NUMBER));
        fields.put(EntryType.organizations, Misc.getAttribute(node,XMLString.TEXT_ORGANIZATIONS));
        fields.put(EntryType.pages, Misc.getAttribute(node,XMLString.TEXT_PAGES));
        fields.put(EntryType.publisher, Misc.getAttribute(node,XMLString.TEXT_PUBLISHER));
        fields.put(EntryType.school, Misc.getAttribute(node,XMLString.TEXT_SCHOOL));
        fields.put(EntryType.series, Misc.getAttribute(node,XMLString.TEXT_SERIES));
        fields.put(EntryType.title, Misc.getAttribute(node,XMLString.TEXT_TITLE));
        fields.put(EntryType.report_type, Misc.getAttribute(node,XMLString.TEXT_REPORT_TYPE));
        fields.put(EntryType.volume, Misc.getAttribute(node,XMLString.TEXT_VOLUME));
        fields.put(EntryType.year, Misc.getAttribute(node,XMLString.TEXT_YEAR));
        fields.put(EntryType.url, Misc.getAttribute(node,XMLString.TEXT_URL));
        fields.put(EntryType.custom1, Misc.getAttribute(node,XMLString.TEXT_CUSTOM1));
        fields.put(EntryType.custom2, Misc.getAttribute(node,XMLString.TEXT_CUSTOM2));
        fields.put(EntryType.custom3, Misc.getAttribute(node,XMLString.TEXT_CUSTOM3));
        fields.put(EntryType.custom4, Misc.getAttribute(node,XMLString.TEXT_CUSTOM4));
        fields.put(EntryType.custom5, Misc.getAttribute(node,XMLString.TEXT_CUSTOM5));
        fields.put(EntryType.isbn, Misc.getAttribute(node,XMLString.TEXT_ISBN));
    }
	
    /** Get the identifier.
     * 
     *  @return the unique identifier of this <code>BibMark</code>
     */
    public String getIdentifier() { return sIdentifier; }
	
    /** Get the entry type.
     * 
     *  @return the entry type of this <code>BibMark</code>
     */
    public String getEntryType() { return sEntryType; }
	
    /** Set a specific field.
     * 
     *  @param entryType the type of field to set
     *  @param sValue the new value of the field
     */
    public void setField(EntryType entryType,String sValue) { fields.put(entryType, sValue); }

    /** Return a specific field.
     * 
     *  @param entryType the type of the field to get
     *  @return the value of the field, or null if the field is not set
     */
    public String getField(EntryType entryType) {
    	return fields.containsKey(entryType) ? fields.get(entryType) : null;
    }
}