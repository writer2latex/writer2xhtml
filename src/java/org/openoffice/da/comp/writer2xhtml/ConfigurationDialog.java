/************************************************************************
*
*  ConfigurationDialog.java
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
*  Copyright: 2002-2023 by Henrik Just
*
*  All Rights Reserved.
* 
*  Version 1.7.1 (2023-08-03)
*
*/ 

package org.openoffice.da.comp.writer2xhtml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.openoffice.da.comp.w2xcommon.filter.ConfigurationDialogBase;
import org.openoffice.da.comp.w2xcommon.helper.DialogAccess;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.ucb.CommandAbortedException;
import com.sun.star.uno.Exception;
import com.sun.star.uno.XComponentContext;

import writer2xhtml.api.Converter;
import writer2xhtml.api.ConverterFactory;

public class ConfigurationDialog extends ConfigurationDialogBase implements XServiceInfo {
	private String sResourceDirName;

    // Implement the interface XServiceInfo

	/** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.ConfigurationDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.ConfigurationDialog";
    
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
    
    // Configure the base class
    @Override protected String getMIMEType() { return "text/html"; }
    
    @Override protected String getDialogLibraryName() { return "W2XDialogs2"; }
    
    @Override protected String getConfigFileName() { return "writer2xhtml.xml"; }
    
    /** Construct a new <code>ConfigurationDialog</code> */
    public ConfigurationDialog(XComponentContext xContext) {
    	super(xContext);
    	
        // Create the resource dir name
        try {
            sResourceDirName = xPathSub.substituteVariables("$(user)/writer2xhtml-resources", false);
        }
		catch (NoSuchElementException e) {
			sResourceDirName = "writer2xhtml-resources";
		}
    	
    	pageHandlers.put("General", new GeneralHandler());
    	pageHandlers.put("Template", new TemplateHandler());
    	pageHandlers.put("Stylesheets", new StylesheetsHandler());
    	pageHandlers.put("Formatting", new FormattingHandler());
    	pageHandlers.put("Styles1", new Styles1Handler());
    	pageHandlers.put("Styles2", new Styles2Handler());
    	pageHandlers.put("Formatting", new FormattingHandler());
    	pageHandlers.put("Content", new ContentHandler());
    }
    
    // Implement remaining method from XContainerWindowEventHandler
    public String[] getSupportedMethodNames() {
    	String[] sNames = { "EncodingChange", // General
    			"CustomTemplateChange", "LoadTemplateClick", "TemplateKeyup", // Template
    			"UseCustomStylesheetChange", "IncludeCustomStylesheetClick", "LoadStylesheetClick",
    			"NewResourceClick", "DeleteResourceClick", // Stylesheet
    			"StyleFamilyChange", "StyleNameChange", "NewStyleClick", "DeleteStyleClick", "LoadDefaultsClick" // Styles1
    	};
    	return sNames;
    }
    
    // the page handlers
	private final String[] sCharElements = { "span", "abbr", "acronym", "b", "big", "cite", "code", "del", "dfn", "em", "i",
			"ins", "kbd", "samp", "small", "strong", "sub", "sup", "tt", "var", "q" };

    private class GeneralHandler extends PageHandler {
    	private final String[] sEncodingValues = { "UTF-8", "UTF-16", "ISO-8859-1", "US-ASCII" };
    	
    	@Override protected void setControls(DialogAccess dlg) {
    		checkBoxFromConfig(dlg, "NoDoctype", "no_doctype");
    		listBoxFromConfig(dlg, "Encoding", "encoding", sEncodingValues, (short) 0);
    		checkBoxFromConfig(dlg, "AddBOM", "add_bom");
    		
    		if ("true".equals(config.getOption("hexadecimal_entities"))) {
    			dlg.setListBoxSelectedItem("HexadecimalEntities", (short) 0);
    		}
    		else {
    			dlg.setListBoxSelectedItem("HexadecimalEntities", (short) 1);	
    		}
    		

    		checkBoxFromConfig(dlg, "UseNamedEntities", "use_named_entities");
    		checkBoxFromConfig(dlg, "PrettyPrint", "pretty_print");
    		
    		encodingChange(dlg);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
    		checkBoxToConfig(dlg, "NoDoctype", "no_doctype");
    		listBoxToConfig(dlg, "Encoding", "encoding", sEncodingValues);
    		checkBoxToConfig(dlg, "AddBOM", "add_bom");
    		
    		config.setOption("hexadecimal_entities", Boolean.toString(dlg.getListBoxSelectedItem("HexadecimalEntities")==(short)0));
    		
    		checkBoxToConfig(dlg, "UseNamedEntities", "use_named_entities");
    		checkBoxToConfig(dlg, "PrettyPrint", "pretty_print");    		
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (sMethod.equals("EncodingChange")) {
    			encodingChange(dlg);
    			return true;
    		}
    		return false;
    	}

