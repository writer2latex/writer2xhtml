/************************************************************************
 *
 *  EpubMetadataDialog.java
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
 *  Copyright: 2002-2011 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2011-07-20)
 *
 */

package org.openoffice.da.comp.writer2xhtml;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openoffice.da.comp.w2xcommon.helper.DialogBase;
import org.openoffice.da.comp.w2xcommon.helper.SimpleDialog;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.DateTime;

import writer2xhtml.util.CSVList;
import writer2xhtml.util.Misc;

// TODO: Create the UNO helper class DocumentPropertiesAccess

/** This class provides a UNO component which implements a custom metadata editor UI for the EPUB export
 */
public class EpubMetadataDialog extends DialogBase {
	// Author data
	private class AuthorInfo {
		String sName = "";
		boolean isCreator = true;
		String sRole = "";
	}
	
	// Date data
	private class DateInfo {
		int nDate = 0;
		String sEvent = "";
	}
	
	// All the user defined properties we handle
	private static final String IDENTIFIER="Identifier";
	private static final String CREATOR="Creator";
	private static final String CONTRIBUTOR="Contributor";
	private static final String DATE="Date";
	private static final String PUBLISHER="Publisher";
	private static final String TYPE="Type";
	private static final String FORMAT="Format";
	private static final String SOURCE="Source";
	private static final String RELATION="Relation";
	private static final String COVERAGE="Coverage";
	private static final String RIGHTS="Rights";
	
	private static final String[] sRoles = {"", "adp", "ann", "arr", "art", "asn", "aut", "aqt", "aft", "aui", "ant", "bkp",
		"clb", "cmm", "dsr", "edt", "ill", "lyr", "mdc", "mus", "nrt", "oth", "pht", "prt", "red", "rev", "spn", "ths", "trc", "trl"};
	private static HashMap<String,Short> backRoles;
	static {
		backRoles = new HashMap<String,Short>();
		int nCount = sRoles.length;
		for (short i=0; i<nCount; i++) {
			backRoles.put(sRoles[i], i);
		}
	}
	
	// Access to the document properties
	private XDocumentProperties xDocumentProperties=null;
	private XPropertyContainer xUserProperties=null;
	private XPropertySet xUserPropertySet=null;
	
	// Author and date bookkeeping
	private Vector<AuthorInfo> authors = new Vector<AuthorInfo>();
	private Vector<DateInfo> dates = new Vector<DateInfo>();
	
	// Number formatter
    private NumberFormat formatter = new  DecimalFormat("00");
    
    // Pattern matcher for dates
	Pattern datePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    
    public EpubMetadataDialog(XComponentContext xContext) {
		super(xContext);
	}

