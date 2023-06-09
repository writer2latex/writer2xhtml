/************************************************************************
 *
 *  UNOPublisher.java
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
 *  Version 1.7 (2022-06-17)
 *  
 */
package org.openoffice.da.comp.w2xcommon.filter;

import java.io.IOException;
import java.util.regex.Pattern;

import org.openoffice.da.comp.w2xcommon.helper.MessageBox;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertyAccess;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.XInputStream;
import com.sun.star.task.XStatusIndicator;
import com.sun.star.task.XStatusIndicatorFactory;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifiable;

import writer2xhtml.util.Misc;

/** This class converts an open office document to another format
 */
public class UNOPublisher {
	
    public enum TargetFormat { xhtml, xhtml11, xhtml_mathml, html5, epub, epub3, latex };
    
    private String sAppName;
    
    protected XComponentContext xContext;
    protected XFrame xFrame;
    private XModel xModel = null;
    private PropertyValue[] mediaProps = null;
        
    /** Create a new <code>UNOPublisher</code> based on a loaded office document
     * 
     * @param xContext the component context from which new UNO services are instantiated
     * @param xFrame the current frame
     * @param sAppName the name of the application using the <code>UNOPublisher</code>
     */
    public UNOPublisher(XComponentContext xContext, XFrame xFrame, String sAppName) {
    	this.xContext = xContext;
    	this.xFrame = xFrame;
    	this.sAppName = sAppName;
        // Get the model for the document from the frame
        XController xController = xFrame.getController();
        if (xController!=null) {
            xModel = xController.getModel();
        }
    }
    
    /** Publish the document associated with this <code>UNOPublisher</code>. This involves five steps:
     *  (1) Check that the document is saved in the local file system.
     *  (2) Display the options dialog.
     *  (3) Save the document (if the modified flag is true).
     *  (4) Convert the document.
     *  (5) Post process the document, e.g. displaying the result
     * 
     * @param format the target format
     * @return true if the publishing was successful
     */
    public boolean publish(TargetFormat format) {
        if (documentSaved() && updateMediaProperties(format)) {
	        // Create a (somewhat coarse grained) status indicator/progress bar
	        XStatusIndicatorFactory xFactory = (com.sun.star.task.XStatusIndicatorFactory)
	            UnoRuntime.queryInterface(com.sun.star.task.XStatusIndicatorFactory.class, xFrame);
	        XStatusIndicator xStatus = xFactory.createStatusIndicator();
	        xStatus.start(sAppName,10);
	        xStatus.setValue(1); // At least we have started, that's 10% :-)
	        
            try {
            	// Save document if required
            	saveDocument();
            	xStatus.setValue(4); // Document saved, that's 40%
            	
	            // Convert to desired format
	            UNOConverter converter = new UNOConverter(mediaProps, xContext);
				// Initialize the file access (to read the office document)
				XSimpleFileAccess2 sfa2 = null;
				try {
					Object sfaObject = xContext.getServiceManager().createInstanceWithContext(
							"com.sun.star.ucb.SimpleFileAccess", xContext); //$NON-NLS-1$
					sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
				}
				catch (com.sun.star.uno.Exception e) {
					// failed to get SimpleFileAccess service (should not happen)
				}
	            XInputStream xis = sfa2.openFileRead(xModel.getURL());
	            converter.convert(xis);
	            xis.closeInput();
	        }
	        catch (IOException e) {
	            xStatus.end();
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            msgBox.showMessage(sAppName,Messages.getString("UNOPublisher.failexport")); //$NON-NLS-1$
	            return false;
	        }
	        catch (com.sun.star.uno.Exception e) {
	            xStatus.end();
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            msgBox.showMessage(sAppName,Messages.getString("UNOPublisher.failexport")); //$NON-NLS-1$
	            return false;
			}
	        xStatus.setValue(7); // Document is converted, that's 70%
	        
	        postProcess(getTargetURL(format),format);
	        
	        xStatus.setValue(10); // Export is finished (The user will usually not see this...)
	        xStatus.end();
	        return true;
	    }
        return false;
    }
    
