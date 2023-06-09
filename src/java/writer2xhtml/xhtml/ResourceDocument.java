/************************************************************************
 *
 *  ResourceDocument.java
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
 
package writer2xhtml.xhtml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import writer2xhtml.api.OutputFile;
import writer2xhtml.util.Misc;

/** An implementation of <code>OutputFile</code> for resource documents.
 *  (A resource document is an arbitrary binary file to include in the converter result)
 */
public class ResourceDocument implements OutputFile {
	
    // Content
	private String sFileName;
	private String sMediaType;
	private byte[] content;
    
    /**
     *  Constructor (creates an empty document)
     *  @param sFileName  <code>Document</code> name.
     *  @param sMediaType the media type
     */
    public ResourceDocument(String sFileName, String sMediaType) {
    	this.sFileName = sFileName;
    	this.sMediaType = sMediaType;
    	content = new byte[0];
    }

    // Implement OutputFile
    
	public String getFileName() {
		return sFileName;
	}

	public String getMIMEType() {
		return sMediaType;
	}

	public boolean isMasterDocument() {
		return false;
	}
	
	public boolean containsMath() {
		return false;
	}

	public void write(OutputStream os) throws IOException {
		os.write(content); 
	}
	
	/** Load the resource document bytes from an arbitrary input stream
	 * 
	 * @param is the input stream
	 * @throws IOException if any error occurs reading the input stream
	 */
	public void read(InputStream is) throws IOException {
		content = Misc.inputStreamToByteArray(is);
	}
    
    
}