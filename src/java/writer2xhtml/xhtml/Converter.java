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
 *  Copyright: 2002-2022 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2022-08-17)
 *
 */

package writer2xhtml.xhtml;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;

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
    private Set<ResourceDocument> resources = new HashSet<ResourceDocument>();
    
    // The xhtml output file(s)
    protected int nType = XhtmlDocument.XHTML10; // the doctype
    private boolean bOPS = false; // Do we need to be OPS conforming?
    Vector<XhtmlDocument> outFiles;
    private int nOutFileIndex;
    private XhtmlDocument htmlDoc; // current outfile
    private Document htmlDOM; // current DOM, usually within htmlDoc
    private boolean bNeedHeaderFooter = false;
    //private int nTocFileIndex = -1;
    //private int nAlphabeticalIndex = -1;

    // Hyperlinks
    Hashtable<String, Integer> targets = new Hashtable<String, Integer>();
    LinkedList<LinkDescriptor> links = new LinkedList<LinkDescriptor>();
    // Strip illegal characters from internal hyperlink targets
    private ExportNameCollection targetNames = new ExportNameCollection(true);
    
    // The current context (currently we only track the content width, but this might be expanded with formatting
    // attributes - at least background color and font size later)
    // The page content width serves as a base, and may possibly change even if the stack is non-empty
    private String pageContentWidth = null;
    private Stack<String> contentWidth = new Stack<String>();
    
    // Constructor setting the DOCTYPE
    public Converter(int nType) {
        super();
        config = new XhtmlConfig();
        this.nType = nType;
    }

    // override methods to read templates, style sheets and resources
    @Override public void readTemplate(InputStream is) throws IOException {
        template = new XhtmlDocument("Template",nType);
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
        sTargetFileName = Misc.trimDocumentName(sTargetFileName,XhtmlDocument.getExtension(nType));    		
		
        outFiles = new Vector<XhtmlDocument>();
        nOutFileIndex = -1;

        bNeedHeaderFooter = !bOPS && (ofr.isSpreadsheet() || ofr.isPresentation() || config.getXhtmlSplitLevel()>0 || config.pageBreakSplit()>XhtmlConfig.NONE || config.getXhtmlUplink().length()>0);

        l10n = new L10n();
        
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

        styleCv = new StyleConverter(ofr,config,this,nType);
        textCv = new TextConverter(ofr,config,this);
        tableCv = new TableConverter(ofr,config,this);
        drawCv = new DrawConverter(ofr,config,this);
        mathCv = new MathConverter(ofr,config,this,nType!=XhtmlDocument.XHTML10 && nType!=XhtmlDocument.XHTML11);

        // Set locale to document language
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
        
        // Set the main content width
        setPageContentWidth(getStyleCv().getPageSc().getTextWidth(ofr.getFirstMasterPage()));

        // Traverse the body
        Element body = ofr.getContent();
        if (ofr.isSpreadsheet()) { tableCv.convertTableContent(body); }
        else if (ofr.isPresentation()) { drawCv.convertDrawContent(body); }
        else { textCv.convertTextContent(body); }
		
        // Set the title page and text page entries
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

        // Resolve links
        ListIterator<LinkDescriptor> iter = links.listIterator();
        while (iter.hasNext()) {
            LinkDescriptor ld = iter.next();
            Integer targetIndex = targets.get(ld.sId);
            if (targetIndex!=null) {
                int nTargetIndex = targetIndex.intValue();
                if (nTargetIndex == ld.nIndex) { // same file
                    ld.element.setAttribute("href","#"+targetNames.getExportName(ld.sId));
                }
                else {
                    ld.element.setAttribute("href",getOutFileName(nTargetIndex,true)
                                            +"#"+targetNames.getExportName(ld.sId));
                }
            }
        }

        // Add included style sheet, if any - and we are creating OPS content
        if (bOPS && styleSheet!=null) {
        	converterResult.addDocument(styleSheet);
        	for (ResourceDocument doc : resources) {
        		converterResult.addDocument(doc);
        	}
        }
        
        // Export styles (XHTML)
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
        
        // Load MathJax
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
        
        // Create headers & footers (if nodes are available)
        if (ofr.isSpreadsheet()) {
            for (int i=0; i<=nOutFileIndex; i++) {

                XhtmlDocument doc = outFiles.get(i);
                Document dom = doc.getContentDOM();
                Element header = doc.getHeaderNode();
                Element footer = doc.getFooterNode();
                Element headerPar = dom.createElement("p");
                Element footerPar = dom.createElement("p");
                footerPar.setAttribute("style","clear:both"); // no floats may pass!

                // Add uplink
                if (config.getXhtmlUplink().length()>0) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    headerPar.appendChild(a);
                    headerPar.appendChild(dom.createTextNode(" "));
                    a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    footerPar.appendChild(a);
                    footerPar.appendChild(dom.createTextNode(" "));
                }
                // Add links to all sheets:
                int nSheets = tableCv.sheetNames.size();
                for (int j=0; j<nSheets; j++) {
                    if (config.xhtmlCalcSplit()) {
                        addNavigationLink(dom,headerPar,tableCv.sheetNames.get(j),j);
                        addNavigationLink(dom,footerPar,tableCv.sheetNames.get(j),j);
                    }
                    else {
                        addInternalNavigationLink(dom,headerPar,tableCv.sheetNames.get(j),"tableheading"+j);
                        addInternalNavigationLink(dom,footerPar,tableCv.sheetNames.get(j),"tableheading"+j);
	                }
                }
                
                if (header!=null) { header.appendChild(headerPar); }
                if (footer!=null) { footer.appendChild(footerPar); }
            }
        }
        else if (nOutFileIndex>0) {
            for (int i=0; i<=nOutFileIndex; i++) {
                XhtmlDocument doc = outFiles.get(i);
                Document dom = doc.getContentDOM();
                //Element content = doc.getContentNode();

                // Header links
                Element header = doc.getHeaderNode();
                if (header!=null) {
                    if (ofr.isPresentation()) {
                        // Absolute placement in presentations (quick and dirty solution)
                        header.setAttribute("style","position:absolute;top:0;left:0");
                    }
                    if (config.getXhtmlUplink().length()>0) {
                        Element a = dom.createElement("a");
                        a.setAttribute("href",config.getXhtmlUplink());
                        a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                        header.appendChild(a);
                        header.appendChild(dom.createTextNode(" "));
                    }
                    addNavigationLink(dom,header,l10n.get(L10n.FIRST),0);
                    addNavigationLink(dom,header,l10n.get(L10n.PREVIOUS),i-1);
                    addNavigationLink(dom,header,l10n.get(L10n.NEXT),i+1);
                    addNavigationLink(dom,header,l10n.get(L10n.LAST),nOutFileIndex);
                    if (textCv.getTocIndex()>=0) {
                        addNavigationLink(dom,header,l10n.get(L10n.CONTENTS),textCv.getTocIndex());
                    }
                    if (textCv.getAlphabeticalIndex()>=0) {
                        addNavigationLink(dom,header,l10n.get(L10n.INDEX),textCv.getAlphabeticalIndex());
                    }
                }

                // Footer links
                Element footer = doc.getFooterNode();
                if (footer!=null && !ofr.isPresentation()) {
                    // No footer in presentations
                    if (config.getXhtmlUplink().length()>0) {
                        Element a = dom.createElement("a");
                        a.setAttribute("href",config.getXhtmlUplink());
                        a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                        footer.appendChild(a);
                        footer.appendChild(dom.createTextNode(" "));
                    }
                    addNavigationLink(dom,footer,l10n.get(L10n.FIRST),0);
                    addNavigationLink(dom,footer,l10n.get(L10n.PREVIOUS),i-1);
                    addNavigationLink(dom,footer,l10n.get(L10n.NEXT),i+1);
                    addNavigationLink(dom,footer,l10n.get(L10n.LAST),nOutFileIndex);
                    if (textCv.getTocIndex()>=0) {
                        addNavigationLink(dom,footer,l10n.get(L10n.CONTENTS),textCv.getTocIndex());
                    }
                    if (textCv.getAlphabeticalIndex()>=0) {
                        addNavigationLink(dom,footer,l10n.get(L10n.INDEX),textCv.getAlphabeticalIndex());
                    }
                }
            }
        }
        else if (config.getXhtmlUplink().length()>0) {
            for (int i=0; i<=nOutFileIndex; i++) {
                XhtmlDocument doc = outFiles.get(i);
                Document dom = doc.getContentDOM();
                //Element content = doc.getContentNode();

                Element header = doc.getHeaderNode();
                if (header!=null) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    header.appendChild(a);
                    header.appendChild(dom.createTextNode(" "));
                }

                Element footer = doc.getFooterNode();
                if (footer!=null) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href",config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    footer.appendChild(a);
                    footer.appendChild(dom.createTextNode(" "));
                }
            }
        }
        
        // Export styles
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
	
    private void addNavigationLink(Document dom, Node node, String s, int nIndex) {
        if (nIndex>=0 && nIndex<=nOutFileIndex) {
            Element a = dom.createElement("a");
            a.setAttribute("href",Misc.makeHref(getOutFileName(nIndex,true)));
            a.appendChild(dom.createTextNode(s));
            //node.appendChild(dom.createTextNode("["));
            node.appendChild(a);
            node.appendChild(dom.createTextNode(" "));
            //node.appendChild(dom.createTextNode("] "));
        }
        else {
            Element span = dom.createElement("span");
            span.setAttribute("class","nolink");
            node.appendChild(span);
            span.appendChild(dom.createTextNode(s));
            node.appendChild(dom.createTextNode(" "));
            //node.appendChild(dom.createTextNode("["+s+"] "));
        }
    }
	
    private void addInternalNavigationLink(Document dom, Node node, String s, String sLink) {
        Element a = dom.createElement("a");
        a.setAttribute("href","#"+sLink);
        a.appendChild(dom.createTextNode(s));
        //node.appendChild(dom.createTextNode("["));
        node.appendChild(a);
        node.appendChild(dom.createTextNode(" "));
        //node.appendChild(dom.createTextNode("] "));
    }
    
    /* get inline text, ignoring any draw objects, footnotes, formatting and hyperlinks */
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
	
    // Create output file name (docname.html, docname1.html, docname2.html etc.)
    public String getOutFileName(int nIndex, boolean bWithExt) {
        return sTargetFileName + (nIndex>0 ? Integer.toString(nIndex) : "") 
                               + (bWithExt ? htmlDoc.getFileExtension() : "");
    }
	
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
    public Element nextOutFile() {
        htmlDoc = new XhtmlDocument(getOutFileName(++nOutFileIndex,false),nType);
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
             + (ofr.isText() ? "Writer" : (ofr.isSpreadsheet() ? "Calc" : "Impress"))
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
        
        // Recreate nested sections, if any
        if (!textCv.sections.isEmpty()) {
            Iterator<Node> iter = textCv.sections.iterator();
            while (iter.hasNext()) {
                Element section = (Element) iter.next();
                String sStyleName = Misc.getAttribute(section,XMLString.TEXT_STYLE_NAME);
                Element div = htmlDOM.createElement("div");
                htmlDoc.getContentNode().appendChild(div);
                htmlDoc.setContentNode(div);
                StyleInfo sectionInfo = new StyleInfo();
                styleCv.getSectionSc().applyStyle(sStyleName,sectionInfo);
                styleCv.getSectionSc().applyStyle(sectionInfo,div);
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
        Element a = htmlDOM.createElement("a");
        LinkDescriptor ld = new LinkDescriptor();
        ld.element = a; ld.sId = sId; ld.nIndex = nOutFileIndex;
        links.add(ld);
        return a;
    }

    // create a link
    public Element createLink(Element onode) {
        // First create the anchor
        String sHref = onode.getAttribute(XMLString.XLINK_HREF);
        Element anchor;
        if (sHref.startsWith("#")) { // internal link
            anchor = createLink(sHref.substring(1));
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