/************************************************************************
*
*  ConfigurationDialogBase.java
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
*  Version 1.6 (2015-04-09)
*
*/ 

package org.openoffice.da.comp.w2xcommon.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openoffice.da.comp.w2xcommon.helper.DialogAccess;
import org.openoffice.da.comp.w2xcommon.helper.FilePicker;
import org.openoffice.da.comp.w2xcommon.helper.StyleNameProvider;

import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.awt.XWindow;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ucb.CommandAbortedException;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XStringSubstitution;

import writer2xhtml.api.ComplexOption;
import writer2xhtml.api.Config;
import writer2xhtml.api.ConverterFactory;
import writer2xhtml.util.Misc;

import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToOutputStreamAdapter;

/** This is a base implementation of a uno component which supports several option pages
 *  with a single <code>XContainerWindowEventHandler</code>. The title of the dialogs
 *  are used to differentiate between the individual pages
 */
public abstract class ConfigurationDialogBase extends WeakBase implements XContainerWindowEventHandler {
	
	// The full path to the configuration file we handle
	private String sConfigFileName = null;
	
	// The component context
	protected XComponentContext xContext;
	
	// File picker wrapper
	protected FilePicker filePicker = null;
	
	// UNO simple file access service
	protected XSimpleFileAccess2 sfa2 = null;

	// UNO path substitution
    protected XStringSubstitution xPathSub = null;
	
	// The configuration implementation
	protected Config config;
	
	// The individual page handlers (the subclass must populate this)
	protected Map<String,PageHandler> pageHandlers = new HashMap<String,PageHandler>();
	
	// The subclass must provide these:
	
	// MIME type of the document type we configure
	protected abstract String getMIMEType();
	
	// The dialog library containing the "new" and "delete" dialogs
	protected abstract String getDialogLibraryName();
	
	// The file name used for persistent storage of the edited configuration
	protected abstract String getConfigFileName();

	/** Create a new <code>ConfigurationDialogBase</code> */
	public ConfigurationDialogBase(XComponentContext xContext) {
       this.xContext = xContext;
       
       // Get the file picker
       filePicker = new FilePicker(xContext);

       // Get the SimpleFileAccess service
       try {
           Object sfaObject = xContext.getServiceManager().createInstanceWithContext(
               "com.sun.star.ucb.SimpleFileAccess", xContext);
           sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
       }
       catch (com.sun.star.uno.Exception e) {
           // failed to get SimpleFileAccess service (should not happen)
       }

       // Create the config file name
       try {
           Object psObject = xContext.getServiceManager().createInstanceWithContext(
              "com.sun.star.util.PathSubstitution", xContext);
           xPathSub = (XStringSubstitution) UnoRuntime.queryInterface(XStringSubstitution.class, psObject);
           sConfigFileName = xPathSub.substituteVariables("$(user)/"+getConfigFileName(), false);
       }
       catch (com.sun.star.uno.Exception e) {
           // failed to get PathSubstitution service (should not happen)
       }
       
       // Create the configuration
       config = ConverterFactory.createConverter(getMIMEType()).getConfig();
       
	}
       	
	// Implement XContainerWindowEventHandler
	public boolean callHandlerMethod(XWindow xWindow, Object event, String sMethod)
		throws com.sun.star.lang.WrappedTargetException {
		XDialog xDialog = (XDialog)UnoRuntime.queryInterface(XDialog.class, xWindow);
		String sTitle = xDialog.getTitle();
		
		if (!pageHandlers.containsKey(sTitle)) {
			throw new com.sun.star.lang.WrappedTargetException("Unknown dialog "+sTitle);
		}
	   
		DialogAccess dlg = new DialogAccess(xDialog);

		try {
			if (sMethod.equals("external_event") ) {
				return handleExternalEvent(dlg, sTitle, event);
			}
		}
		catch (com.sun.star.uno.RuntimeException e) {
			throw e;
		}
		catch (com.sun.star.uno.Exception e) {
			throw new com.sun.star.lang.WrappedTargetException(sMethod, this, e);
		}
		
		return pageHandlers.get(sTitle).handleEvent(dlg, sMethod);
	}
	
