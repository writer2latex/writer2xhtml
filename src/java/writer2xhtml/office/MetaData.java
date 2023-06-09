/************************************************************************
 *
 *  MetaData.java
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

package writer2xhtml.office;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2xhtml.util.*;


/**
 *  <p>This class represents the metadata of an OOo Writer document.</p>
 */
public class MetaData implements writer2xhtml.api.MetaData {
    // Dublin Core
    private String sTitle = "";
    private String sCreator = "";
    private String sInitialCreator = "";
    private String sDate = "";
    private String sDescription = "";
    private String sLanguage = "";
    private String sSubject = "";
    // Keywords
    private String sKeywords = "";
    // User-defined
    private Map<String,String> userdefined = new HashMap<String,String>();
 
    /** <p>Construct a new instance from an OOo Writer document.</p>
     *  @param oooDoc is the OOo document
     */
    public MetaData(OfficeDocument oooDoc) {
        // get the DOM (either package or flat)
        Document dom = oooDoc.getMetaDOM();
        if (dom==null) { dom = oooDoc.getContentDOM(); }

        // get the office:meta element
        NodeList list = dom.getElementsByTagName(XMLString.OFFICE_META);
        if (list.getLength() == 0) { return; } // oops, no metadata - fails silently
        Node meta = list.item(0);
        if (!meta.hasChildNodes()) { return; }

        // traverse the metadata
        CSVList keywords = new CSVList(", ");
        list = meta.getChildNodes();
        int nLen = list.getLength();
        for (int i=0; i<nLen; i++) {
            Node child = list.item(i);
            String sName = child.getNodeName();
            if (XMLString.DC_TITLE.equals(sName)) {
                sTitle = getContent(child);
            }
            else if (XMLString.DC_CREATOR.equals(sName)) {
                sCreator = getContent(child);
            }
            else if (XMLString.DC_DATE.equals(sName)) {
                sDate = getContent(child);
            }
            else if (XMLString.DC_DESCRIPTION.equals(sName)) {
                sDescription = getContent(child);
            }
            else if (XMLString.DC_LANGUAGE.equals(sName)) {
                sLanguage = getContent(child);
            }
            else if (XMLString.DC_SUBJECT.equals(sName)) {
                sSubject = getContent(child);
            }
            else if (XMLString.META_INITIAL_CREATOR.equals(sName)) {
                sInitialCreator = getContent(child);
            }
            else if (XMLString.META_KEYWORD.equals(sName)) { // oasis
                keywords.addValue(getContent(child));
            }
            else if (XMLString.META_KEYWORDS.equals(sName)) {
                // Old format: Keywords are contained within meta:keywords
                if (child.hasChildNodes()) {
                    // traverse the keywords
                    NodeList keywordList = child.getChildNodes();
                    int nWordCount = keywordList.getLength();
                    for (int j=0; j<nWordCount; j++) {
                        Node grandchild = keywordList.item(j);
                        if (XMLString.META_KEYWORD.equals(grandchild.getNodeName())){
                            keywords.addValue(getContent(grandchild));
                        }
                    }
                }
            }
            else if (XMLString.META_USER_DEFINED.equals(sName)) {
            	String sPropertyName = Misc.getAttribute(child, XMLString.META_NAME);
            	if (sPropertyName!=null) {
            		userdefined.put(sPropertyName,getContent(child));
            	}
            }
        }
        sKeywords = keywords.toString();
    }
	
    /** <p> Get the title of this document (may be empty)</p>
     *  @return the title of the document
     */
    public String getTitle() { return sTitle; }

    /** <p> Get the creator of this document (may be empty)</p>
     *  @return the creator of the document (or the initial creator if none is specified)
     */
    public String getCreator() { return sCreator==null ? sInitialCreator : sCreator; }

    /** <p> Get the initial creator of this document (may be empty)</p>
     *  @return the initial creator of the document
     */
    public String getInitialCreator() { return sInitialCreator; }

    /** <p> Get the date of this document (may be empty)</p>
     *  @return the date of the document
     */
    public String getDate() { return sDate; }

    /** <p> Get the description of this document (may be empty)</p>
     *  @return the description of the document
     */
    public String getDescription() { return sDescription; }

    /** <p> Get the language of this document (may be empty)</p>
     *  @return the language of the document
     */
    public String getLanguage() { return sLanguage; }
    
    public void setLanguage(String sLanguage) {
    	this.sLanguage = sLanguage;
    }

    /** <p> Get the subject of this document (may be empty)</p>
     *  @return the subject of the document
     */
    public String getSubject() { return sSubject; }

    /** <p> Get the keywords of this document as a comma separated list (may be epmty)</p>
     *  @return the keywords of the document
     */
    public String getKeywords() { return sKeywords; }

    /** Get the user-defined meta data
	 * 
	 * @return the user-defined meta data as a name-value map
	 */
    public Map<String,String> getUserDefinedMetaData() { return userdefined; }

    private String getContent(Node node) {
        StringBuilder buf = new StringBuilder();
        Node child = node.getFirstChild();
        while (child!=null) {
           if (child.getNodeType()==Node.TEXT_NODE) {
               buf.append(child.getNodeValue());
           }
           child = child.getNextSibling();
        }
        return buf.toString();
    }

}