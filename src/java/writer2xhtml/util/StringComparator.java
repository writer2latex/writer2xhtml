/************************************************************************
 *
 *  StringComparator.java
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
 *  Version 1.6 (2015-05-19)
 *
 */
package writer2xhtml.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/** This is a <code>Comparator</code> implementation specific for objects compared by one or more string values.
 *
 * @param <T> the class to compare
 */
public abstract class StringComparator<T> implements Comparator<T> {
	
	private Collator collator;
	
	protected Collator getCollator() {
		return collator;
	}

	protected StringComparator(String sLanguage, String sCountry) {
        if (sLanguage==null) { // use default locale
            collator = Collator.getInstance();
        }
        else {
            if (sCountry==null) { sCountry=""; }
            collator = Collator.getInstance(new Locale(sLanguage,sCountry));
        }
	}
}
