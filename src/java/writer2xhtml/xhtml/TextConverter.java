/************************************************************************
 *
 *	TextConverter.java
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
 *  Version 1.7.1 (2023-06-28)
 *
 */

package writer2xhtml.xhtml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2xhtml.office.FontDeclaration;
import writer2xhtml.office.ListCounter;
import writer2xhtml.office.ListStyle;
import writer2xhtml.office.MasterPage;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.OfficeStyle;
import writer2xhtml.office.PageLayout;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.TocReader;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Misc;
import writer2xhtml.xhtml.l10n.L10n;

import org.w3c.dom.Element;


/** This class handles text content
 */
public class TextConverter extends ConverterHelper {

    // Data used to handle splitting over several files
    // TODO: Accessor methods for sections
	// Some (Sony?) EPUB readers have a limit on the file size of individual files
	// In any case very large files could be a performance problem, hence we do automatic splitting
	// after this number of characters.
	private int nSplitAfter = 150000;
	private int nPageBreakSplit = XhtmlConfig.NONE; // Should we split at page breaks?
	// TODO: Collect soft page breaks between table rows
	private boolean bPendingPageBreak = false; // We have encountered a page break which should be inserted asap
    private int nSplit = 0;  // The outline level at which to split files (0=no split)
    private int nRepeatLevels = 5; // The number of levels to repeat when splitting (0=no repeat)
    private int nLastSplitLevel = 1; // The outline level at which the last split occurred
    private int nDontSplitLevel = 0; // if > 0 splitting is forbidden
    boolean bAfterHeading=false; // last element was a top level heading
    protected Stack<Node> sections = new Stack<>(); // stack of nested sections
    Element[] currentHeading = new Element[7]; // Last headings (repeated when splitting)
    private int nCharacterCount = 0; // The number of text characters in the current document
    
    // Data used to handle page numbers
    private int nPageCount = 1; // Current physical page number
    private int nPageNumber = 1; // Current page number
    private MasterPage masterPage = null; // Current master page (which determines the numbering style for the page number)
    private int nSoftPageBreaksLimit; // If positive, this is the maximal number of soft page breaks to export before the value is reset
    
    // Counters for generated numbers
    private ListCounter outlineNumbering;
    private Map<String, ListCounter> listCounters = new HashMap<>();
    private Set<String> usedLists = new HashSet<>();
    private String sCurrentListLabel = null;
    
    // Mode used to handle floats (depends on source doc type and config)
    private int nFloatMode; 
	
    // Converter helpers used to handle all sorts of indexes
    private TOCConverter tocCv;
    private UserIndexConverter userCv;
    private LOFConverter lofCv;
    private LOTConverter lotCv;
    private AlphabeticalIndexConverter indexCv;
    private BibliographyConverter bibCv;
    private int nChapterNumber = 0;

    // Converter helpers used to handle footnotes and endnotes
    private FootnoteConverter footCv;
    private EndnoteConverter endCv;
    
    // Sometimes we have to create an inlinenode in a block context
    // (labels for footnotes and endnotes)
    // We put it here and insert it in the first paragraph/heading to come:
    private Node asapNode = null;
    
    // When generating toc, a few things should be done differently
    private boolean bInToc = false;
    
    // Display hidden text?
    private boolean bDisplayHiddenText = false;
    
