/************************************************************************
 *
 *  DOMDocument.java
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
 *  Version 1.6 (2015-05-05)
 *
 */

package writer2xhtml.base;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
//import org.xml.sax.SAXParseException;

import writer2xhtml.api.OutputFile;

/**
 *  This class represents XML-based documents. It is loosely based on a class from the former xmerge project
 *  from OOo.
 */
public class DOMDocument implements OutputFile {

    /** Factory for <code>DocumentBuilder</code> objects. */
    private static DocumentBuilderFactory factory =
       DocumentBuilderFactory.newInstance();

    /** DOM <code>Document</code> of content.xml. */
    private Document contentDoc = null;

    /** DOM <code>Document</code> of styles.xml. */
    //private Document styleDoc = null;

    private String documentName = null;
    private String fileName = null;
    private String fileExt = null;

    /** Resources object. */
    //private Resources res = null;


    /**
     *  Default constructor.
     *
     *  @param  name  <code>Document</code> name.
     *  @param  ext   <code>Document</code> extension.
     */
    public DOMDocument(String name,String ext)
    {
	this(name,ext,true, false);
    }

     /**
     *  Returns the file extension of the <code>Document</code> 
     *  represented.
     *
     *  @return  file extension of the <code>Document</code>.
     */
    protected String getFileExtension() {
        return fileExt;
    }


    /**
     *  Constructor with arguments to set <code>namespaceAware</code>
     *  and <code>validating</code> flags.
     *
     *  @param  name            <code>Document</code> name (may or may not
     *                          contain extension).
     *  @param  ext             <code>Document</code> extension.
     *  @param  namespaceAware  Value for <code>namespaceAware</code> flag.
     *  @param  validating      Value for <code>validating</code> flag.
     */
    public DOMDocument(String name, String ext,boolean namespaceAware, boolean validating) {

        //res = Resources.getInstance();
        factory.setValidating(validating);
        factory.setNamespaceAware(namespaceAware);
        this.fileExt = ext;
	this.documentName = trimDocumentName(name);
        this.fileName = documentName + getFileExtension();
    }


