/************************************************************************
 *
 *  SimpleZipReader.java
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
 *  Version 1.4 (2012-03-27)
 *
 */

package writer2xhtml.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SimpleZipReader {

    private final static int BUFFERSIZE = 1024;

    private Map<String,byte[]> entries = new HashMap<String,byte[]>();


    /** Read a zipped stream
     *
     *  @param  is  <code>InputStream</code> to read
     *
     *  @throws  IOException  if an I/O error occurs
     */
    public void read(InputStream is) throws IOException {
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry = null;
        while ((entry=zis.getNextEntry())!=null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int nLen = 0;
            byte buffer[] = new byte[BUFFERSIZE];
            while ((nLen = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, nLen);
            }
            byte bytes[] = baos.toByteArray();
            entries.put(entry.getName(), bytes);
        }
        zis.close();
    }
    
    /** Get an entry from the ZIP file. Getting should be taken quite literally here:
     *  You can only get an entry once: The <code>SimpleZipReader</code> removes the entry from the
     *  collection when this method is called (memory optimization).
     *
     * @param   sName    the name (path) of the ZIP entry
     *
     * @return  a byte array with the contents of the entry, or null if the entry does not exist
     */
    public byte[] getEntry(String sName) {
    	if (entries.containsKey(sName)) {
    		byte[] bytes = entries.get(sName);
    		entries.remove(sName);
    		return bytes;
    	}
    	return null;
    }
        

}
