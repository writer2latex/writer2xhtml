/************************************************************************
 *
 *  EpubOptionsDialog.java
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

import java.awt.GraphicsEnvironment;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2xcommon.filter.OptionsDialogBase;
import org.openoffice.da.comp.w2xcommon.helper.PropertyHelper;

/** This class provides a UNO component which implements a filter UI for the
 *  EPUB export
 */
public class EpubOptionsDialog extends OptionsDialogBase {
   
	// Translate list box items (image and table settings) to configuration option values 
    private static final String[] SIZE_VALUES = { "auto", "relative", "none" };
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.EpubOptionsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.EpubOptionsDialog";
	
    @Override public String getDialogLibraryName() { return "W2XDialogs2"; }
	
    /** Return the name of the dialog within the library
     */
    @Override public String getDialogName() { return "EpubOptions"; }

    /** Return the name of the registry path
     */
    @Override public String getRegistryPath() {
        return "/org.openoffice.da.Writer2xhtml.Options/EpubOptions";
    }
	
    /** Create a new EpubOptionsDialog */
    public EpubOptionsDialog(XComponentContext xContext) {
        super(xContext);
        xMSF = W2XRegistration.xMultiServiceFactory;
    }

    /** Load settings from the registry to the dialog */
    @Override protected void loadSettings(XPropertySet xProps) {
        // Style
        loadConfig(xProps);
        int nScaling = loadNumericOption(xProps, "Scaling");
        if (nScaling<=1) { // Workaround for an obscure bug in the extension manager
        	setNumericFieldValue("Scaling",100);
        }
        loadListBoxOption(xProps, "TableSize");
        int nColumnScaling = loadNumericOption(xProps, "ColumnScaling");
        if (nColumnScaling<=1) {
        	setNumericFieldValue("ColumnScaling",100);
        }
        loadCheckBoxOption(xProps, "RelativeFontSize");
        loadNumericOption(xProps, "FontScaling");
        int nFontScaling = loadNumericOption(xProps, "FontScaling");
        if (nFontScaling<=1) {
        	setNumericFieldValue("FontScaling",100);
        }
        loadCheckBoxOption(xProps, "RelativeFontSize");
        loadCheckBoxOption(xProps, "UseDefaultFont");
        loadComboBoxOption(xProps, "DefaultFontName");
        loadListBoxOption(xProps, "Units");
        loadListBoxOption(xProps, "ImageSize");

        // Fill the font name list with all installed fonts
        setListBoxStringItemList("DefaultFontName", 
        		GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        
        // AutoCorrect
        loadCheckBoxOption(xProps, "IgnoreHardLineBreaks");
        loadCheckBoxOption(xProps, "IgnoreEmptyParagraphs");
        loadCheckBoxOption(xProps, "IgnoreDoubleSpaces");
        
        // Compatibility
        loadCheckBoxOption(xProps, "IncludeNCX");
        loadCheckBoxOption(xProps, "AvoidHtml5");

        // Special content
        loadCheckBoxOption(xProps, "DisplayHiddenText");
        loadCheckBoxOption(xProps, "Notes");
			
        // Document division
        loadListBoxOption(xProps, "SplitLevel");
        loadListBoxOption(xProps, "PageBreakSplit");
        loadCheckBoxOption(xProps, "UseImageSplit");
        loadNumericOption(xProps, "ImageSplit");
        loadCheckBoxOption(xProps, "CoverImage");
        loadCheckBoxOption(xProps, "UseSplitAfter");
        loadNumericOption(xProps, "SplitAfter");
        
        // Navigation table
        loadListBoxOption(xProps, "ExternalTocDepth");
        loadListBoxOption(xProps, "ExternalTocDepthMarks");
        loadCheckBoxOption(xProps, "IndexLinks");
        loadCheckBoxOption(xProps, "IncludeToc");
        loadCheckBoxOption(xProps, "OriginalPageNumbers");

        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    @Override protected void saveSettings(XPropertySet xProps, PropertyHelper helper) {
        saveNumericOptionAsPercentage(xProps, helper, "Scaling", "scaling");
        saveListBoxOption(xProps, helper, "TableSize", "table_size", SIZE_VALUES);
        saveNumericOptionAsPercentage(xProps, helper, "ColumnScaling", "column_scaling");
        saveCheckBoxOption(xProps, helper, "RelativeFontSize", "relative_font_size");
        saveNumericOptionAsPercentage(xProps, helper, "FontScaling", "font_scaling");
        saveCheckBoxOption(xProps, helper, "UseDefaultFont", "use_default_font");
        saveTextFieldOption(xProps, helper, "DefaultFontName", "default_font_name");
        saveListBoxOption(xProps, "Units");
        short nUnits = saveListBoxOption(xProps, "Units");
        if (!isLocked("units")) {
	    	switch (nUnits) {
	    	case 0: helper.put("units", "original"); break;
	    	case 1: helper.put("units", "px"); break;
	    	case 2:
	    	default: helper.put("units", "rem");
	    	}
        }
        saveListBoxOption(xProps, helper, "ImageSize", "image_size", SIZE_VALUES);

        // AutoCorrect
        saveCheckBoxOption(xProps, helper, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, helper, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, helper, "IgnoreDoubleSpaces", "ignore_double_spaces");

        // Compatibility
        saveCheckBoxOption(xProps, helper, "IncludeNCX", "include_ncx");
        saveCheckBoxOption(xProps, helper, "AvoidHtml5", "avoid_html5");
        
        // Special content
        saveCheckBoxOption(xProps, helper, "DisplayHiddenText", "display_hidden_text");
        saveCheckBoxOption(xProps, helper, "Notes", "notes");
  		
        // Document division
        short nSplitLevel = saveListBoxOption(xProps, "SplitLevel");
        if (!isLocked("split_level")) {
        	helper.put("split_level",Integer.toString(nSplitLevel));
        }
        
        short nPageBreakSplit = saveListBoxOption(xProps, "PageBreakSplit");
        if (!isLocked("page_break_split")) {
        	switch (nPageBreakSplit) {
        	case 0: helper.put("page_break_split","none"); break;
        	case 1: helper.put("page_break_split", "styles"); break;
        	case 2: helper.put("page_break_split", "explicit"); break;
        	case 3: helper.put("page_break_split", "all");
        	}
        }
        
        boolean bUseImageSplit = saveCheckBoxOption(xProps, "UseImageSplit");
        int nImageSplit = saveNumericOption(xProps, "ImageSplit");
        if (!isLocked("image_split")) {
        	if (bUseImageSplit) {
        		helper.put("image_split", nImageSplit+"%");
        	}
        	else {
        		helper.put("image_split", "none");
        	}
        }
        
        saveCheckBoxOption(xProps, helper, "CoverImage", "cover_image");

        boolean bUseSplitAfter = saveCheckBoxOption(xProps, "UseSplitAfter");
        int nSplitAfter = saveNumericOption(xProps, "SplitAfter");
        if (!isLocked("split_after")) {
        	if (bUseSplitAfter) {
        		helper.put("split_after", Integer.toString(nSplitAfter));
        	}
        	else {
        		helper.put("split_after", "0");
        	}
        }
        
        // Navigation table
        short nExternalTocDepth = saveListBoxOption(xProps, "ExternalTocDepth");
        helper.put("external_toc_depth", Integer.toString(nExternalTocDepth));
        short nExternalTocDepthMarks = saveListBoxOption(xProps, "ExternalTocDepthMarks");
        helper.put("external_toc_depth_marks", Integer.toString(nExternalTocDepthMarks));
        saveCheckBoxOption(xProps, helper, "IndexLinks", "index_links");
        saveCheckBoxOption(xProps, helper, "IncludeToc", "include_toc");
        saveCheckBoxOption(xProps, helper, "OriginalPageNumbers", "original_page_numbers");
    }
	
	
    // Implement XDialogEventHandler
    @Override public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange") || sMethod.equals("TableSizeChange")) {
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("RelativeFontSizeChange")) {
        	relativeFontSizeChange();
        }
        else if (sMethod.equals("UseDefaultFontChange")) {
        	useDefaultFontChange();
        }
        else if (sMethod.equals("EditMetadataClick")) {
            editMetadataClick();
        }
        else if (sMethod.equals("UseImageSplitChange")) {
            useImageSplitChange();
        }
        else if (sMethod.equals("UseSplitAfterChange")) {
        	useSplitAfterChange();
        }
        return true;
    }

