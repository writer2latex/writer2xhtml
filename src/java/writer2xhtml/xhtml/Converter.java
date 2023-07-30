/************************************************************************
 *
 *  Converter.java
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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.io.InputStream;
import java.io.IOException;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import writer2xhtml.api.Config;
import writer2xhtml.api.ContentEntry;
import writer2xhtml.api.ConverterFactory;
import writer2xhtml.api.OutputFile;
import writer2xhtml.base.ContentEntryImpl;
import writer2xhtml.base.ConverterBase;
import writer2xhtml.office.MIMETypes;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.ExportNameCollection;
import writer2xhtml.util.Misc;
import writer2xhtml.xhtml.l10n.L10n;

/**
 * <p>This class converts an OpenDocument file to an XHTML(+MathML) or EPUB document.</p>
 *
 */
public class Converter extends ConverterBase {
	private static final String EPUB_STYLES_FOLDER = "styles/";
	private static final String EPUB_STYLESHEET = "styles/styles1.css";
	private static final String EPUB_CUSTOM_STYLESHEET = "styles/styles.css";
	
    // Config
    private XhtmlConfig config;
	
    public Config getConfig() { return config; }
    
    protected XhtmlConfig getXhtmlConfig() { return config; }

    // The locale
    private L10n l10n;
                        
    // The helpers
    private StyleConverter styleCv;
    private TextConverter textCv;
    private TableConverter tableCv;
    private DrawConverter drawCv;
    private MathConverter mathCv;
	
    // The template
    private XhtmlDocument template = null;
    
    // The included style sheet and associated resources
    private CssDocument styleSheet = null;
    private Set<ResourceDocument> resources = new HashSet<>();
    
    // The xhtml output file(s)
    protected int nType = XhtmlDocument.XHTML10; // the doctype
    private boolean bOPS = false; // Do we need to be OPS conforming?
    List<XhtmlDocument> outFiles;
    private int nOutFileIndex;
    private XhtmlDocument htmlDoc; // current outfile
    private Document htmlDOM; // current DOM, usually within htmlDoc
    private boolean bNeedHeaderFooter = false;
    private Set<String> usedFileTitles = new HashSet<>();
    private ExportNameCollection fileNames = new ExportNameCollection("",true,"-");

    // Hyperlinks
    Map<String, Integer> targets = new HashMap<>();
    List<LinkDescriptor> links = new ArrayList<>();
    // Strip illegal characters from internal hyperlink targets
    private ExportNameCollection targetNames = new ExportNameCollection(true);
    
    // The current context (currently we only track the content width, but this might be expanded with formatting
    // attributes - at least background color and font size later)
    // The page content width serves as a base, and may possibly change even if the stack is non-empty
    private String pageContentWidth = null;
    private Stack<String> contentWidth = new Stack<>();
    
    // Constructor setting the DOCTYPE
    public Converter(int nType) {
        super();
        config = new XhtmlConfig();
        this.nType = nType;
    }

    // override methods to read templates, style sheets and resources
    @Override public void readTemplate(InputStream is) throws IOException {
        template = new XhtmlDocument("Template",nType,null,null,-1);
        template.read(is);
    }
	
    @Override public void readTemplate(File file) throws IOException {
        readTemplate(new FileInputStream(file));
    }

    @Override public void readStyleSheet(InputStream is) throws IOException {
    	if (styleSheet==null) {
    		styleSheet = new CssDocument(EPUB_CUSTOM_STYLESHEET);
    	}
    	styleSheet.read(is);
    }
	
    @Override public void readStyleSheet(File file) throws IOException {
        readStyleSheet(new FileInputStream(file));
    }
    
    @Override public void readResource(InputStream is, String sFileName, String sMediaType) throws IOException {
    	if (sMediaType==null) {
    		// Guess the media type from the file extension
    		sMediaType="";
    		String sfilename = sFileName.toLowerCase();
    		// PNG, JPEG and GIF are the basic raster image formats that must be supported by EPUB readers
    		if (sfilename.endsWith(MIMETypes.PNG_EXT)) { sMediaType = MIMETypes.PNG; }
    		else if (sfilename.endsWith(MIMETypes.JPEG_EXT)) { sMediaType = MIMETypes.JPEG; }
    		else if (sfilename.endsWith(".jpeg")) { sMediaType = MIMETypes.JPEG; }
    		else if (sfilename.endsWith(MIMETypes.GIF_EXT)) { sMediaType = MIMETypes.GIF; }
    		// The OPS 2.0.1 specification recommends (and only mentions) OpenType with this media type
    		else if (sfilename.endsWith(".otf")) { sMediaType = "application/vnd.ms-opentype"; }
    		// For convenience we also include a media type for true type fonts (the most common, it seems)
    		else if (sfilename.endsWith(".ttf")) { sMediaType = "application/x-font-ttf"; }
    	}
    	ResourceDocument doc = new ResourceDocument(EPUB_STYLES_FOLDER+sFileName, sMediaType);
    	doc.read(is);
    	resources.add(doc);
    }
    