	private boolean handleExternalEvent(DialogAccess dlg, String sTitle, Object aEventObject) throws com.sun.star.uno.Exception {
		try {
			String sMethod = AnyConverter.toString(aEventObject);
			if (sMethod.equals("ok")) {
				loadConfig(); // The file may have been changed by other pages, thus we reload
				pageHandlers.get(sTitle).getControls(dlg);
				saveConfig();
				return true;
			}
			else if (sMethod.equals("back") || sMethod.equals("initialize")) {
				loadConfig();
				pageHandlers.get(sTitle).setControls(dlg);
				return true;
			}
		}
		catch (com.sun.star.lang.IllegalArgumentException e) {
			throw new com.sun.star.lang.IllegalArgumentException(
				"Method external_event requires a string in the event object argument.", this,(short) -1);
		}
		return false;
	}
	
	// Load the user configuration from file
	private void loadConfig() {
		if (sfa2!=null && sConfigFileName!=null) {
			try {
				XInputStream xIs = sfa2.openFileRead(sConfigFileName);
	            if (xIs!=null) {
	            	InputStream is = new XInputStreamToInputStreamAdapter(xIs);
	                config.read(is);
	                is.close();
	                xIs.closeInput();
	            }
	        }
	        catch (IOException e) {
	            // ignore
	        }
	        catch (NotConnectedException e) {
	            // ignore
	        }
	        catch (CommandAbortedException e) {
	            // ignore
	        }
	        catch (com.sun.star.uno.Exception e) {
	            // ignore
	        }
	    }
	}
	   
	// Save the user configuration
	private void saveConfig() {
		if (sfa2!=null && sConfigFileName!=null) {
			try {
				//Remove the file if it exists
	           	if (sfa2.exists(sConfigFileName)) {
	           		sfa2.kill(sConfigFileName);
	           	}
	           	// Then write the new contents
	            XOutputStream xOs = sfa2.openFileWrite(sConfigFileName);
	            if (xOs!=null) {
	            	OutputStream os = new XOutputStreamToOutputStreamAdapter(xOs);
	                config.write(os);
	                os.close();
	                xOs.closeOutput();
	            }
	        }
	        catch (IOException e) {
	            // ignore
	        }
	        catch (NotConnectedException e) {
	            // ignore
	        }
	        catch (CommandAbortedException e) {
	            // ignore
	        }
	        catch (com.sun.star.uno.Exception e) {
	            // ignore
	        }
	    }
	}
	
	// Inner class to handle the individual option pages
	protected abstract class PageHandler {
		protected abstract void getControls(DialogAccess dlg);
		
		protected abstract void setControls(DialogAccess dlg);
		
		protected abstract boolean handleEvent(DialogAccess dlg, String sMethodName);

		
		// Methods to set and get controls based on config
		protected void checkBoxFromConfig(DialogAccess dlg, String sCheckBoxName, String sConfigName) {
			dlg.setCheckBoxStateAsBoolean(sCheckBoxName, "true".equals(config.getOption(sConfigName)));
		}
		
		protected void checkBoxToConfig(DialogAccess dlg, String sCheckBoxName, String sConfigName) {
			config.setOption(sConfigName, Boolean.toString(dlg.getCheckBoxStateAsBoolean(sCheckBoxName)));
		}
		
		protected void textFieldFromConfig(DialogAccess dlg, String sTextBoxName, String sConfigName) {
			dlg.setTextFieldText(sTextBoxName, config.getOption(sConfigName));	
		}
		
		protected void textFieldToConfig(DialogAccess dlg, String sTextBoxName, String sConfigName) {
			config.setOption(sConfigName, dlg.getTextFieldText(sTextBoxName));
		}
		
		protected void listBoxFromConfig(DialogAccess dlg, String sListBoxName, String sConfigName, String[] sConfigValues, short nDefault) {
			String sCurrentValue = config.getOption(sConfigName);
			int nCount = sConfigValues.length;
			for (short i=0; i<nCount; i++) {
				if (sConfigValues[i].equals(sCurrentValue)) {
					dlg.setListBoxSelectedItem(sListBoxName, i);
					return;
				}
			}
			dlg.setListBoxSelectedItem(sListBoxName, nDefault);
		}
		
		protected void listBoxToConfig(DialogAccess dlg, String sListBoxName, String sConfigName, String[] sConfigValues) {
			config.setOption(sConfigName, sConfigValues[dlg.getListBoxSelectedItem(sListBoxName)]);
		}
				
