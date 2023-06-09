/************************************************************************
 *
 *  XhtmlUNOPublisher.java
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
 *  Version 1.6 (2015-04-05)
 *  
 */
package org.openoffice.da.comp.writer2xhtml;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.openoffice.da.comp.w2xcommon.filter.UNOPublisher;
import org.openoffice.da.comp.w2xcommon.helper.MessageBox;
import org.openoffice.da.comp.w2xcommon.helper.RegistryHelper;
import org.openoffice.da.comp.w2xcommon.helper.StreamGobbler;
import org.openoffice.da.comp.w2xcommon.helper.XPropertySetHelper;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import writer2xhtml.util.Misc;

public class XhtmlUNOPublisher extends UNOPublisher {
	
    public XhtmlUNOPublisher(XComponentContext xContext, XFrame xFrame, String sAppName) {
    	super(xContext, xFrame, sAppName);
    }
    
    
	/** Display the converted document depending on user settings
	 * 
	 *  @param sURL the URL of the converted document
	 *  @param format the target format
	 */
    @Override protected void postProcess(String sURL, TargetFormat format) {
    	RegistryHelper registry = new RegistryHelper(xContext);
    	
    	short nView = 1;
    	String sExecutable = null;
    	
		try {
			Object view = registry.getRegistryView(ToolbarSettingsDialog.REGISTRY_PATH, false);
			XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
			
			if (format==TargetFormat.xhtml || format==TargetFormat.xhtml11 || format==TargetFormat.xhtml_mathml || format==TargetFormat.html5) {
				nView = XPropertySetHelper.getPropertyValueAsShort(xProps, "XhtmlView");
				sExecutable = XPropertySetHelper.getPropertyValueAsString(xProps, "XhtmlExecutable");				
			}
			else { // EPUB				
				nView = XPropertySetHelper.getPropertyValueAsShort(xProps, "EpubView");
				sExecutable = XPropertySetHelper.getPropertyValueAsString(xProps, "EpubExecutable");
			}
		} catch (Exception e) {
    		// Failed to get registry view
		}
		
        File file = Misc.urlToFile(sURL);
        if (file.exists()) {
    		if (nView==0) {
    			return;
    		}
    		 else if (nView==1) {
            	if (openWithDefaultApplication(file)) {
            		return;
            	}
    		}
    		else if (nView==2) {
    			if (openWithCustomApplication(file, sExecutable)) {
    				return;
    			}
    		}
        }
        MessageBox msgBox = new MessageBox(xContext, xFrame);
        msgBox.showMessage("Writer2xhtml","Error: Failed to open exported document");
    }
    
    // Open the file in the default application on this system (if any)
    private boolean openWithDefaultApplication(File file) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
				desktop.open(file);
				return true;
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
        }
        return false;
    }
    
    // Open the file with the user defined application
    private boolean openWithCustomApplication(File file, String sExecutable) {
        try {
			Vector<String> command = new Vector<String>();
			command.add(sExecutable);
			command.add(file.getPath());
			
            ProcessBuilder pb = new ProcessBuilder(command);
            Process proc = pb.start();        

            // Gobble the error stream of the application
            StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), "ERROR");            
            
            // Gobble the output stream of the application
            StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "OUTPUT");
                
            errorGobbler.start();
            outputGobbler.start();
                                    
            // The application exists if the process exits with 0
            return proc.waitFor()==0;
        }
        catch (InterruptedException e) {
            return false;
        }
        catch (IOException e) {
            return false;
        }
	}
    
}