    /**
     *  Removes the file extension from the <code>Document</code>
     *  name.
     *
     *  @param  name  Full <code>Document</code> name with extension.
     *
     *  @return  Name of <code>Document</code> without the extension.
     */
    private String trimDocumentName(String name) {
        String temp = name.toLowerCase();
        String ext = getFileExtension();

        if (temp.endsWith(ext)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - ext.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }


    /**
     *  Return a DOM <code>Document</code> object of the document content
     *  file.  Note that a content DOM is not created when the constructor
     *  is called.  So, either the <code>read</code> method or the
     *  <code>initContentDOM</code> method will need to be called ahead
     *  on this object before calling this method.
     *
     *  @return  DOM <code>Document</code> object.
     */
    public Document getContentDOM() {

        return contentDoc;
    }

    /**
     *  Sets the Content of the <code>Document</code> to the contents of the 
     *  supplied <code>Node</code> list. 
     *
     *  @param newDom  DOM <code>Document</code> object.
     */
    public void setContentDOM( Node newDom) {
	contentDoc=(Document)newDom;
    }


    /**
     *  Return the name of the <code>Document</code>.
     *
     *  @return  The name of <code>Document</code>.
     */
    public String getName() {

        return documentName;
    }


    /**
     *  Return the file name of the <code>Document</code>, possibly
     *  with the standard extension.
     *
     *  @return  The file name of <code>Document</code>.
     */
    public String getFileName() {

        return fileName;
    }


    /**
     *  Read the Office <code>Document</code> from the specified
     *  <code>InputStream</code>.
     *
     *  @param  is  Office document <code>InputStream</code>.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    public void read(InputStream is) throws IOException {
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {  
            throw new IOException(ex.getMessage());
        }
        try {
            contentDoc=  builder.parse(is);
        } catch (SAXException ex) {
            throw new IOException(ex.getMessage());
        }
    }
    
    /**
     *  Write out content to the supplied <code>OutputStream</code>.
     *  (with pretty printing)
     *  @param  os  XML <code>OutputStream</code>.
     *  @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(os,"UTF-8");
        osw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        write(getContentDOM().getDocumentElement(),0,osw);
        osw.flush();
        osw.close();
    }

    // Write nodes; we only need element, text and comment nodes
    private void write(Node node, int nLevel, OutputStreamWriter osw) throws IOException {
        short nType = node.getNodeType();
        switch (nType) {
            case Node.ELEMENT_NODE:
                if (node.hasChildNodes()) {
                    // Block pretty print from this node?
                    NodeList list = node.getChildNodes();
                    int nLen = list.getLength();
                    boolean bBlockPrettyPrint = false;
                    if (nLevel>=0) {
                        for (int i = 0; i < nLen; i++) {
                            bBlockPrettyPrint |= list.item(i).getNodeType()==Node.TEXT_NODE;
                        }
                    }
                    // Print start tag
                    if (nLevel>=0) { writeSpaces(nLevel,osw); }
                    osw.write("<"+node.getNodeName());
                    writeAttributes(node,osw);
                    osw.write(">");
                    if (nLevel>=0 && !bBlockPrettyPrint) { osw.write("\n"); }
                    // Print children
                    for (int i = 0; i < nLen; i++) {
                        int nNextLevel;
                        if (bBlockPrettyPrint || nLevel<0) { nNextLevel=-1; }
                        else { nNextLevel=nLevel+1; }
                        write(list.item(i),nNextLevel,osw);
                    }
                    // Print end tag
                    if (nLevel>=0 && !bBlockPrettyPrint) { writeSpaces(nLevel,osw); }
                    osw.write("</"+node.getNodeName()+">");
                    if (nLevel>=0) { osw.write("\n"); }
                }
                else { // empty element
                    if (nLevel>=0) { writeSpaces(nLevel,osw); }
                    osw.write("<"+node.getNodeName());
                    writeAttributes(node,osw);
                    osw.write(" />");
                    if (nLevel>=0) { osw.write("\n"); }
                }
                break;
            case Node.TEXT_NODE:
                write(node.getNodeValue(),osw);
                break;
            case Node.COMMENT_NODE:
                if (nLevel>=0) { writeSpaces(nLevel,osw); }
                osw.write("<!-- ");
                write(node.getNodeValue(),osw);
                osw.write(" -->");
                if (nLevel>=0) { osw.write("\n"); }
        }
    }
	
    private void writeAttributes(Node node, OutputStreamWriter osw) throws IOException {
        NamedNodeMap attr = node.getAttributes();
        int nLen = attr.getLength();
        for (int i=0; i<nLen; i++) {
            Node item = attr.item(i);
            osw.write(" ");
            write(item.getNodeName(),osw);
            osw.write("=\"");
            write(item.getNodeValue(),osw);
            osw.write("\"");
        }
    }

    private void writeSpaces(int nCount, OutputStreamWriter osw) throws IOException {
        for (int i=0; i<nCount; i++) { osw.write("  "); }
    }
	
    private void write(String s, OutputStreamWriter osw) throws IOException {
        int nLen = s.length();
        char c;
        for (int i=0; i<nLen; i++) {
            c = s.charAt(i);
            switch (c) {
                case ('<'): osw.write("&lt;"); break;
                case ('>'): osw.write("&gt;"); break;
                case ('&'): osw.write("&amp;"); break;
                case ('"'): osw.write("&quot;"); break;
                case ('\''): osw.write( "&apos;"); break;
                default: osw.write(c);
            }
        }
    }
    
    /**
     *  Initializes a new DOM <code>Document</code> with the content
     *  containing minimum XML tags.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    public final void initContentDOM() throws IOException {	
        contentDoc = createDOM("");
	
    }

    /**
     *  <p>Creates a new DOM <code>Document</code> containing minimum
     *  OpenOffice XML tags.</p>
     *
     *  <p>This method uses the subclass
     *  <code>getOfficeClassAttribute</code> method to get the
     *  attribute for <i>office:class</i>.</p>
     *
     *  @param  rootName  root name of <code>Document</code>.
     *
     *  @throws  IOException  If any I/O error occurs.
     */
    private final Document createDOM(String rootName) throws IOException {

        Document doc = null;

        try {

            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();

        } catch (ParserConfigurationException ex) {
        	// This will not happen
             System.err.println("Error:"+ ex);
             throw new IOException(ex);
        }

        Element root = (Element) doc.createElement(rootName);
        doc.appendChild(root);

       
        return doc;
    }

    // We need these because we implement OutputFile
	public String getMIMEType() {
		return "";
	}
	
	public boolean isMasterDocument() {
		return false;
	}
	
	public boolean containsMath() {
		return false;
	}

}




