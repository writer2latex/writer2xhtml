/************************************************************************
 *
 *  CssDocument.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import writer2xhtml.api.OutputFile;

/**
 *  An implementation of <code>OutputFile</code> for CSS documents.
 *  (Actually this is a trivial implementation which never parses the files)
 */
public class CssDocument implements OutputFile {
	
    // Content
	private String sName;
	private String sContent;
    
    /**
     *  Constructor (creates an empty document)
     *  @param  sName  <code>Document</code> name.
     */
    public CssDocument(String sName) {
    	this.sName = sName;
    	sContent = "";
    }

	public String getFileName() {
		return sName;
	}

	public String getMIMEType() {
		return "text/css";
	}

	public boolean isMasterDocument() {
		return false;
	}
	
	public boolean containsMath() {
		return false;
	}

	public void write(OutputStream os) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(os,"UTF-8");
        osw.write(sContent);
        osw.flush();
        osw.close();
	}
	
	public void read(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
		StringBuilder buf = new StringBuilder();
		String sLine;
		while ((sLine=reader.readLine())!=null) {
			buf.append(sLine).append('\n');
		}
		sContent = buf.toString();
	}
	
	public void read(String s) {
		sContent = s;
	}
    
    
}



        




