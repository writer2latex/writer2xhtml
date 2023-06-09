/************************************************************************
 *
 *  HTML5OptionsDialog.java
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
 *  Version 1.7 (2022-06-14)
 *
 */ package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.uno.XComponentContext;

 /** This class provides a UNO component which implements a filter UI for the
  *  HTML5 export.
  */
public class HTML5OptionsDialog extends XhtmlOptionsDialogBase {
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.HTML5OptionsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.HTML5OptionsDialog";
	
    /** Create a new HTML5OptionsDialog */
    public HTML5OptionsDialog(XComponentContext xContext) {
        super(xContext);
    }


}
