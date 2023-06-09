/************************************************************************
 *
 *  FiledMasterNameProvider.java
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
 *  Version 1.2 (2010-12-09)
 *
 */ 

package org.openoffice.da.comp.w2xcommon.helper;

import java.util.HashSet;
import java.util.Set;

import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** This class provides access to the names of all field masters in the current document
 */
public class FieldMasterNameProvider {
	private String[] fieldMasterNames;
	
	/** Construct a new <code>FieldMasterNameProvider</code>
	 * 
	 *  @param xContext the component context to get the desktop from
	 */
	public FieldMasterNameProvider(XComponentContext xContext) {
		fieldMasterNames = new String[0];

		// TODO: This code should be shared (identical with StyleNameProvider...)
		// Get the model for the current frame
		XModel xModel = null;
		try {
			Object desktop = xContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", xContext); 
			XDesktop xDesktop = (XDesktop)UnoRuntime.queryInterface(XDesktop.class, desktop);
            XController xController = xDesktop.getCurrentFrame().getController();
            if (xController!=null) {
                xModel = xController.getModel();
            }
		}
		catch (Exception e) {
			// do nothing
		}

		// Get the field masters from the model
		if (xModel!=null) {
			XTextFieldsSupplier xSupplier = (XTextFieldsSupplier) UnoRuntime.queryInterface(
					XTextFieldsSupplier.class, xModel);
			if (xSupplier!=null) {
				XNameAccess xFieldMasters = xSupplier.getTextFieldMasters();
				fieldMasterNames = xFieldMasters.getElementNames();
			}
		}
	}
	
	/** Get the names of all field masters relative to a given prefix
	 * 
	 * @param sPrefix the prefix to look for, e.g. "com.sun.star.text.fieldmaster.SetExpression."
	 * @return a read only <code>Set</code> containing all known names with the given prefix, stripped for the prefix
	 */
	public Set<String> getFieldMasterNames(String sPrefix) {
		Set<String> names = new HashSet<String>();
		for (String sName : fieldMasterNames) {
			if (sName.startsWith(sPrefix)) {
				names.add(sName.substring(sPrefix.length()));
			}
		}
		return names;
	}
	
}
