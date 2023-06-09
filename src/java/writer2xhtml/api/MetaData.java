/************************************************************************
 *
 *  MetaData.java
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
 *  Version 1.2 (2010-12-15)
 *
 */

package writer2xhtml.api;

import java.util.Map;

/** This interface provides access to the predefined meta data of the
 *  source document (currently incomplete)
 */
public interface MetaData {
	/** Get the title of the source document
	 * 
	 * @return the title (may return an empty string)
	 */
	public String getTitle();
	
	/** Get the subject of the source document
	 * 
	 * @return the subject (may return an empty string)
	 */
	public String getSubject();
	
	/** Get the keywords of the source document
	 * 
	 * @return the keywords as a comma separated list (may return an empty string)
	 */
	public String getKeywords();
	
	/** Get the description of the source document
	 * 
	 * @return the description (may return an empty string)
	 */
	public String getDescription();
	
	/** Get the creator of the source document (or the initial creator if none is specified)
	 * 
	 * @return the creator (may return an empty string)
	 */
	public String getCreator();

	/** Get the (main) language of the document
	 * 
	 * @return the language
	 */
	public String getLanguage();

	/** Get the date of the source document
	 * 
	 * @return the date (may return an empty string)
	 */
	public String getDate();
	
	/** Get the user-defined meta data
	 * 
	 * @return the user-defined meta data as a name-value map
	 */
	public Map<String,String> getUserDefinedMetaData();
}