    	private void encodingChange(DialogAccess dlg) {
    		int nEncoding = dlg.getListBoxSelectedItem("Encoding");
    		dlg.setControlEnabled("AddBOM", nEncoding==0); // Only for UTF-8
    		dlg.setControlEnabled("HexadecimalEntitiesLabel", nEncoding>1); // Not for UNICODE
    		dlg.setControlEnabled("HexadecimalEntities", nEncoding>1); // Not for UNICODE
    	}
    	
    }
    
    private class TemplateHandler extends CustomFileHandler {
    	
    	protected String getSuffix() {
    		return "Template";
    	}
    	
    	protected String getFileName() {
    		return "writer2xhtml-template.xhtml";
    	}
    	
    	protected void useCustomInner(DialogAccess dlg, boolean bEnable) {
    		dlg.setControlEnabled("TestTemplateLabel", bEnable);
    		dlg.setControlEnabled("ContentIdLabel", bEnable);
    		dlg.setControlEnabled("ContentId", bEnable);
    		dlg.setControlEnabled("HeaderIdLabel", bEnable);
    		dlg.setControlEnabled("HeaderId", bEnable);
    		dlg.setControlEnabled("FooterIdLabel", bEnable);
    		dlg.setControlEnabled("FooterId", bEnable);
    		dlg.setControlEnabled("PanelIdLabel", bEnable);
    		dlg.setControlEnabled("PanelId", bEnable);
    	}

    	@Override protected void setControls(DialogAccess dlg) {
    		super.setControls(dlg);
    		String[] sCustomIds = config.getOption("template_ids").split(",");
    		if (sCustomIds.length>0) { dlg.setComboBoxText("ContentId", sCustomIds[0]); }
    		if (sCustomIds.length>1) { dlg.setComboBoxText("HeaderId", sCustomIds[1]); }
    		if (sCustomIds.length>2) { dlg.setComboBoxText("FooterId", sCustomIds[2]); }
    		if (sCustomIds.length>3) { dlg.setComboBoxText("PanelId", sCustomIds[3]); }
    		testTemplate(dlg);
    	}

    	@Override protected void getControls(DialogAccess dlg) {
    		super.getControls(dlg);
    		config.setOption("template_ids",
    				dlg.getComboBoxText("ContentId").trim()+","+
    				dlg.getComboBoxText("HeaderId").trim()+","+
    				dlg.getComboBoxText("FooterId").trim()+","+
    				dlg.getComboBoxText("PanelId").trim());
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (super.handleEvent(dlg, sMethod)) {
    			return true;
    		}
    		if (sMethod.equals("TemplateKeyup")) {
    			testTemplate(dlg);
    			return true;
    		}
    		return false;
    	}
    	
		@Override protected void loadCustomClick(DialogAccess dlg) {
			super.loadCustomClick(dlg);
			testTemplate(dlg);
		}
    	
    	private void testTemplate(DialogAccess dlg) {
    		Converter converter = ConverterFactory.createConverter("text/html");
    		String sTemplate = dlg.getTextFieldText("CustomTemplate").trim();
    		if (sTemplate.length()>0) { // Only display error message if there is content
	    		try {
					converter.readTemplate(new ByteArrayInputStream(sTemplate.getBytes()));
		    		dlg.setLabelText("TestTemplateLabel", "");
				} catch (IOException e) {
		    		dlg.setLabelText("TestTemplateLabel", "ERROR: "+e.getMessage());
				}
    		}
    		else {
	    		dlg.setLabelText("TestTemplateLabel", "");    			
    		}
    	}

    }

    private class StylesheetsHandler extends CustomFileHandler {
    	
    	protected String getSuffix() {
    		return "Stylesheet";
    	}
    	
    	protected String getFileName() {
    		return "writer2xhtml-styles.css";
    	}
    	
