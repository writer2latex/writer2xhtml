/************************************************************************
 *
 *  L10n.java
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
 *  Copyright: 2002-2012 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.4 (2012-03-16)
 *
 */

package writer2xhtml.xhtml.l10n;

import java.util.Locale;
import java.util.ResourceBundle;

/* This class handles localized strings (used for navigation links in the exported document)
 * Note that the US-English strings need duplicated due to ResourceBundles' search order.
 * Default strings are needed for the special case that neither strings for the document language,
 * nor for the system default language are available.
 * US-English strings are needed if the document language is English and the system locale is not.
 */
public class L10n {
    public final static int UP = 0;
    public final static int FIRST = 1;
    public final static int PREVIOUS = 2;
    public final static int NEXT = 3;
    public final static int LAST = 4;
    public final static int CONTENTS = 5;
    public final static int INDEX = 6;
    public final static int HOME = 7;
    public final static int DIRECTORY = 8;
    public final static int DOCUMENT = 9;

    private ResourceBundle resourceBundle = ResourceBundle.getBundle("writer2xhtml.xhtml.l10n.XhtmlStrings",Locale.getDefault());
    private Locale locale = null;
	
    public void setLocale(String sLanguage, String sCountry) {
        if (sLanguage!=null) {
            if (sCountry!=null) {
            	locale = new Locale(sLanguage,sCountry);
            }
            else  {
            	locale = new Locale(sLanguage);
            }
        }
        else {
        	locale = Locale.getDefault();
        }
        
        resourceBundle = ResourceBundle.getBundle("writer2xhtml.xhtml.l10n.XhtmlStrings",locale);
    }
    
    public Locale getLocale() {
    	return locale;
    }
	
    public String get(int nString) {
        switch (nString) {
        case UP: return resourceBundle.getString("up");
        case FIRST : return resourceBundle.getString("first");
        case PREVIOUS : return resourceBundle.getString("previous");
        case NEXT : return resourceBundle.getString("next");
        case LAST : return resourceBundle.getString("last");
        case CONTENTS : return resourceBundle.getString("contents");
        case INDEX : return resourceBundle.getString("index");
        case HOME : return resourceBundle.getString("home");
        case DIRECTORY: return resourceBundle.getString("directory");
        case DOCUMENT: return resourceBundle.getString("document");
        default: return "???";
        }
    }
}