    @Override public void readResource(File file, String sFileName, String sMediaType) throws IOException {
    	readResource(new FileInputStream(file), sFileName, sMediaType);
    }    
    
    protected String getContentWidth() {
    	if (!contentWidth.isEmpty()) {
    		return contentWidth.peek();
    	}
    	else {
    		return pageContentWidth;
    	}
    }
    
    protected void setPageContentWidth(String sWidth) {
    	pageContentWidth = sWidth;
    }
    
    protected String pushContentWidth(String sWidth) {
    	return contentWidth.push(sWidth);
    }
    
    protected void popContentWidth() {
    	contentWidth.pop();
    }
    
    protected boolean isTopLevel() {
    	return contentWidth.size()==1;
    }

    protected StyleConverter getStyleCv() { return styleCv; }
	
    protected TextConverter getTextCv() { return textCv; }
	
    protected TableConverter getTableCv() { return tableCv; }

    protected DrawConverter getDrawCv() { return drawCv; }

    protected MathConverter getMathCv() { return mathCv; }
	
    protected int getType() { return nType; }
    
    public boolean isHTML5() { return nType>=XhtmlDocument.HTML5; }
	
    protected int getOutFileIndex() { return nOutFileIndex; }
    
    protected void addContentEntry(String sTitle, int nLevel, String sTarget) {
    	converterResult.addContentEntry(new ContentEntryImpl(sTitle,nLevel,htmlDoc,sTarget));
    }
    
    protected void setTocFile(String sTarget) {
    	converterResult.setTocFile(new ContentEntryImpl(l10n.get(L10n.CONTENTS),1,htmlDoc,sTarget));
    	//nTocFileIndex = nOutFileIndex;
    }
	
    protected void setLofFile(String sTarget) {
    	converterResult.setLofFile(new ContentEntryImpl("Figures",1,htmlDoc,sTarget));
    }
	
    protected void setLotFile(String sTarget) {
    	converterResult.setLotFile(new ContentEntryImpl("Tables",1,htmlDoc,sTarget));
    }
	
    protected void setIndexFile(String sTarget) {
    	converterResult.setIndexFile(new ContentEntryImpl(l10n.get(L10n.INDEX),1,htmlDoc,sTarget));
    	//nAlphabeticalIndex = nOutFileIndex;
    }
    
    protected void setCoverFile(String sTarget) {
    	converterResult.setCoverFile(new ContentEntryImpl("Cover",0,htmlDoc,sTarget));
    }
	
    protected void setCoverImageFile(OutputFile file, String sTarget) {
    	converterResult.setCoverImageFile(new ContentEntryImpl("Cover image",0,file,sTarget));
    }
	
    protected void addOriginalPageNumber(String sTitle, int nLevel, String sTarget) {
    	converterResult.addOriginalPageNumber(new ContentEntryImpl(sTitle,nLevel,htmlDoc,sTarget));
    }
    
    protected void removeOriginalPageNumber() {
    	converterResult.removeOriginalPageNumber();
    }
    
    protected Element createElement(String s) { return htmlDOM.createElement(s); }
    
    protected Comment createComment(String s) { return htmlDOM.createComment(s); }
    
    Element createAlternativeElement(String sHTML5, String sHTML) {
		if (isHTML5() && !(isOPS() && config.avoidHtml5())) {
			// Exception is for compatibility with older EPUB readers not understanding HTML5 tags
			return createElement(sHTML5);
		}
		else {
			return createElement(sHTML);
		}

    }

    protected Text createTextNode(String s) { return htmlDOM.createTextNode(s); }
	
    protected Node importNode(Node node, boolean bDeep) { return htmlDOM.importNode(node,bDeep); }
    
    protected void setContainsMath() {
    	outFiles.get(nOutFileIndex).setContainsMath();
    }
	
    protected L10n getL10n() { return l10n; }
    
    public void setOPS(boolean b) { bOPS = true; }
    
    public boolean isOPS() { return bOPS; }
    
    @Override public void convertInner() throws IOException {
    	// Set the base target file name
        sTargetFileName = Misc.trimDocumentName(sTargetFileName, XhtmlDocument.getExtension(nType));    		
		
        // Prepare the output files
        outFiles = new ArrayList<>();
        nOutFileIndex = -1;

        // Do we need to autocreate header and footer? (Default template only)
        bNeedHeaderFooter = !bOPS
        		&& (ofr.isSpreadsheet() || config.getXhtmlSplitLevel()>0 || config.pageBreakSplit()>XhtmlConfig.NONE || config.getXhtmlUplink().length()>0);
        
        // Initialize converter
        setLocale();
        initializeImageConverter();
        initializeConverterHelpers();
        
        // Set the main content width (used to calculate relative widths)
        setPageContentWidth(getStyleCv().getPageSc().getTextWidth(ofr.getFirstMasterPage()));

        // Traverse the body
        Element body = ofr.getContent();
        if (ofr.isSpreadsheet()) {
        	tableCv.convertTableContent(body);
        }
        else {
        	textCv.convertTextContent(body);
        }
		
        // Post processing
        addTitleAndTextPageEntries();
        resolveLinks();
        loadMathJax();
        generateHeadersAndFooters();
        generatePanels();
        exportStyles();
    }
    