    	protected void useCustomInner(DialogAccess dlg, boolean bEnable) {
    		dlg.setControlEnabled("ResourceLabel", bEnable);
    		dlg.setControlEnabled("Resources", bEnable);
    		dlg.setControlEnabled("NewResourceButton", bEnable);
    		dlg.setControlEnabled("DeleteResourceButton", bEnable);
    		updateResources(dlg);
    	}

    	
    	@Override protected void setControls(DialogAccess dlg) {
    		super.setControls(dlg);
    		dlg.setCheckBoxStateAsBoolean("LinkCustomStylesheet", config.getOption("custom_stylesheet").length()>0);
    		textFieldFromConfig(dlg, "CustomStylesheetURL", "custom_stylesheet");
    		
    		linkCustomStylesheetChange(dlg);
    		
    		updateResources(dlg);
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
    		super.getControls(dlg);
    		if (dlg.getCheckBoxStateAsBoolean("LinkCustomStylesheet")) {
        		textFieldToConfig(dlg, "CustomStylesheetURL", "custom_stylesheet");    			
    		}
    		else {
    			config.setOption("custom_stylesheet", "");
    		}
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		if (super.handleEvent(dlg, sMethod)) {
    			return true;
    		}
    		if (sMethod.equals("LinkCustomStylesheetChange")) {
    			linkCustomStylesheetChange(dlg);
    			return true;
    		}
    		else if (sMethod.equals("NewResourceClick")) {
    			newResourceClick(dlg);
    			return true;
    		}
    		else if (sMethod.equals("DeleteResourceClick")) {
    			deleteResourceClick(dlg);
    			return true;
    		}
    		return false;
    	}
    	
		private void linkCustomStylesheetChange(DialogAccess dlg) {
    		boolean bLinkCustomStylesheet = dlg.getCheckBoxStateAsBoolean("LinkCustomStylesheet");
    		dlg.setControlEnabled("CustomStylesheetURLLabel", bLinkCustomStylesheet);
    		dlg.setControlEnabled("CustomStylesheetURL", bLinkCustomStylesheet);
    	}
    	
