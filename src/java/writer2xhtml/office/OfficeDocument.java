/************************************************************************
 *
 *  OfficeDocument.java
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
 *  Version 1.4 (2014-08-28)
 *
 */

package writer2xhtml.office;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import writer2xhtml.util.SimpleXMLParser;
import writer2xhtml.util.SimpleZipReader;

/**
 *  This class implements reading of ODF files from various sources
 */
public class OfficeDocument {
    // File names for the XML streams in a package document (settings.xml is ignored)
    protected final static String CONTENTXML = "content.xml";
    protected final static String STYLESXML = "styles.xml";
    private final static String METAXML = "meta.xml";
    private final static String MANIFESTXML = "META-INF/manifest.xml";

    // Some tag and attribute names in manifest.xml
    private final static String MANIFEST_FILE_ENTRY = "manifest:file-entry";
    private final static String MANIFEST_MEDIA_TYPE = "manifest:media-type";
    private final static String MANIFEST_FULL_PATH = "manifest:full-path";
    
    // Identify package format
    private boolean bIsPackageFormat = false;

	/** DOM <code>Document</code> of content.xml. */
	private Document contentDoc = null;

	/** DOM <code>Document</code> of meta.xml. */
	private Document metaDoc = null;

	/** DOM <code>Document</code> of content.xml. */
	private Document styleDoc = null;

	/** DOM <code>Document</code> of META-INF/manifest.xml. */
	private Document manifestDoc = null;

	/** Collection to keep track of the embedded objects in the document. */
	private Map<String, EmbeddedObject> embeddedObjects = null;
	
	/** Package or flat format? 
	 *  @return true if the document is in package format, false if it's flat XML
	 */
	public boolean isPackageFormat() {
		return bIsPackageFormat;
	}

	/**
	 *  Return a DOM <code>Document</code> object of the content.xml file.
	 *  file. Note that a content DOM is not created when the constructor
	 *  is called, but only after the <code>read</code> method has been invoked
	 *
	 *  @return  DOM <code>Document</code> object.
	 */
	public Document getContentDOM() {
		return contentDoc;
	}

	/**
	 *  Return a DOM <code>Document</code> object of the meta.xml
	 *  file. Note that a meta DOM is not created when the constructor
	 *  is called, but only after the <code>read</code> method has been invoked
	 *
	 *  @return  DOM <code>Document</code> object.
	 */
	public Document getMetaDOM() {
		return metaDoc;
	}

	/**
	 *  Return a DOM <code>Document</code> object of the style.xml file.
	 *  Note that a style DOM is not created when the constructor
	 *  is called, but only after the <code>read</code> method has been invoked
	 *
	 *  @return  DOM <code>Document</code> object.
	 */
	public Document getStyleDOM() {
		return styleDoc;
	}

	/**
	 * Collect all the embedded objects (graphics, formulae, etc.) present in
	 * this document. If the document is read from flat XML there will be no embedded objects.
	 */
	private void getEmbeddedObjects(SimpleZipReader zip) {
		embeddedObjects = new HashMap<String, EmbeddedObject>();
		if (manifestDoc != null) {
			// Need to read the manifest file and construct a list of objects                       
			NodeList nl = manifestDoc.getElementsByTagName(MANIFEST_FILE_ENTRY);
			int nLen = nl.getLength();
			for (int i = 0; i < nLen; i++) {
				Element elm = (Element) nl.item(i);
				String sType = elm.getAttribute(MANIFEST_MEDIA_TYPE);
				String sPath = elm.getAttribute(MANIFEST_FULL_PATH);

				/* According to the ODF spec there are only two types of embedded object:
				 *      Objects with an XML representation.
				 *      Objects without an XML representation.
				 * The former are represented by one or more XML files.
				 * The latter are in binary form.
				 */
				if (sType.startsWith("application/vnd.oasis.opendocument") || sType.startsWith("application/vnd.sun.xml")) {
					// Allow either ODF or old OOo 1.x embedded objects
					if (!sPath.equals("/")) { // Exclude the main document entries
						if (sPath.endsWith("/")) { // Remove trailing slash
							sPath=sPath.substring(0, sPath.length()-1);
						}
						embeddedObjects.put(sPath, new EmbeddedXMLObject(sPath, sType, this, zip));
					}
				}
				else if (!sType.equals("text/xml")) {
					// XML entries are either embedded ODF doc entries or main document entries, all other
					// entries are included as binary objects
					embeddedObjects.put(sPath, new EmbeddedBinaryObject(sPath, sType, this, zip));
				}
			}
		}
	}