    // Set the language to the document language
    private void setLocale() {
        l10n = new L10n();
        StyleWithProperties style = ofr.isSpreadsheet() ? ofr.getDefaultCellStyle() : ofr.getDefaultParStyle();
        if (style!=null) {
        	// The only CTL language recognized currently is farsi
        	if ("fa".equals(style.getProperty(XMLString.STYLE_LANGUAGE_COMPLEX))) {
        		l10n.setLocale("fa", "IR");
        	}
        	else {
        		l10n.setLocale(style.getProperty(XMLString.FO_LANGUAGE), style.getProperty(XMLString.FO_COUNTRY));
        	}
        }
    }
    
    private void initializeImageConverter() {
        imageConverter.setUseBase64(config.embedImg());
        
        if (isOPS()) {
        	imageConverter.setBaseFileName("image");
        	imageConverter.setUseSubdir("images");
        }
        else { 
        	imageConverter.setBaseFileName(sTargetFileName+"-img");
        	if (config.saveImagesInSubdir()) {
        		imageConverter.setUseSubdir(sTargetFileName+"-img");        	
        	}
        }

        imageConverter.setDefaultFormat(MIMETypes.PNG);
        imageConverter.addAcceptedFormat(MIMETypes.JPEG);
        imageConverter.addAcceptedFormat(MIMETypes.GIF);
        
        if (isHTML5()) { // HTML5 supports SVG as well
        	imageConverter.setDefaultVectorFormat(MIMETypes.SVG);
        }
    }
    
    private void initializeConverterHelpers() {
        styleCv = new StyleConverter(ofr,config,this,nType);
        textCv = new TextConverter(ofr,config,this);
        tableCv = new TableConverter(ofr,config,this);
        drawCv = new DrawConverter(ofr,config,this);
        mathCv = new MathConverter(ofr,config,this,nType!=XhtmlDocument.XHTML10 && nType!=XhtmlDocument.XHTML11);
    }
    
    // For EPUB export we need to identify the title page and the (first) text page
    private void addTitleAndTextPageEntries() {
        if (converterResult.getContent().isEmpty()) {
        	// No headings in the document: There is no title page and the text page is the first page
        	converterResult.setTextFile(new ContentEntryImpl("Text", 1, outFiles.get(0), null));
        	// We also have to add a toc entry (the ncx file cannot be empty)
        	converterResult.addContentEntry(new ContentEntryImpl("Text", 1, outFiles.get(0), null));
        }
        else {
        	ContentEntry firstHeading = converterResult.getContent().get(0);
        	// The title page is the first page after the cover, unless the first page starts with a heading
        	int nFirstPage = converterResult.getCoverFile()!=null ? 1 : 0;
        	if (outFiles.get(nFirstPage)!=firstHeading.getFile() || firstHeading.getTarget()!=null) {
        		converterResult.setTitlePageFile(new ContentEntryImpl("Title page", 1, outFiles.get(nFirstPage), null));
        	}
        	// The text page is the one containing the first heading
        	converterResult.setTextFile(new ContentEntryImpl("Text", 1, firstHeading.getFile(), firstHeading.getTarget()));
        }    	
    }
    
    private void resolveLinks() {
        for (LinkDescriptor ld : links) {
            Integer targetIndex = targets.get(ld.sId);
            if (targetIndex!=null) {
                int nTargetIndex = targetIndex.intValue();
                if (nTargetIndex == ld.nIndex) { // same file
                    ld.element.setAttribute("href","#"+targetNames.getExportName(ld.sId));
                }
                else {
                    //ld.element.setAttribute("href",getOutFileName(nTargetIndex,true)+"#"+targetNames.getExportName(ld.sId));
                	ld.element.setAttribute("href",outFiles.get(nTargetIndex).getFileName()+"#"+targetNames.getExportName(ld.sId));
                }
                if (ld.bPageRef) { // insert page number
                	ld.element.appendChild(ld.element.getOwnerDocument().createTextNode(Integer.toString(nTargetIndex+1)));
                }
            }
        }
    }
    
