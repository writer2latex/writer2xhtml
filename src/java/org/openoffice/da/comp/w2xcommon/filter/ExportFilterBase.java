/************************************************************************
 *
 *  ExportFilterBase.java
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
 *  Version 1.6 (2014-10-06)
 *  
 */

package org.openoffice.da.comp.w2xcommon.filter;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.uno.Type;
import com.sun.star.uno.XComponentContext;
import com.sun.star.xml.sax.XDocumentHandler;

import writer2xhtml.util.SimpleDOMBuilder;

import com.sun.star.xml.XExportFilter;

import java.io.IOException;

import org.openoffice.da.comp.w2xcommon.helper.MessageBox;


/** This class provides an abstract UNO component which implements an XExportFilter.
 *  The filter is actually generic and only the constructor and 3 strings needs
 *  to be changed by the subclass.
 */
public abstract class ExportFilterBase implements
XExportFilter,						     
XServiceName,
XServiceInfo,
XDocumentHandler,  
XTypeProvider {

	/** Service name for the component */
	public static final String __serviceName = "";

	/** Implementation name for the component */
	public static final String __implementationName = "";

	/** Filter name to include in error messages */
	public String __displayName = "";

	private XComponentContext xComponentContext = null;
	private SimpleDOMBuilder domBuilder = new SimpleDOMBuilder(); 
	private UNOConverter converter = null;

	/** Construct a new ExportFilterBase from a given component context
	 * 
	 * @param xComponentContext the component context used to instantiate new UNO services
	 */
	public ExportFilterBase(XComponentContext xComponentContext) {
		this.xComponentContext = xComponentContext;
	}
	
	// ---------------------------------------------------------------------------
	// Implementation of XExportFilter:

	public boolean exporter(com.sun.star.beans.PropertyValue[] aSourceData, 
			java.lang.String[] msUserData) {
		// Create a suitable converter
		converter = new UNOConverter(aSourceData, xComponentContext);
		return true;
	}

	// ---------------------------------------------------------------------------
	// Implementation of XDocumentHandler:
	// A flat XML DOM tree is created by the SAX events and finally converted

	public void  startDocument () {
		//Do nothing
	}

	public void endDocument()throws com.sun.star.uno.RuntimeException {
		try{
			converter.convert(domBuilder.getDOM());
		}
		catch (IOException e){
			MessageBox msgBox = new MessageBox(xComponentContext);
			msgBox.showMessage(__displayName+": IO error in conversion",
					e.toString()+" at "+e.getStackTrace()[0].toString());
			throw new com.sun.star.uno.RuntimeException(e.getMessage());
		}
		catch (Exception e){
			MessageBox msgBox = new MessageBox(xComponentContext);
			msgBox.showMessage(__displayName+": Internal error in conversion",
					e.toString()+" at "+e.getStackTrace()[0].toString());
			throw new com.sun.star.uno.RuntimeException(__displayName+" Exception");
		}
	}

	public void startElement (String sTagName, com.sun.star.xml.sax.XAttributeList xAttribs) {
		domBuilder.startElement(sTagName);
		int nLen = xAttribs.getLength();
		for (short i=0;i<nLen;i++) {
			domBuilder.setAttribute(xAttribs.getNameByIndex(i), xAttribs.getValueByIndex(i));
		}
	}

	public void endElement(String sTagName){
		domBuilder.endElement();
	}

	public void characters(String sText){
		domBuilder.characters(sText);
	}

	public void ignorableWhitespace(String str){
	}

	public void processingInstruction(String aTarget, String aData){
	}

	public void setDocumentLocator(com.sun.star.xml.sax.XLocator xLocator){
	}

	// ---------------------------------------------------------------------------
	// Implement methods from interface XTypeProvider

	public com.sun.star.uno.Type[] getTypes() {
		Type[] typeReturn = {};

		try {
			typeReturn = new Type[] {
					new Type( XTypeProvider.class ),
					new Type( XExportFilter.class ),
					new Type( XServiceName.class ),
					new Type( XServiceInfo.class ) };
		}
		catch( Exception exception ) {

		}

		return( typeReturn );
	}


	public byte[] getImplementationId() {
		byte[] byteReturn = {};

		byteReturn = new String( "" + this.hashCode() ).getBytes();

		return( byteReturn );
	}

	// ---------------------------------------------------------------------------
	// Implement method from interface XServiceName
	public String getServiceName() {
		return( __serviceName );
	}

	// ---------------------------------------------------------------------------
	// Implement methods from interface XServiceInfo
	public boolean supportsService(String stringServiceName) {
		return( stringServiceName.equals( __serviceName ) );
	}

	public String getImplementationName() {
		return __implementationName;
	}

	public String[] getSupportedServiceNames() {
		String[] stringSupportedServiceNames = { __serviceName };
		return( stringSupportedServiceNames );
	}

}
