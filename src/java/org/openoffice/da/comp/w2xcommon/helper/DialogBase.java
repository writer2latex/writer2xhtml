/************************************************************************
 *
 *  DialogBase.java
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
 *  Version 1.6 (2015-04-15)
 *
 */ 
 
package org.openoffice.da.comp.w2xcommon.helper;

import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogEventHandler;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;


/** This class provides an abstract uno component which implements a dialog
 *  from an xml description (using the DialogProvider2 service)
 */
public abstract class DialogBase extends DialogAccess implements
        XTypeProvider, XServiceInfo, XServiceName, // Uno component
        XExecutableDialog, // Execute the dialog
        XDialogEventHandler { // Event handling for dialog

		
    //////////////////////////////////////////////////////////////////////////
    // The subclass must override the following; and override the
    // implementation of XDialogEventHandler if needed
    
    /** The component will be registered under this name.
     *  The subclass must override this with a suitable name
     */
    public static String __serviceName = "";

    /** The component should also have an implementation name.
     *  The subclass must override this with a suitable name
     */
    public static String __implementationName = "";
	
    /** Return the name of the library containing the dialog
     *  The subclass must override this to provide the name of the library
     */
    public abstract String getDialogLibraryName();

    /** Return the name of the dialog within the library
     *  The subclass must override this to provide the name of the dialog
     */
    public abstract String getDialogName();
	
    /** Initialize the dialog (eg. with settings from the registry)
     *  The subclass must implement this
     */
    protected abstract void initialize();
	
    /** End the dialog after execution (eg. save settings to the registry)
     *  The subclass must implement this
     */
    protected abstract void endDialog();
	
    //////////////////////////////////////////////////////////////////////////
    // Some private global variables

    // The component context (from constructor)
    protected XComponentContext xContext;
	
    // The dialog title (created by XExecutableDialog implementation)
    private String sTitle;
	
	
    //////////////////////////////////////////////////////////////////////////
    // The constructor
	
    /** Create a new OptionsDialogBase */
    public DialogBase(XComponentContext xContext) {
    	super(null);
        this.xContext = xContext;
        sTitle = null;
    }
	

    //////////////////////////////////////////////////////////////////////////
    // Implement uno interfaces
	
    // Implement the interface XTypeProvider
    public Type[] getTypes() {
        Type[] typeReturn = {};
        try {
            typeReturn = new Type[] {
            new Type( XServiceName.class ),
            new Type( XServiceInfo.class ),
            new Type( XTypeProvider.class ),
            new Type( XExecutableDialog.class ),
            new Type( XDialogEventHandler.class ) };
        } catch(Exception exception) {
        }
        return typeReturn;
    }

    public byte[] getImplementationId() {
        byte[] byteReturn = {};
        byteReturn = new String( "" + this.hashCode() ).getBytes();
        return( byteReturn );
    }


    // Implement the interface XServiceName
    public String getServiceName() {
        return __serviceName;
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
	

    // Implement the interface XExecutableDialog
    public void setTitle(String sTitle) {
        this.sTitle = sTitle;
    }
	
    public short execute() {
        try {
            // Create the dialog
            XMultiComponentFactory xMCF = xContext.getServiceManager();
            Object provider = xMCF.createInstanceWithContext(
                "com.sun.star.awt.DialogProvider2", xContext);
            XDialogProvider2 xDialogProvider = (XDialogProvider2)
                UnoRuntime.queryInterface(XDialogProvider2.class, provider);
            String sDialogUrl = "vnd.sun.star.script:"+getDialogLibraryName()+"."
                +getDialogName()+"?location=application";
            setDialog(xDialogProvider.createDialogWithHandler(sDialogUrl, this));
            if (sTitle!=null) { getDialog().setTitle(sTitle); }

            // Do initialization using method from subclass
            initialize();

            // Execute the dialog
            short nResult = getDialog().execute();

            if (nResult == ExecutableDialogResults.OK) {
                // Finalize after execution of dialog using method from subclass
                endDialog();
            }
            getDialog().endExecute();
            return nResult;
        }
        catch (Exception e) {
            // continue as if the dialog was executed OK
            return ExecutableDialogResults.OK; 
        }
    }


    // Implement the interface XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        // Do nothing, leaving the responsibility to the subclass
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        // We do not support any method names, subclass should take care of this
        return new String[0];
    }
}