	/**
	 * Returns the embedded object corresponding to the name provided.
	 * The name should be stripped of any preceding path characters, such as 
	 * '/', '.' or '#'.
	 *
	 * @param   sName    The name of the embedded object to retrieve.
	 *
	 * @return  An <code>EmbeddedObject</code> instance representing the named 
	 *          object.
	 */
	public EmbeddedObject getEmbeddedObject(String sName) {
		if (sName!=null && embeddedObjects!=null && embeddedObjects.containsKey(sName)) {
			return embeddedObjects.get(sName);
		}
		return null;
	}
	
	protected void removeEmbeddedObject(String sName) {
		if (sName!=null && embeddedObjects!=null && embeddedObjects.containsKey(sName)) {
			embeddedObjects.remove(sName);
		}		
	}

	/**
	 * Read the document from a DOM tree (flat XML format)
	 * 
	 * @param dom the DOM tree
	 */
	public void read(org.w3c.dom.Document dom) {
		contentDoc = dom;
		styleDoc = null;
		metaDoc = null;
		manifestDoc = null;
		bIsPackageFormat = false;
		embeddedObjects = null;
	}


	/**
	 *  Read the Office <code>Document</code> from the given
	 *  <code>InputStream</code>.
	 *  Performs simple type detection to determine package or flat format
	 *
	 *  @param  is  Office document <code>InputStream</code>.
	 *
	 *  @throws  IOException  If any I/O error occurs.
	 * @throws SAXException 
	 */
	public void read(InputStream is) throws IOException {
		// We need to read 4 bytes ahead to detect flat or zip format
		BufferedInputStream inbuf = new BufferedInputStream(is);
		byte[] bytes = new byte[4];
		inbuf.mark(4);
		inbuf.read(bytes);
		inbuf.reset();
		boolean bZip = MIMETypes.ZIP.equals(MIMETypes.getMagicMIMEType(bytes));
		if (bZip) {
			readZip(inbuf);
		}
		else {
			readFlat(inbuf);
		}
	}

	private void readZip(InputStream is) throws IOException {
		SimpleZipReader zip = new SimpleZipReader();
		zip.read(is);

		byte contentBytes[] = zip.getEntry(CONTENTXML);
		if (contentBytes == null) {
			throw new IOException("Entry content.xml not found in file");
		}
		try {
			contentDoc = parse(contentBytes);
		} catch (SAXException ex) {
			throw new IOException(ex);
		}

		byte styleBytes[] = zip.getEntry(STYLESXML);
		if (styleBytes != null) {
			try {
				styleDoc = parse(styleBytes);
			} catch (SAXException ex) {
				throw new IOException(ex);
			}
		}

		byte metaBytes[] = zip.getEntry(METAXML);
		if (metaBytes != null) {
			try {
				metaDoc = parse(metaBytes);
			} catch (SAXException ex) {
				throw new IOException(ex);
			}
		}

		byte manifestBytes[] = zip.getEntry(MANIFESTXML);
		if (manifestBytes != null) {
			try {
				manifestDoc = parse(manifestBytes);
			} catch (SAXException ex) {
				throw new IOException(ex);
			}
		}
		
		bIsPackageFormat = true;
		getEmbeddedObjects(zip);
	}


	private void readFlat(InputStream is) throws IOException {
		try {
			contentDoc = SimpleXMLParser.parse(is);
		} catch (SAXException e) {
			throw new IOException(e);
		}
		styleDoc = null;
		metaDoc = null;
		manifestDoc = null;
		bIsPackageFormat = false;
		embeddedObjects = null;
	}

	/**
	 *  Parse given <code>byte</code> array into a DOM
	 *  <code>Document</code> object using the
	 *  <code>DocumentBuilder</code> object.
	 *
	 *  @param  builder  <code>DocumentBuilder</code> object for parsing.
	 *  @param  bytes    <code>byte</code> array for parsing.
	 *
	 *  @return  Resulting DOM <code>Document</code> object.
	 *
	 *  @throws  SAXException  If any parsing error occurs.
	 */
	static Document parse(byte bytes[]) throws SAXException, IOException {
		SAXParserFactory factory=SAXParserFactory.newInstance();
		SimpleXMLParser handler = new SimpleXMLParser();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new ByteArrayInputStream(bytes),handler);
			return handler.getDOM();
		}
		catch (ParserConfigurationException e) {
			System.err.println("Oops - failed to get XML parser!?");
			e.printStackTrace();
		}
		return null;
	}

}