    private void loadMathJax() {
        // TODO: Should we support different configurations of MathJax?
        if ((isHTML5() || nType==XhtmlDocument.XHTML_MATHML) && config.useMathJax()) {
        	for (int i=0; i<=nOutFileIndex; i++) {
        		if (outFiles.get(i).containsMath()) {
        			XhtmlDocument doc = outFiles.get(i);
        			Element head = doc.getHeadNode();
        			if (head!=null) {
        				Element script = doc.getContentDOM().createElement("script");
        				head.appendChild(script);
        				if (!isHTML5()) { script.setAttribute("type", "text/javascript"); }
        				script.setAttribute("src", "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.4/latest.js?config=TeX-MML-AM_CHTML");
        				script.setAttribute("async","async");
        			}
        		}
        	}
        }    	
    }
    
    // Add headers and footers to all files; slightly different between spreadsheets and text documents
    private void generateHeadersAndFooters() {
        for (int nFileIndex=0; nFileIndex<=nOutFileIndex; nFileIndex++) {
	        XhtmlDocument doc = outFiles.get(nFileIndex);
	        if (ofr.isSpreadsheet()) {
    	        generateHeaderOrFooterCalc(nFileIndex, doc.getContentDOM(), doc.getHeaderNode());
    	        generateHeaderOrFooterCalc(nFileIndex, doc.getContentDOM(), doc.getFooterNode());
	        }
	        else {
    	        generateHeaderOrFooterWriter(nFileIndex, doc.getContentDOM(), doc.getHeaderNode());
    	        generateHeaderOrFooterWriter(nFileIndex, doc.getContentDOM(), doc.getFooterNode());
	        }
	        if (doc.getFooterNode()!=null) {
	        	doc.getFooterNode().setAttribute("style","clear:both"); // no floats may pass the footer!
	        }
        }
    }
    
    // For spreadsheets the header/footer contains an uplink + links to all sheets
    private void generateHeaderOrFooterCalc(int nFileIndex, Document dom, Element hnode) {
        if (hnode!=null) {
        	addUplink(dom, hnode);
            int nSheets = tableCv.sheetNames.size();
            for (int nSheetIndex=0; nSheetIndex<nSheets; nSheetIndex++) {
                if (config.xhtmlCalcSplit()) {
                    addNavigationLink(dom,hnode,tableCv.sheetNames.get(nSheetIndex),nFileIndex,nSheetIndex);
                }
                else {
                    addInternalNavigationLink(dom,hnode,tableCv.sheetNames.get(nSheetIndex),"tableheading"+nSheetIndex);
                }
        	}
        }    	
    }
    
    // For text documents the header/footer contains first-previous-next-last links + links to table of contents and alphabetical index
    private void generateHeaderOrFooterWriter(int nFileIndex, Document dom, Element hnode) {
        if (hnode!=null) {
        	addUplink(dom, hnode);
        	if (config.getXhtmlSplitLevel()>0) {
	            addNavigationLink(dom, hnode, l10n.get(L10n.FIRST), nFileIndex, 0);
	            addNavigationLink(dom, hnode, l10n.get(L10n.PREVIOUS), nFileIndex, nFileIndex-1);
	            addNavigationLink(dom, hnode, l10n.get(L10n.NEXT), nFileIndex, nFileIndex+1);
	            addNavigationLink(dom, hnode, l10n.get(L10n.LAST), nFileIndex, nOutFileIndex);
	            if (textCv.getTocIndex()>=0) {
	                addNavigationLink(dom, hnode, l10n.get(L10n.CONTENTS), nFileIndex, textCv.getTocIndex());
	            }
	            if (textCv.getAlphabeticalIndex()>=0) {
	                addNavigationLink(dom, hnode, l10n.get(L10n.INDEX), nFileIndex, textCv.getAlphabeticalIndex());
	            }
        	}
        }    	
    }
    
    private void addUplink(Document dom, Element hnode) {
    	if (!config.getXhtmlUplink().isEmpty()) {
    		Element a = dom.createElement("a");
		    a.setAttribute("href", config.getXhtmlUplink());
		    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
		    hnode.appendChild(a);
		    hnode.appendChild(dom.createTextNode(" "));
    	}
    }
    