		// Method to get a named dialog
		protected XDialog getDialog(String sDialogName) {
			XMultiComponentFactory xMCF = xContext.getServiceManager();
		   	try {
		   		Object provider = xMCF.createInstanceWithContext(
		   				"com.sun.star.awt.DialogProvider2", xContext);
		   		XDialogProvider2 xDialogProvider = (XDialogProvider2)
		   		UnoRuntime.queryInterface(XDialogProvider2.class, provider);
		   		String sDialogUrl = "vnd.sun.star.script:"+sDialogName+"?location=application";
		   		return xDialogProvider.createDialog(sDialogUrl);
		   	}
		   	catch (Exception e) {
		   		return null;
		   	}
		}

		// Method to display delete dialog
		protected boolean deleteItem(String sName) {
			XDialog xDialog=getDialog(getDialogLibraryName()+".DeleteDialog");
		   	if (xDialog!=null) {
		   		DialogAccess ddlg = new DialogAccess(xDialog);
		   		String sLabel = ddlg.getLabelText("DeleteLabel");
		   		sLabel = sLabel.replaceAll("%s", sName);
		   		ddlg.setLabelText("DeleteLabel", sLabel);
		   		boolean bDelete = xDialog.execute()==ExecutableDialogResults.OK;
		   		xDialog.endExecute();
		   		return bDelete;
		   	}
		   	return false;
		}
		   
		
	}
	
	protected abstract class CustomFileHandler extends PageHandler {
		
		// The file name
		private String sCustomFileName;
		
		public CustomFileHandler() {
			super();
			try {
				sCustomFileName = xPathSub.substituteVariables("$(user)/"+getFileName(), false);
			}
			catch (NoSuchElementException e) {
				sCustomFileName = getFileName();
			}
		}
		
		// The subclass must provide these
		protected abstract String getSuffix();
		
		protected abstract String getFileName();
		
		protected abstract void useCustomInner(DialogAccess dlg, boolean bUseCustom);
		
		@Override protected void setControls(DialogAccess dlg) {
			String sText = "";
			boolean bUseCustom = false;
			if (fileExists(sCustomFileName)) {
				sText = loadFile(sCustomFileName);
				bUseCustom = true;
			}
			else if (fileExists(sCustomFileName+".bak")) {
				sText = loadFile(sCustomFileName+".bak");
			}
			// Currently ignore failure to load the file
			if (sText==null) { sText=""; }
			
			dlg.setCheckBoxStateAsBoolean("UseCustom"+getSuffix(), bUseCustom);
			dlg.setTextFieldText("Custom"+getSuffix(), sText);
			
			useCustomChange(dlg);
		}
		
		@Override protected void getControls(DialogAccess dlg) {
			if (dlg.getCheckBoxStateAsBoolean("UseCustom"+getSuffix())) {
				saveFile(sCustomFileName,dlg.getTextFieldText("Custom"+getSuffix()));
				killFile(sCustomFileName+".bak");
			}
			else {
				saveFile(sCustomFileName+".bak",dlg.getTextFieldText("Custom"+getSuffix()));
				killFile(sCustomFileName);				
			}
		}
		
		@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
			if (sMethod.equals("UseCustom"+getSuffix()+"Change")) {
				useCustomChange(dlg);
				return true;
			}
			else if (sMethod.equals("Load"+getSuffix()+"Click")) {
				loadCustomClick(dlg);
				return true;
			}
			return false;
		}

		private void useCustomChange(DialogAccess dlg) {
			boolean bUseCustom = dlg.getCheckBoxStateAsBoolean("UseCustom"+getSuffix());
			dlg.setControlEnabled("Custom"+getSuffix(), bUseCustom);
			dlg.setControlEnabled("Load"+getSuffix()+"Button", bUseCustom);
			useCustomInner(dlg,bUseCustom);
		}
		
		protected void loadCustomClick(DialogAccess dlg) {
			String sFileName=filePicker.getPath();
			if (sFileName!=null) {
				String sText = loadFile(sFileName);
				if (sText!=null) {
					dlg.setTextFieldText("Custom"+getSuffix(), sText);
				}
			}
		}
		
		// Helpers for sfa2
		
