/************************************************************************
 *
 *  CellView.java
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
 *  Copyright: 2002-2008 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.0 (2008-09-07) 
 *
 */

package writer2xhtml.office;

import org.w3c.dom.Element;

/**
 *  This class represent a cell in a table view
 */
public class CellView {
    public Element cell = null;
    public int nRowSpan = 1;
    public int nColSpan = 1;
    public int nOriginalRow = -1;
    public int nOriginalCol = -1;
}