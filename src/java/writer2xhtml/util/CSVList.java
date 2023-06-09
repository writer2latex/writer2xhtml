/************************************************************************
 *
 *  CSVList.java
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
 *  Copyright: 2002-2023 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 2.0 (2023-06-07)
 *
 */

package writer2xhtml.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** This class maintains a list of items separated by commas or another separation character.
* The items may be simple values or key/value pairs separated by a colon or another separation character.
* Simple values and key/values pairs may be freely mixed within the same <code>CSVList</code>.
*/
public class CSVList{
   private String sItemSep;
   private String sKeyValueSep;
   // The CSVList is backed by a Map, which is accessible for other CSVList instances
   Map<String,String> items = new LinkedHashMap<>();
   
   /** Create a new <code>CSVList</code> with specific separators
    * 
    * @param sItemSep the separator between items
    * @param sKeyValueSep the separator between keys and values
    */
   public CSVList(String sItemSep, String sKeyValueSep) {
       this.sItemSep=sItemSep;
       this.sKeyValueSep=sKeyValueSep;
   }
	
   /** Create a new <code>CSVList</code> with a specific item separator (use default colon for key/values separator)
    * 
    * @param sItemSep the separator between items
    */
   public CSVList(String sItemSep) {
       this(sItemSep,":");
   }
   
   /** Create a new <code>CSVList</code> with a specific character as item separator (use default colon for key/values separator)
    * 
    * @param cItemSep the separator between items
    */
   public CSVList(char cItemSep) {
       this(Character.toString(cItemSep),":");
   }

   /** Add a simple value to the <code>CSVList</code>
    * 
    * @param sVal the value (ignored if null)
    */
   public void addValue(String sVal){
   	if (sVal!=null) {
   		items.put(sVal, null);
   	}
   }

   /** Add a key/value pair to the <code>CSVList</code>, replacing a previous value if the key already exists in the <code>CSVList</code>
    * 
    * @param sKey the key of the pair (ignored if null)
    * @param sVal the value of the pair (may be null, which creates a simple value)
    */
   public void addValue(String sKey, String sVal) {
   	if (sKey!=null) {
   		items.put(sKey, sVal);
   	}
   }
   
   /** Add all items from another <code>CSVList</code>. The separator strings for the other list is ignored.
    * 
    * @param list the <code>CSVList</code> containing the items to add
    */
   public void addValues(CSVList list) {
   	for (String sKey : list.items.keySet()) {
   		items.put(sKey, list.items.get(sKey));
   	}
   }
   
   /** Return the value associated with a key
    * 
    * @param sKey the key of the value pair
    * @return the value, or null if the key does not exist or represents a simple value
    */
   public String getValue(String sKey) {
   	if (items.containsKey(sKey)) {
   		return items.get(sKey);
   	}
   	return null;
   }
   
   /** Remove all values from the list
    */
   public void clear() {
   	items.clear();
   }
	
   /** Test whether this <code>CSVList</code> contains any items
    * 
    * @return true if the list is empty
    */
   public boolean isEmpty() {
       return items.size()==0;
   }
	
   public String toString() {
       StringBuilder buf = new StringBuilder();
       boolean bFirst=true;
       for (String sKey : items.keySet()) {
       	if (bFirst) { bFirst=false; } else { buf.append(sItemSep); }
       	buf.append(sKey);
       	if (items.get(sKey)!=null) {
       		buf.append(sKeyValueSep).append(items.get(sKey));
       	}
       }
       return buf.toString();
   }
	
}
