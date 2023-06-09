/************************************************************************
 *
 *  XhtmlStyleMap.java
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
 *  Copyright: 2002-2014 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6 (2014-10-24)
 *
 */

package writer2xhtml.xhtml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XhtmlStyleMap {
	private Map<String,XhtmlStyleMapItem> items = new HashMap<String,XhtmlStyleMapItem>();
    
	public boolean contains(String sName) {
        return sName!=null && items.containsKey(sName);
    }
	
    public void put(String sName, XhtmlStyleMapItem item) {
        items.put(sName, item);
    }

    public XhtmlStyleMapItem get(String sName) {
        return items.get(sName);
    }

    public Iterator<String> getNames() {
        return items.keySet().iterator();
    }
	
}
