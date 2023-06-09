/************************************************************************
 *
 *  ContentEntry.java
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
 *  Version 1.2 (2010-03-15)
 *
 */

package writer2xhtml.api;

/** This interface represents a content entry, that is a named reference
 *  to a position within the output document.
 */
public interface ContentEntry {
	/** Get the outline level of this <code>ContentEntry</code>.
	 *  The top level is 1 (entries corresponding to indexes are considered
	 *  top level).
	 *  Note that intermediate levels may be missing (e.g. a heading of
	 *  level 3 may follow immediately after a heading of level 1).
	 * 
	 * @return the outline level
	 */
	public int getLevel();
	
	/** Get the title for this entry
	 * 
	 * @return the title
	 */
	public String getTitle();
	
	/** Get the file associated with the entry
	 * 
	 * @return the output file
	 */
	public OutputFile getFile();
	
	/** Get the name of a target within the file, if any 
	 * 
	 * @return the target name, or null if no target is needed
	 */
	public String getTarget();

}
