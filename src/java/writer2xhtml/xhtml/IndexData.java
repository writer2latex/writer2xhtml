/************************************************************************
 *
 *	IndexData.java
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
 *  Copyright: 2002-2018 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6.1 (2018-08-07)
 *
 */
package writer2xhtml.xhtml;

import org.w3c.dom.Element;

/** This class holds data about an index, which should be populated later
 */
class IndexData {
	// Source data
	int nChapterNumber; // The chapter number containing this index
	Element onode; // The index source in the ODF document
	
	// Target data
	int nOutFileIndex; // The outfile containing this index
	Element hnode; // The container to hold the generated index

}