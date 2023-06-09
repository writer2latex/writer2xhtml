/************************************************************************
 *
 *  StyleNameProvider.java
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
 *  Copyright: 2002-2009 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2009-11-08)
 *
 */ 

package org.openoffice.da.comp.w2xcommon.helper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** This class provides access to the style names and localized style names of the current document
 */
public class StyleNameProvider {
	private Map<String,Map<String,String>> displayNameCollection;
	private Map<String,Map<String,String>> internalNameCollection;
	
	/** Construct a new <code>StyleNameProvider</code>
	 * 
	 *  @param xContext the componemt context to get the desktop from
	 */
	public StyleNameProvider(XComponentContext xContext) {
		displayNameCollection = new HashMap<String,Map<String,String>>();
		internalNameCollection = new HashMap<String,Map<String,String>>();
		
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

		// Get the styles from the model
		if (xModel!=null) { 
			XStyleFamiliesSupplier xSupplier = (XStyleFamiliesSupplier) UnoRuntime.queryInterface(
					XStyleFamiliesSupplier.class, xModel);
			if (xSupplier!=null) {
				XNameAccess xFamilies = xSupplier.getStyleFamilies();
				String[] sFamilyNames = xFamilies.getElementNames();
				for (String sFamilyName : sFamilyNames) {
					Map<String,String> displayNames = new HashMap<String,String>();
					displayNameCollection.put(sFamilyName, displayNames);
					Map<String,String> internalNames = new HashMap<String,String>();
					internalNameCollection.put(sFamilyName, internalNames);
					try {
						XNameContainer xFamily = (XNameContainer) UnoRuntime.queryInterface( 
								XNameContainer.class, xFamilies.getByName(sFamilyName));
						if (xFamily!=null) {
							String[] sStyleNames = xFamily.getElementNames();
							for (String sStyleName : sStyleNames) {
								XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(
										XPropertySet.class, xFamily.getByName(sStyleName));
								if (xProps!=null) {
									String sDisplayName = (String) xProps.getPropertyValue("DisplayName");
									displayNames.put(sStyleName, sDisplayName);
									internalNames.put(sDisplayName, sStyleName);
								}
							}
						}
					}
					catch (WrappedTargetException e) {
						// ignore
					}
					catch (NoSuchElementException e) {
						// will not happen
					}
					catch (UnknownPropertyException e) {
						// will not happen
					}
				}
			}
		}
	}
	
	/** Get the mapping of internal names to display names for a style family
	 * 
	 * @param sFamily the style family (for text documents this should be CharacterStyles, ParagraphStyles, FrameStyles, PageStyles or NumberingStyles)
	 * @return a read only map from internal names to display names, or null if the family is not known to the provider
	 */
	public Map<String,String> getDisplayNames(String sFamily) {
		if (displayNameCollection.containsKey(sFamily)) {
			return Collections.unmodifiableMap(displayNameCollection.get(sFamily));
		}
		return null;
	}
	
	/** Get the mapping of display names to internal names for a style family
	 * 
	 * @param sFamily the style family (for text documents this should be CharacterStyles, ParagraphStyles, FrameStyles, PageStyles or NumberingStyles)
	 * @return a read only map from display names to internal names, or null if the family is not known to the provider
	 */
	public Map<String,String> getInternalNames(String sFamily) {
		if (internalNameCollection.containsKey(sFamily)) {
			return Collections.unmodifiableMap(internalNameCollection.get(sFamily));
		}
		return null;
	}
	
	
	

}