    public TextConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        tocCv = new TOCConverter(ofr, config, converter);
        userCv = new UserIndexConverter(ofr, config, converter);
        lofCv = new LOFConverter(ofr, config, converter);
        lotCv = new LOTConverter(ofr, config, converter);
        bibCv = new BibliographyConverter(ofr, config, converter);
        indexCv = new AlphabeticalIndexConverter(ofr, config, converter);
        footCv = new FootnoteConverter(ofr, config, converter);
        endCv = new EndnoteConverter(ofr, config, converter);
        nSplitAfter = 1000*config.splitAfter();
        nPageBreakSplit = config.pageBreakSplit();
        nSplit = config.getXhtmlSplitLevel();
        nRepeatLevels = converter.isOPS() ? 0 : config.getXhtmlRepeatLevels(); // never repeat headings in EPUB
        nFloatMode = ofr.isText() && config.xhtmlFloatObjects() ? 
            DrawConverter.FLOATING : DrawConverter.ABSOLUTE;
        outlineNumbering = new ListCounter(ofr.getOutlineStyle());
        bDisplayHiddenText = config.displayHiddenText();
    }
	
    /** Converts an office node as a complete text document
     *
     *  @param onode the Office node containing the content to convert
     *  @param l10n translations for the document language
     */
    public void convertTextContent(Element onode) {
        Element hnode = converter.nextOutFile(converter.getL10n().get(L10n.HOME),1);

        // Start with page 1
        setSoftPageBreaksLimit(-1);
        masterPage = ofr.getFirstMasterPage();
    	insertPageNumber(hnode,"div");

        // Create form
        if (nSplit==0) {
            Element form = getDrawCv().createForm();
            if (form!=null) {
                hnode.appendChild(form);
                hnode = form;
            }
        }
        
        // Add cover image
        hnode = getDrawCv().insertCoverImage(hnode);

        // Convert content
        hnode = (Element)traverseBlockText(onode,hnode);
        
        // Add footnotes and endnotes
        footCv.insertFootnotes(hnode,true);
        endCv.insertEndnotes(hnode);

        // Generate all indexes
        bInToc = true;
        tocCv.generate();
        userCv.generate();
        indexCv.generate();
        bibCv.generate();
        bInToc = false;
    }
	
    protected int getTocIndex() { return tocCv.getFileIndex(); }
	
    protected int getAlphabeticalIndex() { return indexCv.getFileIndex(); }
    
    protected void setAsapNode(Element node) {
    	asapNode = node;
    }
    
    ////////////////////////////////////////////////////////////////////////
    // PAGE NUMBERS
    ////////////////////////////////////////////////////////////////////////

    // Insert page break by style; return true if there is a page break *after*
    boolean maybePageBreak(Node node, StyleWithProperties style) {
        if (style!=null) {
        	// A page break can be a simple page break before or after...
        	if ("page".equals(style.getProperty(XMLString.FO_BREAK_BEFORE))) {
        		if (!atTopOfPage(node)) { // A page break has no effect if we are already at the top of the page
        			insertPageBreak(node, "div");
        		}
        	}
        	else if ("page".equals(style.getProperty(XMLString.FO_BREAK_AFTER))) {
        		return true;
        	}
        	else { // ...or it can be a new master page
	        	String sMasterPage = style.getMasterPageName();
	        	if (sMasterPage!=null && sMasterPage.length()>0) {
	        		if (atTopOfPage(node)) {
	        			// Page break has no effect, but page numbering is changed. Hence we need to replace the previous page break mark
	        			node.removeChild(node.getLastChild());
	        	    	converter.removeOriginalPageNumber();
	        			nPageCount--;
	        			nPageNumber--;
	        		}
	        		// The new master page determines the number format
	        		masterPage = ofr.getMasterPage(sMasterPage);
        			// The style may also set a new page number 
        			String sPageNumber = style.getParProperty(XMLString.STYLE_PAGE_NUMBER,true);
        			if (sPageNumber!=null) {
        				try { nPageNumber = Integer.parseInt(sPageNumber); }
        				catch (NumberFormatException ex) { }
    				}
	        		insertPageNumber(node, "div");
	        	}        	
        	}
        }
        return false;
    }
    
    // Return true if no content has been inserted on the current page
	// TODO: This may not work if we ignore empty paragraphs
    private boolean atTopOfPage(Node node) {
    	Node child = node.getLastChild();
    	return child!=null && Misc.isElement(child, "div") && "pagebreak".equals(Misc.getAttribute(child, "epub:type"));
    }
    
    // A page number can be represented like e.g. <span epub:type="pagebreak" id="page57" title="57" />
    private void insertPageNumber(Node node, String sTagName) {
    	if (converter.isOPS() && config.originalPageNumbers()) {
	    	Element elm = converter.createElement(sTagName);
	    	elm.setAttribute("epub:type", "pagebreak");
	    	elm.setAttribute("id", "page"+nPageCount);
	    	String sPageNumber = null;
	    	if (masterPage!=null) {
	    		elm.setAttribute("masterpage", masterPage.getDisplayName());
	    		PageLayout pageLayout = ofr.getPageLayout(masterPage.getPageLayoutName());
	    		if (pageLayout!=null) {
	    			String sNumFormat = pageLayout.getProperty(XMLString.STYLE_NUM_FORMAT,true);
	    			boolean bLetterSync = "true".equals(pageLayout.getProperty(XMLString.STYLE_NUM_LETTER_SYNC, true));
	    			if (sNumFormat!=null) {
	    				sPageNumber = ListCounter.formatNumber(nPageNumber, sNumFormat, bLetterSync);
		    		}
		    	}
		    	if (sPageNumber==null) {
		    		sPageNumber = Integer.toString(nPageNumber);
		    	}
	    	}
			elm.setAttribute("title", sPageNumber);
	    	node.appendChild(elm);
	    	nPageCount++;
	    	nPageNumber++;
	    	converter.addOriginalPageNumber(sPageNumber, 1, elm.getAttribute("id"));
    	}
    }
    
    void insertPageBreak(Node node, String sTagName) {
    	String sNewMasterPage = masterPage.getProperty(XMLString.STYLE_NEXT_STYLE_NAME);
    	if (sNewMasterPage!=null) {
    		masterPage = ofr.getMasterPage(sNewMasterPage);
    	}
    	insertPageNumber(node,sTagName);
    }
    
    // Set number of soft page breaks to accept (usually -1 (no limit), 0 or 1)
    void setSoftPageBreaksLimit(int n) {
    	nSoftPageBreaksLimit = n;
    }
    
    ////////////////////////////////////////////////////////////////////////
    // FILE SPLITTING
    ////////////////////////////////////////////////////////////////////////

    private Node maybeSplitIndex(Node onode, Node hnode, String sSourceXML) {
    	String sFileTitle = null;
    	Element source = Misc.getChildByTagName(onode, sSourceXML);
    	if (source!=null && !"chapter".equals(Misc.getAttribute(source, XMLString.TEXT_INDEX_SCOPE))) {
    		Element title = Misc.getChildByTagName(source, XMLString.TEXT_INDEX_TITLE_TEMPLATE);
    		if (title!=null) {
    			sFileTitle = Misc.getPCDATA(title);
    		}
    	}
    	return maybeSplit(hnode,null,sFileTitle,1);
    }
    
    private Node maybeSplit(Node node, StyleWithProperties style) {
    	return maybeSplit(node, style, null, -1);
    }
    
    private Node maybeSplit(Node node, StyleWithProperties style, String sFileTitle, int nLevel) {
    	if (bPendingPageBreak) {
    		return doMaybeSplit(node, null, 0);
    	}
    	if (getPageBreak(style)) {
    		return doMaybeSplit(node, null, 0);
    	}
    	if (converter.isOPS() && nSplitAfter>0 && nCharacterCount>nSplitAfter) {
    		return doMaybeSplit(node, null, 0);
    	}
    	if (nLevel>=0) {
    		return doMaybeSplit(node, sFileTitle, nLevel);
    	}
    	else {
    		return node;
    	}
    }

    protected Element doMaybeSplit(Node node, String sFileTitle, int nLevel) {
        if (nDontSplitLevel>1) { // we cannot split due to a nested structure
            return (Element) node;
        }
        if (!converter.isOPS() && bAfterHeading && nLevel-nLastSplitLevel<=nRepeatLevels) {
            // we cannot split because we are right after a heading and the
            // maximum number of parent headings on the page is not reached
        	// TODO: Something wrong here....nLastSplitLevel is never set???
            return (Element) node;
        }
        if (nSplit>=nLevel && converter.outFileHasContent()) {
            // No objections, this is a level that causes splitting
        	nCharacterCount = 0;
        	bPendingPageBreak = false;
            if (converter.getOutFileIndex()>=0) { footCv.insertFootnotes(node,false); }
            usedLists.clear();
            Element newHnode = converter.nextOutFile(sFileTitle, nLevel);
            // Recreate nested sections, if any
            for (Node section : sections) {
            	// Create div for section
            	Element div = converter.createElement("div");
                newHnode.appendChild(div);
                // Apply the section style
                String sStyleName = Misc.getAttribute(section,XMLString.TEXT_STYLE_NAME);
                StyleInfo sectionInfo = new StyleInfo();
                converter.getStyleCv().getSectionSc().applyStyle(sStyleName,sectionInfo);
                converter.getStyleCv().getSectionSc().applyStyle(sectionInfo,div);
                // The div is our new node
                newHnode = div;
            }
            return newHnode;
        }
        return (Element) node;
    }
    	
    private boolean getPageBreak(StyleWithProperties style) {
        if (style!=null && nPageBreakSplit>XhtmlConfig.NONE) {
        	// If we don't consider manual page breaks, we may have to consider the parent style
        	if (style.isAutomatic() && nPageBreakSplit<XhtmlConfig.EXPLICIT) {
        		OfficeStyle parentStyle = style.getParentStyle();
        		if (parentStyle!=null && parentStyle instanceof StyleWithProperties) {
        			style = (StyleWithProperties) parentStyle;
        		}
        		else {
        			return false;
        		}
        	}
        	// A page break can be a simple page break before or after...
        	if ("page".equals(style.getProperty(XMLString.FO_BREAK_BEFORE))) {
        		return true;
        	}
        	if ("page".equals(style.getProperty(XMLString.FO_BREAK_AFTER))) {
        		bPendingPageBreak = true;
        		return false;
        	}
        	// ...or it can be a new master page
        	String sMasterPage = style.getMasterPageName();
        	if (sMasterPage!=null && sMasterPage.length()>0) {
        		return true;
        	}
        }
        return false;
    }
    
    ////////////////////////////////////////////////////////////////////////
    // BLOCK TEXT (returns current html node at end of block)
    ////////////////////////////////////////////////////////////////////////

    public Node traverseBlockText(Node onode, Node hnode) {
        return traverseBlockText(onode,0,null,hnode);
    } 
	
    private Node traverseBlockText(Node onode, int nLevel, String styleName, Node hnode) {
        if (!onode.hasChildNodes()) { return hnode; }
        bAfterHeading = false;
        NodeList nList = onode.getChildNodes();
        int nLen = nList.getLength();
        int i = 0;
        while (i < nLen) {
            Node child = nList.item(i);
            
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                boolean bPageBreakAfter = false;
                String nodeName = child.getNodeName();
                // Block splitting
                nDontSplitLevel++;
                
                if (OfficeReader.isDrawElement(child)) {
                    getDrawCv().handleDrawElement((Element)child,(Element)hnode,null,nFloatMode);
                }
                else if (nodeName.equals(XMLString.TEXT_P)) {
                	StyleWithProperties style = ofr.getParStyle(Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME));
                	hnode = maybeSplit(hnode, style);
                	bPageBreakAfter = maybePageBreak(hnode, style);
                	nCharacterCount+=OfficeReader.getCharacterCount(child);
                    // is there a block element, we should use?
                    XhtmlStyleMap xpar = config.getXParStyleMap();
                    String sDisplayName = style!=null ? style.getDisplayName() : null;
					
                    if (sDisplayName!=null && xpar.contains(sDisplayName)) {
                        Node curHnode = hnode;
                        XhtmlStyleMapItem map = xpar.get(sDisplayName);
                        String sBlockElement = map.sBlockElement;
                        String sBlockCss = map.sBlockCss;
                        if (map.sBlockElement.length()>0) {
                            Element block = converter.createElement(map.sBlockElement);
                            if (!"(none)".equals(map.sBlockCss)) {
                                block.setAttribute("class",map.sBlockCss);
                            }
                            hnode.appendChild(block);
                            curHnode = block;
                        }
                        boolean bMoreParagraphs = true;
                        do {
                            handleParagraph(child,curHnode);
                            bMoreParagraphs = false;
                            if (++i<nLen) {
                                child = nList.item(i);
                                String cnodeName = child.getNodeName();
                                if (cnodeName.equals(XMLString.TEXT_P)) {
                                    String sCurDisplayName = ofr.getParStyles().getDisplayName(Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME));
                                    if (sCurDisplayName!=null && xpar.contains(sCurDisplayName)) {
                                    	XhtmlStyleMapItem newmap = xpar.get(sCurDisplayName);
                                        if (sBlockElement.equals(newmap.sBlockElement) &&
	                                        sBlockCss.equals(newmap.sBlockCss)) {
                                            bMoreParagraphs = true;
                                         }
                                    }
                                }
                            }
                        } while (bMoreParagraphs);
                        i--;
                    }
                    else {
                        handleParagraph(child,hnode);
                    }
                }
                else if(nodeName.equals(XMLString.TEXT_H)) {
                	StyleWithProperties style = ofr.getParStyle(Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME));
                	bPageBreakAfter = maybePageBreak(hnode, style);
                	nCharacterCount+=OfficeReader.getCharacterCount(child);
                	// splitting is handled by handleHeading because we need to add the label
                	hnode = handleHeading((Element)child, (Element)hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_LIST) || // oasis
                         nodeName.equals(XMLString.TEXT_UNORDERED_LIST) || // old
                         nodeName.equals(XMLString.TEXT_ORDERED_LIST)) // old
                    {
	                	hnode = maybeSplit(hnode,null);
	                	if (listIsOnlyHeadings(child)) {
	                        nDontSplitLevel--;
	                        hnode = handleFakeList(child,nLevel+1,styleName,hnode);
	                        nDontSplitLevel++;
                    }
                    else {
                        handleList(child,nLevel+1,styleName,hnode);
                    }
                }
                else if (nodeName.equals(XMLString.TABLE_TABLE)) {
                	StyleWithProperties style = ofr.getTableStyle(Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME));
                	hnode = maybeSplit(hnode,style);
                	bPageBreakAfter = maybePageBreak(hnode, style);
                    getTableCv().handleTable(child,hnode);
                }
                else if (nodeName.equals(XMLString.TABLE_SUB_TABLE)) {
                    getTableCv().handleTable(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_SECTION)) {
                	hnode = maybeSplit(hnode,null);
                    nDontSplitLevel--;
                    hnode = handleSection(child,hnode);
                    nDontSplitLevel++;
                }
                else if (nodeName.equals(XMLString.TEXT_TABLE_OF_CONTENT)) {
                	if (config.includeToc()) {
                		TocReader tocReader = ofr.getTocReader((Element)child);
	                    if (!tocReader.isByChapter()) {
	                    	Element title = tocReader.getIndexTitleTemplate();
	                    	if (title!=null) {
	                    		hnode = maybeSplit(hnode,null,Misc.getPCDATA(title),1);
	                    	}
	                    	else { // Error in document
	                    		hnode = maybeSplit(hnode,null,converter.getL10n().get(L10n.CONTENTS),1);	                    		
	                    	}
	                    }
	                    tocCv.handleIndex((Element)child,(Element)hnode,nChapterNumber);
                	}
                }
                else if (nodeName.equals(XMLString.TEXT_ILLUSTRATION_INDEX)) {
                    // TODO
                    lofCv.handleLOF(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_TABLE_INDEX)) {
                    // TODO
                    lotCv.handleLOT(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_OBJECT_INDEX)) {
                    // TODO
                }
                else if (nodeName.equals(XMLString.TEXT_USER_INDEX)) {
                	hnode = maybeSplitIndex(child,hnode,XMLString.TEXT_USER_INDEX_SOURCE);
                    userCv.handleIndex((Element)child,(Element)hnode,nChapterNumber);
                }
                else if (nodeName.equals(XMLString.TEXT_ALPHABETICAL_INDEX)) {
                	hnode = maybeSplitIndex(child,hnode,XMLString.TEXT_ALPHABETICAL_INDEX_SOURCE);
                    indexCv.handleIndex((Element)child,(Element)hnode,nChapterNumber);
                }
                else if (nodeName.equals(XMLString.TEXT_BIBLIOGRAPHY)) {
                	hnode = maybeSplitIndex(child,hnode,XMLString.TEXT_BIBLIOGRAPHY_SOURCE);
                    bibCv.handleIndex((Element)child,(Element)hnode,nChapterNumber);
                }
                else if (nodeName.equals(XMLString.TEXT_SOFT_PAGE_BREAK)) {
                	if (nPageBreakSplit==XhtmlConfig.ALL) { bPendingPageBreak = true; }
                	if (nSoftPageBreaksLimit--!=0) { insertPageBreak(hnode,"div"); }
                }
                else if (nodeName.equals(XMLString.OFFICE_ANNOTATION)) {
                    converter.handleOfficeAnnotation(child,hnode);
                }
                else if (nodeName.equals(XMLString.TEXT_SEQUENCE_DECLS)) {
                    //handleSeqeuenceDecls(child);
                }
                // Reenable splitting
                nDontSplitLevel--;
                // Remember if this was a heading
                if (nDontSplitLevel==0) {
                    bAfterHeading = nodeName.equals(XMLString.TEXT_H);
                    hnode = getDrawCv().flushFullscreenFrames((Element)hnode);
                }
                if (bPageBreakAfter) {
                	insertPageBreak(hnode,"div");
                }
            }
            i++;
        }
        return hnode;
    }
    
    /* Process a text:section tag (returns current html node) */
    private Node handleSection(Node onode, Node hnode) {
    	// Unlike headings, paragraphs and spans, text:display is not attached to the style:
        if (!bDisplayHiddenText && "none".equals(Misc.getAttribute(onode,XMLString.TEXT_DISPLAY))) { return hnode; }
        String sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
        Element div = converter.createElement("div");
        hnode.appendChild(div);
        converter.addTarget(div,sName+"|region");
        StyleInfo sectionInfo = new StyleInfo();
        getSectionSc().applyStyle(sStyleName,sectionInfo);
        applyStyle(sectionInfo,div);
        sections.push(onode);
        Node newhnode = traverseBlockText(onode, div);
        sections.pop();
        return newhnode.getParentNode();
    }
	
    private Element handleHeading(Element onode, Element hnode) {
        int nListLevel = getOutlineLevel((Element)onode);
        if (nListLevel==1) { nChapterNumber++; }
        boolean bUnNumbered = "true".equals(Misc.getAttribute(onode,XMLString.TEXT_IS_LIST_HEADER));
        boolean bRestart = "true".equals(Misc.getAttribute(onode,XMLString.TEXT_RESTART_NUMBERING));
        int nStartValue = Misc.getPosInteger(Misc.getAttribute(onode,XMLString.TEXT_START_VALUE),1)-1;
        return handleHeading(onode, hnode, ofr.getOutlineStyle(), nListLevel, bUnNumbered, bRestart, nStartValue);        
    }

    /*
     * Process a text:h tag
     */
    private Element handleHeading(Element onode, Element oldhnode,
        ListStyle listStyle, int nListLevel, boolean bUnNumbered, boolean bRestart, int nStartValue) {

    	handlePageWidth(onode);

        // Note: nListLevel may in theory be different from the outline level,
        // though the ui in OOo does not allow this

        // TODO: Currently the label is exported as text. Offer CSS solution using counters?

        // Note: Conditional styles are not supported
        int nLevel = getOutlineLevel(onode);
        if (nLevel<=6) { // Export as heading
        	String sStyleName = onode.getAttribute(XMLString.TEXT_STYLE_NAME);
    		StyleWithProperties style = ofr.getParStyle(sStyleName);
    		
    		// Check for hidden text
            if (!bDisplayHiddenText && style!=null && "none".equals(style.getProperty(XMLString.TEXT_DISPLAY))) { return oldhnode; }
            
            // Numbering
        	if (!bUnNumbered) {
        		// If the heading uses a paragraph style which sets an explicit empty list style name, it's unnumbered
        		if (style!=null) {
        			String sListStyleName = style.getListStyleName();
        			if (sListStyleName!=null && sListStyleName.length()==0) {
        				bUnNumbered = true;
        			}
        		}
        	}
        	ListCounter counter = null;
        	String sLabel="";
            if (!bUnNumbered) {
            	counter = getListCounter(listStyle); 
            	if (bRestart) { counter.restart(nListLevel,nStartValue); }
            	sLabel = counter.step(nListLevel).getLabel();
            }  
            
            // Now that we have the label, we are ready to do splitting
            Element hnode =  (Element)maybeSplit(oldhnode,style,sLabel+converter.getPlainInlineText(onode),nLevel);
        	
    		// In EPUB export, a striked out heading will only appear in the external toc            
        	boolean bTocOnly = false;
        	if (converter.isOPS() && style!=null) {
        		String sStrikeOut = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE, true);
        		if (sStrikeOut!=null && !"none".equals(sStrikeOut)) {
        			bTocOnly = true;
        		}
        	}

        	// Export the heading
        	if (!bTocOnly) {
        		// If split output, add headings of higher levels
        		if (hnode!=oldhnode && nSplit>0) {
        			int nFirst = nLevel-nRepeatLevels;
        			if (nFirst<0) { nFirst=0; }                
        			for (int i=nFirst; i<nLevel; i++) {
        				if (currentHeading[i]!=null) {
        					hnode.appendChild(converter.importNode(currentHeading[i],true));
        				}
        			}
        		}	

        		// Apply style
        		StyleInfo info = new StyleInfo();
        		info.sTagName = "h"+nLevel;
        		getHeadingSc().applyStyle(nLevel, sStyleName, info);

        		// add root element
        		Element heading = converter.createElement(info.sTagName);
        		hnode.appendChild(heading);
        		applyStyle(info,heading);
        		traverseFloats(onode,hnode,heading);
        		// Apply writing direction
        		/*String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
            StyleWithProperties style = ofr.getParStyle(sStyleName);
            if (style!=null) {
                StyleInfo headInfo = new StyleInfo(); 
                StyleConverterHelper.applyDirection(style,headInfo);
                getParSc().applyStyle(headInfo,heading);
            }*/

        		// Prepend asapNode
        		prependAsapNode(heading);

        		// Prepend numbering
        		if (!bUnNumbered) {
    				insertListLabel(listStyle,nListLevel,"SectionNumber",null,sLabel,heading);            	
        		}
        		
        		// Add to toc
        		if (!bInToc) {
        			tocCv.handleHeading(onode,heading,sLabel,nChapterNumber);
        		}

        		// Convert content
        		StyleInfo innerInfo = new StyleInfo();
        		getHeadingSc().applyInnerStyle(nLevel, sStyleName, innerInfo);
        		Element content = heading;
        		if (innerInfo.sTagName!=null && innerInfo.sTagName.length()>0) {
        			content = converter.createElement(innerInfo.sTagName);
        			heading.appendChild(content);
        			applyStyle(innerInfo, content);
        		}
        		traverseInlineText(onode,content);

            	// Add before/after text if required
            	addBeforeAfter(heading,ofr.getParStyle(getParSc().getRealParStyleName(sStyleName)),config.getXHeadingStyleMap());
        		
        		// Keep track of current headings for split output
                currentHeading[nLevel] = heading;
                for (int i=nLevel+1; i<=6; i++) {
                    currentHeading[i] = null;
                }
        	}
        	else {
        		if (!bInToc) {
        			tocCv.handleHeadingExternal(onode, hnode, sLabel);
        		}
                // Keep track of current headings for split output
                currentHeading[nLevel] = null;
                for (int i=nLevel+1; i<=6; i++) {
                    currentHeading[i] = null;
                }
        		
        	}
            sCurrentListLabel = null;
            return hnode;
        }
        else { // beyond h6 - export as ordinary paragraph
            handleParagraph(onode,oldhnode);
            sCurrentListLabel = null;
            return oldhnode;
        }
    }

    /*
     * Process a text:p tag
     */
    private void handleParagraph(Node onode, Node hnode) {
    	handlePageWidth(onode);
        boolean bIsEmpty = OfficeReader.isWhitespaceContent(onode);
        if (config.ignoreEmptyParagraphs() && bIsEmpty) {
        	// We mainly need this to avoid loosing page breaks
        	hnode.appendChild(converter.createComment("empty paragraph"));
        	return;
        }
        String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (!bDisplayHiddenText && style!=null && "none".equals(style.getProperty(XMLString.TEXT_DISPLAY))) { return; }
        
        Element par;
        if (ofr.isSpreadsheet()) { // attach inline text directly to parent (always a table cell)
            par = (Element) hnode;
        }
        else {
            // Hack because createParagraph doesn't work the way we need here :-(
            Element temp = converter.createElement("temp");
            par = createParagraph(temp, sStyleName);
            prependAsapNode(par);
            traverseFloats(onode,hnode,par);
            hnode.appendChild(temp.getFirstChild());
        }

        // Maybe add to toc
        tocCv.handleParagraph((Element)onode, par, sCurrentListLabel);
        sCurrentListLabel = null;

        if (!bIsEmpty) {
            par = createTextBackground(par, sStyleName);
            traverseInlineText(onode,par);
        }
        else {
            // An empty paragraph (this includes paragraphs that only contains
            // whitespace) is ignored by the browser, hence we add &nbsp;
            par.appendChild( converter.createTextNode("\u00A0") );
        }        
        
        if (converter.isOPS() && !par.hasChildNodes()) {
            // Finally, in EPUB export, if the exported paragraph turns out to be empty, remove it
    		// Note that par may not be the p-element, but some child of the p-element
        	// The method applyAttributes causes this effect.
        	// Hence we cannot do hnode.removeChild(par), but must use:
    		hnode.removeChild(hnode.getLastChild());
        }
        else {
        	// Otherwise, add before/after text if required
        	addBeforeAfter(par,ofr.getParStyle(getParSc().getRealParStyleName(sStyleName)),config.getXParStyleMap());
        }
    }
    
    private void prependAsapNode(Node node) {
        if (asapNode!=null) {
            // May float past a split; check this first
            if (asapNode.getOwnerDocument()!=node.getOwnerDocument()) {
                asapNode = converter.importNode(asapNode,true);
            }
            node.appendChild(asapNode); asapNode = null;
        }
    }
    
    private void handlePageWidth(Node onode) {
    	String sStyleName = Misc.getAttribute(onode, XMLString.TEXT_STYLE_NAME);
    	if (sStyleName!=null) {
	    	StyleWithProperties style = ofr.getParStyle(sStyleName);
	    	String sMasterPageName = style.getMasterPageName();
	    	if (sMasterPageName!=null) {
	    		converter.setPageContentWidth(converter.getStyleCv().getPageSc().getTextWidth(ofr.getMasterPage(sMasterPageName)));
	    	}
    	}
    }
	
	
    ///////////////////////////////////////////////////////////////////////////
    // LISTS
    ///////////////////////////////////////////////////////////////////////////
	
    // Helper: Get a list counter for a list style
    private ListCounter getListCounter(ListStyle style) {
        if (style==ofr.getOutlineStyle()) {
            // Outline numbering has a special counter
            return outlineNumbering;
        }
        else if (style!=null) {
            // Get existing or create new counter
            if (listCounters.containsKey(style.getName())) {
                return listCounters.get(style.getName());
            }
            else {
                ListCounter counter = new ListCounter(style);
                listCounters.put(style.getName(),counter);
                return counter;
            }
        }
        else {
            // No style, return a dummy
            return new ListCounter();
        }
    }
    
    // Helper: Insert a list label formatted with a list style
    private void insertListLabel(ListStyle style, int nLevel, String sDefaultStyle, String sPrefix, String sLabel, Element hnode) {
        if (sLabel!=null && sLabel.length()>0) {
        	if (sPrefix!=null) {
        		Element prefix = converter.createElement("span");
        		prefix.setAttribute("class", "chapter-name");
        		hnode.appendChild(prefix);
        		prefix.appendChild( converter.createTextNode(sPrefix));
        	}
            StyleInfo info = new StyleInfo();
            if (style!=null) {
                String sTextStyleName = style.getLevelProperty(nLevel,XMLString.TEXT_STYLE_NAME);
                getTextSc().applyStyle(sTextStyleName, info);
            }

            if (info.sTagName==null) { info.sTagName = "span"; }
            if (info.sClass==null) { info.sClass = sDefaultStyle; }

            Element content = converter.createElement(info.sTagName);
            getTextSc().applyStyle(info, content);
            hnode.appendChild(content);
            content.appendChild( converter.createTextNode(sLabel) );
        }
    }
	
    private void handleList(Node onode, int nLevel, String sStyleName, Node hnode) {
        String sStyleName1 = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
        if (sStyleName1!=null) { sStyleName = sStyleName1; }
        
        // List are created on the fly because we may need several lists if an item restarts numbering
        // We also keep track of the numbering to ensure proper continuation with file split
        // The method createList handles this
        Element currentList = null;
        

        // Traverse the list, creating list elements on the way
        Node child = onode.getFirstChild();
        while (child!=null) {
            if (Misc.isElement(child,XMLString.TEXT_LIST_ITEM)) {
            	String sLocalStyleName = sStyleName;
            	// A single list item may override the list style. In this case force a new list for this single item
            	String sStyleOverride = Misc.getAttribute(child, XMLString.TEXT_STYLE_OVERRIDE);
            	if (sStyleOverride!=null) {
            		currentList = null;
            		sLocalStyleName = sStyleOverride;
            	}
                String sStartValue = Misc.getAttribute(child, XMLString.TEXT_START_VALUE);
                currentList = createList(child,nLevel,sLocalStyleName,sStartValue,hnode,currentList);
                ListCounter counter = getListCounter(ofr.getListStyle(sLocalStyleName));
                sCurrentListLabel = counter.getLabel();
                Element item = converter.createElement("li");
                currentList.appendChild(item);
                traverseListItem(child,nLevel,sLocalStyleName,item);
                counter.step(nLevel);
            	if (sStyleOverride!=null) {
            		currentList = null;
            	}
            }
            else if (Misc.isElement(child, XMLString.TEXT_LIST_HEADER)) {
                currentList = createList(child,nLevel,sStyleName,null,hnode,currentList);
                Element item = converter.createElement("li");
                currentList.appendChild(item);
                StyleInfo info = new StyleInfo();
                getListSc().applyUnnumberedItemStyle(sStyleName, info);
                applyStyle(info,item);
                traverseListItem(child,nLevel,sStyleName,item);
                sCurrentListLabel = null;
            }
        	child = child.getNextSibling();
        }
    }
    
    // Determine restart of numbering, create a new list if required and attach it to the current hnode
    private Element createList(Node itemnode, int nLevel, String sStyleName, String sStartValue, Node hnode, Element currentList) {
    	// In addition the the list style we may need the paragraph style
    	String sParStyleName = null;
    	Element par = Misc.getFirstChildElement(itemnode);
    	if (Misc.isElement(par, XMLString.TEXT_P)) {
    		sParStyleName = Misc.getAttribute(par, XMLString.TEXT_STYLE_NAME);
    	}
    	
    	ListCounter counter = getListCounter(ofr.getListStyle(sStyleName));
    	if (sStartValue!=null) { // The list restarts at this item; must create a new list to do this with CSS
            counter.restart(nLevel,Misc.getPosInteger(sStartValue, 1)-1).step(nLevel);
        	return createList(nLevel,sStyleName,sStartValue,sParStyleName,hnode);
        }
    	if (continueNumbering(itemnode.getParentNode()) && currentList==null) { // This is the first item of a continued list
    		if (usedLists.contains(sStyleName)) {
    			return createList(nLevel,sStyleName,"none",sParStyleName,hnode);
    		}
    		else { // Continued list in start of file; must add explicit restart
    			return createList(nLevel,sStyleName,Integer.toString(counter.getValue(nLevel)),sParStyleName,hnode);
    		}
    	}
        else if (currentList==null) { // This is the first item of a list
			counter.restart(nLevel).step(nLevel);
        	ListStyle style = ofr.getListStyle(sStyleName); 
        	if (style!=null) {
        		String sListStartValue = style.getLevelProperty(nLevel,XMLString.TEXT_START_VALUE);
        		if (sListStartValue!=null) {
        			counter.restart(nLevel,Misc.getPosInteger(sListStartValue,1)-1).step(nLevel);
        		}
        	}
			return createList(nLevel,sStyleName,null,sParStyleName,hnode);
        }
        else {
        	return currentList;
        }
    }
    
    // Create a new list and attach it to the current hnode
    private Element createList(int nLevel, String sStyleName, String sStartValue, String sParStyleName, Node hnode) {
	    ListStyle style = ofr.getListStyle(sStyleName);
	    if (style!=null) {
	    	usedLists.add(style.getName());
		    Element list = converter.createElement(style.isNumber(nLevel) ? "ol" : "ul");
		    hnode.appendChild(list);
		    StyleInfo info = new StyleInfo();
		    getListSc().applyStyle(nLevel, sStyleName, sStartValue, sParStyleName, info);
		    applyStyle(info,list);
		    return list;
	    }
	    else {
	    	Element list = converter.createElement("ul");
	    	hnode.appendChild(list);
	    	return list;
	    }
    }
    
    /*
     * Process the contents of a list item
     * (a list header should only contain paragraphs, but we don't care)
     */
    private void traverseListItem (Node onode, int nLevel, String styleName, Node hnode) { 
        // First check if we have a single paragraph to be omitted
        // This should happen if we ignore styles and have no style-map
        // for the paragraph style used        
        if (config.xhtmlFormatting()!=XhtmlConfig.CONVERT_ALL && onode.hasChildNodes()) {
            NodeList list = onode.getChildNodes();
            int nLen = list.getLength();
            int nParCount = 0;
            boolean bNoPTag = true;
            for (int i=0; i<nLen; i++) {
                if (list.item(i).getNodeType()==Node.ELEMENT_NODE) {
                    if (list.item(i).getNodeName().equals(XMLString.TEXT_P)) {
                        nParCount++;
                        if (bNoPTag) {
                            String sDisplayName = ofr.getParStyles().getDisplayName(Misc.getAttribute(list.item(0),XMLString.TEXT_STYLE_NAME));
                            if (config.getXParStyleMap().contains(sDisplayName)) {
                                bNoPTag = false;
                            }
                        }
                    }
                    else { // found non-text:p element
                        bNoPTag=false;
                    }
                }
            }
            if (bNoPTag && nParCount<=1) {
                // traverse the list
                for (int i = 0; i < nLen; i++) {
                    Node child = list.item(i);
          
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String nodeName = child.getNodeName();
                    
                        if (nodeName.equals(XMLString.TEXT_P)) {
                            traverseInlineText(child,hnode);
                        }
                        if (nodeName.equals(XMLString.TEXT_LIST)) { // oasis
                            handleList(child,nLevel+1,styleName,hnode);
                        }
                        if (nodeName.equals(XMLString.TEXT_ORDERED_LIST)) { // old
                            handleList(child,nLevel+1,styleName,hnode);
                        }
                        if (nodeName.equals(XMLString.TEXT_UNORDERED_LIST)) { // old
                            handleList(child,nLevel+1,styleName,hnode);
                        }
                    }
                }
                return;
            }
        }
        // Still here? - traverse block text as usual!
        traverseBlockText(onode,nLevel,styleName,hnode);
    }
    
    private boolean continueNumbering(Node onode) {
    	return "true".equals(Misc.getAttribute(onode,XMLString.TEXT_CONTINUE_NUMBERING)) || (Misc.getAttribute(onode, XMLString.TEXT_CONTINUE_LIST)!=null);
    }
	
    ///////////////////////////////////////////////////////////////////////////
    // FAKE LISTS
    ///////////////////////////////////////////////////////////////////////////
	
    // A fake list is a list which is converted into a sequence of numbered
    // paragraphs rather than into a list.
    // Currently this is done for list which only contains headings
	
    // Helper: Check to see, if this list contains only headings
    // (If so, we will ignore the list and apply the numbering to the headings)   
    private boolean listIsOnlyHeadings(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                if (nodeName.equals(XMLString.TEXT_LIST_ITEM)) {
                    if (!itemIsOnlyHeadings(child)) return false;
                }
                else if (nodeName.equals(XMLString.TEXT_LIST_HEADER)) {
                    if (!itemIsOnlyHeadings(child)) return false;
                }
            }
            child = child.getNextSibling();
        }
        return true;
    }
    
    private boolean itemIsOnlyHeadings(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                if (nodeName.equals(XMLString.TEXT_LIST)) {
                    if (!listIsOnlyHeadings(child)) return false;
                }
                else if (nodeName.equals(XMLString.TEXT_ORDERED_LIST)) {
                    if (!listIsOnlyHeadings(child)) return false;
                }
                else if (nodeName.equals(XMLString.TEXT_UNORDERED_LIST)) {
                    if (!listIsOnlyHeadings(child)) return false;
                }
                else if(!nodeName.equals(XMLString.TEXT_H)) {
                    return false;
                }
            }
            child = child.getNextSibling();
        }
        return true;
    }
	
    // Splitting may occur inside a fake list, so we return the (new) hnode 
    private Node handleFakeList(Node onode, int nLevel, String sStyleName, Node hnode) {
        String sStyleName1 = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
        if (sStyleName1!=null) { sStyleName = sStyleName1; }
        return traverseFakeList(onode,hnode,nLevel,sStyleName);
    }

    // Traverse a list which is not exported as a list but as a sequence of
    // numbered headings/paragraphs
    private Node traverseFakeList (Node onode, Node hnode, int nLevel, String sStyleName) {
        // Restart numbering?
        boolean bContinueNumbering = continueNumbering(onode);
        if (!bContinueNumbering) {
            getListCounter(ofr.getListStyle(sStyleName)).restart(nLevel);
        }

        Node child = onode.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sNodeName = child.getNodeName();
                
                if (sNodeName.equals(XMLString.TEXT_LIST_ITEM)) {
                    boolean bRestart = "true".equals(Misc.getAttribute(child,
                        XMLString.TEXT_RESTART_NUMBERING));
                    int nStartValue = Misc.getPosInteger(Misc.getAttribute(child,
                        XMLString.TEXT_START_VALUE),1);
                    hnode = traverseFakeListItem(child, hnode, nLevel, sStyleName, false, bRestart, nStartValue);
                }
                else if (sNodeName.equals(XMLString.TEXT_LIST_HEADER)) {
                    hnode = traverseFakeListItem(child, hnode, nLevel, sStyleName, true, false, 0);
                }
            }
            child = child.getNextSibling();
        }
        return hnode;
    }
    
    
    // Process the contents of a fake list item
    private Node traverseFakeListItem (Node onode, Node hnode, int nLevel,
        String sStyleName, boolean bUnNumbered, boolean bRestart, int nStartValue) { 
        Node child = onode.getFirstChild();
        while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sNodeName = child.getNodeName();
            
                if (sNodeName.equals(XMLString.TEXT_H)) {
                    nDontSplitLevel++;
                    StyleWithProperties style = ofr.getParStyle(Misc.getAttribute(child, XMLString.TEXT_STYLE_NAME));
                	boolean bPageBreakAfter = maybePageBreak(hnode, style);
                	// Splitting is handled by handleHeading because we need to add the label
                    hnode = handleHeading((Element)child, (Element)hnode, ofr.getListStyle(sStyleName), nLevel,
                        bUnNumbered, bRestart, nStartValue);
                    if (bPageBreakAfter) {
                    	insertPageBreak((Element)hnode, "div");
                    }
                    nDontSplitLevel--;
                    if (nDontSplitLevel==0) { bAfterHeading=true; }
                }
                else if (sNodeName.equals(XMLString.TEXT_P)) {
                     // Currently we only handle fakes lists containing headings
                }
                else if (sNodeName.equals(XMLString.TEXT_LIST)) { // oasis
                     return traverseFakeList(child, hnode, nLevel+1, sStyleName);
                }
                else if (sNodeName.equals(XMLString.TEXT_ORDERED_LIST)) { // old
                     return traverseFakeList(child, hnode, nLevel+1, sStyleName);
                }
                else if (sNodeName.equals(XMLString.TEXT_UNORDERED_LIST)) { // old
                     return traverseFakeList(child, hnode, nLevel+1, sStyleName);
                }
            }
            child = child.getNextSibling();
        }
        return hnode;
    }
	
    ////////////////////////////////////////////////////////////////////////
    // INLINE TEXT
    ////////////////////////////////////////////////////////////////////////
	
    /* Process floating frames bound to this inline text (ie. paragraph) */
    private void traverseFloats(Node onode, Node hnodeBlock, Node hnodeInline) {
        Node child = onode.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                Element elm = (Element) child;
                if (OfficeReader.isDrawElement(elm)) {
                    elm = getDrawCv().getRealDrawElement(elm);
                    String sAnchor = elm.getAttribute(XMLString.TEXT_ANCHOR_TYPE);
                    if (Misc.isElement(elm, XMLString.DRAW_FRAME)) {
                    	elm = Misc.getFirstChildElement(elm);
                    }
                    if (elm!=null) {
                        String sTag = elm.getTagName();
                        // Convert only floating frames; text-boxes must always float
                        if (!"as-char".equals(sAnchor)) {
                            getDrawCv().handleDrawElement(elm,(Element)hnodeBlock,
                                (Element)hnodeInline,nFloatMode);
                        }
                        else if (XMLString.DRAW_TEXT_BOX.equals(sTag)) {
                            getDrawCv().handleDrawElement(elm,(Element)hnodeBlock,
                                (Element)hnodeInline,DrawConverter.INLINE);
                        }
                    }
                }
                else if (OfficeReader.isTextElement(elm)) {
                    // Do not descend into {foot|end}notes
                    if (!OfficeReader.isNoteElement(elm)) {
                        traverseFloats(elm,hnodeBlock,hnodeInline);
                    }
                }
            }
            child = child.getNextSibling();
        }
    }

    /*
     * Process inline text
     */
    protected void traverseInlineText (Node onode,Node hnode) {        
        //String styleName = Misc.getAttribute(onode, XMLString.TEXT_STYLE_NAME);
                              
        if (onode.hasChildNodes()) {
            NodeList nList = onode.getChildNodes();
            int nLen = nList.getLength();
                       
            for (int i = 0; i < nLen; i++) {
                
                Node child = nList.item(i);
                short nodeType = child.getNodeType();
               
                switch (nodeType) {
                    case Node.TEXT_NODE:
                        String s = child.getNodeValue();
                        if (s.length() > 0) {
                            hnode.appendChild( converter.createTextNode(s) );
                        }
                        break;
                        
                    case Node.ELEMENT_NODE:
                        String sName = child.getNodeName();
                        if (OfficeReader.isDrawElement(child)) {
                            Element elm = getDrawCv().getRealDrawElement((Element)child);
                            if (elm!=null) {
                                String sAnchor = (elm.getAttribute(XMLString.TEXT_ANCHOR_TYPE));
                                if ("as-char".equals(sAnchor)) {
                                    getDrawCv().handleDrawElement(elm,null,(Element)hnode,DrawConverter.INLINE);
                                }
                            }
                        }
                        else if (child.getNodeName().equals(XMLString.TEXT_S)) {
                            if (config.ignoreDoubleSpaces()) {
                                hnode.appendChild( converter.createTextNode(" ") );
                            }
                            else {
                                int count= Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_C),1);
                                for ( ; count > 0; count--) {
                                    hnode.appendChild( converter.createTextNode("\u00A0") );
                                }
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_TAB_STOP)) {
                            handleTabStop(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_TAB)) { // oasis
                            handleTabStop(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_LINE_BREAK)) {
                            if (!config.ignoreHardLineBreaks()) {
                                hnode.appendChild( converter.createElement("br") );
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_SPAN)) {
                            handleSpan(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_A)) {
                            handleAnchor(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_FOOTNOTE)) {
                            footCv.handleNote(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_ENDNOTE)) {
                            endCv.handleNote(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_NOTE)) { // oasis
                            if ("endnote".equals(Misc.getAttribute(child,XMLString.TEXT_NOTE_CLASS))) {
                                endCv.handleNote(child,hnode);
                            }
                            else {
                                footCv.handleNote(child,hnode);
                            }
                        }
                        else if (sName.equals(XMLString.TEXT_SEQUENCE)) {
	                        handleSequence(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_PAGE_NUMBER)) {
	                        handlePageNumber(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_PAGE_COUNT)) {
	                        handlePageCount(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_SEQUENCE_REF)) {
	                        handleSequenceRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_FOOTNOTE_REF)) {
	                        handleNoteRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_ENDNOTE_REF)) {
	                        handleNoteRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_NOTE_REF)) { // oasis
	                        handleNoteRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK)) {
	                        handleReferenceMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_MARK_START)) {
	                        handleReferenceMark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_REFERENCE_REF)) {
	                        handleReferenceRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK)) {
	                        handleBookmark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK_START)) {
	                        handleBookmark(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_BOOKMARK_REF)) {
	                        handleBookmarkRef(child,hnode);
                        }
                        else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK)) {
	                        if (!bInToc) { indexCv.handleIndexMark(child,hnode); }
                        }
                        else if (sName.equals(XMLString.TEXT_ALPHABETICAL_INDEX_MARK_START)) {
                        	if (!bInToc) { indexCv.handleIndexMarkStart(child,hnode); }
                        }
                        else if (sName.equals(XMLString.TEXT_TOC_MARK)) {
	                        if (!bInToc) { tocCv.handleTocMark(child,hnode); }
                        }
                        else if (sName.equals(XMLString.TEXT_TOC_MARK_START)) {
                        	if (!bInToc) { tocCv.handleTocMark(child,hnode); }
                        }
                        else if (sName.equals(XMLString.TEXT_USER_INDEX_MARK)) {
                        	if (!bInToc) { userCv.handleUserMark(child,hnode,nChapterNumber); }
                        }
                        else if (sName.equals(XMLString.TEXT_USER_INDEX_MARK_START)) {
                        	if (!bInToc) { userCv.handleUserMark(child,hnode,nChapterNumber); }
                        }
                        else if (sName.equals(XMLString.TEXT_BIBLIOGRAPHY_MARK)) {
                        	if (!bInToc) { handleBibliographyMark(child,hnode); }
                        }
                        else if (sName.equals(XMLString.TEXT_SOFT_PAGE_BREAK)) {
                        	if (nPageBreakSplit==XhtmlConfig.ALL) { bPendingPageBreak = true; }
                        	if (nSoftPageBreaksLimit--!=0) { insertPageBreak(hnode,"span"); }
                        }
                        else if (sName.equals(XMLString.OFFICE_ANNOTATION)) {
                            converter.handleOfficeAnnotation(child,hnode);
                        }
						else if (sName.startsWith("text:")) {
							 traverseInlineText(child,hnode);
						}
                        // other tags are ignored;
                        break;
                    default:
                        // Do nothing
                }
            }
        }
    }
	
    private void handleTabStop(Node onode, Node hnode) {
        // xhtml does not have tab stops, but we export and ASCII TAB character, which the
        // user may choose to format
        if (config.getXhtmlTabstopStyle().length()>0) {
            Element span = converter.createElement("span");
            hnode.appendChild(span);
            span.setAttribute("class",config.getXhtmlTabstopStyle());
            span.appendChild(converter.createTextNode("\t"));
        }
        else {
            hnode.appendChild(converter.createTextNode("\t"));
        }
    }
	
    private void handleSpan(Node onode, Node hnode) {
        StyleWithProperties style = ofr.getTextStyle(Misc.getAttribute(onode, XMLString.TEXT_STYLE_NAME));
        if (!bDisplayHiddenText && style!=null && "none".equals(style.getProperty(XMLString.TEXT_DISPLAY))) { return; }

    	if (!bInToc) {
    		String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
    		Element span = createInline((Element) hnode,sStyleName);
    		traverseInlineText(onode,span);
    	}
    	else {
    		traverseInlineText(onode,hnode);
    	}
    }

    protected void traversePCDATA(Node onode, Node hnode) {
        if (onode.hasChildNodes()) {
            NodeList nl = onode.getChildNodes();
            int nLen = nl.getLength();
            for (int i=0; i<nLen; i++) {
                if (nl.item(i).getNodeType()==Node.TEXT_NODE) {
                    hnode.appendChild( converter.createTextNode(nl.item(i).getNodeValue()) );
                }
            }
        }
    }
    
    protected void handleAnchor(Node onode, Node hnode) {
        Element anchor = converter.createLink((Element)onode);
        hnode.appendChild(anchor);
        traverseInlineText(onode,anchor);
    }
    
    private void handlePageNumber(Node onode, Node hnode) {
        // doesn't make any sense...
        hnode.appendChild( converter.createTextNode("(Page number)") );
    }
    
    private void handlePageCount(Node onode, Node hnode) {
       // also no sense
        hnode.appendChild( converter.createTextNode("(Page count)") );
    }

    private void handleSequence(Node onode, Node hnode) {
        // Use current value, but turn references into hyperlinks
        String sName = Misc.getAttribute(onode,XMLString.TEXT_REF_NAME);
        if (sName!=null && !bInToc && ofr.hasSequenceRefTo(sName)) {
            Element anchor = converter.createTarget("seq"+sName);
            hnode.appendChild(anchor);
            traversePCDATA(onode,anchor);
        }
        else {
            traversePCDATA(onode,hnode);
        }        
    }
	
    private void createReference(Node onode, Node hnode, String sPrefix) {
        // Turn reference into hyperlink
        String sFormat = Misc.getAttribute(onode,XMLString.TEXT_REFERENCE_FORMAT);
        String sName = Misc.getAttribute(onode,XMLString.TEXT_REF_NAME);
        Element anchor = converter.createLink(sPrefix+sName,"page".equals(sFormat));
        hnode.appendChild(anchor);
        if (!"page".equals(sFormat)) { // use current value if not a page reference
            traversePCDATA(onode,anchor);
        }
    }
		
    private void handleSequenceRef(Node onode, Node hnode) {
   		createReference(onode,hnode,"seq");
    } 

    private void handleNoteRef(Node onode, Node hnode) {
        createReference(onode,hnode,"");
    } 
        
    private void handleReferenceMark(Node onode, Node hnode) {
        String sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        if (sName!=null && !bInToc && ofr.hasReferenceRefTo(sName)) {
            hnode.appendChild(converter.createTarget("ref"+sName));
        }
    }
	
    private void handleReferenceRef(Node onode, Node hnode) {
   		createReference(onode,hnode,"ref");
    } 

    private void handleBookmark(Node onode, Node hnode) {
        // Note: Two targets (may be the target of a hyperlink or a reference)
        String sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        if (sName!=null && !bInToc) {
            hnode.appendChild(converter.createTarget(sName));
            if (ofr.hasBookmarkRefTo(sName)) {
            	hnode.appendChild(converter.createTarget("bkm"+sName));
            }
        }
    }
	
    private void handleBookmarkRef(Node onode, Node hnode) {
        createReference(onode,hnode,"bkm");
    } 
	
    private void handleBibliographyMark(Node onode, Node hnode) {
        if (bInToc) {
            traversePCDATA(onode,hnode);
        }
        else {
        	bibCv.handleBibliographyMark(onode, hnode);
        }
    }
	
    ///////////////////////////////////////////////////////////////////////////
    // UTILITY METHODS
    ///////////////////////////////////////////////////////////////////////////
    
    // Insert text before/after in an element
    private void addBeforeAfter(Element elm, StyleWithProperties style, XhtmlStyleMap styleMap) {
    	if (style!=null && styleMap.contains(style.getDisplayName())) {
    		XhtmlStyleMapItem mapItem = styleMap.get(style.getDisplayName());
    		if (mapItem.sBefore!=null && mapItem.sBefore.length()>0) {
    			Node child = elm.getFirstChild();
    			if (child!=null) {
    				elm.insertBefore(converter.createTextNode(mapItem.sBefore),child);
    			}
    			else {
    				elm.appendChild(converter.createTextNode(mapItem.sBefore));
    			}
    		}
    		if (mapItem.sAfter!=null && mapItem.sAfter.length()>0) {
				elm.appendChild(converter.createTextNode(mapItem.sAfter));    			
    		}
    		
    	}
    }
    
    // Methods to query individual formatting properties (no inheritance)
	
    // Does this style contain the bold attribute?
    private boolean isBold(StyleWithProperties style) {
        String s = style.getProperty(XMLString.FO_FONT_WEIGHT,false);
        return s!=null && "bold".equals(s);
    }

    // Does this style contain the italics/oblique attribute?
    private boolean isItalics(StyleWithProperties style) {
        String s = style.getProperty(XMLString.FO_FONT_STYLE,false);
        return s!=null && !"normal".equals(s);
    }
	
    // Does this style contain a fixed pitch font?
    private boolean isFixed(StyleWithProperties style) {
        String s = style.getProperty(XMLString.STYLE_FONT_NAME,false);
        String s2 = null;
        String s3 = null;
        if (s!=null) {
            FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(s);
            if (fd!=null) {
                s2 = fd.getFontFamilyGeneric();
                s3 = fd.getFontPitch();
            }
        }
        else {            
            s = style.getProperty(XMLString.FO_FONT_FAMILY,false);
            s2 = style.getProperty(XMLString.STYLE_FONT_FAMILY_GENERIC,false);
            s3 = style.getProperty(XMLString.STYLE_FONT_PITCH,false);
        }
        if ("fixed".equals(s3)) { return true; }
        if ("modern".equals(s2)) { return true; }
        return false;
    }

    // Does this style specify superscript?
    private boolean isSuperscript(StyleWithProperties style) {
        String sPos = style.getProperty(XMLString.STYLE_TEXT_POSITION,false);
        if (sPos==null) return false;
        if (sPos.startsWith("sub")) return false;
        if (sPos.startsWith("-")) return false;
        if (sPos.startsWith("0%")) return false;
        return true;
    }

    // Does this style specify subscript?
    private boolean isSubscript(StyleWithProperties style) {
        String sPos = style.getProperty(XMLString.STYLE_TEXT_POSITION,false);
        if (sPos==null) return false;
        if (sPos.startsWith("sub")) return true;
        if (sPos.startsWith("-")) return true;
        return false;
    }
    
    // Does this style specify underline?
    private boolean isUnderline(StyleWithProperties style) {
    	String s;
        if (ofr.isOpenDocument()) {
            s = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,false);
        }
        else {
            s = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE,false);
        }
        return s!=null && !"none".equals(s);
    }
	
    // Does this style specify overstrike?
    private boolean isOverstrike(StyleWithProperties style) {
    	String s;
        if (ofr.isOpenDocument()) {
            s = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE,false);
        }
        else {
            s = style.getProperty(XMLString.STYLE_TEXT_CROSSING_OUT,false);
        }
        return s!=null && !"none".equals(s);
    }
	
    /* apply hard formatting attribute style maps */
    private Element applyAttributes(Element node, StyleWithProperties style) {
        // Do nothing if we convert hard formatting
        if (config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES) { return node; }
        // Do nothing if this is not an automatic style
        if (style==null) { return node; }
        if (!style.isAutomatic()) { return node; }
        node = applyAttribute(node,"bold",isBold(style));
        node = applyAttribute(node,"italics",isItalics(style));
        node = applyAttribute(node,"fixed",isFixed(style));
        node = applyAttribute(node,"superscript",isSuperscript(style));
        node = applyAttribute(node,"subscript",isSubscript(style));
        node = applyAttribute(node,"underline",isUnderline(style));
        node = applyAttribute(node,"overstrike",isOverstrike(style));
        return node;
    }
	
    /* apply hard formatting attribute style maps */
    private Element applyAttribute(Element node, String sAttr, boolean bApply) {
    	if (bApply) {
    		XhtmlStyleMap xattr = config.getXAttrStyleMap();
    		if (xattr.contains(sAttr) && xattr.get(sAttr).sElement.length()>0) {
    			XhtmlStyleMapItem map = xattr.get(sAttr);
    			Element attr = converter.createElement(map.sElement);
    			if (!"(none)".equals(map.sCss)) {
    				attr.setAttribute("class",map.sCss);
    			}
    			node.appendChild(attr);
    			return attr;
    		}
    	}
    	return node;
    }
	
    /* Create and append a  styled paragraph node */
    protected Element createParagraph(Element hnode, String sStyleName) {
        StyleInfo info = new StyleInfo();
        getParSc().applyStyle(sStyleName,info);
        Element par = converter.createElement(info.sTagName);
        hnode.appendChild(par);
        applyStyle(info,par);
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style!=null && style.isAutomatic()) {
            return applyAttributes(par,style);
        }
        else {
            return par;
        }
    }
	
    /* Create an inline node with background style from paragraph style */
    private Element createTextBackground(Element node, String sStyleName) {
        if (config.xhtmlFormatting()==XhtmlConfig.IGNORE_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD) {
            return node;
        } 
        String sBack = getParSc().getTextBackground(sStyleName);
        if (sBack.length()>0) {
            Element span = converter.createElement("span");
            span.setAttribute("style",sBack);
            node.appendChild(span);
            return span;
        }
        else {
            return node;
        }
    }
		
    /* Create a styled inline node */
    protected Element createInline(Element node, String sStyleName) {
        StyleInfo info = new StyleInfo();
        getTextSc().applyStyle(sStyleName,info);
        Element newNode = node;
        if (info.hasAttributes() || !"span".equals(info.sTagName)) {
            // We (probably) need to create a new element
            newNode = converter.createElement(info.sTagName);
            applyStyle(info,newNode);
            // But we may want to merge it with the previous element
            Node prev = node.getLastChild();
            if (prev!=null && Misc.isElement(prev, info.sTagName)) {
            	// The previous node is of the same type, compare attributes
            	Element prevNode = (Element) prev;
            	if (newNode.getAttribute("class").equals(prevNode.getAttribute("class")) &&
            		newNode.getAttribute("style").equals(prevNode.getAttribute("style")) &&
            		newNode.getAttribute("xml:lang").equals(prevNode.getAttribute("xml:lang")) &&
            		newNode.getAttribute("dir").equals(prevNode.getAttribute("dir"))) {
            			// Attribute style mapped elements are *not* merged, we will live with that
            			return applyAttributes(prevNode,ofr.getTextStyle(sStyleName));
            	}
            }
            node.appendChild(newNode);
       }
       return applyAttributes(newNode,ofr.getTextStyle(sStyleName));
    }

    protected int getOutlineLevel(Element node) {
        return ofr.isOpenDocument() ?
            Misc.getPosInteger(node.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1):
            Misc.getPosInteger(node.getAttribute(XMLString.TEXT_LEVEL),1);
    }

}
