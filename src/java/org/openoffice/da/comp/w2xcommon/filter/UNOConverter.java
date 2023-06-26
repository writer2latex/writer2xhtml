/************************************************************************
 *
 *  UNOConverter.java
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
 *  Version 1.7.1 (2023-06-26)
 *  
 */
package org.openoffice.da.comp.w2xcommon.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Document;

import com.sun.star.beans.PropertyValue;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToOutputStreamAdapter;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import writer2xhtml.api.Converter;
import writer2xhtml.api.ConverterFactory;
import writer2xhtml.api.ConverterResult;
import writer2xhtml.api.OutputFile;
import writer2xhtml.util.Misc;

/** This class provides conversion using UNO:
 *  Graphics conversion is done using appropriate UNO services.
 *  Files are written to an URL using UCB.
 *  The document source document can be provided as an <code>XInputStream</code> or as a DOM tree
 */
public class UNOConverter {
	private XComponentContext xComponentContext;
	private Converter converter;
	private String sTargetFormat = null;
	private XOutputStream xos = null;
	private String sURL = null;

	/** Construct a new UNODocumentConverter from an array of arguments
	 *
	 * @param xComponentContext the component context used to instantiate new UNO services
	 * @param lArguments arguments providing FilterName, URL, OutputStream (optional), FilterData (optional)
	 * and FilterOptions (optional, alternative to FilterData)
	 */
	public UNOConverter(PropertyValue[] lArguments, XComponentContext xComponentContext) {
		this.xComponentContext = xComponentContext;
		
		// Create mapping from filter names to target media types
		HashMap<String,String> filterNames = new HashMap<String,String>();
		filterNames.put("org.openoffice.da.writer2xhtml","text/html");
		filterNames.put("org.openoffice.da.writer2xhtml11","application/xhtml11");
		filterNames.put("org.openoffice.da.writer2xhtml5","text/html5");
		filterNames.put("org.openoffice.da.writer2xhtml.mathml","application/xhtml+xml");
		filterNames.put("org.openoffice.da.writer2xhtml.epub","application/epub+zip");
		filterNames.put("org.openoffice.da.writer2xhtml.epub3","epub3");
		filterNames.put("org.openoffice.da.calc2xhtml","text/html");
		filterNames.put("org.openoffice.da.calc2xhtml11","application/xhtml11");
		filterNames.put("org.openoffice.da.calc2xhtml5","text/html5");

		// Get the arguments
		Object filterData = null;
		Object filterOptions = null;
		PropertyValue[] pValue = lArguments;
		for  (int  i = 0 ; i < pValue.length; i++) {
			try {
				if (pValue[i].Name.compareTo("FilterName")==0) {
					String sFilterName = (String)AnyConverter.toObject(new Type(String.class), pValue[i].Value);
					if (filterNames.containsKey(sFilterName)) {
						sTargetFormat = filterNames.get(sFilterName);
					}
					else {
						sTargetFormat = sFilterName;
					}
				}
				if (pValue[i].Name.compareTo("OutputStream")==0){
					xos = (XOutputStream)AnyConverter.toObject(new Type(XOutputStream.class), pValue[i].Value);
				}
				if (pValue[i].Name.compareTo("URL")==0){
					sURL = (String)AnyConverter.toObject(new Type(String.class), pValue[i].Value);
				}
				if (pValue[i].Name.compareTo("FilterData")==0) {
					filterData = pValue[i].Value;
				}
				if (pValue[i].Name.compareTo("FilterOptions")==0) {
					filterOptions = pValue[i].Value;
				}
			} 
			catch(com.sun.star.lang.IllegalArgumentException AnyExec){
				System.err.println("\nIllegalArgumentException "+AnyExec);
			}
		}
		if (sURL==null){
			sURL="";
		}
		
		// Create converter and supply it with filter data and a suitable graphic converter
		converter = ConverterFactory.createConverter(sTargetFormat);
		if (converter==null) {
			throw new com.sun.star.uno.RuntimeException("Failed to create converter to "+sTargetFormat);
		}
		if (filterData!=null) {
			FilterDataParser fdp = new FilterDataParser(xComponentContext);
			fdp.applyFilterData(filterData,converter);
		}
		else if (filterOptions!=null) {
			FilterDataParser fdp = new FilterDataParser(xComponentContext);
			fdp.applyFilterOptions(filterOptions,converter);			
		}
		converter.setGraphicConverter(new GraphicConverterImpl(xComponentContext));
		
	}
	