    private void generatePanels() {
        for (int nSourceIndex=0; nSourceIndex<=nOutFileIndex; nSourceIndex++) {
            XhtmlDocument doc = outFiles.get(nSourceIndex);
            Element panel = doc.getPanelNode();
            if (panel!=null) {
                Document dom = doc.getContentDOM();
                int nCurrentLevel = 1;
            	for (int nTargetIndex=0; nTargetIndex<=nOutFileIndex; nTargetIndex++) {
            		int nThisLevel = outFiles.get(nTargetIndex).getOutlineLevel();
            		if (nThisLevel<nCurrentLevel) {
            			// We are leaving a lower level
            			nCurrentLevel = nThisLevel;
            		}
            		else if (nThisLevel>nCurrentLevel) {
            			if (nTargetIndex==nSourceIndex+1) {
            				// We are descending into a lower level which should be expanded because it immediately follows the source file
            				nCurrentLevel = nThisLevel;
            			}
            			else {
            				// We descend into this level only if it contains the source file
            				int n = nTargetIndex;
            				while (n<=nOutFileIndex && outFiles.get(n).getOutlineLevel()>nCurrentLevel) {
            					if (n++==nSourceIndex) {
            						// Found the source, so we will descend
            						nCurrentLevel = nThisLevel;
            					}
            				}
            			}
            		}
            		if (nThisLevel==nCurrentLevel) {
	            		Element p = dom.createElement("p");
	            		p.setAttribute("class", "level"+nThisLevel);
	                    panel.appendChild(p);
	                    addNavigationLink(dom, p, outFiles.get(nTargetIndex).getFileLabel( )+outFiles.get(nTargetIndex).getFileTitle( ),
	                    		nSourceIndex, nTargetIndex);
            		}
            	}
            }
        }      
    }
    
    // Add a navigation link to another output file and return the link element
    private void addNavigationLink(Document dom, Node node, String sTitle, int nSourceIndex, int nTargetIndex) {
        if (nTargetIndex>=0 && nTargetIndex<=nOutFileIndex && nSourceIndex!=nTargetIndex) {
            Element a = dom.createElement("a");
            //a.setAttribute("href",Misc.makeHref(getOutFileName(nTargetIndex,true)));
            a.setAttribute("href",Misc.makeHref(outFiles.get(nTargetIndex).getFileName()));
            a.appendChild(dom.createTextNode(sTitle));
            node.appendChild(a);
            node.appendChild(dom.createTextNode(" "));
        }
        else {
            Element span = dom.createElement("span");
            span.setAttribute("class","nolink");
            node.appendChild(span);
            span.appendChild(dom.createTextNode(sTitle));
            node.appendChild(dom.createTextNode(" "));
        }
    }
	
    private void addInternalNavigationLink(Document dom, Node node, String s, String sLink) {
        Element a = dom.createElement("a");
        a.setAttribute("href","#"+sLink);
        a.appendChild(dom.createTextNode(s));
        node.appendChild(a);
        node.appendChild(dom.createTextNode(" "));
    }
    
    private void exportStyles() {
        // Add included style sheet, if any - and we are creating OPS content
        if (bOPS && styleSheet!=null) {
        	converterResult.addDocument(styleSheet);
        	for (ResourceDocument doc : resources) {
        		converterResult.addDocument(doc);
        	}
        }
    	
        // Add styles to all HTML files (never used for EPUB)
        if (!isOPS() && !config.separateStylesheet()) {
        	for (int i=0; i<=nOutFileIndex; i++) {
        		Element head = outFiles.get(i).getHeadNode();
        		if (head!=null) {
        			Node styles = styleCv.exportStyles(outFiles.get(i).getContentDOM());
        			if (styles!=null) {
        				head.appendChild(styles);
        			}
        		}
        	}
        }
        
        // Export separate stylesheet
        if (config.xhtmlFormatting()>XhtmlConfig.IGNORE_STYLES) {
        	if (isOPS()) { // EPUB
        		CssDocument cssDoc = new CssDocument(EPUB_STYLESHEET);
        		cssDoc.read(styleCv.exportStyles(false));
        		converterResult.addDocument(cssDoc);
        	}
        	else if (config.separateStylesheet()) { // XHTML
        		CssDocument cssDoc = new CssDocument(sTargetFileName+"-styles.css");
        		cssDoc.read(styleCv.exportStyles(false));
        		converterResult.addDocument(cssDoc);
        	}
        }    	
    }
    
    // get inline text, ignoring any draw objects, footnotes, formatting and hyperlinks
    protected String getPlainInlineText(Node node) {
    	StringBuilder buf = new StringBuilder();
        Node child = node.getFirstChild();
        while (child!=null) {
            short nodeType = child.getNodeType();
               
            switch (nodeType) {
                case Node.TEXT_NODE:
                    buf.append(child.getNodeValue());
                    break;
                        
                case Node.ELEMENT_NODE:
                    String sName = child.getNodeName();
                    if (sName.equals(XMLString.TEXT_S)) {
                           buf.append(" ");
                    }
                    else if (sName.equals(XMLString.TEXT_LINE_BREAK) || sName.equals(XMLString.TEXT_TAB_STOP) || sName.equals(XMLString.TEXT_TAB)) { // text:tab in oasis
                        buf.append(" ");
                    }
                    else if (OfficeReader.isNoteElement(child)) {
                        // ignore
                    }
                    else if (OfficeReader.isTextElement(child)) {
                    	buf.append(getPlainInlineText(child));
                    }
                    break;
                default:
                    // Do nothing
            }
            child = child.getNextSibling();
        }
        return buf.toString();
    }


