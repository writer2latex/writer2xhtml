/************************************************************************
 *
 *  MacroExpander.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2010-03-12)
 *
 */ 

package org.openoffice.da.comp.w2xcommon.helper;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XMacroExpander;

public class MacroExpander {
	
	private XMacroExpander xExpander; 
	
	/** Convenience wrapper class for the UNO Macro Expander singleton
	 * 
	 *  @param xContext the UNO component context from which "theMacroExpander" can be created
	 */
	public MacroExpander(XComponentContext xContext) {
        Object expander = xContext.getValueByName("/singletons/com.sun.star.util.theMacroExpander");
        xExpander = (XMacroExpander) UnoRuntime.queryInterface (XMacroExpander.class, expander);
	}
	
	/** Expand macros in a string
	 * 
	 * @param s the string
	 * @return the expanded string
	 */
    public String expandMacros(String s) {
        if (xExpander!=null && s.startsWith("vnd.sun.star.expand:")) {
            // The string contains a macro, usually as a result of using %origin% in the registry
            s = s.substring(20);
            try {
                return xExpander.expandMacros(s);
            }
            catch (IllegalArgumentException e) {
                // Unknown macro name found, proceed and hope for the best
                return s;
            }
        }
        else {
            return s;
        }
    }
	
}
