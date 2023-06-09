/************************************************************************
 *
 *  XhtmlOptionsDialogBase.java
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
 *  Version 1.7 (2022-06-22)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2xcommon.filter.OptionsDialogBase;
import org.openoffice.da.comp.w2xcommon.helper.PropertyHelper;

/** This class provides a uno component which implements a filter ui for the
 *  Xhtml export
 */
public class XhtmlOptionsDialogBase extends OptionsDialogBase {
	
    // Translate list box items (image and table settings) to configuration option values 
    private static final String[] SIZE_VALUES = { "auto", "relative", "none" };
    
    public String getDialogLibraryName() { return "W2XDialogs"; }
	
    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "XhtmlOptions"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2xhtml.Options/XhtmlOptions";
    }
	
    /** Create a new XhtmlOptionsDialogBase */
    public XhtmlOptionsDialogBase(XComponentContext xContext) {
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
        loadCheckBoxOption(xProps, "Multilingual");

        // Files
        loadCheckBoxOption(xProps, "Split");
        loadListBoxOption(xProps, "SplitLevel");
        loadListBoxOption(xProps, "RepeatLevels");
        loadCheckBoxOption(xProps, "SaveImagesInSubdir");
        
        // Special content
        loadCheckBoxOption(xProps, "Notes");
        loadCheckBoxOption(xProps, "UseDublinCore");
			
        // Figures, tables and formulas
        loadListBoxOption(xProps, "ImageSize");
        loadCheckBoxOption(xProps, "EmbedSVG");
        loadCheckBoxOption(xProps, "EmbedImg");
        loadListBoxOption(xProps, "TableSize");
        int nColumnScaling = loadNumericOption(xProps, "ColumnScaling");
        if (nColumnScaling<=1) {
        	setNumericFieldValue("ColumnScaling",100);
        }
        loadCheckBoxOption(xProps, "UseMathjax");
        loadListBoxOption(xProps, "Formulas");
		
        // AutoCorrect
        loadCheckBoxOption(xProps, "IgnoreHardLineBreaks");
        loadCheckBoxOption(xProps, "IgnoreEmptyParagraphs");
        loadCheckBoxOption(xProps, "IgnoreDoubleSpaces");
       
        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create filterdata */
    protected void saveSettings(XPropertySet xProps, PropertyHelper helper) {
        // Style
        short nConfig = saveConfig(xProps, helper);
        String[] sCoreStyles = { "Chocolate", "Midnight", "Modernist", "Oldstyle", "Steely", "Swiss", "Traditional", "Ultramarine" };
        switch (nConfig) {
            case 0: helper.put("ConfigURL","*default.xml"); break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: helper.put("ConfigURL","*cleanxhtml.xml");
                    helper.put("custom_stylesheet", "http://www.w3.org/StyleSheets/Core/"+sCoreStyles[nConfig-1]);
                    break;
            case 9: helper.put("ConfigURL","$(user)/writer2xhtml.xml");
            		helper.put("AutoCreate","true");
            		helper.put("TemplateURL", "$(user)/writer2xhtml-template.xhtml");
            		helper.put("StyleSheetURL", "$(user)/writer2xhtml-styles.css");
        }
        
        // General
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
        saveCheckBoxOption(xProps, helper, "Multilingual", "multilingual");
        
        // Files
        boolean bSplit = saveCheckBoxOption(xProps, "Split");
        short nSplitLevel = saveListBoxOption(xProps, "SplitLevel");
        short nRepeatLevels = saveListBoxOption(xProps, "RepeatLevels");
        if (!isLocked("split_level")) {
            if (bSplit) {
               helper.put("split_level",Integer.toString(nSplitLevel+1));
               helper.put("repeat_levels",Integer.toString(nRepeatLevels));
            }
            else {
                helper.put("split_level","0");
            }
        }    		
        saveCheckBoxOption(xProps, helper, "SaveImagesInSubdir", "save_images_in_subdir");

        // Special content
        saveCheckBoxOption(xProps, helper, "Notes", "notes");
        saveCheckBoxOption(xProps, helper, "UseDublinCore", "use_dublin_core");
  		
        // Figures, tables and formulas
        saveListBoxOption(xProps, helper, "ImageSize", "image_size", SIZE_VALUES);
        saveCheckBoxOption(xProps, helper, "EmbedSVG","embed_svg");
        saveCheckBoxOption(xProps, helper, "EmbedImg","embed_img");
        saveListBoxOption(xProps, helper, "TableSize", "table_size", SIZE_VALUES);
        saveNumericOptionAsPercentage(xProps, helper, "ColumnScaling", "column_scaling");
        saveCheckBoxOption(xProps, helper, "UseMathjax", "use_mathjax");
        short nFormulas = saveListBoxOption(xProps, "Formulas");
        if (!isLocked("formulas")) {
	    	switch (nFormulas) {
	    	case 0: helper.put("formulas", "image+starmath"); break;
	    	case 1:
	    	default: helper.put("formulas", "starmath");
	    	}
        }

        // AutoCorrect
        saveCheckBoxOption(xProps, helper, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, helper, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, helper, "IgnoreDoubleSpaces", "ignore_double_spaces");
    }
	
	
    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange") || sMethod.equals("TableSizeChange")) {
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("SplitChange")) {
            enableSplitLevel();
        }
        return true;
    }

    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "SplitChange", "TableSizeChange" };
        return sNames;
    }
	
    private void enableControls() {
    	// General
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Units",!isLocked("units"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("Multilingual",!isLocked("multilingual"));

        // Files
        boolean bSplit = getCheckBoxStateAsBoolean("Split");
        setControlEnabled("Split",!isLocked("split_level"));
        setControlEnabled("SplitLevelLabel",!isLocked("split_level") && bSplit);
        setControlEnabled("SplitLevel",!isLocked("split_level") && bSplit);
        setControlEnabled("RepeatLevelsLabel",!isLocked("repeat_levels") && !isLocked("split_level") && bSplit);
        setControlEnabled("RepeatLevels",!isLocked("repeat_levels") && !isLocked("split_level") && bSplit);
        setControlEnabled("SaveImagesInSubdir",!isLocked("save_images_in_subdir"));
        
        // Special content
        setControlEnabled("Notes",!isLocked("notes"));
        setControlEnabled("UseDublinCore",!isLocked("use_dublin_core"));

        // Figures, tables and formulas
        setControlEnabled("ImageSize",!isLocked("image_size") && !isLocked("original_image_size"));
        setControlEnabled("EmbedSVG",this instanceof HTML5OptionsDialog && !isLocked("embed_svg"));
        setControlEnabled("EmbedImg",!isLocked("embed_img"));
        setControlEnabled("TableSize",!isLocked("table_size"));
        short nTableSize = this.getListBoxSelectedItem("TableSize");
        setControlEnabled("ColumnScalingLabel",!isLocked("column_scaling") && nTableSize==0);
        setControlEnabled("ColumnScaling",!isLocked("column_scaling") && nTableSize==0);
        setControlEnabled("ColumnScalingPercentLabel",!isLocked("column_scaling") && nTableSize==0);
        if (this instanceof HTML5OptionsDialog || this instanceof XhtmlOptionsDialogMath) {
        	setControlVisible("FormulasLabel",false);
        	setControlVisible("Formulas",false);
            setControlEnabled("UseMathjax",!isLocked("use_mathjax"));        	
        }
        else {
        	setControlVisible("Formulas",!isLocked("formulas"));
            setControlVisible("UseMathjax",false);
        }

        // AutoCorrect
        setControlEnabled("IgnoreHardLineBreaks",!isLocked("ignore_hard_line_breaks"));
        setControlEnabled("IgnoreEmptyParagraphs",!isLocked("ignore_empty_paragraphs"));
        setControlEnabled("IgnoreDoubleSpaces",!isLocked("ignore_double_spaces"));
    }
	
    private void enableSplitLevel() {
        if (!isLocked("split_level")) {
            boolean bState = getCheckBoxStateAsBoolean("Split");
            setControlEnabled("SplitLevelLabel",bState);
            setControlEnabled("SplitLevel",bState);
            if (!isLocked("repeat_levels")) {
                setControlEnabled("RepeatLevelsLabel",bState);
                setControlEnabled("RepeatLevels",bState);
            }
        }
    }

	
}