		// Checks that the file exists
		protected boolean fileExists(String sFileName) {
			try {
				return sfa2!=null && sfa2.exists(sFileName);
			}
			catch (CommandAbortedException e) {
			}
			catch (com.sun.star.uno.Exception e) {
			}
			return false;
		}
		
		// Delete a file if it exists, return true on success
		protected boolean killFile(String sFileName) {
			try {
				if (sfa2!=null && sfa2.exists(sFileName)) {
					sfa2.kill(sFileName);
					return true;
				}
			}
			catch (com.sun.star.uno.Exception e) {
			}
			return false;
		}
		
		// Load a text file, returns null on failure
		private String loadFile(String sFileName) {
			if (sfa2!=null) {
				try {
					XInputStream xIs = sfa2.openFileRead(sFileName);
					if (xIs!=null) {
						InputStream is = new XInputStreamToInputStreamAdapter(xIs);
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						StringBuilder buf = new StringBuilder();
						String sLine;
						try {
							while ((sLine = reader.readLine())!=null) {
								buf.append(sLine).append('\n');
							}
							reader.close();
							is.close();
						}
						catch (IOException e) {
						}
						xIs.closeInput();
						return buf.toString();
					}
				}
				catch (com.sun.star.uno.Exception e) {		
				}
			}
			return null;
		}
		
