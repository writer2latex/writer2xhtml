/************************************************************************
*
*  ConverterResultImpl.java
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
*  Copyright: 2002-2010 by Henrik Just
*
*  All Rights Reserved.
* 
*  Version 1.2 (2010-03-24)
*
*/ 

package writer2xhtml.base;

import writer2xhtml.api.ContentEntry;
import writer2xhtml.api.OutputFile;

public class ContentEntryImpl implements ContentEntry {
	private String sTitle;
	private int nLevel;
	private OutputFile file;
	private String sTarget;
	
	public ContentEntryImpl(String sTitle, int nLevel, OutputFile file, String sTarget) {
		this.sTitle = sTitle;
		this.nLevel = nLevel;
		this.file = file;
		this.sTarget = sTarget;
	}
	
	public String getTitle() {
		return sTitle;
	}
	
	public int getLevel() {
		return nLevel;
	}
	
	public OutputFile getFile() {
		return file;
	}
	
	public String getTarget() {
		return sTarget;
	}
}