    public void handleOfficeAnnotation(Node onode, Node hnode) {
        if (config.xhtmlNotes()) {
            // Extract the text from the paragraphs, separate paragraphs with newline
        	StringBuilder buf = new StringBuilder();
        	Element creator = null;
        	Element date = null;
        	Node child = onode.getFirstChild();
        	while (child!=null) {
        		if (Misc.isElement(child, XMLString.TEXT_P)) {
        			if (buf.length()>0) { buf.append('\n'); }
        			buf.append(getPlainInlineText(child));
        		}
        		else if (Misc.isElement(child, XMLString.DC_CREATOR)) {
        			creator = (Element) child;
        		}
        		else if (Misc.isElement(child, XMLString.DC_DATE)) {
        			date = (Element) child;
        		}
        		child = child.getNextSibling();
        	}
        	if (creator!=null) {
    			if (buf.length()>0) { buf.append('\n'); }
    			buf.append(getPlainInlineText(creator));        		
        	}
        	if (date!=null) {
    			if (buf.length()>0) { buf.append('\n'); }
    			buf.append(Misc.formatDate(OfficeReader.getTextContent(date), l10n.getLocale().getLanguage(), l10n.getLocale().getCountry()));
        	}
            Node commentNode = htmlDOM.createComment(buf.toString());
            hnode.appendChild(commentNode);
        }
    }
	
    /////////////////////////////////////////////////////////////////////////
    // UTILITY METHODS
	
    // Return true if the current outfile has a non-empty body
    public boolean outFileHasContent() {
        if (htmlDoc.getContentNode().hasChildNodes()) {
        	Node child = htmlDoc.getContentNode().getFirstChild();
        	if (Misc.isElement(child, "div") && "pagebreak".equals(Misc.getAttribute(child, "epub:type"))) {
        		// We start with an EPUB page break, this does not count as content unless there is more
        		return child.getNextSibling()!=null;
        	}
        	return true;
        }
        return false;
    }
    
    // Use another document. TODO: This is very ugly; clean it up!!!
    public void changeOutFile(int nIndex) {
        nOutFileIndex = nIndex;
        htmlDoc = outFiles.get(nIndex);
        htmlDOM = htmlDoc.getContentDOM();
    }
	
    public Element getPanelNode() {
        return htmlDoc.getPanelNode();
    }
	
