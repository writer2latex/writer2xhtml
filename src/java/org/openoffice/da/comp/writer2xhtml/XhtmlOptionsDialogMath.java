/************************************************************************
 *
 *  XhtmlOptionsDialogMath.java
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
 *  Copyright: 2002-2004 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.4 (2014-08-18)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.uno.XComponentContext;

/** This class provides a uno component which implements a filter ui for the
 *  Xhtml export for the XHTML+MathML and HTML export.
 *  This variant of the dialog has the MathJax setting enabled
 */
public class XhtmlOptionsDialogMath extends XhtmlOptionsDialogBase {
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.XhtmlOptionsDialogMath";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogMath";
	
    /** Create a new XhtmlOptionsDialogMath */
    public XhtmlOptionsDialogMath(XComponentContext xContext) {
        super(xContext);
    }

}