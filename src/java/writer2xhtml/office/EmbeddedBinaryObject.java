/************************************************************************
 *
 *  EmbeddedBinaryObject.java
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
 *  Version 1.4 (2012-03-28)
 *
 */

package writer2xhtml.office;

import writer2xhtml.util.SimpleZipReader;

/**
 * This class represents an embedded object with a binary representation in an ODF package document
 */
public class EmbeddedBinaryObject extends EmbeddedObject {
    
    /** The object's binary representation. */
    private byte[] blob = null;
        
    /**
     * Package private constructor for use when reading an object from a 
     * package ODF file
     *
     * @param   sName    The name of the object.
     * @param   sType    The MIME-type of the object.
     * @param   doc      The document containing the object.
     * @param   source  A <code>SimpleZipReader</code> containing the object
     */    
    protected EmbeddedBinaryObject(String sName, String sType, OfficeDocument doc, SimpleZipReader source) {
    	super(sName,sType,doc);
    	blob = source.getEntry(sName);
    }
    
    /** Get the binary data for this object
     *
     * @return  A <code>byte</code> array containing the object's data.
     */
    public byte[] getBinaryData() {
        return blob;
    }    
    
    public void dispose() {
    	super.dispose();
    	blob = null;
    }

}