    // Prepare next output file
    public Element nextOutFile(String sFileLabel, String sFileTitle, int nLevel) {
        nOutFileIndex++;
        // Use the given file name for the master document
        String sFinalFileName = sTargetFileName;
        // Construct a file name according to the option filenames for remaining documents
        if (nOutFileIndex>0) { // Create the document using a file name created according to the filenames option
	    	if (config.getFilenames()==XhtmlConfig.NAME_NUMBER || sFileTitle==null) { // Sequential numbering of files
	   			sFinalFileName = sTargetFileName+nOutFileIndex;
	    	}
	    	else { // Use modified (for friendly URL) file title for file name
	    		String sModifiedTitle = sFileTitle.replace(' ', '-').toLowerCase();
	    		if (usedFileTitles.contains(sModifiedTitle)) { // We have seen this title before; ensure a unique name
	    			int n=0;
	    			while (usedFileTitles.contains(sModifiedTitle+(++n)));
	    			sModifiedTitle = sModifiedTitle + n;
	    		}
	    		usedFileTitles.add(sModifiedTitle);
	    		if (config.getFilenames()==XhtmlConfig.NAME_SECTION) {
		    		sFinalFileName = sTargetFileName+"-"+fileNames.getExportName(sModifiedTitle);	    			
	    		}
	    		else { // Must be SECTION
		    		sFinalFileName = fileNames.getExportName(sModifiedTitle);
	    		}
	    	}
        }
       	htmlDoc = new XhtmlDocument(sFinalFileName, nType, sFileLabel, sFileTitle, nLevel);
    	
        // Configure and populate the document, and add to result
        htmlDoc.setConfig(config);
        if (template!=null) { htmlDoc.readFromTemplate(template); }
        else if (bNeedHeaderFooter) { htmlDoc.createHeaderFooter(); }
        outFiles.add(nOutFileIndex,htmlDoc);
        converterResult.addDocument(htmlDoc);
        
        // Create head + body
        htmlDOM = htmlDoc.getContentDOM();
        Element rootElement = htmlDOM.getDocumentElement();
        styleCv.applyDefaultLanguage(rootElement);
        addEpubNs(rootElement);
        rootElement.insertBefore(htmlDOM.createComment(
             "This file was converted to HTML by "
             + (ofr.isSpreadsheet() ? "Calc" : "Writer")
             + "2xhtml ver. " + ConverterFactory.getVersion() +
             ". See http://writer2xhtml.sourceforge.net for more info."),
             rootElement.getFirstChild());
		
        // Apply default writing direction
        if (!ofr.isPresentation()) {
            StyleInfo pageInfo = new StyleInfo();
            styleCv.getPageSc().applyDefaultWritingDirection(pageInfo);
            styleCv.getPageSc().applyStyle(pageInfo,htmlDoc.getContentNode());
        }

        // Add title (required by xhtml)
        Element title = htmlDoc.getTitleNode();
        if (title!=null) {
        	String sTitle = metaData.getTitle();
        	if (sTitle==null || sTitle.length()==0) { // use filename as fallback
        		sTitle = Misc.removeExtension(htmlDoc.getFileName());
        	}
        	title.appendChild( htmlDOM.createTextNode(sTitle) );
        }

        Element head = htmlDoc.getHeadNode();
        if (head!=null) {
        	// Declare charset (we need this for XHTML 1.0 strict and HTML5 because we have no <?xml ... ?>)
        	if (nType==XhtmlDocument.XHTML10) {
        		Element meta = htmlDOM.createElement("meta");
        		meta.setAttribute("http-equiv","Content-Type");
        		meta.setAttribute("content","text/html; charset="+htmlDoc.getEncoding().toLowerCase());
        		head.appendChild(meta);
        	}
        	else if (isHTML5()) {
        		// The encoding should be UTF-8, but we still respect the user's setting
        		Element meta = htmlDOM.createElement("meta");
        		meta.setAttribute("charset",htmlDoc.getEncoding().toUpperCase());
        		head.appendChild(meta);        		
        	}

        	// Add meta data (for EPUB the meta data belongs to the .opf file)
        	if (!bOPS) {
        		// "Traditional" meta data
        		//createMeta("generator","Writer2LaTeX "+Misc.VERSION);
        		createMeta(head,"description",metaData.getDescription());
        		createMeta(head,"keywords",metaData.getKeywords());

        		// Dublin core meta data (optional)
        		// Format as recommended on dublincore.org (http://dublincore.org/documents/dc-html/)
        		// Declare meta data profile
        		if (config.xhtmlUseDublinCore()) {
        			if (!isHTML5()) { head.setAttribute("profile","http://dublincore.org/documents/2008/08/04/dc-html/"); }
        			// Add link to declare namespace
        			Element dclink = htmlDOM.createElement("link");
        			dclink.setAttribute("rel","schema.DC");
        			dclink.setAttribute("href","http://purl.org/dc/elements/1.1/");
        			head.appendChild(dclink);
        			// Insert the actual meta data
        			createMeta(head,"DC.title",metaData.getTitle());
        			// DC.subject actually contains subject+keywords, so we merge them
        			String sDCSubject = "";
        			if (metaData.getSubject()!=null && metaData.getSubject().length()>0) {
        				sDCSubject = metaData.getSubject();
        			}
        			if (metaData.getKeywords()!=null && metaData.getKeywords().length()>0) {
        				if (sDCSubject.length()>0) { sDCSubject+=", "; }
        				sDCSubject += metaData.getKeywords();
        			}
        			createMeta(head,"DC.subject",sDCSubject);
        			createMeta(head,"DC.description",metaData.getDescription());
        			createMeta(head,"DC.creator",metaData.getCreator());
        			createMeta(head,"DC.date",metaData.getDate());
        			createMeta(head,"DC.language",metaData.getLanguage());
        		}
        	}

        	// Add link to custom stylesheet, if producing normal XHTML
        	if (!bOPS && config.xhtmlCustomStylesheet().length()>0) {
        		Element htmlStyle = htmlDOM.createElement("link");
        		htmlStyle.setAttribute("rel","stylesheet");
        		if (!isHTML5()) { htmlStyle.setAttribute("type","text/css"); }
        		htmlStyle.setAttribute("media","all");
        		htmlStyle.setAttribute("href",config.xhtmlCustomStylesheet());
        		head.appendChild(htmlStyle);
        	}
        	
        	// Add link to generated stylesheet if producing normal XHTML and the user wants separate css
        	if (!bOPS && config.separateStylesheet() && config.xhtmlFormatting()>XhtmlConfig.IGNORE_STYLES) {
        		Element htmlStyle = htmlDOM.createElement("link");
        		htmlStyle.setAttribute("rel","stylesheet");
        		if (!isHTML5()) { htmlStyle.setAttribute("type","text/css"); }
        		htmlStyle.setAttribute("media","all");
        		htmlStyle.setAttribute("href",sTargetFileName+"-styles.css");
        		head.appendChild(htmlStyle);
        	}

        	// Add link to included style sheet if producing OPS content
        	if (bOPS && styleSheet!=null) {
        		Element sty = htmlDOM.createElement("link");
        		sty.setAttribute("rel", "stylesheet");
        		if (!isHTML5()) { sty.setAttribute("type", "text/css"); }
        		sty.setAttribute("href", EPUB_CUSTOM_STYLESHEET);
        		head.appendChild(sty);
        	}

        	// Add link to generated stylesheet if producing OPS content
        	if (isOPS() && config.xhtmlFormatting()>XhtmlConfig.IGNORE_STYLES) {
        		Element htmlStyle = htmlDOM.createElement("link");
        		htmlStyle.setAttribute("rel","stylesheet");
        		if (!isHTML5()) { htmlStyle.setAttribute("type","text/css"); }
        		htmlStyle.setAttribute("href",EPUB_STYLESHEET);
        		head.appendChild(htmlStyle);
        	}

        }
        return htmlDoc.getContentNode();
    }
    