    @Override public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "TableSizeChange", "RelativeFontSizeChange", "UseDefaultFontChange", "EditMetadataClick",
        		"UseImageSplitChange", "UseSplitAfterChange" };
        return sNames;
    }
	
    private void enableControls() {
        // Style
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("TableSize",!isLocked("table_size"));
        short nTableSize = this.getListBoxSelectedItem("TableSize");
        setControlEnabled("ColumnScalingLabel",!isLocked("column_scaling") && nTableSize==0);
        setControlEnabled("ColumnScaling",!isLocked("column_scaling") && nTableSize==0);
        setControlEnabled("ColumnScalingPercentLabel",!isLocked("column_scaling") && nTableSize==0);
        
        boolean bRelativeFontSize = getCheckBoxStateAsBoolean("RelativeFontSize");
        setControlEnabled("RelativeFontSize",!isLocked("relative_font_size"));
		setControlEnabled("FontScalingLabel", !isLocked("font_scaling") && bRelativeFontSize);    		
        setControlEnabled("FontScaling",!isLocked("font_scaling") && bRelativeFontSize);
		setControlEnabled("FontScalingPercentLabel", !isLocked("font_scaling") && bRelativeFontSize);
		
		boolean bUseDefaultFont = getCheckBoxStateAsBoolean("UseDefaultFont");
		setControlEnabled("UseDefaultFont",!isLocked("use_default_font"));
		setControlEnabled("DefaultFontNameLabel",!isLocked("default_font_name") && bUseDefaultFont);
		setControlEnabled("DefaultFontName",!isLocked("default_font_name") && bUseDefaultFont);
        
		setControlEnabled("Units",!isLocked("units"));
        setControlEnabled("ImageSize",!isLocked("image_size"));

        // AutoCorrect
        setControlEnabled("IgnoreHardLineBreaks",!isLocked("ignore_hard_line_breaks"));
        setControlEnabled("IgnoreEmptyParagraphs",!isLocked("ignore_empty_paragraphs"));
        setControlEnabled("IgnoreDoubleSpaces",!isLocked("ignore_double_spaces"));

        // Compatibility
        setControlEnabled("CompatibilityLabel", this instanceof Epub3OptionsDialog);
        setControlEnabled("IncludeNCX", (this instanceof Epub3OptionsDialog) && !isLocked("include_ncx"));
        setControlEnabled("AvoidHtml5", (this instanceof Epub3OptionsDialog) && !isLocked("avoid_html5"));

        // Special content
        setControlEnabled("DisplayHiddenText",!isLocked("display_hidden_text"));
        setControlEnabled("Notes",!isLocked("notes"));
			
        // Document division
        setControlEnabled("SplitLevelLabel",!isLocked("split_level"));
        setControlEnabled("SplitLevel",!isLocked("split_level"));
        
        setControlEnabled("PageBreakSplitLabel",!isLocked("page_break_split"));
        setControlEnabled("PageBreakSplit",!isLocked("page_break_split"));
        
        boolean bUseImageSplit = getCheckBoxStateAsBoolean("UseImageSplit");
        setControlEnabled("UseImageSplit",!isLocked("image_split"));
        setControlEnabled("ImageSplitLabel",!isLocked("image_split") && bUseImageSplit);
        setControlEnabled("ImageSplit",!isLocked("image_split") && bUseImageSplit);
        setControlEnabled("ImageSplitPercentLabel",!isLocked("image_split") && bUseImageSplit);
        
        setControlEnabled("CoverImage", !isLocked("cover_image"));
        
        boolean bUseSplitAfter = getCheckBoxStateAsBoolean("UseSplitAfter");
        setControlEnabled("UseSplitAfter",!isLocked("split_after"));
        setControlEnabled("SplitAfterLabel",!isLocked("split_after") && bUseSplitAfter);
        setControlEnabled("SplitAfter",!isLocked("split_after") && bUseSplitAfter);
        
        // Navigation table
        setControlEnabled("ExternalTocDepthLabel", !isLocked("external_toc_depth"));
        setControlEnabled("ExternalTocDepth", !isLocked("external_toc_depth"));
        setControlEnabled("ExternalTocDepthMarksLabel", !isLocked("external_toc_depth_marks"));
        setControlEnabled("ExternalTocDepthMarks", !isLocked("external_toc_depth_marks"));
        setControlEnabled("IndexLinks", !isLocked("index_links"));
        setControlEnabled("IncludeToc", !isLocked("include_toc"));
        setControlEnabled("OriginalPageNumbers", !isLocked("original_page_numbers"));
    }
	
    private void relativeFontSizeChange() {
    	if (!isLocked("font_scaling")) {
    		boolean bState = getCheckBoxStateAsBoolean("RelativeFontSize");
    		setControlEnabled("FontScalingLabel", bState);
    		setControlEnabled("FontScaling", bState);
    		setControlEnabled("FontScalingPercentLabel", bState);    		
    	}
    }
    
    private void useDefaultFontChange() {
    	if (!isLocked("default_font_name")) {
    		boolean bState = getCheckBoxStateAsBoolean("UseDefaultFont");
    		setControlEnabled("DefaultFontNameLabel", bState);
    		setControlEnabled("DefaultFontName", bState);
    	}    	
    }
    
    private void editMetadataClick() {
        Object dialog;
		try {
			dialog = xContext.getServiceManager().createInstanceWithContext("org.openoffice.da.writer2xhtml.EpubMetadataDialog", xContext);
	        XExecutableDialog xDialog = (XExecutableDialog) UnoRuntime.queryInterface(XExecutableDialog.class, dialog);
	        xDialog.execute();
	        // Dispose the dialog after execution (to free up the memory)
	        XComponent xComponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, dialog);
	        if (xComponent!=null) {
	        	xComponent.dispose();
	        }
		} catch (Exception e) {
			// Failed to get dialog
		}
    }
    
    private void useImageSplitChange() {
        if (!isLocked("image_split")) {
            boolean bEnable = getCheckBoxStateAsBoolean("UseImageSplit");
            setControlEnabled("ImageSplitLabel",bEnable);
            setControlEnabled("ImageSplit",bEnable);
            setControlEnabled("ImageSplitPercentLabel",bEnable);
        }
    }
    
    private void useSplitAfterChange() {
        if (!isLocked("split_after")) {
            boolean bState = getCheckBoxStateAsBoolean("UseSplitAfter");
            setControlEnabled("SplitAfterLabel",bState);
            setControlEnabled("SplitAfter",bState);
        }
    }

}
