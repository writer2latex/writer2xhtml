/************************************************************************
 *
 *  XhtmlStyleMapItem.java
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

/** This is a simple struct to hold data about a single style map
 */
public class XhtmlStyleMapItem {
    public String sBlockElement=null;
    public String sBlockCss=null;
    public String sElement=null;
    public String sCss=null;
    public String sBefore=null;
    public String sAfter=null;
    
    public XhtmlStyleMapItem(String sBlockElement, String sBlockCss, String sElement, String sCss, String sBefore, String sAfter) {
        this.sBlockElement=sBlockElement;
        this.sBlockCss=sBlockCss;
        this.sElement=sElement;
        this.sCss=sCss;
        this.sBefore=sBefore;
        this.sAfter=sAfter;
    }

}
