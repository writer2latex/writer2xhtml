/************************************************************************
 *
 *  OfficeReader.java
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
 *  Copyright: 2002-2022 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2022-08-17)
 *
 */

package writer2xhtml.office;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Text;

import writer2xhtml.util.Misc;

/** <p> This class reads and collects global information about an OOo document.
  * This includes styles, forms, information about indexes and references etc.
  * </p> 
  */
public class OfficeReader {

    ///////////////////////////////////////////////////////////////////////////
    // Static methods
	
    /** Checks, if a node is an element in the text namespace
     *  @param node the node to check
     *  @return true if this is a text element
     */
    public static boolean isTextElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               node.getNodeName().startsWith(XMLString.TEXT_);
    }

    /** Checks, if a node is an element in the table namespace
     *  @param node the node to check
     *  @return true if this is a table element
     */
    public static boolean isTableElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               node.getNodeName().startsWith(XMLString.TABLE_);
    }

    /** Checks, if a node is an element in the draw namespace
     *  @param node the node to check
     *  @return true if this is a draw element
     */
    public static boolean isDrawElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               node.getNodeName().startsWith(XMLString.DRAW_);
    }
	
    /** Checks, if a node is an element representing a note (footnote/endnote)
     *  @param node the node to check
     *  @return true if this is a note element
     */
    public static boolean isNoteElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE &&
               ( node.getNodeName().equals(XMLString.TEXT_NOTE)     ||
                 node.getNodeName().equals(XMLString.TEXT_FOOTNOTE) ||
                 node.getNodeName().equals(XMLString.TEXT_ENDNOTE)  );
    }

    /** Get the paragraph or heading containing a node (or null if this node is not contained in a paragraph or heading)
     * 
     * @param node the node in question
     * @return the paragraph or heading or null
     */
    public static Element getParagraph(Element node) {
        Node parent = node.getParentNode();
        if (Misc.isElement(parent, XMLString.TEXT_P) || Misc.isElement(parent, XMLString.TEXT_H)) {
            return (Element) parent;
        }
        else if (Misc.isElement(parent)) {
        	return getParagraph((Element) parent);
        }
        return null;
    }

    
    /** Checks, if this node contains at most one element, and that this is a
     *  paragraph.
     *  @param node the node to check
     *  @return true if the node contains a single paragraph or nothing
     */
    public static boolean isSingleParagraph(Node node) {
        boolean bFoundPar = false;
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                if (child.getNodeName().equals(XMLString.TEXT_P)) {
                    if (bFoundPar) { return false; }
                    else { bFoundPar = true; }
                }
                else {
                    return false;
                }
            }
            child = child.getNextSibling();
        }
        return bFoundPar;
    }
	
    /** Checks, if the only text content of this node is whitespace.
     *  Other (draw) content is allowed.
     *  @param node the node to check (should be a paragraph node or a child
     *  of a paragraph node)
     *  @return true if the node contains whitespace only
     */
    public static boolean isNoTextPar(Node node) {
	    Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                if (isTextElement(child)) {
                    if (!isWhitespaceContent(child)) { return false; }
                }
            }
            else if (child.getNodeType()==Node.TEXT_NODE) {
                if (!isWhitespace(child.getNodeValue())) { return false; }
            }
            child = child.getNextSibling();
        }
        return true; // found nothing!
    }

    /** <p>Checks, if the only text content of this node is whitespace</p>
     *  @param node the node to check (should be a paragraph node or a child
     *  of a paragraph node)
     *  @return true if the node contains whitespace only
     */
    public static boolean isWhitespaceContent(Node node) {
	    Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                if (isTextElement(child)) {
                    if (!isWhitespaceContent(child)) { return false; }
                }
                else {
                    return false; // found non-text content!
                }
            }
            else if (child.getNodeType()==Node.TEXT_NODE) {
                if (!isWhitespace(child.getNodeValue())) { return false; }
            }
            child = child.getNextSibling();
        }
        return true; // found nothing!
    }
	
    /** <p>Checks, if this text is whitespace</p>
     *  @param s the String to check
     *  @return true if the String contains whitespace only
     */
    public static boolean isWhitespace(String s) {
        int nLen = s.length();
        for (int i=0; i<nLen; i++) {
            if (!Character.isWhitespace(s.charAt(i))) { return false; }
        }
        return true;
    }
	
    /** Counts the number of characters (text nodes) in this element
     *  excluding footnotes etc.
     *  @param node the node to count in
     *  @return the number of characters
     */
    public static int getCharacterCount(Node node) {
        Node child = node.getFirstChild();
        int nCount = 0;
        while (child!=null) {
            short nodeType = child.getNodeType();
               
            switch (nodeType) {
                case Node.TEXT_NODE:
                    nCount += child.getNodeValue().length();
                    break;
                        
                case Node.ELEMENT_NODE:
                    String sName = child.getNodeName();
                    if (sName.equals(XMLString.TEXT_S)) {
                        nCount += Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_C),1);
                    }
                    else if (sName.equals(XMLString.TEXT_TAB_STOP)) {
                        nCount++; // treat as single space
                    }
                    else if (sName.equals(XMLString.TEXT_TAB)) { // oasis
                        nCount++; // treat as single space
                    }
                    else if (isNoteElement(child)) {
                        // ignore
                    }
                    else if (isTextElement(child)) {
                        nCount += getCharacterCount(child);
                    }
            }
            child = child.getNextSibling();
        }
        return nCount;
    }

    public static String getTextContent(Node node) {
        String s = "";
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                s += getTextContent(child);
            }
            else if (child.getNodeType()==Node.TEXT_NODE) {
                s += child.getNodeValue();
            }
            child = child.getNextSibling();
        }
        return s;
    }
	
    /** Return the next character in logical order
     */
    public static char getNextChar(Node node) {
        Node next = node;
        do {
            next = getNextNode(next);
            if (next!=null && next.getNodeType()==Node.TEXT_NODE &&
                next.getNodeValue().length()>0) {
                // Found the next character!
                return next.getNodeValue().charAt(0);
            }
            //else if (next!=null && next.getNodeType()==Node.ELEMENT_NODE &&
                //XMLString.TEXT_S.equals(next.getNodeName())) {
                // Next character is a space (first of several)
                //return ' ';
            //}
        } while (next!=null);
        // No more text in this paragraph!
        return '\u0000';
    }

    // Return the next node of *this paragraph* in logical order
    // (Parents before children, siblings from left to right)
    // Do not descend into draw elements and footnotes/endnotes
    private static Node getNextNode(Node node) {
        // If element node: Next node is first child
        if (node.getNodeType()==Node.ELEMENT_NODE && node.hasChildNodes() &&
            !isDrawElement(node) && !isNoteElement(node)) {
            return node.getFirstChild();
        }
        // else iterate for next node, but don't leave this paragraph
        Node next = node;
        do {
            // First look for next sibling
            if (next.getNextSibling()!=null) { return next.getNextSibling(); }
            // Then move to parent, if this is the text:p node, we are done
            next = next.getParentNode();
            if (next!=null && next.getNodeType()==Node.ELEMENT_NODE &&
                next.getNodeName().equals(XMLString.TEXT_P)) {
                return null;
            }
        } while (next!=null);
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Data
	
    // The Document
    private OfficeDocument oooDoc = null;

    // Font declarations
    private OfficeStyleFamily font = new OfficeStyleFamily(FontDeclaration.class);

    // Styles
    private OfficeStyleFamily text = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily par = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily section = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily table = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily column = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily row = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily cell = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily frame = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily presentation = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily drawingPage = new OfficeStyleFamily(StyleWithProperties.class);
    private OfficeStyleFamily list = new OfficeStyleFamily(ListStyle.class);
    private OfficeStyleFamily pageLayout = new OfficeStyleFamily(PageLayout.class);
    private OfficeStyleFamily masterPage = new OfficeStyleFamily(MasterPage.class);

    // Document-wide styles
    private ListStyle outline = new ListStyle();
    private PropertySet footnotes = null;
    private PropertySet endnotes = null;
    
    // Bibliographic data
    private Element bibliographyConfiguration = null;
    private List<Element> bibliographyMarks = new ArrayList<Element>();
	
    // Special styles
    private StyleWithProperties[] heading = new StyleWithProperties[11];
    private MasterPage firstMasterPage = null;
    //private String sFirstMasterPageName = null;
	
    // All indexes
    private Map<Element, Object> indexes = new Hashtable<Element, Object>();
    private Set<String> indexSourceStyles = new HashSet<String>();
    private Set<String> figureSequenceNames = new HashSet<String>();
    private Set<String> tableSequenceNames = new HashSet<String>();
    private String sAutoFigureSequenceName = null;
    private String sAutoTableSequenceName = null;
	
    // Map paragraphs to sequence names (caption helper)
    private Map<Element, String> sequenceNames = new Hashtable<Element, String>();
	
    // Map sequence reference names to sequence names
    private Map<String, String> seqrefNames = new Hashtable<String, String>();
	
    // All references
    private Set<String> footnoteRef = new HashSet<String>();
    private Set<String> endnoteRef = new HashSet<String>();
    private Set<String> referenceRef = new HashSet<String>();
    private Set<String> bookmarkRef = new HashSet<String>();
    private Set<String> sequenceRef = new HashSet<String>();
	
    // Reference marks and bookmarks contained in headings or lists
    private Map<String,Integer> referenceHeading = new HashMap<String,Integer>();
    private Map<String,Integer> bookmarkHeading = new HashMap<String,Integer>();
    private Map<String,String> bookmarkList = new HashMap<String,String>();
    private Map<String,Integer> bookmarkListLevel = new HashMap<String,Integer>();
	
    // All internal hyperlinks
    private Set<String> links = new HashSet<String>();
	
    // Forms
    private FormsReader forms = new FormsReader();
	
    // The main content element
    private Element content = null;
    
    // The first image in the document
    private Element firstImage = null;
	
    // Identify OASIS OpenDocument format
    private boolean bOpenDocument = false;

    // Identify individual genres
    private boolean bText = false;
    private boolean bSpreadsheet = false;
    private boolean bPresentation = false;	
    
    ///////////////////////////////////////////////////////////////////////////
    // Various methods
    
    /** Checks whether or not this document is in package format
     *  @return true if it's in package format
     */
    public boolean isPackageFormat() { return oooDoc.isPackageFormat(); }
	
    /** Checks whether this url is internal to the package
     *  @param sUrl the url to check
     *  @return true if the url is internal to the package
     */
    public boolean isInPackage(String sUrl) {
        if (!bOpenDocument && sUrl.startsWith("#")) { return true; } // old format
        if (sUrl.startsWith("./")) { sUrl=sUrl.substring(2); }
        return oooDoc.getEmbeddedObject(sUrl)!=null; 
    } 
    
    /** In OpenDocument package format ../ means "leave the package".
     *  Consequently this prefix must be removed to obtain a valid link 
     *  
     * @param sLink
     * @return the corrected link
     */
    public String fixRelativeLink(String sLink) {
        if (isOpenDocument() && isPackageFormat() && sLink.startsWith("../")) {
            return sLink.substring(3);
        }
        return sLink;
    }
	
    ///////////////////////////////////////////////////////////////////////////
    // Accessor methods
    
    /** Get an embedded object in this office document
     * 
     */
	public EmbeddedObject getEmbeddedObject(String sName) {
		return oooDoc.getEmbeddedObject(sName);
	}
    
    /** <p>Get the collection of all font declarations.</p>
     *  @return the <code>OfficeStyleFamily</code> of font declarations   
     */
    public OfficeStyleFamily getFontDeclarations() { return font; }
	
    /** <p>Get a specific font declaration</p>
     * @param sName the name of the font declaration
     * @return a <code>FontDeclaration</code> representing the font
     */
    public FontDeclaration getFontDeclaration(String sName) {
        return (FontDeclaration) font.getStyle(sName);
    }

    // Accessor methods for styles
    public OfficeStyleFamily getTextStyles() { return text; }
    public StyleWithProperties getTextStyle(String sName) {
        return (StyleWithProperties) text.getStyle(sName);
    }

    public OfficeStyleFamily getParStyles() { return par; }
    public StyleWithProperties getParStyle(String sName) {
        return (StyleWithProperties) par.getStyle(sName);
    }
    public StyleWithProperties getDefaultParStyle() {
        return (StyleWithProperties) par.getDefaultStyle();
    }

    public OfficeStyleFamily getSectionStyles() { return section; }
    public StyleWithProperties getSectionStyle(String sName) {
        return (StyleWithProperties) section.getStyle(sName);
    }

    public OfficeStyleFamily getTableStyles() { return table; }
    public StyleWithProperties getTableStyle(String sName) {
        return (StyleWithProperties) table.getStyle(sName);
    }
    public OfficeStyleFamily getColumnStyles() { return column; }
    public StyleWithProperties getColumnStyle(String sName) {
        return (StyleWithProperties) column.getStyle(sName);
    }

    public OfficeStyleFamily getRowStyles() { return row; }
    public StyleWithProperties getRowStyle(String sName) {
        return (StyleWithProperties) row.getStyle(sName);
    }

    public OfficeStyleFamily getCellStyles() { return cell; }
    public StyleWithProperties getCellStyle(String sName) {
        return (StyleWithProperties) cell.getStyle(sName);
    }
    public StyleWithProperties getDefaultCellStyle() {
        return (StyleWithProperties) cell.getDefaultStyle();
    }

    public OfficeStyleFamily getFrameStyles() { return frame; }
    public StyleWithProperties getFrameStyle(String sName) {
        return (StyleWithProperties) frame.getStyle(sName);
    }
    public StyleWithProperties getDefaultFrameStyle() {
        return (StyleWithProperties) frame.getDefaultStyle();
    }

    public OfficeStyleFamily getPresentationStyles() { return presentation; }
    public StyleWithProperties getPresentationStyle(String sName) {
        return (StyleWithProperties) presentation.getStyle(sName);
    }
    public StyleWithProperties getDefaultPresentationStyle() {
        return (StyleWithProperties) presentation.getDefaultStyle();
    }

    public OfficeStyleFamily getDrawingPageStyles() { return drawingPage; }
    public StyleWithProperties getDrawingPageStyle(String sName) {
        return (StyleWithProperties) drawingPage.getStyle(sName);
    }
    public StyleWithProperties getDefaultDrawingPageStyle() {
        return (StyleWithProperties) drawingPage.getDefaultStyle();
    }

    public OfficeStyleFamily getListStyles() { return list; }
    public ListStyle getListStyle(String sName) {
        return (ListStyle) list.getStyle(sName);
    }
	
    public OfficeStyleFamily getPageLayouts() { return pageLayout; }
    public PageLayout getPageLayout(String sName) {
        return (PageLayout) pageLayout.getStyle(sName);
    }
	
    public OfficeStyleFamily getMasterPages() { return masterPage; }
    public MasterPage getMasterPage(String sName) {
        return (MasterPage) masterPage.getStyle(sName);
    }
	
    public ListStyle getOutlineStyle() { return outline; }
	
    public PropertySet getFootnotesConfiguration() { return footnotes; }
	
    public PropertySet getEndnotesConfiguration() { return endnotes; }
	
    /** <p>Returns the paragraph style associated with headings of a specific
     *  level. Returns <code>null</code> if no such style is known.
     *  <p>In principle, different styles can be used for each heading, in
     *  practice the same (soft) style is used for all headings of a specific
     *  level.
     *  @param nLevel the level of the heading
     *  @return a <code>StyleWithProperties</code> object representing the style
     */
    public StyleWithProperties getHeadingStyle(int nLevel) {
        return 1<=nLevel && nLevel<=10 ? heading[nLevel] : null;
    }
	
    /** <p>Returns the first master page used in the document. If no master
     *  page is used explicitly, the first master page found in the styles is
     *  returned. Returns null if no master pages exists.
     *  @return a <code>MasterPage</code> object representing the master page
     */
    public MasterPage getFirstMasterPage() { return firstMasterPage; }
	
    /** Return the iso language used in most paragaph styles (in a well-structured
     * document this will be the default language)
     * TODO: Base on content rather than style 
     * @return the iso language
     */ 
    public String getMajorityLanguage() {
        Hashtable<String, Integer> langs = new Hashtable<String, Integer>();

        // Read the default language from the default paragraph style
        String sDefaultLang = null;
        StyleWithProperties style = getDefaultParStyle();
        if (style!=null) { 
            sDefaultLang = style.getProperty(XMLString.FO_LANGUAGE);
        }

        // Collect languages from paragraph styles
        Enumeration<OfficeStyle> enumeration = getParStyles().getStylesEnumeration();
        while (enumeration.hasMoreElements()) {
            style = (StyleWithProperties) enumeration.nextElement();
            String sLang = style.getProperty(XMLString.FO_LANGUAGE);
            if (sLang==null) { sLang = sDefaultLang; }
            if (sLang!=null) {
                int nCount = 1;
                if (langs.containsKey(sLang)) {
                    nCount = langs.get(sLang).intValue()+1;
                }
                langs.put(sLang,Integer.valueOf(nCount));
            }
        }
		
        // Find the most common language
        int nMaxCount = 0;
        String sMajorityLanguage = null;
        Enumeration<String> langenum = langs.keys();
        while (langenum.hasMoreElements()) {
            String sLang = langenum.nextElement();
            int nCount = langs.get(sLang).intValue();
            if (nCount>nMaxCount) {
                nMaxCount = nCount;
                sMajorityLanguage = sLang;
            }
        }
        return sMajorityLanguage;        
    }

	
    /** <p>Returns a reader for a specific toc
     *  @param onode the <code>text:table-of-content-node</code>
     *  @return the reader, or null
     */
    public TocReader getTocReader(Element onode) {
        if (indexes.containsKey(onode)) { return (TocReader) indexes.get(onode); }
        else { return null; }
    } 
	
    /** <p>Is this style used in some toc as an index source style?</p>
     *  @param sStyleName the name of the style
     *  @return true if this is an index source style
     */
    public boolean isIndexSourceStyle(String sStyleName) {
        return indexSourceStyles.contains(sStyleName);
    }
    
    /** Get the text:bibliography-configuration element
     * 
     * @return the bibliography configuration
     */
    public Element getBibliographyConfiguration() {
    	return bibliographyConfiguration;
    }
	
    /** <p>Does this sequence name belong to a lof?</p>
     *  @param sName the name of the sequence
     *  @return true if it belongs to an index
     */
    public boolean isFigureSequenceName(String sName) {
        return figureSequenceNames.contains(sName);
    }
	
    /** <p>Does this sequence name belong to a lot?</p>
     *  @param sName the name of the sequence
     *  @return true if it belongs to an index
     */
    public boolean isTableSequenceName(String sName) {
        return tableSequenceNames.contains(sName);
    }
	
    /** <p>Add a sequence name for table captions.</p>
     *  <p>OpenDocument has a very weak notion of table captions: A caption is a
     *  paragraph containing a text:sequence element. Moreover, the only source
     *  to identify which sequence number to use is the list(s) of tables.
     *  If there's no list of tables, captions cannot be identified.
     *  Thus this method lets the user add a sequence name to identify the
     *  table captions.
     *  @param sName the name to add
     */
    public void addTableSequenceName(String sName) {
        tableSequenceNames.add(sName);
    }
	
    /** <p>Add a sequence name for figure captions.</p>
     *  <p>OpenDocument has a very weak notion of figure captions: A caption is a
     *  paragraph containing a text:sequence element. Moreover, the only source
     *  to identify which sequence number to use is the list(s) of figures.
     *  If there's no list of figures, captions cannot be identified.
     *  Thus this method lets the user add a sequence name to identify the
     *  figure captions.
     *  @param sName the name to add
     */
    public void addFigureSequenceName(String sName) {
        figureSequenceNames.add(sName);
    }
    /** <p>Get the sequence name associated with a paragraph</p>
     *  @param par the paragraph to look up
     *  @return the sequence name or null
     */
    public String getSequenceName(Element par) {
        return sequenceNames.containsKey(par) ? sequenceNames.get(par) : null;
    }
	
    /** <p>Get the sequence name associated with a reference name</p>
     *  @param sRefName the reference name to use
     *  @return the sequence name or null
     */
    public String getSequenceFromRef(String sRefName) {
        return seqrefNames.get(sRefName);
    }
	
	
    /** <p>Is there a reference to this note id?
     *  @param sId the id of the note
     *  @return true if there is a reference
     */
    public boolean hasNoteRefTo(String sId) {
        return footnoteRef.contains(sId) || endnoteRef.contains(sId);
    }

    /** <p>Is there a reference to this footnote id?
     *  @param sId the id of the footnote
     *  @return true if there is a reference
     */
    public boolean hasFootnoteRefTo(String sId) {
        return footnoteRef.contains(sId);
    }

    /** <p>Is there a reference to this endnote?
     *  @param sId the id of the endnote
     *  @return true if there is a reference
     */
    public boolean hasEndnoteRefTo(String sId) {
        return endnoteRef.contains(sId);
    }

    /** Is this reference mark contained in a heading?
     *  @param sName the name of the reference mark 
     *  @return true if so
     */
    public boolean referenceMarkInHeading(String sName) {
        return referenceHeading.containsKey(sName);
    }

    /** Is there a reference to this reference mark?
     *  @param sName the name of the reference mark 
     *  @return true if there is a reference
     */
    public boolean hasReferenceRefTo(String sName) {
        return referenceRef.contains(sName);
    }

    /** Is this bookmark contained in a heading?
     *  @param sName the name of the bookmark 
     *  @return true if so
     */
    public boolean bookmarkInHeading(String sName) {
        return bookmarkHeading.containsKey(sName);
    }
    
    /** Get the level of the heading associated with this bookmark
     *  @param sName the name of the bookmark 
     *  @return the level or 0 if the bookmark does not exist
     */
    public int getBookmarkHeadingLevel(String sName) {
        return bookmarkHeading.get(sName);
    }
    
    /** Is this bookmark contained in a list?
     *  @param sName the name of the bookmark
     *  @return true if so
     */
    public boolean bookmarkInList(String sName) {
    	return bookmarkList.containsKey(sName);
    }
    
    /** Get the list style name associated with a bookmark in a list
     *  @param sName the name of the bookmark
     *  @return the list style name or null if the bookmark does not exist or the list does not have a style name
     */
    public String getBookmarkListStyle(String sName) {
    	if (bookmarkList.containsKey(sName)) {
    		return bookmarkList.get(sName);
    	}
    	else {
    		return null;
    	}
    }

    /** Get the list level associated with a bookmark in a list
     *  @param sName the name of the bookmark
     *  @return the level or 0 if the bookmark does not exist
     */
    public int getBookmarkListLevel(String sName) {
    	if (bookmarkListLevel.containsKey(sName)) {
    		return bookmarkListLevel.get(sName);
    	}
    	else {
    		return 0;
    	}
    }

    /** <p>Is there a reference to this bookmark?
     *  @param sName the name of the bookmark
     *  @return true if there is a reference
     */
    public boolean hasBookmarkRefTo(String sName) {
        return bookmarkRef.contains(sName);
    }
    
    /** Get the raw list of all text:bibliography-mark elements. The marks are returned in document order and
     *  includes any duplicates
     * 
     * @return the list
     */
    public List<Element> getBibliographyMarks() {
    	return bibliographyMarks;
    }

    /** <p>Is there a reference to this sequence field?
     *  @param sId the id of the sequence field
     *  @return true if there is a reference
     */
    public boolean hasSequenceRefTo(String sId) {
        return sequenceRef.contains(sId);
    }

    /** <p>Is there a link to this sequence anchor name?
     *  @param sName the name of the anchor
     *  @return true if there is a link
     */
    public boolean hasLinkTo(String sName) {
        return links.contains(sName);
    }
	
    /** <p>Is this an OASIS OpenDocument or an OOo 1.0 document?
     *  @return true if it's an OASIS OpenDocument
     */
    public boolean isOpenDocument() { return bOpenDocument; }
	
    /** <p>Is this an text document?
     *  @return true if it's a text document
     */
    public boolean isText() { return bText; }

    /** <p>Is this a spreadsheet document?
     *  @return true if it's a spreadsheet document
     */
    public boolean isSpreadsheet() { return bSpreadsheet; }

    /** <p>Is this a presentation document?
     *  @return true if it's a presentation document
     */
    public boolean isPresentation() { return bPresentation; }

    /** <p>Get the content element</p>
     *  <p>In the old file format this means the <code>office:body</code> element
     *  <p>In the OpenDocument format this means a <code>office:text</code>,
     *     <code>office:spreadsheet</code> or <code>office:presentation</code>
     *     element.</p>
     *  @return the content <code>Element</code>
     */
    public Element getContent() {
        return content;
    }
	
    /** <p>Get the forms belonging to this document.</p>
     *  @return a <code>FormsReader</code> representing the forms
     */
    public FormsReader getForms() { return forms; }
	
    /** <p>Read a table from a table:table node</p>
     *  @param node the table:table Element node
     *  @return a <code>TableReader</code> object representing the table
     */
    public TableReader getTableReader(Element node) {
        return new TableReader(this,node);
    }
    
    /** Get the very first image in this document, if any 
     * 
     *  @return the first image, or null if no images exists
     */
    public Element getFirstImage() {
    	return firstImage;
    }

    /** Constructor; read a document */
    public OfficeReader(OfficeDocument oooDoc, boolean bAllParagraphsAreSoft, boolean bDestructive) {
        this.oooDoc = oooDoc;
        loadStylesFromDOM(oooDoc.getStyleDOM(),oooDoc.getContentDOM(),bAllParagraphsAreSoft);
        loadContentFromDOM(oooDoc.getContentDOM(),bDestructive);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers
	
    /*private void collectMasterPage(StyleWithProperties style) {
        if (style==null || firstMasterPage!=null) { return; }
        String s = style.getMasterPageName();
        if (s!=null && s.length()>0) {
            firstMasterPage = getMasterPage(s);
        }
    }*/

    private void loadStylesFromDOM(Node node, boolean bAllParagraphsAreSoft) {
        // node should be office:master-styles, office:styles or office:automatic-styles
        boolean bAutomatic = XMLString.OFFICE_AUTOMATIC_STYLES.equals(node.getNodeName());
        if (node.hasChildNodes()){
            NodeList nl = node.getChildNodes();
            int nLen = nl.getLength();
            for (int i = 0; i < nLen; i++ ) {                                   
                Node child=nl.item(i);
                if (child.getNodeType()==Node.ELEMENT_NODE){
                    if (child.getNodeName().equals(XMLString.STYLE_STYLE)){
                        String sFamily = Misc.getAttribute(child,XMLString.STYLE_FAMILY);
                        if ("text".equals(sFamily)){ 
                            text.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("paragraph".equals(sFamily)){ 
                            par.loadStyleFromDOM(child,bAutomatic && !bAllParagraphsAreSoft); 
                        }
                        else if ("section".equals(sFamily)){ 
                            section.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table".equals(sFamily)){ 
                            table.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table-column".equals(sFamily)){
                            column.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table-row".equals(sFamily)){
                            row.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("table-cell".equals(sFamily)){
                            cell.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("graphics".equals(sFamily)){
                            frame.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("graphic".equals(sFamily)){ // oasis
                            frame.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("presentation".equals(sFamily)){
                            presentation.loadStyleFromDOM(child,bAutomatic); 
                        }
                        else if ("drawing-page".equals(sFamily)){
                            // Bug in OOo 1.x: The same name may be used for a real and an automatic style...
                            if (drawingPage.getStyle(Misc.getAttribute(child,XMLString.STYLE_NAME))==null) {
                                drawingPage.loadStyleFromDOM(child,bAutomatic);
                            }
                        }
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_PAGE_MASTER)) { // old
                        pageLayout.loadStyleFromDOM(child,bAutomatic);
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_PAGE_LAYOUT)) { // oasis
                        pageLayout.loadStyleFromDOM(child,bAutomatic);
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_MASTER_PAGE)) {
                        masterPage.loadStyleFromDOM(child,bAutomatic);
                        if (firstMasterPage==null) {
                            firstMasterPage = (MasterPage) masterPage.getStyle(Misc.getAttribute(child,XMLString.STYLE_NAME));
                        }
                    }
                    else if (child.getNodeName().equals(XMLString.TEXT_LIST_STYLE)) {
                        list.loadStyleFromDOM(child,bAutomatic);
                    }
                    else if (child.getNodeName().equals(XMLString.TEXT_OUTLINE_STYLE)) {
                        outline.loadStyleFromDOM(child);
                    }
                    else if (child.getNodeName().equals(XMLString.STYLE_DEFAULT_STYLE)){
                        String sFamily = Misc.getAttribute(child,XMLString.STYLE_FAMILY);
                        if ("paragraph".equals(sFamily)) {
                            StyleWithProperties defaultPar = new StyleWithProperties();
                            defaultPar.loadStyleFromDOM(child);
                            par.setDefaultStyle(defaultPar);
                        }
                        else if ("graphics".equals(sFamily) || "graphic".equals(sFamily)) { // oasis: no s
                            StyleWithProperties defaultFrame = new StyleWithProperties();
                            defaultFrame.loadStyleFromDOM(child);
                            frame.setDefaultStyle(defaultFrame);
                        }
                        else if ("table-cell".equals(sFamily)) {
                            StyleWithProperties defaultCell = new StyleWithProperties();
                            defaultCell.loadStyleFromDOM(child);
                            cell.setDefaultStyle(defaultCell);
                        }
                    }
                }
            }
        }                                            
    }
	
    private void loadStylesFromDOM(Document stylesDOM, Document contentDOM, boolean bAllParagraphsAreSoft){
        // Flat xml: stylesDOM will be null and contentDOM contain everything
        // This is only the case for old versions of xmerge; newer versions
        // creates DOM for styles, content, meta and settings.
        NodeList list;

        // font declarations: Try old format first
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.OFFICE_FONT_DECLS);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_FONT_DECLS);
        }
        // If that fails, try oasis format
        if (list.getLength()==0) {
            if (stylesDOM==null) {
                list = contentDOM.getElementsByTagName(XMLString.OFFICE_FONT_FACE_DECLS);
            }
            else {
                list = stylesDOM.getElementsByTagName(XMLString.OFFICE_FONT_FACE_DECLS);
            }
        }
		
        if (list.getLength()!=0) {
            Node node = list.item(0);
            if (node.hasChildNodes()){
                NodeList nl = node.getChildNodes();
                int nLen = nl.getLength();
                for (int i = 0; i < nLen; i++ ) {                                   
                    Node child = nl.item(i);
                    if (child.getNodeType()==Node.ELEMENT_NODE){
                        if (child.getNodeName().equals(XMLString.STYLE_FONT_DECL)){
                            font.loadStyleFromDOM(child,false);
                        }
                        else if (child.getNodeName().equals(XMLString.STYLE_FONT_FACE)){
                            font.loadStyleFromDOM(child,false);
                        }
                    }
                } 
            }
        }

        // soft formatting:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.OFFICE_STYLES);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_STYLES);
        }
        if (list.getLength()!=0) {
            loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
        }
		
        // master styles:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.OFFICE_MASTER_STYLES);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_MASTER_STYLES);
        }
        if (list.getLength()!=0) {
            loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
        }
    
        // hard formatting:
        // Load from styles.xml first. Problem: There may be name clashes
        // with automatic styles from content.xml
        if (stylesDOM!=null) {
            list = stylesDOM.getElementsByTagName(XMLString.OFFICE_AUTOMATIC_STYLES);
            if (list.getLength()!=0) {
                loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
            }
        }	
        list = contentDOM.getElementsByTagName(XMLString.OFFICE_AUTOMATIC_STYLES);
        if (list.getLength()!=0) {
            loadStylesFromDOM(list.item(0),bAllParagraphsAreSoft);
        }

        // footnotes configuration:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.TEXT_FOOTNOTES_CONFIGURATION);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.TEXT_FOOTNOTES_CONFIGURATION);
        }
        if (list.getLength()!=0) {
            footnotes = new PropertySet();
            footnotes.loadFromDOM(list.item(0));
        }
		
        // endnotes configuration:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.TEXT_ENDNOTES_CONFIGURATION);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.TEXT_ENDNOTES_CONFIGURATION);
        }
        if (list.getLength()!=0) {
            endnotes = new PropertySet();
            endnotes.loadFromDOM(list.item(0));
        }
		
        // if it failed, try oasis format
        if (footnotes==null || endnotes==null) {
            if (stylesDOM==null) {
                list = contentDOM.getElementsByTagName(XMLString.TEXT_NOTES_CONFIGURATION);
            }
            else {
                list = stylesDOM.getElementsByTagName(XMLString.TEXT_NOTES_CONFIGURATION);
            }
            int nLen = list.getLength();
            for (int i=0; i<nLen; i++) {
                String sClass = Misc.getAttribute(list.item(i),XMLString.TEXT_NOTE_CLASS);
                if ("endnote".equals(sClass)) {
                    endnotes = new PropertySet();
                    endnotes.loadFromDOM(list.item(i));
                }
                else {
                    footnotes = new PropertySet();
                    footnotes.loadFromDOM(list.item(i));
                }
            }
       }
        
        // bibliography configuration:
        if (stylesDOM==null) {
            list = contentDOM.getElementsByTagName(XMLString.TEXT_BIBLIOGRAPHY_CONFIGURATION);
        }
        else {
            list = stylesDOM.getElementsByTagName(XMLString.TEXT_BIBLIOGRAPHY_CONFIGURATION);
        }
        if (list.getLength()!=0) {
            bibliographyConfiguration = (Element) list.item(0);
        }

    }
	
    private void loadContentFromDOM(Document contentDOM, boolean bDestructive) {
     // Get the office:body element
        NodeList list = contentDOM.getElementsByTagName(XMLString.OFFICE_BODY);
        if (list.getLength()>0) {
            // There may be several bodies, but the first one is the main body
            Element body = (Element) list.item(0);

            // Now get the content and identify the type of document
            content = Misc.getChildByTagName(body,XMLString.OFFICE_TEXT);
            if (content!=null) { // OpenDocument Text
                bOpenDocument = true; bText = true;
            }
            else {
                content = Misc.getChildByTagName(body,XMLString.OFFICE_SPREADSHEET);
                if (content!=null) { // OpenDocument Spreadsheet
                    bOpenDocument = true; bSpreadsheet = true;
                }
                else {
                    content = Misc.getChildByTagName(body,XMLString.OFFICE_PRESENTATION);
                    if (content!=null) { // OpenDocument Presentation
                        bOpenDocument = true; bPresentation = true;
                    }
                    else {
                        content = body;
                        // OOo 1.x file format - look through content to determine genre
                        bSpreadsheet = true;
                        bPresentation = false;
                        Node child = body.getFirstChild();
                        while (child!=null) {
                            if (child.getNodeType()==Node.ELEMENT_NODE) {
                                String sName = child.getNodeName();
                                if (XMLString.TEXT_P.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_H.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_ORDERED_LIST.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_ORDERED_LIST.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.TEXT_SECTION.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                                else if (XMLString.DRAW_PAGE.equals(sName)) {
                                    bPresentation = true; bSpreadsheet = false;
                                }
                                else if (XMLString.DRAW_PAGE.equals(sName)) {
                                    bSpreadsheet = false;
                                }
                            }
                            child = child.getNextSibling();
                        }
                        bText = !bSpreadsheet && !bPresentation;
                    }
                }
            }                

            traverseContent(body,null,0,-1,bDestructive);

            if (sAutoFigureSequenceName!=null) {
                addFigureSequenceName(sAutoFigureSequenceName);
            }
            if (sAutoTableSequenceName!=null) {
                addTableSequenceName(sAutoTableSequenceName);
            }
        }

        /*if (firstMasterPage==null) {
            firstMasterPage = getMasterPage(sFirstMasterPageName);
        }*/
    }
	
    private void traverseContent(Element node, String sListStyleName, int nListLevel, int nParLevel, boolean bDestructive) {
        // Handle this node first
        String sName = node.getTagName();
        if (sName.equals(XMLString.TEXT_P)) {
        	nParLevel=0;
        	if (bDestructive) { optimize(node); }
            //collectMasterPage(getParStyle(node.getAttribute(XMLString.TEXT_STYLE_NAME)));
        }
        else if (sName.equals(XMLString.TEXT_H)) {
        	int nLevel;
        	if (node.hasAttribute(XMLString.TEXT_OUTLINE_LEVEL)) {
        		nLevel = Misc.getPosInteger(node.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
        	}
        	else {
        		nLevel = Misc.getPosInteger(node.getAttribute(XMLString.TEXT_LEVEL),1);
        	}
        	nParLevel = nLevel;
            StyleWithProperties style = getParStyle(node.getAttribute(XMLString.TEXT_STYLE_NAME));
            //collectMasterPage(style);
            if (1<=nLevel && nLevel<=10 && heading[nLevel]==null) {
                if (style!=null && style.isAutomatic()) {
                    heading[nLevel] = getParStyle(style.getParentName());
                }
                else {
                    heading[nLevel] = style;
                }
            }
        	if (bDestructive) { optimize(node); }
        }
        else if (sName.equals(XMLString.TEXT_LIST) || 
        		 sName.equals(XMLString.TEXT_ORDERED_LIST) || sName.equals(XMLString.TEXT_UNORDERED_LIST)) {
        	nListLevel++;
        	String sStyleName = Misc.getAttribute(node, XMLString.TEXT_STYLE_NAME);
        	if (sStyleName!=null) sListStyleName = sStyleName;
        }
        else if (sName.equals(XMLString.TEXT_NOTE) ||
        		 sName.equals(XMLString.TEXT_FOOTNOTE) || sName.equals(XMLString.TEXT_ENDNOTE) ||
        		 sName.equals(XMLString.TABLE_TABLE)) {
        	// Various block elements; all resetting the list and par level
        	sListStyleName=null;
        	nListLevel=0;
        	nParLevel=-1;
        }
        else if (sName.equals(XMLString.TEXT_SEQUENCE)) {
            String sSeqName = Misc.getAttribute(node,XMLString.TEXT_NAME);
            String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
            if (sSeqName!=null) {
                Element par = getParagraph(node);
                if (!sequenceNames.containsKey(par)) {
                    // Only the first text:seqence should be registered as possible caption sequence
                    sequenceNames.put(par,sSeqName);
                }
                if (sRefName!=null) {
                    seqrefNames.put(sRefName,sSeqName);
                }
            }
        }
        else if (sName.equals(XMLString.TEXT_FOOTNOTE_REF)) {
            collectRefName(footnoteRef,node);
        }
        else if (sName.equals(XMLString.TEXT_ENDNOTE_REF)) {
            collectRefName(endnoteRef,node);
        }
        else if (sName.equals(XMLString.TEXT_NOTE_REF)) { // oasis
            String sClass = Misc.getAttribute(node,XMLString.TEXT_NOTE_CLASS);
            if ("footnote".equals(sClass)) { collectRefName(footnoteRef,node); }
            else if ("endnote".equals(sClass)) { collectRefName(endnoteRef,node); }
        }
        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK)) {
            collectMarkByPosition(referenceHeading,null,null,node,sListStyleName,nListLevel,nParLevel);
        }
        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK_START)) {
            collectMarkByPosition(referenceHeading,null,null,node,sListStyleName,nListLevel,nParLevel);
        }
        else if (sName.equals(XMLString.TEXT_REFERENCE_REF)) {
            collectRefName(referenceRef,node);
        }
        else if (sName.equals(XMLString.TEXT_BOOKMARK)) {
            collectMarkByPosition(bookmarkHeading,bookmarkList,bookmarkListLevel,node,sListStyleName,nListLevel,nParLevel);
        }
        else if (sName.equals(XMLString.TEXT_BOOKMARK_START)) {
            collectMarkByPosition(bookmarkHeading,bookmarkList,bookmarkListLevel,node,sListStyleName,nListLevel,nParLevel);
        }
        else if (sName.equals(XMLString.TEXT_BOOKMARK_REF)) {
            collectRefName(bookmarkRef,node);
        }
        else if (sName.equals(XMLString.TEXT_BIBLIOGRAPHY_MARK)) {
        	bibliographyMarks.add(node);
        }
        else if (sName.equals(XMLString.TEXT_SEQUENCE_REF)) {
            collectRefName(sequenceRef,node);
        }
        else if (sName.equals(XMLString.TEXT_A)) {
            String sHref = node.getAttribute(XMLString.XLINK_HREF);
            if (sHref!=null && sHref.startsWith("#")) {
                links.add(sHref.substring(1));
            }
        }
        else if (sName.equals(XMLString.OFFICE_FORMS)) {
            forms.read(node);
        }
        else if (sName.equals(XMLString.TEXT_TABLE_OF_CONTENT)) {
            TocReader tocReader = new TocReader(node);
            indexes.put(node,tocReader);
            indexSourceStyles.addAll(tocReader.getIndexSourceStyles());
        }
        else if (sName.equals(XMLString.TEXT_TABLE_INDEX) ||
                 sName.equals(XMLString.TEXT_ILLUSTRATION_INDEX)) {
            LoftReader loftReader = new LoftReader(node);
            indexes.put(node,loftReader);
            if (loftReader.useCaption()) {
                if (loftReader.isTableIndex()) {
                  tableSequenceNames.add(loftReader.getCaptionSequenceName());
                }
                else {
                  figureSequenceNames.add(loftReader.getCaptionSequenceName());
                }
            }
        }
        // todo: other indexes
        else if (firstImage==null && sName.equals(XMLString.DRAW_FRAME)) {
        	// This may be an image (note that a replacement image for an object is OK by this definition)
        	Element image = Misc.getChildByTagName(node, XMLString.DRAW_IMAGE);
        	if (image!=null) { firstImage=image; }
        }
		
        // Traverse the children
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                traverseContent((Element) child, sListStyleName, nListLevel, nParLevel, bDestructive);
            }
            child = child.getNextSibling();
        }

        // Collect automatic captions sequences
        // Use OOo defaults: Captions have style names Illustration and Table resp.
        if ((sAutoFigureSequenceName==null || sAutoTableSequenceName==null) && sName.equals(XMLString.TEXT_P)) {
            String sStyleName = getParStyles().getDisplayName(node.getAttribute(XMLString.TEXT_STYLE_NAME)); 
            if (sAutoFigureSequenceName==null) {
                if ("Illustration".equals(sStyleName)) {
                    sAutoFigureSequenceName = getSequenceName(node);
                }
            }
            if (sAutoTableSequenceName==null) {
                if ("Table".equals(sStyleName)) {
                    sAutoTableSequenceName = getSequenceName(node);
                }
            }
        }
       
    }
	
    private void collectRefName(Set<String> ref, Element node) {
        String sRefName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (sRefName!=null && sRefName.length()>0) {
            ref.add(sRefName);
        }
    }
	
    private void collectMarkByPosition(Map<String,Integer> headingmarklevels, Map<String,String> listmarknames,
    		Map<String,Integer> listmarklevels, Element node,
    		String sListStyleName, int nListLevel, int nParLevel) {
        String sName = node.getAttribute(XMLString.TEXT_NAME);
        if (sName!=null && sName.length()>0) {
            if (nParLevel>0) { // Mark contained in a heading
                headingmarklevels.put(sName,nParLevel);
            }
            else if (listmarknames!=null && nListLevel>0) {
            		listmarknames.put(sName,sListStyleName);
            		listmarklevels.put(sName, nListLevel);
            }
        }
    }

    // Optimize a heading or paragraph
    private void optimize(Node node) {
    	Node child = node.getFirstChild();
    	while (child!=null) {
    		if (child.getNodeType()==Node.ELEMENT_NODE && child.getNodeName().equals(XMLString.TEXT_SPAN)) {
    			String sStyleName = Misc.getAttribute(child, XMLString.TEXT_STYLE_NAME);
    			StyleWithProperties style = getTextStyle(sStyleName);
    			if (style!=null && style.isEmpty()) {
    				// Found a text span which only represents a session id
    				child = removeSpan(node, child); 
    			}
    			else {
    				Node next = child.getNextSibling();
    				if (next!=null && sStyleName.equals(Misc.getAttribute(next, XMLString.TEXT_STYLE_NAME))) {
    					// Found two adjacent text spans with the same style name
    					child = mergeSpan(node, child, next);
    				}
    				else {
    					// Found ordinary text span, optimize children and continue with next child
    					optimize(child);
    					child = child.getNextSibling();
    				}
    			}
    		}
    		else if (child.getNodeType()==Node.TEXT_NODE) {
    			Node next = child.getNextSibling();
    			if (next!=null && next.getNodeType()==Node.TEXT_NODE) {
    				// Found two adjacent text nodes
    				child = mergeText(node, child, next);
    			}
    			else {
    				// Found isolated text node
    				child = child.getNextSibling();
    			}
    		}
    		else {
    			// All other nodes are ignored
    			child = child.getNextSibling();
    		}
    	}
    }
    
    private Node removeSpan(Node parent, Node span) {
    	// After removing the span, we should reexamine the previous node (if any)
    	// (This may be a text node to be merged with a text node from the span)
    	Node next = span.getPreviousSibling();
    	// Move all children out of the span
    	Node child = span.getFirstChild();
    	while (child!=null) {
    		span.removeChild(child);
    		parent.insertBefore(child, span);
    		child = span.getFirstChild();
    	}
    	// Remove the (now empty) span
    	parent.removeChild(span);
    	// Continue with the previous node (if any), or the first node of the parent
    	return next!=null ? next : parent.getFirstChild();
    }
    
    private Node mergeSpan(Node parent, Node span1, Node span2) {
    	// Move all child nodes from the second span into the first span
    	Node child;
    	while ((child=span2.getFirstChild())!=null) {
    		span2.removeChild(child);
    		span1.appendChild(child);
    	}
    	// Remove the second (now empty) span
    	parent.removeChild(span2);
    	// Continue with the merged node, as more merges may happen (this will also ensure the node gets optimized)
    	return span1;
    }
    
    private Node mergeText(Node parent, Node text1, Node text2) {
    	// Replace the two text nodes with a single text node
    	Text merged = parent.getOwnerDocument().createTextNode(text1.getNodeValue()+text2.getNodeValue());
    	parent.insertBefore(merged, text1);
    	parent.removeChild(text1);
    	parent.removeChild(text2);
    	// After the merge, we should examine the merged node, as more text nodes may follow
    	return merged;
    }

}