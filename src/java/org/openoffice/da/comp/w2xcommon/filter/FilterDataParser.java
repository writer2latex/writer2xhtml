/************************************************************************
 *
 *  FilterDataParser.java
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
 *  Version 1.6 (2015-05-06)
 *
 */ 
 
package org.openoffice.da.comp.w2xcommon.filter;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import org.openoffice.da.comp.w2xcommon.helper.PropertyHelper;

import com.sun.star.beans.PropertyValue;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.ucb.CommandAbortedException;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XStringSubstitution;

import writer2xhtml.api.Converter;

import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToOutputStreamAdapter;


/** This class parses the FilterData property passed to the filter and
 *  applies it to a <code>Converter</code>
 *  All errors are silently ignored
 */
public class FilterDataParser {
	// TODO: Use JSON format
    
    //private static XComponentContext xComponentContext = null;
    
    private XSimpleFileAccess2 sfa2;
    private XStringSubstitution xPathSub;
    
    public FilterDataParser(XComponentContext xComponentContext) {
        //this.xComponentContext = xComponentContext;

        // Get the SimpleFileAccess service
        sfa2 = null;
        try {
            Object sfaObject = xComponentContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.ucb.SimpleFileAccess", xComponentContext);
            sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get SimpleFileAccess service (should not happen)
        }
        
        // Get the PathSubstitution service
        xPathSub = null;
        try {
            Object psObject = xComponentContext.getServiceManager().createInstanceWithContext(
               "com.sun.star.util.PathSubstitution", xComponentContext);
            xPathSub = (XStringSubstitution) UnoRuntime.queryInterface(XStringSubstitution.class, psObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get PathSubstitution service (should not happen)
        }     
    }
    
    /** Apply the given FilterOptions property to the given converter.
     *  The property must be a comma separated list of name=value items.
     * @param options an <code>Any</code> containing the FilterOptions property
     * @param converter a <code>writer2xhtml.api.Converter</code> implementation
     */
    public void applyFilterOptions(Object options, Converter converter) {
    	// Get the string from the data, if possible
    	if (AnyConverter.isString(options)) {
    		String sOptions = AnyConverter.toString(options);
    		if (sOptions!=null) {
	    		// Convert to array
	    		String[] sItems = sOptions.split(",");
	    		int nItemCount = sItems.length;
	        	PropertyValue[] filterData = new PropertyValue[nItemCount];
	        	for (int i=0; i<nItemCount; i++) {
	        		String[] sItem = sItems[i].split("=");
	        		filterData[i] = new PropertyValue();
	        		filterData[i].Name = sItem[0];
	        		filterData[i].Value = sItem.length>1 ? sItem[1] : "";
	        		System.out.println(filterData[i].Name+" "+filterData[i].Value);
	        	}
	        	applyParsedFilterData(filterData,converter);
    		}
    	}
    }
    
    /** Apply the given FilterData property to the given converter.
     *  The property must be an array of PropertyValue objects.
     *  @param data an <code>Any</code> containing the FilterData property
     *  @param converter a <code>writer2xhtml.api.Converter</code> implementation
     */
    public void applyFilterData(Object data, Converter converter) {
        // Get the array from the data, if possible
        PropertyValue[] filterData = null;
        if (AnyConverter.isArray(data)) {
            try {
                Object[] arrayData = (Object[]) AnyConverter.toArray(data);
                if (arrayData instanceof PropertyValue[]) {
                    filterData = (PropertyValue[]) arrayData;
                    if (filterData!=null) {
                    	applyParsedFilterData(filterData,converter);
                    }
                }
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to array; should not happen - ignore   
            }
        }
    }
    
    private void applyParsedFilterData(PropertyValue[] filterData, Converter converter) {
        PropertyHelper props = new PropertyHelper(filterData);
        
        // Get the special properties TemplateURL, StyleSheetURL, ResourceURL, Resources, ConfigURL and AutoCreate
        Object tpl = props.get("TemplateURL");
        String sTemplate = null;
        if (tpl!=null && AnyConverter.isString(tpl)) {
            try {
                sTemplate = substituteVariables(AnyConverter.toString(tpl));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }
        
        Object styles = props.get("StyleSheetURL");
        String sStyleSheet = null;
        if (styles!=null && AnyConverter.isString(styles)) {
            try {
                sStyleSheet = substituteVariables(AnyConverter.toString(styles));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }

        // This property accepts an URL pointing to a folder containing the resources to include
        Object resourcedir = props.get("ResourceURL");
        String sResourceURL = null;
        if (resourcedir!=null && AnyConverter.isString(resourcedir)) {
            try {
                sResourceURL = substituteVariables(AnyConverter.toString(resourcedir));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }

        // This property accepts a semicolon separated list of <URL>[[::<file name>]::<mime type>]
        Object resources = props.get("Resources");
        String[] sResources = null;
        if (resources!=null && AnyConverter.isString(resources)) {
            try {
                sResources = substituteVariables(AnyConverter.toString(resources)).split(";");
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }

        Object auto = props.get("AutoCreate");
        boolean bAutoCreate = false;
        if (auto!=null && AnyConverter.isString(auto)) {
            try {
                if ("true".equals(AnyConverter.toString(auto))) {
                    bAutoCreate = true;
                }
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }
        
        Object cfg = props.get("ConfigURL");
        String sConfig = null;
        if (cfg!=null && AnyConverter.isString(cfg)) {
            try {
                sConfig = substituteVariables(AnyConverter.toString(cfg));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }

        // Load the template from the specified URL, if any
        if (sfa2!=null && sTemplate!=null && sTemplate.length()>0) {
            try {
                XInputStream xIs = sfa2.openFileRead(sTemplate);
                if (xIs!=null) {
                    InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                    converter.readTemplate(is);
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

        // Load the style sheet from the specified URL, if any
        if (sfa2!=null && sStyleSheet!=null && sStyleSheet.length()>0) {
            try {
                XInputStream xIs = sfa2.openFileRead(sStyleSheet);
                if (xIs!=null) {
                    InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                    converter.readStyleSheet(is);
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
        
        // Load the resource from the specified folder URL, if any
        if (sfa2!=null && sResourceURL!=null && sResourceURL.length()>0) {
        	String[] sURLs;
        	try {
        		sURLs = sfa2.getFolderContents(sResourceURL, false); // do not include folders
        		for (String sURL : sURLs) {
        			XInputStream xIs = sfa2.openFileRead(sURL);
        			if (xIs!=null) {
        				String sFileName = sURL.substring(sURL.lastIndexOf('/')+1);
        				InputStream is = new XInputStreamToInputStreamAdapter(xIs);
        				converter.readResource(is,sFileName,null);
        				is.close();
        				xIs.closeInput();
        			}
        		}
        	} catch (IOException e) {
        		// ignore
        	} catch (CommandAbortedException e1) {
        		// ignore
        	} catch (Exception e1) {
        		// ignore
        	}
        }
        
        // Load the resources from the specified URLs, if any
        if (sfa2!=null && sResources!=null) {
        	for (String sResource : sResources) {
        		// Format is <URL>[[::<file name>]::<mime type>]
        		String[] sParts = sResource.split("::");
        		if (sParts.length>0) {
            		String sURL=sParts[0];
            		String sFileName;
            		String sMediaType=null;
        			if (sParts.length==3) {
        				sFileName = sParts[1];
        				sMediaType = sParts[2];
        			}
        			else {
        				sFileName = sURL.substring(sURL.lastIndexOf('/')+1);
        				if (sParts.length==2) {
        					sMediaType = sParts[1];
        				}
        			}
        			try {
        				XInputStream xIs = sfa2.openFileRead(sURL);
        				if (xIs!=null) {
        					InputStream is = new XInputStreamToInputStreamAdapter(xIs);
        					converter.readResource(is, sFileName, sMediaType);
        					is.close();
        					xIs.closeInput();
        				} // otherwise wrong format, ignore
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
        }
        
        // Create config if required
        try {
            if (bAutoCreate && sfa2!=null && sConfig!=null && !sConfig.startsWith("*") && !sfa2.exists(sConfig)) {
                // Note: Requires random access, ie. must be a file URL:
                XOutputStream xOs = sfa2.openFileWrite(sConfig);
                if (xOs!=null) {
                    OutputStream os = new XOutputStreamToOutputStreamAdapter(xOs);
                    converter.getConfig().write(os);
                    os.flush();
                    os.close();
                    xOs.closeOutput();
                }
            }
        }
        catch (IOException e) {
            // ignore
        }
        catch (NotConnectedException e) {
            // ignore
        }
        catch (CommandAbortedException e) {
            // Ignore
        }
        catch (com.sun.star.uno.Exception e) {
          // Ignore
        }

        // Load the configuration from the specified URL, if any
        if (sConfig!=null) {
            if (sConfig.startsWith("*")) { // internal configuration
                try {
                    converter.getConfig().readDefaultConfig(sConfig.substring(1)); 
                }
                catch (IllegalArgumentException e) {
                    // ignore
                }
            }
            else if (sfa2!=null) { // real URL
                try {
                    XInputStream xIs = sfa2.openFileRead(sConfig);;
                    if (xIs!=null) {
                        InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                        converter.getConfig().read(is);
                        is.close();
                        xIs.closeInput();
                    }
                }
                catch (IOException e) {
                    // Ignore
                }
                catch (NotConnectedException e) {
                    // Ignore
                }
                catch (CommandAbortedException e) {
                    // Ignore
                }
                catch (com.sun.star.uno.Exception e) {
                    // Ignore
                }
            }
        }
        
        // Read further configuration properties
        Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements()) {
            String sKey = keys.nextElement();
            if (!"ConfigURL".equals(sKey) && !"TemplateURL".equals(sKey) && !"StyleSheetURL".equals(sKey)
            		&& !"Resources".equals(sKey) && !"AutoCreate".equals(sKey)) {
                Object value = props.get(sKey);
                if (AnyConverter.isString(value)) {
                    try {
                        converter.getConfig().setOption(sKey,AnyConverter.toString(value));
                    }
                    catch (com.sun.star.lang.IllegalArgumentException e) {
                        // Failed to convert to String; should not happen - ignore   
                    }
                }
            } 
        }
    }
    
    private String substituteVariables(String sUrl) {
        if (xPathSub!=null) {
            try {
                return xPathSub.substituteVariables(sUrl, false);
            }
            catch (com.sun.star.container.NoSuchElementException e) {
                // Found an unknown variable, no substitution
                // (This will only happen if false is replaced by true above)
                return sUrl;
            }
        }
        else { // Not path substitution available
            return sUrl;
        }
    }
    	
}