		// Save a text file, return true on success
		protected boolean saveFile(String sFileName, String sText) {
			killFile(sFileName);
			try {
				XOutputStream xOs = sfa2.openFileWrite(sFileName);
				if (xOs!=null) {
					OutputStream os = new XOutputStreamToOutputStreamAdapter(xOs);
					try {
						OutputStreamWriter osw = new OutputStreamWriter(os,"UTF-8");
						osw.write(sText);
						osw.flush();
						os.close();
					}
					catch (IOException e) {
						xOs.closeOutput();
						return false;
					}
					xOs.closeOutput();
					return true;
				}
			}
			catch (com.sun.star.uno.Exception e) {
			}
			return false;
		}
	}
	
	protected abstract class UserListPageHandler extends PageHandler {
		
		// Methods to handle user controlled lists
		protected boolean deleteCurrentItem(DialogAccess dlg, String sListName) {
		   	String[] sItems = dlg.getListBoxStringItemList(sListName);
		   	short nSelected = dlg.getListBoxSelectedItem(sListName);
		   	if (nSelected>=0 && deleteItem(sItems[nSelected])) {
		   		int nOldLen = sItems.length;
		   		String[] sNewItems = new String[nOldLen-1];
		   		if (nSelected>0) {
		   			System.arraycopy(sItems, 0, sNewItems, 0, nSelected);
		   		}
		   		if (nSelected<nOldLen-1) {
		       		System.arraycopy(sItems, nSelected+1, sNewItems, nSelected, nOldLen-1-nSelected);
		   		}
		   		dlg.setListBoxStringItemList(sListName, sNewItems);
		   		short nNewSelected = nSelected<nOldLen-1 ? nSelected : (short)(nSelected-1);
					dlg.setListBoxSelectedItem(sListName, nNewSelected);
					return true;
		   	}
		   	return false;
		}
		   
		private String newItem(Set<String> suggestions) {
		   	XDialog xDialog=getDialog(getDialogLibraryName()+".NewDialog");
		   	if (xDialog!=null) {
		   		String[] sItems = Misc.sortStringSet(suggestions);
		   		DialogAccess ndlg = new DialogAccess(xDialog);
		   		ndlg.setListBoxStringItemList("Name", sItems);
		   		String sResult = null;
		   		if (xDialog.execute()==ExecutableDialogResults.OK) {
		   			DialogAccess dlg = new DialogAccess(xDialog);
		   			sResult = dlg.getTextFieldText("Name");
		   		}
		   		xDialog.endExecute();
		   		return sResult;
		   	}
		   	return null;
		}
		   
		protected String appendItem(DialogAccess dlg, String sListName, Set<String> suggestions) {
		   	String[] sItems = dlg.getListBoxStringItemList(sListName);
		   	String sNewItem = newItem(suggestions);
		   	if (sNewItem!=null) {
		   		int nOldLen = sItems.length;
		   		for (short i=0; i<nOldLen; i++) {
		   			if (sNewItem.equals(sItems[i])) {
		   				// Item already exists, select the existing one
		   				dlg.setListBoxSelectedItem(sListName, i);
		   				return null;
		   			}
		   		}
		   		String[] sNewItems = new String[nOldLen+1];
		   		System.arraycopy(sItems, 0, sNewItems, 0, nOldLen);
		   		sNewItems[nOldLen]=sNewItem;
		   		dlg.setListBoxStringItemList(sListName, sNewItems);
		   		dlg.setListBoxSelectedItem(sListName, (short)nOldLen);
		   	}
		   	return sNewItem;
		}
		
	}
	
	protected abstract class StylesPageHandler extends UserListPageHandler {
		// The subclass must define these
		protected String[] sFamilyNames;
		protected String[] sOOoFamilyNames;
		
		// Our data
		private ComplexOption[] styleMap;
		protected short nCurrentFamily = -1;
		private String sCurrentStyleName = null;
		
		// Access to display names of the styles in the current document
		protected StyleNameProvider styleNameProvider = null;
		
		// Some methods to be implemented by the subclass
		protected abstract String getDefaultConfigName();
		
		protected abstract void getControls(DialogAccess dlg, Map<String,String> attr);
		
		protected abstract void setControls(DialogAccess dlg, Map<String,String> attr);
		
		protected abstract void clearControls(DialogAccess dlg);
		
		protected abstract void prepareControls(DialogAccess dlg, boolean bHasDefinitions);
		
		// Constructor
		protected StylesPageHandler(int nCount) {
			// Get the style name provider
			styleNameProvider = new StyleNameProvider(xContext);
			
			// Reset the options
			styleMap = new ComplexOption[nCount];
			for (int i=0; i<nCount; i++) {
				styleMap[i] = new ComplexOption();
			}
		}
		
		// Implement abstract methods from super
		protected void setControls(DialogAccess dlg) {
	    	// Load style maps from config (translating keys to display names)
			int nCount = sFamilyNames.length;
			for (int i=0; i<nCount; i++) {
				ComplexOption configMap = config.getComplexOption(sFamilyNames[i]+"-map"); 
				styleMap[i].clear();
		    	Map<String,String> displayNames = styleNameProvider.getDisplayNames(sOOoFamilyNames[i]);
				copyStyles(configMap, styleMap[i], displayNames);
			}
	    	
			// Display paragraph maps first
	    	nCurrentFamily = -1;
	    	sCurrentStyleName = null;
	    	dlg.setListBoxSelectedItem("StyleFamily", (short)1);
	    	styleFamilyChange(dlg);	    	
		}
		
		protected void getControls(DialogAccess dlg) {
			updateStyleMaps(dlg);

			// Save style maps to config (translating keys back to internal names)
			int nCount = sFamilyNames.length;
			for (int i=0; i<nCount; i++) {
				ComplexOption configMap = config.getComplexOption(sFamilyNames[i]+"-map"); 
				configMap.clear();
				Map<String,String> internalNames = styleNameProvider.getInternalNames(sOOoFamilyNames[i]);
				copyStyles(styleMap[i], configMap, internalNames);
			}
		}
		
		protected boolean handleEvent(DialogAccess dlg, String sMethod) {
			if (sMethod.equals("StyleFamilyChange")) {
				styleFamilyChange(dlg);
				return true;
			}
			else if (sMethod.equals("StyleNameChange")) {
				styleNameChange(dlg);
				return true;
			}
			else if (sMethod.equals("NewStyleClick")) {
				newStyleClick(dlg);
				return true;
			}
			else if (sMethod.equals("DeleteStyleClick")) {
				deleteStyleClick(dlg);
				return true;
			}
			else if (sMethod.equals("LoadDefaultsClick")) {
				loadDefaultsClick(dlg);
				return true;
			}
			return false;
		}
		
		// Internal methods
		private void updateStyleMaps(DialogAccess dlg) {
			// Save the current style map, if any
			if (nCurrentFamily>-1 && sCurrentStyleName!=null) {
				getControls(dlg,styleMap[nCurrentFamily].get(sCurrentStyleName));
			}
		}
		
		private void styleFamilyChange(DialogAccess dlg) {	
	    	short nNewFamily = dlg.getListBoxSelectedItem("StyleFamily");
	    	if (nNewFamily>-1 && nNewFamily!=nCurrentFamily) {
	    		// The user has changed the family; load and display the corresponding style names 
		    	updateStyleMaps(dlg);
		    	nCurrentFamily = nNewFamily;
		    	sCurrentStyleName = null;

	        	String[] sStyleNames = Misc.sortStringSet(styleMap[nNewFamily].keySet());
	        	dlg.setListBoxStringItemList("StyleName", sStyleNames);
	        	if (sStyleNames.length>0) {
	        		dlg.setListBoxSelectedItem("StyleName", (short)0);
	        	}
	        	else {
	        		dlg.setListBoxSelectedItem("StyleName", (short)-1);
	        	}
	        	
	        	updateStyleControls(dlg);
	        	styleNameChange(dlg);
	    	}
		}
		
		private void styleNameChange(DialogAccess dlg) {
			if (nCurrentFamily>-1) {
				updateStyleMaps(dlg);
				short nStyleNameItem = dlg.getListBoxSelectedItem("StyleName");
				if (nStyleNameItem>=0) {
					sCurrentStyleName = dlg.getListBoxStringItemList("StyleName")[nStyleNameItem];
					setControls(dlg,styleMap[nCurrentFamily].get(sCurrentStyleName));
				}
				else {
					sCurrentStyleName = null;
					clearControls(dlg);
				}
			}
		}
		
		private void newStyleClick(DialogAccess dlg) {
			if (nCurrentFamily>-1) {
				updateStyleMaps(dlg);
				// Invalidate current style name in any case (appendItem returns null if the user
				// selects an existing style, but it still changes the current item)
				sCurrentStyleName = null;
				String sNewName = appendItem(dlg, "StyleName",styleNameProvider.getInternalNames(sOOoFamilyNames[nCurrentFamily]).keySet());
				if (sNewName!=null) {
					styleMap[nCurrentFamily].put(sNewName, new HashMap<String,String>());
					clearControls(dlg);
				}
				styleNameChange(dlg);
				updateStyleControls(dlg);
			}
		}

		private void deleteStyleClick(DialogAccess dlg) {
			if (nCurrentFamily>-1 && sCurrentStyleName!=null) {
				String sStyleName = sCurrentStyleName;
				if (deleteCurrentItem(dlg,"StyleName")) {
					styleMap[nCurrentFamily].remove(sStyleName);
					sCurrentStyleName=null;
					styleNameChange(dlg);
				}
				updateStyleControls(dlg);
			}
		}
		
		private void loadDefaultsClick(DialogAccess dlg) {
			updateStyleMaps(dlg);

			// Count styles that we will overwrite
			Config clean = ConverterFactory.createConverter(getMIMEType()).getConfig();
			clean.readDefaultConfig(getDefaultConfigName());

			int nCount = 0;
			int nFamilyCount = sFamilyNames.length;
			for (int i=0; i<nFamilyCount; i++) {
				ComplexOption cleanMap = clean.getComplexOption(sFamilyNames[i]+"-map"); 
				Map<String,String> displayNames = styleNameProvider.getDisplayNames(sOOoFamilyNames[i]);
				for (String sName : cleanMap.keySet()) {
					String sDisplayName = (displayNames!=null && displayNames.containsKey(sName)) ? displayNames.get(sName) : ""; 
					if (styleMap[i].containsKey(sDisplayName)) { nCount++; }
				}
			}

			// Display confirmation dialog
			boolean bConfirm = false;
			XDialog xDialog=getDialog(getDialogLibraryName()+".LoadDefaults");
			if (xDialog!=null) {
				DialogAccess ldlg = new DialogAccess(xDialog);
				if (nCount>0) {
					String sLabel = ldlg.getLabelText("OverwriteLabel");
					sLabel = sLabel.replaceAll("%s", Integer.toString(nCount));
					ldlg.setLabelText("OverwriteLabel", sLabel);
				}
				else {
					ldlg.setLabelText("OverwriteLabel", "");
				}
				bConfirm = xDialog.execute()==ExecutableDialogResults.OK;
				xDialog.endExecute();
			}

			// Do the replacement
			if (bConfirm) { 
				for (int i=0; i<nFamilyCount; i++) {
					ComplexOption cleanMap = clean.getComplexOption(sFamilyNames[i]+"-map"); 
					Map<String,String> displayNames = styleNameProvider.getDisplayNames(sOOoFamilyNames[i]);
					copyStyles(cleanMap, styleMap[i], displayNames);
				}
			}

			// Force update of the user interface
			nCurrentFamily = -1;
			sCurrentStyleName = null;
			styleFamilyChange(dlg);
		}
		
		private void updateStyleControls(DialogAccess dlg) {
			boolean bHasMappings = dlg.getListBoxStringItemList("StyleName").length>0;
			dlg.setControlEnabled("DeleteStyleButton", bHasMappings);
			prepareControls(dlg,bHasMappings);
		}

		private void copyStyles(ComplexOption source, ComplexOption target, Map<String,String> nameTranslation) {
			for (String sName : source.keySet()) {
				String sNewName = sName;
				if (nameTranslation!=null && nameTranslation.containsKey(sName)) {
					sNewName = nameTranslation.get(sName);
				}
				target.copy(sNewName, source.get(sName));
			}
		}	
	}
	
	protected abstract class AttributePageHandler extends PageHandler {
		// The subclass must define this
		protected String[] sAttributeNames;
		
		// Our data
		private ComplexOption attributeMap = new ComplexOption();
		private int nCurrentAttribute = -1;
		
		// Some methods to be implemented by subclass
		protected abstract void getControls(DialogAccess dlg, Map<String,String> attr);
		
		protected abstract void setControls(DialogAccess dlg, Map<String,String> attr);
		
		protected abstract void prepareControls(DialogAccess dlg, boolean bEnable);
		
		// Implement abstract methods from our super
		protected void setControls(DialogAccess dlg) {
			// Load attribute maps from config
			attributeMap.clear();
			attributeMap.copyAll(config.getComplexOption("text-attribute-map"));
			
			// Update the dialog
			nCurrentAttribute = -1;
			dlg.setListBoxSelectedItem("FormattingAttribute", (short)0);
			formattingAttributeChange(dlg);
		}
		
		protected void getControls(DialogAccess dlg) {
			updateAttributeMaps(dlg);
			
			// Save attribute maps to config
			ComplexOption configMap = config.getComplexOption("text-attribute-map");
			configMap.clear();
			for (String s: attributeMap.keySet()) {
				Map<String,String> attr = attributeMap.get(s);
				if (!attr.containsKey("deleted") || attr.get("deleted").equals("false")) {
					configMap.copy(s, attr);
				}
			}
		}
		
		protected boolean handleEvent(DialogAccess dlg, String sMethod) {
			if (sMethod.equals("FormattingAttributeChange")) {
				formattingAttributeChange(dlg);
				return true;
			}
			else if (sMethod.equals("CustomAttributeChange")) {
				customAttributeChange(dlg);
				return true;	
			}
			return false;
		}
		
		// Internal methods
		private void updateAttributeMaps(DialogAccess dlg) {
			// Save the current attribute map, if any
			if (nCurrentAttribute>-1) {
				String sName = sAttributeNames[nCurrentAttribute];
				if (!attributeMap.containsKey(sName)) {
					attributeMap.put(sName, new HashMap<String,String>());
				}
				Map<String,String> attr = attributeMap.get(sName);
				attr.put("deleted", Boolean.toString(!dlg.getCheckBoxStateAsBoolean("CustomAttribute")));
				getControls(dlg,attr);
				attributeMap.put(sName, attr);
			}
		}
		
		private void formattingAttributeChange(DialogAccess dlg) {
			updateAttributeMaps(dlg);
			short nNewAttribute = dlg.getListBoxSelectedItem("FormattingAttribute");
			if (nNewAttribute>-1 && nNewAttribute!=nCurrentAttribute) {
				nCurrentAttribute = nNewAttribute;
				String sName = sAttributeNames[nCurrentAttribute];
				if (!attributeMap.containsKey(sName)) {
					attributeMap.put(sName, new HashMap<String,String>());
					attributeMap.get(sName).put("deleted", "true");
				}
				Map<String,String> attr = attributeMap.get(sName);
				dlg.setCheckBoxStateAsBoolean("CustomAttribute", !attr.containsKey("deleted") || attr.get("deleted").equals("false"));
				customAttributeChange(dlg);
				setControls(dlg,attr);
			}
		}
		
		private void customAttributeChange(DialogAccess dlg) {
			prepareControls(dlg,dlg.getCheckBoxStateAsBoolean("CustomAttribute"));
		}
	}

}