    	private void newResourceClick(DialogAccess dlg) {
    		String[] sFileNames=filePicker.getPaths();
    		if (sFileNames!=null) {
    			createResourceDir();
    			for (String sFileName : sFileNames) {
    				String sBaseFileName = sFileName.substring(sFileName.lastIndexOf('/')+1);
    				try {
    					String sTargetFileName = sResourceDirName+"/"+sBaseFileName;
    					if (fileExists(sTargetFileName)) { killFile(sTargetFileName); }
    					sfa2.copy(sFileName, sTargetFileName);
    				} catch (CommandAbortedException e) {
    					e.printStackTrace();
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
    			}
    			updateResources(dlg);
    		}
    	}

    	private void deleteResourceClick(DialogAccess dlg) {
    		int nItem = dlg.getListBoxSelectedItem("Resources");
    		if (nItem>=0) {
    			String sFileName = dlg.getListBoxStringItemList("Resources")[nItem];
    			if (deleteItem(sFileName)) {
    				killFile(sResourceDirName+"/"+sFileName);
    				updateResources(dlg);
    			}
    		}
    	}
    	
    	private void updateResources(DialogAccess dlg) {
    		createResourceDir();
    		try {
    			String[] sFiles = sfa2.getFolderContents(sResourceDirName, false); // do not include folders
    			int nCount = sFiles.length;
    			for (int i=0; i<nCount; i++) {
    				sFiles[i] = sFiles[i].substring(sFiles[i].lastIndexOf('/')+1);
    			}
				dlg.setListBoxStringItemList("Resources", sFiles);
			} catch (CommandAbortedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}

    	private void createResourceDir() {
    		try {
				if (!sfa2.isFolder(sResourceDirName)) {
					sfa2.createFolder(sResourceDirName);
				}
			} catch (CommandAbortedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	
    }
    
    private class Styles1Handler extends StylesPageHandler {
    	private final String[] sXhtmlFamilyNames = { "text", "paragraph", "heading", "list", "frame" };
    	private final String[] sXhtmlOOoFamilyNames = { "CharacterStyles", "ParagraphStyles", "ParagraphStyles", "NumberingStyles", "FrameStyles" };
    	
    	private final String[] sParElements = { "p", "h1", "h2", "h3", "h4", "h5", "h6", "address", "dd", "dt", "pre" };
    	private final String[] sParBlockElements = { "div", "blockquote", "dl" };
    	private final String[] sEmpty = { };
    	
    	private String[][] sElements = new String[5][];
    	private String[][] sBlockElements = new String[5][];
    	
    	protected Styles1Handler() {
    		super(5);
    		sFamilyNames = sXhtmlFamilyNames;
    		sOOoFamilyNames = sXhtmlOOoFamilyNames;
 
    		sElements[0] = sCharElements;
    		sElements[1] = sParElements;
    		sElements[2] = sParElements;
    		sElements[3] = sEmpty;
    		sElements[4] = sEmpty;
    		
    		sBlockElements[0] = sEmpty;
    		sBlockElements[1] = sParBlockElements;
    		sBlockElements[2] = sParBlockElements;
    		sBlockElements[3] = sEmpty;
    		sBlockElements[4] = sEmpty;
    	}
    	
    	protected String getDefaultConfigName() {
    		return "cleanxhtml.xml";
    	}
		
		protected void setControls(DialogAccess dlg, Map<String,String> attr) {
			if (!attr.containsKey("element")) { attr.put("element", ""); }
			if (!attr.containsKey("css")) { attr.put("css", ""); }
			dlg.setComboBoxText("Element", attr.get("element"));
			dlg.setTextFieldText("Css", none2empty(attr.get("css")));
			
			if (nCurrentFamily==1 || nCurrentFamily==2) {
				if (!attr.containsKey("before")) { attr.put("before", ""); }
				if (!attr.containsKey("after")) { attr.put("after", ""); }
				dlg.setTextFieldText("Before", attr.get("before"));
				dlg.setTextFieldText("After", attr.get("after"));
			}
			else {
				dlg.setTextFieldText("Before", "");
				dlg.setTextFieldText("After", "");				
			}
			
			if (nCurrentFamily==1 || nCurrentFamily==2) {
				if (!attr.containsKey("block-element")) { attr.put("block-element", ""); }
				if (!attr.containsKey("block-css")) { attr.put("block-css", ""); }
				dlg.setComboBoxText("BlockElement", attr.get("block-element"));
				dlg.setTextFieldText("BlockCss", none2empty(attr.get("block-css")));
			}
			else {
				dlg.setComboBoxText("BlockElement", "");
				dlg.setTextFieldText("BlockCss", "");								
			}
		}
		
		protected void getControls(DialogAccess dlg, Map<String,String> attr) {
			attr.put("element", dlg.getComboBoxText("Element"));
			attr.put("css", empty2none(dlg.getTextFieldText("Css")));
			if (nCurrentFamily==1 || nCurrentFamily==2) {
				attr.put("before", dlg.getTextFieldText("Before"));
				attr.put("after", dlg.getTextFieldText("After"));
			}
			if (nCurrentFamily==1 || nCurrentFamily==2) {
				attr.put("block-element", dlg.getComboBoxText("BlockElement"));
				attr.put("block-css", empty2none(dlg.getTextFieldText("BlockCss")));
			}
		}
		
		protected void clearControls(DialogAccess dlg) {
			dlg.setComboBoxText("Element", "");
			dlg.setTextFieldText("Css", "");
			dlg.setTextFieldText("Before", "");
			dlg.setTextFieldText("After", "");
			dlg.setComboBoxText("BlockElement", "");
			dlg.setTextFieldText("BlockCss", "");
		}
		
		protected void prepareControls(DialogAccess dlg, boolean bHasMappings) {
			dlg.setListBoxStringItemList("Element", sElements[nCurrentFamily]);
			dlg.setListBoxStringItemList("BlockElement", sBlockElements[nCurrentFamily]);
			dlg.setControlEnabled("ElementLabel", bHasMappings && nCurrentFamily<=2);			
			dlg.setControlEnabled("Element", bHasMappings && nCurrentFamily<=2);			
			dlg.setControlEnabled("CssLabel", bHasMappings);			
			dlg.setControlEnabled("Css", bHasMappings);
			dlg.setControlEnabled("BeforeLabel", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));
			dlg.setControlEnabled("Before", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));
			dlg.setControlEnabled("AfterLabel", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));
			dlg.setControlEnabled("After", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));
			dlg.setControlEnabled("BlockElementLabel", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));
			dlg.setControlEnabled("BlockElement", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));
			dlg.setControlEnabled("BlockCssLabel", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));		
			dlg.setControlEnabled("BlockCss", bHasMappings && (nCurrentFamily==1 || nCurrentFamily==2));		
		}
	}
    
    private class Styles2Handler extends AttributePageHandler {
    	private String[] sXhtmlAttributeNames = { "bold", "italics", "fixed", "superscript", "subscript", "underline", "overstrike" };
    	
    	public Styles2Handler() {
    		sAttributeNames = sXhtmlAttributeNames;
    	}
    	
    	@Override public void setControls(DialogAccess dlg) {
    		super.setControls(dlg);
    		textFieldFromConfig(dlg,"TabstopStyle","tabstop_style");
    	}
    	
    	@Override public void getControls(DialogAccess dlg) {
    		super.getControls(dlg);
    		textFieldToConfig(dlg,"TabstopStyle","tabstop_style");
    	}
    	
    	protected void setControls(DialogAccess dlg, Map<String,String> attr) {
    		if (!attr.containsKey("element")) { attr.put("element", ""); }
    		if (!attr.containsKey("css")) { attr.put("css", ""); }
    		dlg.setListBoxStringItemList("Element", sCharElements);
    		dlg.setComboBoxText("Element", attr.get("element"));
    		dlg.setTextFieldText("Css", none2empty(attr.get("css")));
    	}
    	
    	protected void getControls(DialogAccess dlg, Map<String,String> attr) {
    		attr.put("element", dlg.getComboBoxText("Element"));
    		attr.put("css", empty2none(dlg.getTextFieldText("Css")));
    	}
    	
    	protected void prepareControls(DialogAccess dlg, boolean bEnable) {
    		dlg.setControlEnabled("ElementLabel", bEnable);
    		dlg.setControlEnabled("Element", bEnable);
    		dlg.setControlEnabled("CssLabel", bEnable);
    		dlg.setControlEnabled("Css", bEnable);
    	}
    }

    private class FormattingHandler extends PageHandler {
    	private final String[] sExportValues = { "convert_all", "ignore_styles", "ignore_hard", "ignore_all" };
    	private final String[] sListExportValues = { "convert_all", "convert_labels", "convert_label_styles", "ignore_all" };
    	
    	@Override protected void setControls(DialogAccess dlg) {
    		listBoxFromConfig(dlg, "Formatting", "formatting", sExportValues, (short) 0);
    		listBoxFromConfig(dlg, "FrameFormatting", "frame_formatting", sExportValues, (short) 0);
    		listBoxFromConfig(dlg, "ListFormatting", "list_formatting", sListExportValues, (short) 0);
    		
    		// OOo does not support styles for sections and tables, hence this simplified variant
    		dlg.setCheckBoxStateAsBoolean("SectionFormatting",
    			config.getOption("section_formatting").equals("convert_all") ||
    			config.getOption("section_formatting").equals("ignore_styles"));
    		dlg.setCheckBoxStateAsBoolean("TableFormatting",
        		config.getOption("table_formatting").equals("convert_all") ||
        		config.getOption("table_formatting").equals("ignore_styles"));
    		
    		textFieldFromConfig(dlg, "MaxWidth", "max_width");

    		checkBoxFromConfig(dlg, "SeparateStylesheet", "separate_stylesheet");
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
    		listBoxToConfig(dlg, "Formatting", "formatting", sExportValues);
    		listBoxToConfig(dlg, "FrameFormatting", "frame_formatting", sExportValues);
    		listBoxToConfig(dlg, "ListFormatting", "list_formatting", sListExportValues);
    		
    		config.setOption("section_formatting", dlg.getCheckBoxStateAsBoolean("SectionFormatting") ? "convert_all" : "ignore_all");
    		config.setOption("table_formatting", dlg.getCheckBoxStateAsBoolean("TableFormatting") ? "convert_all" : "ignore_all");
    		
    		textFieldToConfig(dlg, "MaxWidth", "max_width");

    		checkBoxToConfig(dlg, "SeparateStylesheet", "separate_stylesheet");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		return false;
    	}
    	
    }

    private class ContentHandler extends PageHandler {
    	@Override protected void setControls(DialogAccess dlg) {
    		textFieldFromConfig(dlg, "EndnotesHeading", "endnotes_heading");
    		textFieldFromConfig(dlg, "FootnotesHeading", "footnotes_heading");
    	}
    	
    	@Override protected void getControls(DialogAccess dlg) {
    		textFieldToConfig(dlg, "EndnotesHeading", "endnotes_heading");
    		textFieldToConfig(dlg, "FootnotesHeading", "footnotes_heading");
    	}
    	
    	@Override protected boolean handleEvent(DialogAccess dlg, String sMethod) {
    		return false;
    	}
    	
    }
    
    private String none2empty(String s) {
    	return s.equals("(none)") ? "" : s;
    }
    
    private String empty2none(String s) {
    	String t = s.trim();
    	return t.length()==0 ? "(none)" : t;
    }
	

}