	/** Convert a document given by a DOM tree
	 * 
	 * @param dom the DOMsource
	 * @throws IOException 
	 */
	public void convert(Document dom) throws IOException {
		writeFiles(converter.convert(dom, Misc.makeFileName(getFileName(sURL)),true));
	}
	
	/** Convert a document given by an XInputStream
	 * 
	 * @param xis the input stream
	 * @throws IOException 
	 */
	public void convert(XInputStream xis) throws IOException {
		InputStream is = new XInputStreamToInputStreamAdapter(xis);
		writeFiles(converter.convert(is, Misc.makeFileName(getFileName(sURL))));
	}
	
	private void writeFiles(ConverterResult result) throws IOException {
		Iterator<OutputFile> docEnum = result.iterator();
		if (docEnum.hasNext()) {
			// The master document is written to the supplied XOutputStream, if any
			if (xos!=null) {
				XOutputStreamToOutputStreamAdapter newxos =new XOutputStreamToOutputStreamAdapter(xos);
				docEnum.next().write(newxos);
				newxos.flush();
				newxos.close();
			}				
			// Additional files are written directly using UCB
			if (docEnum.hasNext() && sURL.startsWith("file:")) {
				// Initialize the file access (used to write all additional output files)
				XSimpleFileAccess2 sfa2 = null;
				try {
					Object sfaObject = xComponentContext.getServiceManager().createInstanceWithContext(
							"com.sun.star.ucb.SimpleFileAccess", xComponentContext);
					sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
				}
				catch (com.sun.star.uno.Exception e) {
					// failed to get SimpleFileAccess service (should not happen)
				}
				
				if (sfa2!=null) {
					// Remove the file name part of the URL
					String sNewURL = null;
					if (sURL.lastIndexOf("/")>-1) {
						// Take the URL up to and including the last slash
						sNewURL = sURL.substring(0,sURL.lastIndexOf("/")+1);
					}
					else {
						// The URL does not include a path; this should not really happen,
						// but in this case we will write to the current default directory
						sNewURL = "";
					}
				
					while (docEnum.hasNext()) {
						OutputFile docOut = docEnum.next();
						// Get the file name and the (optional) directory name
						String sFullFileName = Misc.makeHref(docOut.getFileName());
						String sDirName = "";
						String sFileName = sFullFileName;
						int nSlash = sFileName.indexOf("/");
						if (nSlash>-1) {
							sDirName = sFileName.substring(0,nSlash);
							sFileName = sFileName.substring(nSlash+1);
						}
		
						try {
							// Create subdirectory if required
							if (sDirName.length()>0 && !sfa2.exists(sNewURL+sDirName)) {
								sfa2.createFolder(sNewURL+sDirName);
							}
		
							// writeFile demands an InputStream, so we use a Pipe for the transport
							Object xPipeObj = xComponentContext.getServiceManager().createInstanceWithContext(
									"com.sun.star.io.Pipe", xComponentContext);
							XInputStream xInStream	= (XInputStream) UnoRuntime.queryInterface(XInputStream.class, xPipeObj);
							XOutputStream xOutStream = (XOutputStream) UnoRuntime.queryInterface(XOutputStream.class, xPipeObj);
							OutputStream outStream = new XOutputStreamToOutputStreamAdapter(xOutStream);
							// Feed the pipe with content...
							docOut.write(outStream);
							outStream.flush();
							outStream.close();
							xOutStream.closeOutput();
							// ...and then write the content to the URL
							sfa2.writeFile(sNewURL+sFullFileName,xInStream);
						}
						catch (Throwable e){
							throw new IOException("Error writing file "+sFileName+" "+e.getMessage());
						}
					}
				}
			}
		}
		else {
			// The converter did not produce any files (should not happen)
			throw new IOException("Conversion failed: Internal error");
		}			
	}
	
	private String getFileName(String origName) {
		String name=null;
		if (origName !=null) {
			if(origName.equalsIgnoreCase(""))
				name = "OutFile"; 
			else {
				if (origName.lastIndexOf("/")>=0) {
					origName=origName.substring(origName.lastIndexOf("/")+1,origName.length());
				}
				if (origName.lastIndexOf(".")>=0) {
					name = origName.substring(0,(origName.lastIndexOf(".")));
				}
				else {
					name=origName;
				}
			}
		}
		else{   
			name = "OutFile"; 
		}

		return name;
	}
	
}