    /** Filter the file name to avoid unwanted characters
     * 
     * @param sFileName the original file name
     * @return the filtered file name
     */	
    protected String filterFileName(String sFileName) {
    	return sFileName;
    }
    
    /** Post process the media properties after displaying the dialog
     * 
     * @param mediaProps the media properties as set by the dialog
     * @return the updated media properties
     */
    protected PropertyValue[] postProcessMediaProps(PropertyValue[] mediaProps) {
    	return mediaProps;
    }
    
    /** Post process the document after conversion.
     * 
     *  @param sTargetURL URL of the converted document
     *  @param format the target format
     */
    protected void postProcess(String sTargetURL, TargetFormat format) {
    }
    
    /** Check that the document is saved in a location, we can use
     * 
     * @return true if everthing is o.k.
     */
    public boolean documentSaved() {
        String sDocumentUrl = xModel.getURL();
        if (sDocumentUrl.length()==0) { // The document has no location
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage(sAppName,Messages.getString("UNOPublisher.savedocument")); //$NON-NLS-1$
            return false;
        }
        else if (!".odt".equals(Misc.getFileExtension(sDocumentUrl)) && !".fodt".equals(Misc.getFileExtension(sDocumentUrl)) && !".ods".equals(Misc.getFileExtension(sDocumentUrl)) && !".fods".equals(Misc.getFileExtension(sDocumentUrl))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage(sAppName,Messages.getString("UNOPublisher.saveodt")); //$NON-NLS-1$
            return false;        	        	
        }
        else if (!sDocumentUrl.startsWith("file:")) { //$NON-NLS-1$
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage(sAppName,Messages.getString("UNOPublisher.savefilesystem")); //$NON-NLS-1$
            return false;        	
        }
        else if (System.getProperty("os.name").startsWith("Windows")) { //$NON-NLS-1$ //$NON-NLS-2$
        	// Avoid UNC paths (LaTeX does not like them)
    		Pattern windowsPattern = Pattern.compile("^file:///[A-Za-z][|:].*"); //$NON-NLS-1$
    		if (!windowsPattern.matcher(sDocumentUrl).matches()) {
                MessageBox msgBox = new MessageBox(xContext, xFrame);
                msgBox.showMessage(sAppName,
                		Messages.getString("UNOPublisher.savedrivename")); //$NON-NLS-1$
                return false;        		
    		}
		}
        return true;
    }
    
