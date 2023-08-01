/************************************************************************
 *
 *  XhtmlOptionsDialogCalc.java
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
 *  Version 1.7.1 (2023-07-31)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2xcommon.filter.OptionsDialogBase;
import org.openoffice.da.comp.w2xcommon.helper.PropertyHelper;

/** This class provides a uno component which implements a filter ui for the
 *  Xhtml export in Calc
 */
public class XhtmlOptionsDialogCalc extends OptionsDialogBase {
	
    // Translate list box items to configuration option values 
    private static final String[] FILENAMES_VALUES = { "name_number", "name_section", "section" };
    private static final String[] SIZE_VALUES = { "auto", "relative", "none" };
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writerxhtml.XhtmlOptionsDialogCalc";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogCalc";

    public String getDialogLibraryName() { return "W2XDialogs"; }

    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "XhtmlOptionsCalc"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2xhtml.Options/XhtmlOptionsCalc";
    }
	
    /** Create a new XhtmlOptionsDialogCalc */
    public XhtmlOptionsDialogCalc(XComponentContext xContext) {
        super(xContext);
        xMSF = W2XRegistration.xMultiServiceFactory;
    }
	
    /** Load settings from the registry to the dialog */
    protected void loadSettings(XPropertySet xProps) {
        // General
        loadConfig(xProps);
        loadListBoxOption(xProps, "Units");
        int nScaling = loadNumericOption(xProps, "Scaling");
        if (nScaling<=1) { // Workaround for an obscure bug in the extension manager
        	setNumericFieldValue("Scaling",100);
        }
        int nColumnScaling = loadNumericOption(xProps, "ColumnScaling");
        if (nColumnScaling<=1) {
        	setNumericFieldValue("ColumnScaling",100);
        }

        // Sheets
        loadCheckBoxOption(xProps, "DisplayHiddenSheets");
        loadCheckBoxOption(xProps, "DisplayHiddenRowsCols");
        loadCheckBoxOption(xProps, "DisplayFilteredRowsCols");
        loadCheckBoxOption(xProps, "ApplyPrintRanges");
        loadCheckBoxOption(xProps, "UseTitleAsHeading");
        loadCheckBoxOption(xProps, "UseSheetNamesAsHeadings");

        // Files
        loadCheckBoxOption(xProps, "CalcSplit");
        loadListBoxOption(xProps, "Filenames");
        loadCheckBoxOption(xProps, "SaveImagesInSubdir");
        
        // Special content
        loadCheckBoxOption(xProps, "Notes");
        loadCheckBoxOption(xProps, "UseDublinCore");

        // Figures
        loadListBoxOption(xProps, "ImageSize");
        loadCheckBoxOption(xProps, "EmbedSVG");
        loadCheckBoxOption(xProps, "EmbedImg");

        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    protected void saveSettings(XPropertySet xProps, PropertyHelper helper) {
        // General
        saveConfig(xProps, helper);
        short nUnits = saveListBoxOption(xProps, "Units");
        if (!isLocked("units")) {
	    	switch (nUnits) {
	    	case 0: helper.put("units", "original"); break;
	    	case 1: helper.put("units", "px"); break;
	    	case 2:
	    	default: helper.put("units", "rem");
	    	}
        }
        saveNumericOptionAsPercentage(xProps, helper, "Scaling", "scaling");
        saveNumericOptionAsPercentage(xProps, helper, "ColumnScaling", "column_scaling");

        // Sheets
        saveCheckBoxOption(xProps, helper, "DisplayHiddenSheets", "display_hidden_sheets");
        saveCheckBoxOption(xProps, helper, "DisplayHiddenRowsCols", "display_hidden_rows_cols");
        saveCheckBoxOption(xProps, helper, "DisplayFilteredRowsCols", "display_filtered_rows_cols");
        saveCheckBoxOption(xProps, helper, "ApplyPrintRanges", "apply_print_ranges");
        saveCheckBoxOption(xProps, helper, "UseTitleAsHeading", "use_title_as_heading"); 
        saveCheckBoxOption(xProps, helper, "UseSheetNamesAsHeadings", "use_sheet_names_as_headings");

        // Files
        saveCheckBoxOption(xProps, helper, "CalcSplit", "calc_split");
        saveListBoxOption(xProps, helper, "Filenames", "filenames", FILENAMES_VALUES);
        saveCheckBoxOption(xProps, helper, "SaveImagesInSubdir", "save_images_in_subdir");

        // Special content
        saveCheckBoxOption(xProps, helper, "Notes", "notes");
        saveCheckBoxOption(xProps, helper, "UseDublinCore", "use_dublin_core");

        // Figures
        saveListBoxOption(xProps, helper, "ImageSize", "image_size", SIZE_VALUES);
        saveCheckBoxOption(xProps, helper, "EmbedSVG","embed_svg");
        saveCheckBoxOption(xProps, helper, "EmbedImg","embed_img");
        
        // TODO: Always use auto, perhaps this option should be disabled for calc2xhtml in the core? 
        helper.put("table_size", "auto");

    }
	
    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange")) {
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("CalcSplitChange")) {
        	enableSplitSettings();
        }
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "CalcSplitChange" };
        return sNames;
    }
	
    private void enableControls() {
        // General
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Units",!isLocked("units"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("ColumnScaling",!isLocked("column_scaling"));			

        // Sheets
        setControlEnabled("DisplayHiddenSheets", !isLocked("display_hidden_sheets"));
        setControlEnabled("DisplayHiddenRowsCols", !isLocked("display_hidden_rows_cols"));
        setControlEnabled("DisplayFilteredRowsCols", !isLocked("display_filtered_rows_cols"));
        setControlEnabled("ApplyPrintRanges", !isLocked("apply_print_ranges"));
        setControlEnabled("UseTitleAsHeading", !isLocked("use_title_as_heading")); 
        setControlEnabled("UseSheetNamesAsHeadings", !isLocked("use_sheet_names_as_headings"));

        // Files
        setControlEnabled("CalcSplit",!isLocked("calc_split"));
        enableSplitSettings();
        setControlEnabled("SaveImagesInSubdir",!isLocked("save_images_in_subdir"));

        // Special content
        setControlEnabled("Notes",!isLocked("notes"));
        setControlEnabled("UseDublinCore",!isLocked("use_dublin_core"));
        
        // Figures and tables
        setControlEnabled("ImageSize",!isLocked("image_size") && !isLocked("original_image_size"));
        setControlEnabled("EmbedSVG",this instanceof HTML5OptionsDialogCalc && !isLocked("embed_svg"));
        setControlEnabled("EmbedImg",!isLocked("embed_img"));
    }
    
    private void enableSplitSettings() {
        setControlEnabled("FilenamesLabel",getCheckBoxStateAsBoolean("CalcSplit") && !isLocked("save_images_in_subdir"));
        setControlEnabled("Filenames",getCheckBoxStateAsBoolean("CalcSplit") && !isLocked("save_images_in_subdir"));
    }
		
}



