/************************************************************************
 *
 *  FilePicker.java
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
 *  Version 1.4 (2014-09-24)
 *
 */ 

package org.openoffice.da.comp.w2xcommon.helper;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.ui.dialogs.XFilePicker;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class FilePicker {
	
	private XComponentContext xContext;
	
	// The default directory for the dialog
	private String sDirectoryURL;
	
	/** Convenience wrapper class for the UNO file picker service
	 * 
	 *  @param xContext the UNO component context from which the file picker can be created
	 */
	public FilePicker(XComponentContext xContext) {
        this.xContext = xContext;
        sDirectoryURL = null;
	}
	
	/** Get one or more user selected paths with a file picker
	 * 
	 *  Warning: This does not work on all platforms when using native file pickers
	 *  (but always when using Office file pickers)
	 * 
	 * @return array containing the path URLs or null if the dialog is canceled
	 */
	public String[] getPaths() {
		return getPaths(true);
	}

	/** Get a user selected path with a file picker
	 * 
	 * @return the path URL or null if the dialog is canceled
	 */
	public String getPath() {
		String[] sPaths = getPaths(false);
		if (sPaths!=null && sPaths.length>0) {
			return sPaths[0];
		}
		return null;
	}
	
	private String[] getPaths(boolean bAllowMultiSelection) {
		// Create FilePicker
		Object filePicker = null;
		try {
			// Note: Could be changed for OfficeFilePicker to always use internal file pickers
			filePicker = xContext.getServiceManager().createInstanceWithContext("com.sun.star.ui.dialogs.FilePicker", xContext);
		}
		catch (com.sun.star.uno.Exception e) {
			return null;
		}

		// Get the required interfaces
		XFilePicker xFilePicker = (XFilePicker) UnoRuntime.queryInterface(XFilePicker.class, filePicker);
		XExecutableDialog xExecutable = (XExecutableDialog) UnoRuntime.queryInterface(XExecutableDialog.class, xFilePicker);
		
		// Configure the file picker
		xFilePicker.setMultiSelectionMode(bAllowMultiSelection);
		if (sDirectoryURL!=null) {
			try {
				xFilePicker.setDisplayDirectory(sDirectoryURL);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		
		// Get the paths
		String[] sPaths = null;
		if (xExecutable.execute() == ExecutableDialogResults.OK) {
			sDirectoryURL = xFilePicker.getDisplayDirectory();
			String[] sPathList = xFilePicker.getFiles();
			int nCount = sPathList.length;
			if (nCount>1) {
				// According to the spec, the first entry is the path and remaining entries are file names
				sPaths = new String[nCount-1];
				for (int i=1; i<nCount; i++) {
					sPaths[i-1]=sPathList[0] + sPathList[i];
				}
			}
			else if (nCount==1) {
				sPaths = sPathList;
			}
		}

		// Dispose the file picker
		XComponent xComponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, xFilePicker);
		xComponent.dispose();

		return sPaths;
	}

}
