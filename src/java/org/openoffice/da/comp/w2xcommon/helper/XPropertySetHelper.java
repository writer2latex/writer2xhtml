/************************************************************************
 *
 *  XPropertySetHelper.java
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
 *  Version 1.7 (2022-08-16)
 *
 */
package org.openoffice.da.comp.w2xcommon.helper;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

/** Helper class providing staic convenience methods for accesing an XPropertySet
 * The helpers will fail silently if names or data is provided, but the user is expected to
 * apply them with correct data only...
 */
public class XPropertySetHelper {

	public static Object getPropertyValue(XPropertySet xProps, String sName) {
        try {
            return xProps.getPropertyValue(sName);
        }
        catch (UnknownPropertyException e) {
            return null;
        }
        catch (WrappedTargetException e) {
            return null;
        }
    } 
	
    public static void setPropertyValue(XPropertySet xProps, String sName, Object value) {
        try {
            xProps.setPropertyValue(sName,value);
        }
        catch (UnknownPropertyException e) {
        }
        catch (PropertyVetoException e) { // unacceptable value
        }
        catch (IllegalArgumentException e) {
        }
        catch (WrappedTargetException e) {
        }
    }
	
    public static String getPropertyValueAsString(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof String ? (String) value : "";
    }
	
    public static int getPropertyValueAsInteger(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }
	
    public static void setPropertyValue(XPropertySet xProps, String sName, int nValue) {
        setPropertyValue(xProps,sName,Integer.valueOf(nValue));
    }

    public static short getPropertyValueAsShort(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Short ? ((Short) value).shortValue() : 0;
    }
	
    public static void setPropertyValue(XPropertySet xProps, String sName, short nValue) {
        setPropertyValue(xProps,sName,Short.valueOf(nValue));
    }

    public static boolean getPropertyValueAsBoolean(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : false;
    }
	
    public static void setPropertyValue(XPropertySet xProps, String sName, boolean bValue) {
        setPropertyValue(xProps,sName,Boolean.valueOf(bValue));
    }



}