    private boolean saveDocument() {
        XModifiable xModifiable = (XModifiable) UnoRuntime.queryInterface(XModifiable.class, xModel);
        if (xModifiable.isModified()) { // The document is modified and need to be saved
	        XStorable xStorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, xModel);
	        try {
				xStorable.store();
			} catch (com.sun.star.io.IOException e) {
				return false;
			}
        }
        return true;
    }
    
    // Some utility methods

    /** Get the target path (or null if the document is not saved)
     * 
     * @return the path
     */
    public String getTargetPath() {
    	if (xModel.getURL().length()>0) {
	    	String sBaseURL = Misc.removeExtension(xModel.getURL());
	    	return Misc.getPath(sBaseURL);   
    	}
    	return null;
    }
    
    /** Get the target file name (or null if the document is not saved)
     * 
     * @return the file name
     */
    public String getTargetFileName() {
    	if (xModel.getURL().length()>0) {
	    	String sBaseURL = Misc.removeExtension(xModel.getURL());
	    	return filterFileName(Misc.getFileName(sBaseURL));
    	}
    	return null;
    }
    
    protected String getTargetURL(TargetFormat format) {
    	return getTargetPath()+getTargetFileName()+getTargetExtension(format);
    }
    
    private void prepareMediaProperties(TargetFormat format) {
        // Create inital media properties
        mediaProps = new PropertyValue[2];
        mediaProps[0] = new PropertyValue();
        mediaProps[0].Name = "FilterName"; //$NON-NLS-1$
        mediaProps[0].Value = getFilterName(format);
        mediaProps[1] = new PropertyValue();
        mediaProps[1].Name = "URL"; //$NON-NLS-1$
        mediaProps[1].Value = getTargetURL(format);
    }
    
    private boolean updateMediaProperties(TargetFormat format) {
    	prepareMediaProperties(format);
    	
    	String sDialogName = xModel.getURL().endsWith(".odt") || xModel.getURL().endsWith(".fodt") ? //$NON-NLS-1$ //$NON-NLS-2$
    			getDialogName(format) : getDialogNameCalc(format);
    	if (sDialogName!=null) {
	    	try {
	            // Display options dialog
	            Object dialog = xContext.getServiceManager()
	                .createInstanceWithContext(sDialogName, xContext);
	
	            XPropertyAccess xPropertyAccess = (XPropertyAccess)
	                UnoRuntime.queryInterface(XPropertyAccess.class, dialog);
	            xPropertyAccess.setPropertyValues(mediaProps);
	
	            XExecutableDialog xDialog = (XExecutableDialog)
	                UnoRuntime.queryInterface(XExecutableDialog.class, dialog);
	            if (xDialog.execute()==ExecutableDialogResults.OK) {
	                mediaProps = postProcessMediaProps(xPropertyAccess.getPropertyValues());
	                return true;
	            }
	        }
	        catch (com.sun.star.beans.UnknownPropertyException e) {
	            // setPropertyValues will not fail..
	        }
	        catch (com.sun.star.uno.Exception e) {
	            // getServiceManager will not fail..
	        }
    	}
    	// No dialog exists, or the dialog was cancelled
    	mediaProps = null;
    	return false;
    }
    
    private static String getTargetExtension(TargetFormat format) {
    	switch (format) {
    	case xhtml: return ".html"; //$NON-NLS-1$
    	case xhtml11: return ".xhtml"; //$NON-NLS-1$
    	case xhtml_mathml: return ".xhtml"; //$NON-NLS-1$
    	case html5: return ".html"; //$NON-NLS-1$
    	case epub: return ".epub"; //$NON-NLS-1$
    	case epub3: return ".epub"; //$NON-NLS-1$
    	case latex: return ".tex"; //$NON-NLS-1$
    	default: return ""; //$NON-NLS-1$
    	}
    }
      
    private static String getDialogName(TargetFormat format) {
    	switch (format) {
    	case xhtml: 
    	case xhtml11: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialog"; //$NON-NLS-1$
    	case xhtml_mathml: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogMath"; //$NON-NLS-1$
    	case html5: return "org.openoffice.da.comp.writer2xhtml.HTML5OptionsDialog"; //$NON-NLS-1$
    	case epub: return "org.openoffice.da.comp.writer2xhtml.EpubOptionsDialog"; //$NON-NLS-1$
    	case epub3: return "org.openoffice.da.comp.writer2xhtml.Epub3OptionsDialog"; //$NON-NLS-1$
    	case latex: return "org.openoffice.da.comp.writer2latex.LaTeXOptionsDialog"; //$NON-NLS-1$
    	default: return null;
    	}
    }
    
    private static String getDialogNameCalc(TargetFormat format) {
    	switch (format) {
    	case xhtml: 
    	case xhtml11: 
    	case xhtml_mathml: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogCalc";  //$NON-NLS-1$
    	case html5: return "org.openoffice.da.comp.writer2xhtml.HTML5OptionsDialogCalc";  //$NON-NLS-1$
    	case epub: 
    	case epub3: 
    	case latex:
    	default: return null;
    	}    	
    }
      
    private static String getFilterName(TargetFormat format) {
    	switch (format) {
    	case xhtml: return "org.openoffice.da.writer2xhtml"; //$NON-NLS-1$
    	case xhtml11: return "org.openoffice.da.writer2xhtml11"; //$NON-NLS-1$
    	case xhtml_mathml: return "org.openoffice.da.writer2xhtml.mathml"; //$NON-NLS-1$
    	case html5: return "org.openoffice.da.writer2xhtml5"; //$NON-NLS-1$
    	case epub: return "org.openoffice.da.writer2xhtml.epub"; //$NON-NLS-1$
    	case epub3: return "org.openoffice.da.writer2xhtml.epub3"; //$NON-NLS-1$
    	case latex: return "org.openoffice.da.writer2latex"; //$NON-NLS-1$
    	default: return ""; //$NON-NLS-1$
    	}
    }

}
