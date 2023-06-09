/************************************************************************
 *
 *  FolderPicker.java
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
 *  Version 1.2 (2010-10-11)
 *
 */ 

package org.openoffice.da.comp.w2xcommon.helper;

import com.sun.star.lang.XComponent;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.ui.dialogs.XFolderPicker;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class FolderPicker {
	
	private XComponentContext xContext; 
	
	/** Convenience wrapper class for the UNO folder picker service
	 * 
	 *  @param xContext the UNO component context from which the folder picker can be created
	 */
	public FolderPicker(XComponentContext xContext) {
        this.xContext = xContext;
	}
	
	/** Get a user selected path with a folder picker
	 * 
	 * @return the path or null if the dialog is canceled
	 */
	public String getPath() {
		// Create FolderPicker
		Object folderPicker = null;
		try {
			folderPicker = xContext.getServiceManager().createInstanceWithContext("com.sun.star.ui.dialogs.FolderPicker", xContext);
		}
		catch (com.sun.star.uno.Exception e) {
			return null;
		}

		// Display the FolderPicker
		XFolderPicker xFolderPicker = (XFolderPicker) UnoRuntime.queryInterface(XFolderPicker.class, folderPicker);
		XExecutableDialog xExecutable = (XExecutableDialog) UnoRuntime.queryInterface(XExecutableDialog.class, xFolderPicker);

		// Get the path
		String sPath = null;
		
		if (xExecutable.execute() == ExecutableDialogResults.OK) {
			sPath = xFolderPicker.getDirectory();
		}

		// Dispose the folder picker
		XComponent xComponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, folderPicker);
		if (xComponent!=null) { // Seems not to be ??
			xComponent.dispose();
		}
		
		return sPath;
	}

}
