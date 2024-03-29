/************************************************************************
 *
 *	TOCConverter.java
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
 *  Version 1.7.1 (2023-07-26)
 *
 */
package writer2xhtml.xhtml;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.office.IndexMark;
import writer2xhtml.office.ListCounter;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.TocReader;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Misc;

//Helper class (a struct) to contain information about a toc entry (ie. a heading, other paragraph or toc-mark)
final class TocEntry {
	Element onode; // the original node
	int nChapterNumber; // The chapter number for this heading
	String sLabel = null; // generated label for the entry
	int nFileIndex; // the file index for the generated content
	int nOutlineLevel; // the outline level for this heading
	int[] nOutlineNumber; // the natural outline number for this heading
}


// TODO: This class needs some refactoring

/** This class processes table of content index marks and the associated table of content
 */
class TOCConverter extends IndexConverterHelper {
	
	private static final String TOC_LINK_PREFIX = "toc";
	
	private List<TocEntry> tocEntries = new ArrayList<TocEntry>(); // All potential(!) toc items
	private int nTocFileIndex = -1; // file index for main toc
	private int nTocIndex = -1; // Current index for id's (of form tocN)
	private ListCounter naturalOutline = new ListCounter(); // Current "natural" outline number 

	private int nExternalTocDepth = 1; // The number of levels to include in the "external" table of contents
	private int nExternalTocDepthMarks = 1; // The number of levels of index makrs to include in the "external" table of contents

	TOCConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
		super(ofr,config,converter,XMLString.TEXT_TABLE_OF_CONTENT_SOURCE);
        nExternalTocDepth = config.externalTocDepth();
        if (nExternalTocDepth==-1) { // A value of -1 means auto (i.e. determine from split level)
        	nExternalTocDepth = Math.max(config.getXhtmlSplitLevel(),1);
        }
        nExternalTocDepthMarks = config.externalTocDepthMarks();
	}
   
	/** Return the id of the file containing the alphabetical index
	* 
	* @return the file id
	*/
	int getFileIndex() {
		return nTocFileIndex;
	}
	
	/** Handle a heading as a table of content entry
	 * 
	 * @param onode the text:h element
     * @param heading the link target will be added to this inline HTML node
	 * @param sLabel the numbering label of this heading
	 * @param nChapterNumber the chapter containing this heading
	 */
	void handleHeading(Element onode, Element heading, String sLabel, int nChapterNumber) {
		int nLevel = getTextCv().getOutlineLevel(onode);
		String sTarget = TOC_LINK_PREFIX+(++nTocIndex);
		converter.addTarget(heading,sTarget);
		
		// Add in external content.
		// Targets are added only when the toc level is deeper than the split level 
		if (nLevel<=nExternalTocDepth) {
			converter.addContentEntry(sLabel+converter.getPlainInlineText(onode), nLevel,
					nLevel>config.getXhtmlSplitLevel() ? sTarget : null);
		}
	
		// Add to real toc
		TocEntry entry = new TocEntry();
		entry.onode = onode;
		entry.nChapterNumber = nChapterNumber;
		entry.sLabel = sLabel;
		entry.nFileIndex = converter.getOutFileIndex();
		entry.nOutlineLevel = nLevel; 
		entry.nOutlineNumber = naturalOutline.step(nLevel).getValues();
		tocEntries.add(entry);
	}
	
	// Add in external content. For single file output we include all level 1 headings + their target
	// Targets are added only when the toc level is deeper than the split level
	void handleHeadingExternal(Element onode, Element hnode, String sLabel) {
		int nLevel = getTextCv().getOutlineLevel(onode);
		if (nLevel<=nExternalTocDepth) {
			// Add an empty div to use as target, if required
			String sTarget = null;
			if (nLevel>config.getXhtmlSplitLevel()) {
				Element div = converter.createElement("div");        			
				hnode.appendChild(div);
				sTarget = TOC_LINK_PREFIX+(++nTocIndex);
				converter.addTarget(div,sTarget);
			}
			converter.addContentEntry(sLabel+converter.getPlainInlineText(onode), nLevel, sTarget);
		}
	}
	
	void handleParagraph(Element onode, Element par, String sCurrentListLabel) {
        String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
		if (ofr.isIndexSourceStyle(getParSc().getRealParStyleName(sStyleName))) {
	        converter.addTarget(par,TOC_LINK_PREFIX+(++nTocIndex));
	        TocEntry entry = new TocEntry();
	        entry.onode = onode;
	        entry.sLabel = sCurrentListLabel;  
	        entry.nFileIndex = converter.getOutFileIndex();
	        tocEntries.add(entry);
	    }
	}

    /** Handle a table of contents mark
     * 
     * @param onode a text:toc-mark or text:toc-mark-start node
     * @param hnode the link target will be added to this inline HTML node
     */
    void handleTocMark(Node onode, Node hnode) {
    	String sTarget = TOC_LINK_PREFIX+(++nTocIndex);
        hnode.appendChild(converter.createTarget(sTarget));
        TocEntry entry = new TocEntry();
        entry.onode = (Element) onode;
        entry.nFileIndex = converter.getOutFileIndex();
        tocEntries.add(entry);
        
        // Add to external toc
        int nLevel = Misc.getPosInteger(Misc.getAttribute(entry.onode, XMLString.TEXT_OUTLINE_LEVEL),1);
		if (nLevel<=nExternalTocDepthMarks) {
			converter.addContentEntry(IndexMark.getIndexValue(entry.onode),
				Misc.getPosInteger(entry.onode.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1),
				sTarget);
		}
    }
    
    /** Handle a table of contents
     * 
     * @param onode a text:alphabetical-index node
     * @param hnode the index will be added to this block HTML node
     */
    @Override void handleIndex(Element onode, Element hnode, int nChapterNumber) {
		// Identify main toc
    	if (nTocFileIndex==-1 && !ofr.getTocReader(onode).isByChapter()) { 
    		nTocFileIndex = converter.getOutFileIndex(); 
    	}
    	converter.setTocFile(null);
    	super.handleIndex(onode,hnode,nChapterNumber);
    }

    /** Generate the content of all tables of content
     * 
     */
	@Override
	void generate(IndexData data) {
		converter.getTextCv().setSoftPageBreaksLimit(0);
		
    	Element onode = data.onode;
    	int nChapterNumber = data.nChapterNumber;
        Element ul = data.hnode;

        TocReader tocReader = ofr.getTocReader((Element)onode.getParentNode());

        // TODO: Read the entire content of the entry templates!
        String[] sEntryStyleName = new String[11];
        for (int i=1; i<=10; i++) {
            Element entryTemplate = tocReader.getTocEntryTemplate(i);
            if (entryTemplate!=null) {
                sEntryStyleName[i] = Misc.getAttribute(entryTemplate,XMLString.TEXT_STYLE_NAME);
            }
        }

        int nStart = 0;
        int nLen = tocEntries.size();

        // Find the chapter
        if (tocReader.isByChapter() && nChapterNumber>0) {
            for (int i=0; i<nLen; i++) {
                TocEntry entry = tocEntries.get(i);
                if (entry.nChapterNumber==nChapterNumber) { nStart=i; break; }
            }
        }

        // Generate entries
        for (int i=nStart; i<nLen; i++) {
            TocEntry entry = tocEntries.get(i);
            String sNodeName = entry.onode.getTagName();
            if (XMLString.TEXT_H.equals(sNodeName)) {
                int nLevel = getTextCv().getOutlineLevel(entry.onode);

                if (nLevel==1 && tocReader.isByChapter() && entry.nChapterNumber>nChapterNumber) { break; }
                if (tocReader.useOutlineLevel() && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createEntry(ul,sEntryStyleName[nLevel]);
                    if (entry.sLabel!=null) {
                        Element span = converter.createElement("span");
                        p.appendChild(span);
                        span.setAttribute("class","SectionNumber");
                        span.appendChild(converter.createTextNode(entry.sLabel));
                    }
                    Element a = converter.createLink(TOC_LINK_PREFIX+i);
                    p.appendChild(a);
                    getTextCv().traverseInlineText(entry.onode,a);
                }
                else {
                    String sStyleName = getParSc().getRealParStyleName(entry.onode.getAttribute(XMLString.TEXT_STYLE_NAME));
                    nLevel = tocReader.getIndexSourceStyleLevel(sStyleName);
                    if (tocReader.useIndexSourceStyles() && 1<=nLevel && nLevel<=tocReader.getOutlineLevel()) {
                        Element p = createEntry(ul,sEntryStyleName[nLevel]);
                        if (entry.sLabel!=null) {
                            p.appendChild(converter.createTextNode(entry.sLabel));
                        }
                        Element a = converter.createLink(TOC_LINK_PREFIX+i);
                        p.appendChild(a);
                        getTextCv().traverseInlineText(entry.onode,a);
                    }
                }
            }
            else if (XMLString.TEXT_P.equals(sNodeName)) {
                String sStyleName = getParSc().getRealParStyleName(entry.onode.getAttribute(XMLString.TEXT_STYLE_NAME));
                int nLevel = tocReader.getIndexSourceStyleLevel(sStyleName);
                if (tocReader.useIndexSourceStyles() && 1<=nLevel && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createEntry(ul,sEntryStyleName[nLevel]);
                    if (entry.sLabel!=null) {
                        p.appendChild(converter.createTextNode(entry.sLabel));
                    }
                    Element a = converter.createLink(TOC_LINK_PREFIX+i);
                    p.appendChild(a);
                    getTextCv().traverseInlineText(entry.onode,a);
                }
            }
            else if (XMLString.TEXT_TOC_MARK.equals(sNodeName)) {
                int nLevel = Misc.getPosInteger(entry.onode.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
                if (tocReader.useIndexMarks() && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createEntry(ul,sEntryStyleName[nLevel]);
                    Element a = converter.createLink(TOC_LINK_PREFIX+i);
                    p.appendChild(a);
                    a.appendChild(converter.createTextNode(IndexMark.getIndexValue(entry.onode)));
                }
            }
            else if (XMLString.TEXT_TOC_MARK_START.equals(sNodeName)) {
                int nLevel = Misc.getPosInteger(entry.onode.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
                if (tocReader.useIndexMarks() && nLevel<=tocReader.getOutlineLevel()) {
                    Element p = createEntry(ul,sEntryStyleName[nLevel]);
                    Element a = converter.createLink(TOC_LINK_PREFIX+i);
                    p.appendChild(a);
                    a.appendChild(converter.createTextNode(IndexMark.getIndexValue(entry.onode)));
                }
            }
        }
        
		converter.getTextCv().setSoftPageBreaksLimit(-1);
    }
	
	// Create an entry in this list with that paragraph style name
	private Element createEntry(Element ul, String sStyleName) {
    	Element li = converter.createElement("li");
    	ul.appendChild(li);
    	return getTextCv().createParagraph(li,sStyleName);
	}    
    
}