    // Add epub namespace for the purpose of semantic inflection in EPUB 3
    public void addEpubNs(Element elm) {
    	if (bOPS && isHTML5()) {
           	elm.setAttribute("xmlns:epub", "http://www.idpf.org/2007/ops");
    	}    	
    }
    
	// Add a type from the structural semantics vocabulary of EPUB 3
    public void addEpubType(Element elm, String sType) {
    	if (bOPS && isHTML5() && sType!=null) {
    		elm.setAttribute("epub:type", sType);
    	}
    }
	
    // create a target
    public Element createTarget(String sId) {
        Element a = htmlDOM.createElement("a");
        a.setAttribute("id",targetNames.getExportName(sId));
        targets.put(sId, Integer.valueOf(nOutFileIndex));
        return a;
    }
	
    // put a target id on an existing element
    public void addTarget(Element node,String sId) {
        node.setAttribute("id",targetNames.getExportName(sId));
        targets.put(sId, Integer.valueOf(nOutFileIndex));
    }
    
    // get at target id
    String getTarget(String sId) {
    	return targetNames.getExportName(sId);
    }

    // create an internal link
    public Element createLink(String sId) {
    	return createLink(sId,false);
    }

    // create an internal link
    public Element createLink(String sId, boolean bPageRef) {
        Element a = htmlDOM.createElement("a");
        LinkDescriptor ld = new LinkDescriptor();
        ld.element = a; ld.sId = sId; ld.nIndex = nOutFileIndex;; ld.bPageRef = bPageRef;
        links.add(ld);
        return a;
    }

    // create a link
    public Element createLink(Element onode) {
        // First create the anchor
        String sHref = onode.getAttribute(XMLString.XLINK_HREF);
        Element anchor;
        if (sHref.startsWith("#")) { // internal link
            anchor = createLink(sHref.substring(1),false);
        }
        else { // external link
            anchor = htmlDOM.createElement("a");
            
            sHref = ofr.fixRelativeLink(sHref);
	
            // Workaround for an OOo problem:
            if (sHref.indexOf("?")==-1) { // No question mark
                int n3F=sHref.indexOf("%3F");
                if (n3F>0) { // encoded question mark
                    sHref = sHref.substring(0,n3F)+"?"+sHref.substring(n3F+3);
                }
            }
            anchor.setAttribute("href",sHref);
            String sName = Misc.getAttribute(onode,XMLString.OFFICE_NAME);
            if (sName!=null) {
            	// The name attribute is rarely use (usually the user will insert a bookmark)
            	// Hence we (mis)use it to set additional attributes that are not supported by OOo
            	if (sName.indexOf(";")==-1 && sName.indexOf("=")==-1) {
            		// Simple case, use the value to set name and title
                    anchor.setAttribute("name",sName);
                    anchor.setAttribute("title",sName);
            	}
            	else {
                	// Complex case - using the syntax: name=value;name=value;...
            		String[] sElements = sName.split(";");
            		for (String sElement : sElements) {
            			String[] sNameVal = sElement.split("=");
            			if (sNameVal.length>=2) {
            				anchor.setAttribute(sNameVal[0].trim(),sNameVal[1].trim());
            			}
            		}
            	}
            }
            // TODO: target has been deprecated in xhtml 1.0 strict
            String sTarget = Misc.getAttribute(onode,XMLString.OFFICE_TARGET_FRAME_NAME);
            if (sTarget!=null) { anchor.setAttribute("target",sTarget); }
        }

        // Then style it
        String sStyleName = onode.getAttribute(XMLString.TEXT_STYLE_NAME);
        String sVisitedStyleName = onode.getAttribute(XMLString.TEXT_VISITED_STYLE_NAME);
        StyleInfo anchorInfo = new StyleInfo();
        styleCv.getTextSc().applyAnchorStyle(sStyleName,sVisitedStyleName,anchorInfo);
        styleCv.getTextSc().applyStyle(anchorInfo,anchor);

        return anchor;
    }

	
    private void createMeta(Element head, String sName, String sValue) {
        if (sValue==null) { return; }
        Element meta = htmlDOM.createElement("meta");
        meta.setAttribute("name",sName);
        meta.setAttribute("content",sValue);
        head.appendChild(meta);
    }
	
		
}