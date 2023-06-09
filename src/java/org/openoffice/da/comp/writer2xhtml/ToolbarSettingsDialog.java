/************************************************************************
 *
 *  ToolbarSettingsDialog.java
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
 *  Copyright: 2002-2022 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2022-06-08)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.openoffice.da.comp.w2xcommon.helper.DialogAccess;
import org.openoffice.da.comp.w2xcommon.helper.FilePicker;
import org.openoffice.da.comp.w2xcommon.helper.RegistryHelper;
import org.openoffice.da.comp.w2xcommon.helper.XPropertySetHelper;

import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

import com.sun.star.lib.uno.helper.WeakBase;

/** This class provides a uno component which implements the configuration
 *  of the writer2xhtml toolbar
 */
public final class ToolbarSettingsDialog
    extends WeakBase
    implements XServiceInfo, XContainerWindowEventHandler {
	
	public static final String REGISTRY_PATH = "/org.openoffice.da.Writer2xhtml.toolbar.ToolbarOptions/Settings";

    private XComponentContext xContext;
    private FilePicker filePicker;
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.ToolbarSettingsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.ToolbarSettingsDialog";

    /** Create a new ToolbarSettingsDialog */
    public ToolbarSettingsDialog(XComponentContext xContext) {
        this.xContext = xContext;
        filePicker = new FilePicker(xContext);
    }
	
    // Implement XContainerWindowEventHandler
    public boolean callHandlerMethod(XWindow xWindow, Object event, String sMethod)
        throws com.sun.star.lang.WrappedTargetException {
		XDialog xDialog = (XDialog)UnoRuntime.queryInterface(XDialog.class, xWindow);
		DialogAccess dlg = new DialogAccess(xDialog);

        try {
            if (sMethod.equals("external_event") ){
                return handleExternalEvent(dlg, event);
            }
            else if (sMethod.equals("XhtmlFormatChange")) {
            	return true;
            }
            else if (sMethod.equals("XhtmlViewChange")) {
            	return xhtmlViewChange(dlg);
            }
            else if (sMethod.equals("XhtmlBrowseClick")) {
            	return xhtmlBrowseClick(dlg);
            }
            else if (sMethod.equals("EpubFormatChange")) {
            	return true;
            }
            else if (sMethod.equals("EpubViewChange")) {
            	return epubViewChange(dlg);
            }
            else if (sMethod.equals("EpubBrowseClick")) {
            	return epubBrowseClick(dlg);
            }
        }
        catch (com.sun.star.uno.RuntimeException e) {
            throw e;
        }
        catch (com.sun.star.uno.Exception e) {
            throw new com.sun.star.lang.WrappedTargetException(sMethod, this, e);
        }
        return false;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "external_event", "XhtmlFormatChange", "XhtmlViewChange", "XhtmlBrowseClick",
        		"EpupFormatChange", "EpubViewChange", "EpubBrowseClick" };
        return sNames;
    }
    
    // Implement the interface XServiceInfo
    public boolean supportsService(String sServiceName) {
        return sServiceName.equals(__serviceName);
    }

    public String getImplementationName() {
        return __implementationName;
    }
    
    public String[] getSupportedServiceNames() {
        String[] sSupportedServiceNames = { __serviceName };
        return sSupportedServiceNames;
    }
	
    // Private stuff
    
    private boolean handleExternalEvent(DialogAccess dlg, Object aEventObject)
        throws com.sun.star.uno.Exception {
        try {
            String sMethod = AnyConverter.toString(aEventObject);
            if (sMethod.equals("ok")) {
                saveConfiguration(dlg);
                return true;
            } else if (sMethod.equals("back") || sMethod.equals("initialize")) {
                loadConfiguration(dlg);
                enableXhtmlExecutable(dlg);
                enableEpubExecutable(dlg);
                return true;
            }
        }
        catch (com.sun.star.lang.IllegalArgumentException e) {
            throw new com.sun.star.lang.IllegalArgumentException(
            "Method external_event requires a string in the event object argument.", this,(short) -1);
        }
        return false;
    }
    
    private void loadConfiguration(DialogAccess dlg) {
    	RegistryHelper registry = new RegistryHelper(xContext);
    	
		try {
			Object view = registry.getRegistryView(REGISTRY_PATH, false);
			XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
			
        	dlg.setListBoxSelectedItem("XhtmlFormat",
        			XPropertySetHelper.getPropertyValueAsShort(xProps, "XhtmlFormat"));
        	dlg.setListBoxSelectedItem("XhtmlView",
        			XPropertySetHelper.getPropertyValueAsShort(xProps, "XhtmlView"));
        	dlg.setTextFieldText("XhtmlExecutable",
        			XPropertySetHelper.getPropertyValueAsString(xProps, "XhtmlExecutable"));
        	dlg.setListBoxSelectedItem("EpubFormat",
        			XPropertySetHelper.getPropertyValueAsShort(xProps, "EpubFormat"));
        	dlg.setListBoxSelectedItem("EpubView",
        			XPropertySetHelper.getPropertyValueAsShort(xProps, "EpubView"));
        	dlg.setTextFieldText("EpubExecutable",
        			XPropertySetHelper.getPropertyValueAsString(xProps, "EpubExecutable"));
		} catch (Exception e) {
    		// Failed to get registry view
		}
    }
    
    private void saveConfiguration(DialogAccess dlg) {
		RegistryHelper registry = new RegistryHelper(xContext);
    	try {
    		Object view = registry.getRegistryView(REGISTRY_PATH, true);
    		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
   			XPropertySetHelper.setPropertyValue(xProps, "XhtmlFormat", dlg.getListBoxSelectedItem("XhtmlFormat"));
   			XPropertySetHelper.setPropertyValue(xProps, "XhtmlView", dlg.getListBoxSelectedItem("XhtmlView"));
   			XPropertySetHelper.setPropertyValue(xProps, "XhtmlExecutable", dlg.getTextFieldText("XhtmlExecutable"));
   			XPropertySetHelper.setPropertyValue(xProps, "EpubFormat", dlg.getListBoxSelectedItem("EpubFormat"));
   			XPropertySetHelper.setPropertyValue(xProps, "EpubView", dlg.getListBoxSelectedItem("EpubView"));
   			XPropertySetHelper.setPropertyValue(xProps, "EpubExecutable", dlg.getTextFieldText("EpubExecutable"));
   			
            // Commit registry changes
            XChangesBatch  xUpdateContext = (XChangesBatch)
                UnoRuntime.queryInterface(XChangesBatch.class,view);
            try {
                xUpdateContext.commitChanges();
            }
            catch (Exception e) {
                // ignore
            }
                        
        	registry.disposeRegistryView(view);
    	}
    	catch (Exception e) {
    		// Failed to get registry view
    	}		    	
    }
    
    private boolean xhtmlViewChange(DialogAccess dlg) {
    	enableXhtmlExecutable(dlg);
    	return true;
    }

    private void enableXhtmlExecutable(DialogAccess dlg) {
    	int nItem = dlg.getListBoxSelectedItem("XhtmlView");
    	dlg.setControlEnabled("XhtmlExecutableLabel", nItem==2);
    	dlg.setControlEnabled("XhtmlExecutable", nItem==2);
    	dlg.setControlEnabled("XhtmlBrowseButton", nItem==2);
    }
    
    private boolean xhtmlBrowseClick(DialogAccess dlg) {
    	browseForExecutable(dlg,"XhtmlExecutable");
    	return true;
    }
	
    private boolean epubViewChange(DialogAccess dlg) {
    	enableEpubExecutable(dlg);
    	return true;
    }
	
    private void enableEpubExecutable(DialogAccess dlg) {
    	int nItem = dlg.getListBoxSelectedItem("EpubView");
    	dlg.setControlEnabled("EpubExecutableLabel", nItem==2);
    	dlg.setControlEnabled("EpubExecutable", nItem==2);
    	dlg.setControlEnabled("EpubBrowseButton", nItem==2);
    }
    
    private boolean epubBrowseClick(DialogAccess dlg) {
    	browseForExecutable(dlg,"EpubExecutable");
    	return true;
    }
    
    private boolean browseForExecutable(DialogAccess dlg, String sControlName) {
    	String sPath = filePicker.getPath();
    	if (sPath!=null) {
    		try {
				dlg.setComboBoxText(sControlName, new File(new URI(sPath)).getCanonicalPath());
			}
    		catch (IOException e) {
			}
    		catch (URISyntaxException e) {
			}
    	}     
    	return true;
    }

	
}