	/** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.EpubMetadataDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.EpubMetadataDialog";

    // --------------------------------------------------
    // Ensure that the super can find us :-)
    @Override public String getDialogLibraryName() {
		return "W2XDialogs2";
	}

	@Override public String getDialogName() {
		return "EpubMetadata";
	}
	
    // --------------------------------------------------
    // Implement the interface XDialogEventHandler
    @Override public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("UseCustomIdentifierChange")) {
        	return useCustomIdentifierChange();
        }
        else if (sMethod.equals("AuthorAddClick")) {
        	return authorAddclick();
        }
        else if (sMethod.equals("AuthorModifyClick")) {
        	return authorModifyclick();
        }
        else if (sMethod.equals("AuthorDeleteClick")) {
        	return authorDeleteclick();
        }
        else if (sMethod.equals("AuthorUpClick")) {
        	return authorUpclick();
        }
        else if (sMethod.equals("AuthorDownClick")) {
        	return authorDownclick();
        }
        else if (sMethod.equals("DateAddClick")) {
        	return dateAddClick();
        }
        else if (sMethod.equals("DateModifyClick")) {
        	return dateModifyClick();
        }
        else if (sMethod.equals("DateDeleteClick")) {
        	return dateDeleteClick();
        }
        else if (sMethod.equals("DateUpClick")) {
        	return dateUpClick();
        }
        else if (sMethod.equals("DateDownClick")) {
        	return dateDownClick();
        }
        return false;
    }
	
    @Override public String[] getSupportedMethodNames() {
        String[] sNames = { "UseCustomIdentifierChange",
        		"AuthorAddClick", "AuthorModifyClick", "AuthorDeleteClick", "AuthorUpClick", "AuthorDownClick",
        		"DataAddClick", "DateModifyClick", "DateDeleteClick", "DateUpClick", "DateDownClick"};
        return sNames;
    }
    
    private boolean useCustomIdentifierChange() {
    	boolean bEnabled = getCheckBoxStateAsBoolean("UseCustomIdentifier");
    	setControlEnabled("IdentifierLabel",bEnabled);
    	setControlEnabled("Identifier",bEnabled);
    	setControlEnabled("IdentifierTypeLabel",bEnabled);
    	setControlEnabled("IdentifierType",bEnabled);
    	return true;
    }
    
    private boolean authorAddclick() {
    	SimpleDialog dialog = new SimpleDialog(xContext,"W2XDialogs2.AuthorDialog");
    	if (dialog.getDialog()!=null) {
    		dialog.getControls().setListBoxSelectedItem("Type", (short) 0);
    		dialog.getControls().setListBoxSelectedItem("Role", (short) 0); 		
    		if (dialog.getDialog().execute()==ExecutableDialogResults.OK) {
    			AuthorInfo author = new AuthorInfo();
    			author.sName = dialog.getControls().getTextFieldText("Author");
    			author.sRole = sRoles[dialog.getControls().getListBoxSelectedItem("Role")];
    			author.isCreator = dialog.getControls().getListBoxSelectedItem("Type")==0;
    			authors.add(author);
    			updateAuthorList((short) (authors.size()-1));
    		}
    		dialog.getDialog().endExecute();
    	}
    	return true;
    }
    
    private boolean authorModifyclick() {
    	short nIndex = getListBoxSelectedItem("Authors");
    	AuthorInfo author = authors.get(nIndex);
    	SimpleDialog dialog = new SimpleDialog(xContext,"W2XDialogs2.AuthorDialog");
    	if (dialog.getDialog()!=null) {
    		dialog.getControls().setTextFieldText("Author", author.sName);
    		dialog.getControls().setListBoxSelectedItem("Type", author.isCreator ? (short)0 : (short) 1);
    		dialog.getControls().setListBoxSelectedItem("Role", backRoles.containsKey(author.sRole)? backRoles.get(author.sRole) : (short)0); 		
    		if (dialog.getDialog().execute()==ExecutableDialogResults.OK) {
    			author.sName = dialog.getControls().getTextFieldText("Author");
    			author.sRole = sRoles[dialog.getControls().getListBoxSelectedItem("Role")];
    			author.isCreator = dialog.getControls().getListBoxSelectedItem("Type")==0;
    			updateAuthorList(nIndex);
    		}
    		dialog.getDialog().endExecute();
    	}    	    	
    	return true;
    }
    
    private boolean authorDeleteclick() {
    	if (authors.size()>0) {
    		SimpleDialog dialog = new SimpleDialog(xContext,"W2XDialogs2.DeleteDialog");
    		if (dialog.getDialog()!=null) {
    			short nIndex = getListBoxSelectedItem("Authors");
    			String sLabel = dialog.getControls().getLabelText("DeleteLabel");
    			sLabel = sLabel.replaceAll("%s", authors.get(nIndex).sName);
    			dialog.getControls().setLabelText("DeleteLabel", sLabel);
    			if (dialog.getDialog().execute()==ExecutableDialogResults.OK) {
    				authors.remove(nIndex);
    				updateAuthorList(nIndex<authors.size() ? (short) nIndex : (short) (nIndex-1));
    			}
    		}
    	}
    	return true;
    }
    
    private boolean authorUpclick() {
		short nIndex = getListBoxSelectedItem("Authors");
		if (nIndex>0) {
			AuthorInfo author = authors.get(nIndex);
			authors.set(nIndex, authors.get(nIndex-1));
			authors.set(nIndex-1, author);
			updateAuthorList((short) (nIndex-1));
		}
    	return true;
    }
    
    private boolean authorDownclick() {
		short nIndex = getListBoxSelectedItem("Authors");
		if (nIndex+1<authors.size()) {
			AuthorInfo author = authors.get(nIndex);
			authors.set(nIndex, authors.get(nIndex+1));
			authors.set(nIndex+1, author);
			updateAuthorList((short) (nIndex+1));
		}
    	return true;
    }
    
    private boolean dateAddClick() {
    	SimpleDialog dialog = new SimpleDialog(xContext,"W2XDialogs2.DateDialog");
    	if (dialog.getDialog()!=null) {
    		dialog.getControls().setDateFieldValue("Date", datetime2int(xDocumentProperties.getModificationDate()));
    		if (dialog.getDialog().execute()==ExecutableDialogResults.OK) {
    			DateInfo date = new DateInfo();
    			date.nDate = dialog.getControls().getDateFieldValue("Date");
    			date.sEvent = dialog.getControls().getTextFieldText("Event").trim();
    			dates.add(date);
    			updateDateList((short) (dates.size()-1));
    		}
    		dialog.getDialog().endExecute();
    	}
    	return true;
    }
    
    private boolean dateModifyClick() {
    	short nIndex = getListBoxSelectedItem("Dates");
    	DateInfo date = dates.get(nIndex);
    	SimpleDialog dialog = new SimpleDialog(xContext,"W2XDialogs2.DateDialog");
    	if (dialog.getDialog()!=null) {
    		dialog.getControls().setDateFieldValue("Date", date.nDate);
    		dialog.getControls().setTextFieldText("Event", date.sEvent);
    		if (dialog.getDialog().execute()==ExecutableDialogResults.OK) {
    			date.nDate = dialog.getControls().getDateFieldValue("Date");
    			date.sEvent = dialog.getControls().getTextFieldText("Event").trim();
    			updateDateList(nIndex);
    		}
    		dialog.getDialog().endExecute();
    	}    	    	
    	return true;
    }
    
    private boolean dateDeleteClick() {
    	if (dates.size()>0) {
    		SimpleDialog dialog = new SimpleDialog(xContext,"W2XDialogs2.DeleteDialog");
    		if (dialog.getDialog()!=null) {
    			short nIndex = getListBoxSelectedItem("Dates");
    			String sLabel = dialog.getControls().getLabelText("DeleteLabel");
    			sLabel = sLabel.replaceAll("%s", formatDate(dates.get(nIndex).nDate));
    			dialog.getControls().setLabelText("DeleteLabel", sLabel);
    			if (dialog.getDialog().execute()==ExecutableDialogResults.OK) {
    				dates.remove(nIndex);
    				updateDateList(nIndex<dates.size() ? (short) nIndex : (short) (nIndex-1));
    			}
    		}
    	}
    	return true;
    }
    
    private boolean dateUpClick() {
		short nIndex = getListBoxSelectedItem("Dates");
		if (nIndex>0) {
			DateInfo date = dates.get(nIndex);
			dates.set(nIndex, dates.get(nIndex-1));
			dates.set(nIndex-1, date);
			updateDateList((short) (nIndex-1));
		}
    	return true;
    }
    
    private boolean dateDownClick() {
		short nIndex = getListBoxSelectedItem("Dates");
		if (nIndex+1<dates.size()) {
			DateInfo date = dates.get(nIndex);
			dates.set(nIndex, dates.get(nIndex+1));
			dates.set(nIndex+1, date);
			updateDateList((short) (nIndex+1));
		}
    	return true;
    }
    
    // --------------------------------------------------
    // Get and set properties from and to current document 
    
	@Override protected void initialize() {
		// Get the document properties
    	XDesktop xDesktop;
    	Object desktop;
		try {
			desktop = xContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
		} catch (Exception e) {
			// Failed to get desktop
			return;
		}
    	xDesktop = (XDesktop) UnoRuntime.queryInterface(com.sun.star.frame.XDesktop.class, desktop);
		XComponent xComponent = xDesktop.getCurrentComponent();
		XDocumentPropertiesSupplier xSupplier = (XDocumentPropertiesSupplier) UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, xComponent);
		
		// Get the document properties (we need several interfaces)
		xDocumentProperties = xSupplier.getDocumentProperties();
		xUserProperties= xDocumentProperties.getUserDefinedProperties();
		xUserPropertySet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xUserProperties);
		
		// Get the custom identifier and set the text fields
		String[] sIdentifiers = getProperties(IDENTIFIER,false);
		setCheckBoxStateAsBoolean("UseCustomIdentifier",sIdentifiers.length>0);
		useCustomIdentifierChange();
		if (sIdentifiers.length>0) { // Use the first if we have several...
			setTextFieldText("Identifier",getStringValue(sIdentifiers[0]));
			setTextFieldText("IdentifierType",getSuffix(sIdentifiers[0]));
		}
		
		// Get the authors and set the list box
		String[] sCreators = getProperties(CREATOR,false);
		for (String sCreator : sCreators) {
			AuthorInfo creator = new AuthorInfo();
			creator.sName = getStringValue(sCreator);
			creator.sRole = getSuffix(sCreator);
			creator.isCreator = true;
			authors.add(creator);
		}
		String[] sContributors = getProperties(CONTRIBUTOR,false);
		for (String sContributor : sContributors) {
			AuthorInfo contributor = new AuthorInfo();
			contributor.sName = getStringValue(sContributor);
			contributor.sRole = getSuffix(sContributor);
			contributor.isCreator = false;
			authors.add(contributor);
		}
		updateAuthorList((short) 0);
		
		// Get the dates and set the list box
		String[] sDates = getProperties(DATE,false);
		for (String sDate : sDates) {
			DateInfo date = new DateInfo();
			DateTime dt = getDateValue(sDate);
			if (dt!=null) { // We accept either a date
				date.nDate = datetime2int(dt);
			}
			else { // Or a string in the form yyyy-mm-dd
				date.nDate = parseDate(getStringValue(sDate));
			}
			date.sEvent = getSuffix(sDate);
			dates.add(date);
		}
		updateDateList((short) 0);
		
		// Get the standard properties and set the text fields
		setTextFieldText("Title",xDocumentProperties.getTitle());
		setTextFieldText("Subject",xDocumentProperties.getSubject());
		String[] sKeywords = xDocumentProperties.getKeywords();
		CSVList keywords = new CSVList(", ");
		for (String sKeyword : sKeywords) {
			keywords.addValue(sKeyword);
		}
		setTextFieldText("Keywords",keywords.toString());
		setTextFieldText("Description",xDocumentProperties.getDescription());
		
		// Get the simple user properties and set the text fields
		readSimpleProperty(PUBLISHER);
		readSimpleProperty(TYPE);
		readSimpleProperty(FORMAT);
		readSimpleProperty(SOURCE);
		readSimpleProperty(RELATION);
		readSimpleProperty(COVERAGE);
		readSimpleProperty(RIGHTS);

	}
		
	@Override protected void endDialog() {
		// Set the custom identifier from the text fields
		String[] sIdentifiers = getProperties(IDENTIFIER,false);
		for (String sIdentifier : sIdentifiers) { // Remove old identifier(s)
			removeProperty(sIdentifier);
		}
		if (getCheckBoxStateAsBoolean("UseCustomIdentifier")) {
			String sName = IDENTIFIER;
			if (getTextFieldText("IdentifierType").trim().length()>0) {
				sName+="."+getTextFieldText("IdentifierType").trim();
			}
			addProperty(sName);
			setValue(sName,getTextFieldText("Identifier"));
		}
		
		// Set the authors from the list box
		String[] sCreators = getProperties(CREATOR,false);
		for (String sCreator : sCreators) { // remove old creators
			removeProperty(sCreator);
		}
		String[] sContributors = getProperties(CONTRIBUTOR,false); 
		for (String sContributor : sContributors) { // remove old contributors
			removeProperty(sContributor);
		}
		int i=0;
		for (AuthorInfo author : authors) {
			String sName = (author.isCreator ? CREATOR : CONTRIBUTOR)+formatter.format(++i);
			if (author.sRole.length()>0) {
				sName+="."+author.sRole;
			}
			addProperty(sName);
			setValue(sName,author.sName);
		}
		
		// Set the dates from the list box
		String[] sDates = getProperties(DATE,false); 
		for (String sDate : sDates) { // remove old dates
			removeProperty(sDate);
		}
		i=0;
		for (DateInfo date : dates) {
			String sName = DATE+formatter.format(++i);
			if (date.sEvent.length()>0) {
				sName+="."+date.sEvent;
			}
			addProperty(sName);
			setValue(sName,formatDate(date.nDate));
			// Doesn't work (why not?)
			//setValue(sName,int2datetime(date.nDate));
		}
		
		// Set the standard properties from the text fields
		xDocumentProperties.setTitle(getTextFieldText("Title"));
		xDocumentProperties.setSubject(getTextFieldText("Subject"));
		String[] sKeywords = getTextFieldText("Keywords").split(",");
		for (int j=0; j<sKeywords.length; j++) {
			sKeywords[j] = sKeywords[j].trim();
		}
		xDocumentProperties.setKeywords(sKeywords);
		xDocumentProperties.setDescription(getTextFieldText("Description"));
		
		// Set the simple user properties from the text fields
		writeSimpleProperty(PUBLISHER);
		writeSimpleProperty(TYPE);
		writeSimpleProperty(FORMAT);
		writeSimpleProperty(SOURCE);
		writeSimpleProperty(RELATION);
		writeSimpleProperty(COVERAGE);
		writeSimpleProperty(RIGHTS);
	}
	
	// Get the suffix of a user defined property (portion after fist ., if any)
	private String getSuffix(String sPropertyName) {
		int nDot = sPropertyName.indexOf(".");
		return nDot>-1 ? sPropertyName.substring(nDot+1) : "";
	}

	// Get all currently defined user properties with a specific name or prefix
	private String[] getProperties(String sPrefix, boolean bComplete) {
		HashSet<String> names = new HashSet<String>();
		Property[] xProps = xUserPropertySet.getPropertySetInfo().getProperties();
		for (Property prop : xProps) {
			String sName = prop.Name;
			String sLCName = sName.toLowerCase();
			String sLCPrefix = sPrefix.toLowerCase();
			if ((bComplete && sLCName.equals(sLCPrefix)) || (!bComplete && sLCName.startsWith(sLCPrefix))) {
				names.add(sName);
			}
		}
		return Misc.sortStringSet(names);
	}
	
	// Add a user property
	private void addProperty(String sName) {
		try {
			xUserProperties.addProperty(sName, (short) 128, ""); // 128 means removeable, last parameter is default value
		} catch (PropertyExistException e) {
		} catch (IllegalTypeException e) {
		} catch (IllegalArgumentException e) {
		}
	}
	
	// Delete a user property
	private void removeProperty(String sName) {
		try {
			xUserProperties.removeProperty(sName);
		} catch (UnknownPropertyException e) {
		} catch (NotRemoveableException e) {
		}
	}
	
	// Set the value of a user property (failing silently if the property does not exist)
	private void setValue(String sName, Object value) {
		try {
			xUserPropertySet.setPropertyValue(sName, value);
		} catch (UnknownPropertyException e) {
		} catch (PropertyVetoException e) {
		} catch (IllegalArgumentException e) {
		} catch (WrappedTargetException e) {
		}
	}
	
	// Get the string value of a user property (returning null if the property does not exist)
	private String getStringValue(String sName) {
		Object value = getValue(sName);
		
		if (value!=null && AnyConverter.isString(value)) {
			try {
				return AnyConverter.toString(value);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		return null;
	}
	
	private DateTime getDateValue(String sName) {
		Object value = getValue(sName);
		
		if (value!=null && value instanceof DateTime) {
			return (DateTime) value;
		}
		return null;
	}
	
	// Get the value of a user property (returning null if the property does not exist)
	private Object getValue(String sName) {
		try {
			return xUserPropertySet.getPropertyValue(sName);
		} catch (UnknownPropertyException e) {
			return null;
		} catch (WrappedTargetException e) {
			return null;
		}
	}
	
	private void updateAuthorList(short nItem) {
		int nCount = authors.size();
		if (nCount>0) {
			String[] sAuthors = new String[nCount];
			for (int i=0; i<nCount; i++) {
				AuthorInfo author = authors.get(i);
				sAuthors[i] = author.sName
				+" ("
				+(author.isCreator ? "creator":"contributor")
				+(author.sRole.length()>0 ? ", "+author.sRole : "")
				+")";
			}
			setListBoxStringItemList("Authors", sAuthors);
			setListBoxSelectedItem("Authors",nItem);
			setControlEnabled("Authors", true);
		}
		else { // Display the fall-back author
			String[] sAuthors = new String[1];
			//sAuthors[0] = xDocumentProperties.getAuthor()+" (default creator)";
			sAuthors[0] = xDocumentProperties.getModifiedBy()+" (default creator)";
			setListBoxStringItemList("Authors", sAuthors);
			setControlEnabled("Authors", false);
		}
		setControlEnabled("ModifyAuthorButton",nCount>0);
		setControlEnabled("DeleteAuthorButton",nCount>0);
		setControlEnabled("AuthorUpButton",nCount>1);
		setControlEnabled("AuthorDownButton",nCount>1);
	}
	
	private void updateDateList(short nItem) {
		int nCount = dates.size();
		if (nCount>0) {
			String[] sDates = new String[nCount];
			for (int i=0; i<nCount; i++) {
				DateInfo date = dates.get(i);
				sDates[i] = formatDate(date.nDate);
				if (date.sEvent.length()>0) {
					sDates[i]+=" (" + date.sEvent + ")";
				}
			}
			setListBoxStringItemList("Dates", sDates);
			setListBoxSelectedItem("Dates",nItem);
			setControlEnabled("Dates", true);
		}
		else { // Display the fall-back date
			String[] sDates = new String[1];
			sDates[0] = formatDate(datetime2int(xDocumentProperties.getModificationDate()))+" (default date)";
			setListBoxStringItemList("Dates", sDates);
			setControlEnabled("Dates", false);
		}
		setControlEnabled("ModifyDateButton",nCount>0);
		setControlEnabled("DeleteDateButton",nCount>0);
		setControlEnabled("DateUpButton",nCount>1);
		setControlEnabled("DateDownButton",nCount>1);
	}
	
	private void readSimpleProperty(String sName) {
		String[] sNames = getProperties(sName,true);
		if (sNames.length>0) {
			String sValue = getStringValue(sNames[0]);
			if (sValue!=null) {
				setTextFieldText(sName, sValue);
			}
		}
	}
	
	private void writeSimpleProperty(String sName) {
		String[] sOldNames = getProperties(sName,true);
		for (String sOldName : sOldNames) {
			removeProperty(sOldName);
		}
		String sValue = getTextFieldText(sName);
		if (sValue.length()>0) {
			addProperty(sName);
			setValue(sName,sValue);
		}
	}
	
	// Date fields uses integers for dates (format yyyymmdd)
	// Document properties uses com.sun.star.util.DateTime
	// Also dates should be formatted as yyyy-mm-dd as strings
	// Thus we need a few conversion methods

	// Format a integer date as yyyy-mm-dd
	private String formatDate(int nDate) {
		String sDate = Integer.toString(nDate);
		if (sDate.length()==8) {
			return sDate.substring(0,4)+"-"+sDate.substring(4, 6)+"-"+sDate.substring(6);
		}
		else {
			return "???";
		}
	}
	
	// Parse a string as a date in the format yyyy-mm-dd (returning 0 on failure)
	private int parseDate(String sDate) {
		Matcher matcher = datePattern.matcher(sDate);
		if (matcher.matches()) {
			return Misc.getPosInteger(matcher.group(1)+matcher.group(2)+matcher.group(3),0);
		}
		return 0;
	}
	
	// Convert an integer to com.sun.star.util.DateTime
	/*private DateTime int2datetime(int nDate) {
		DateTime date = new DateTime();
		date.Year = (short) (nDate/10000);
		date.Month = (short) ((nDate%10000)/100);
		date.Day = (short) (nDate%100);
		return date;
	}*/
	
	// Convert a com.sun.star.util.DateTime to integer
	private int datetime2int(DateTime date) {
		return 10000*date.Year+100*date.Month+date.Day;				
	}
    
}
