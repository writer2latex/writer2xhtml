/************************************************************************
 *
 *	AlphabeticalIndexConverter.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.office.IndexMark;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Misc;
import writer2xhtml.util.StringComparator;

// Helper class (a struct) to contain information about an alphabetical index entry.
final class AlphabeticalEntry {
	String[] sWord=new String[3]; // the words for the index in the order key1,key2,word
	int nIndex; // the original index of this entry
}

/** This class processes alphabetical index marks and the associated index table
 */
class AlphabeticalIndexConverter extends IndexConverterHelper {
	
    private List<AlphabeticalEntry> index = new ArrayList<AlphabeticalEntry>(); // All words for the index
    private int nIndexIndex = -1; // Current index used for id's (of form idxN) 
    private int nAlphabeticalIndex = -1; // File containing main alphabetical index

    AlphabeticalIndexConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,XMLString.TEXT_ALPHABETICAL_INDEX_SOURCE);
    }
    
    /** Return the id of the file containing the alphabetical index
     * 
     * @return the file id
     */
    int getFileIndex() {
    	return nAlphabeticalIndex;
    }
    
    /** Handle an alphabetical index mark
     * 
     * @param onode a text:alphabetical-index-mark node
     * @param hnode the link target will be added to this inline HTML node
     */
    void handleIndexMark(Node onode, Node hnode) {
        handleIndexMark(Misc.getAttribute(onode,XMLString.TEXT_STRING_VALUE),onode,hnode);
    }

    /** Handle an alphabetical index mark start
     * 
     * @param onode a text:alphabetical-index-mark-start node
     * @param hnode the link target will be added to this inline HTML node
     */
    void handleIndexMarkStart(Node onode, Node hnode) {
        handleIndexMark(IndexMark.getIndexValue(onode),onode,hnode);
    }
    
    // Create an entry for an index mark
    void handleIndexMark(String sWord, Node onode, Node hnode) {
        if (sWord!=null) {
	        AlphabeticalEntry entry = new AlphabeticalEntry();
	        short i=0;
	        String sKey1 = Misc.getAttribute(onode, XMLString.TEXT_KEY1);
	        if (sKey1!=null) { entry.sWord[i++]=sKey1; }
	        String sKey2 = Misc.getAttribute(onode, XMLString.TEXT_KEY2);
	        if (sKey2!=null) { entry.sWord[i++]=sKey2; }
	        entry.sWord[i] = sWord;
	        entry.nIndex = ++nIndexIndex; 
	        index.add(entry);
	        hnode.appendChild(converter.createTarget("idx"+nIndexIndex));
        }    	
    }

    /** Handle an alphabetical index
     * 
     * @param onode a text:alphabetical-index node
     * @param hnode the index will be added to this block HTML node
     */
    @Override void handleIndex(Element onode, Element hnode, int nChapterNumber) {
    	// Register the file index (we assume that there is only one alphabetical index)
        nAlphabeticalIndex = converter.getOutFileIndex();
        super.handleIndex(onode, hnode, nChapterNumber);
    }
    
    void generate(IndexData data) {
    	Element source = data.onode;
    	Element container = data.hnode;

       	sortEntries(source);
        String[] sEntryStyleName = getEntryStyleName(source);
        String[] sLastKey = new String[2];
        for (int i=0; i<=nIndexIndex; i++) {
            AlphabeticalEntry entry = index.get(i);
            for (int j=0; j<3; j++) {
            	if (entry.sWord[j]!=null) {
		            Element li = converter.createElement("li");
		            container.appendChild(li);
		            Element p = getTextCv().createParagraph(li,sEntryStyleName[j],false);
		            if (j<2 && entry.sWord[j+1]!=null) {
		            	// This is a key, and may already be inserted
		            	if (!entry.sWord[j].equals(sLastKey[j])) {
			            	// The key is inserted as plain text
			            	p.appendChild(converter.createTextNode(entry.sWord[j]));
		            	}
		            }
		            else {
		            	// This is the word itself and is added with a link to the text
			            Element a = converter.createLink("idx"+entry.nIndex);
			            p.appendChild(a);
			            a.appendChild(converter.createTextNode(entry.sWord[j]));
		            }
            	}
            }
            // Update the current keys
            sLastKey[0]=entry.sWord[0];
            sLastKey[1]=entry.sWord[1];
        }   
        
        // Add to external content
        if (config.indexLinks()) {
	        Element title = Misc.getChildByTagName(source, XMLString.TEXT_INDEX_TITLE_TEMPLATE);
	        String sTitle = title!=null ? Misc.getPCDATA(title) : "Alphabetical Index";
	        converter.addContentEntry(sTitle, 1, 
	            converter.getTarget(Misc.getAttribute(source.getParentNode(),XMLString.TEXT_NAME)));
        }
    }
    
    // Sort the list of words based on the language defined by the index source
    private void sortEntries(Element source) {
		Comparator<AlphabeticalEntry> comparator = new StringComparator<AlphabeticalEntry>(
				Misc.getAttribute(source,XMLString.FO_LANGUAGE),
        		Misc.getAttribute(source, XMLString.FO_COUNTRY)) {

			public int compare(AlphabeticalEntry a, AlphabeticalEntry b) {
				int nResult=0;
				for (int i=0; i<3; i++) {
					nResult = getCollator().compare(a.sWord[i], b.sWord[i]);
					if (nResult!=0) { // We have a final result
						return nResult;
					}
					else if (i<2) { // the final result depends on the next values
						if (a.sWord[i+1]==null && b.sWord[i+1]!=null) { // null<string
							return -1;
						}
						else if (a.sWord[i+1]!=null && b.sWord[i+1]==null) { // string>null
							return 1;
						}
						else if (a.sWord[i+1]==null && b.sWord[i+1]==null) { // null==null
							return nResult;
						}
						// if both are not null, we will try again with the next level
					}
				}
				return nResult;
			}
		};
		Collections.sort(index,comparator);
    }
    
    // Get the style names to use for the individual words from the index source
    private String[] getEntryStyleName(Element source) {
        // TODO: Should read the entire template
    	String[] sStyleName = new String[3];
    	Node child = source.getFirstChild();
    	while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE
                && child.getNodeName().equals(XMLString.TEXT_ALPHABETICAL_INDEX_ENTRY_TEMPLATE)) {
                // Note: The last value of text:outline-level is "separator"
                int nLevel = Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_OUTLINE_LEVEL),0);
                if (1<=nLevel && nLevel<=3) {
                    sStyleName[nLevel-1]=Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME);
                }
            }
            child = child.getNextSibling();
        }
        return sStyleName;
    }    

}
