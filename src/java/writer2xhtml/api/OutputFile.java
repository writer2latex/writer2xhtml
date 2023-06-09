/************************************************************************
 *
 *  OutputFile.java
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
 *  Version 1.6 (2015-05-05)
 *
 */
 
package writer2xhtml.api;

import java.io.OutputStream;
import java.io.IOException;

/** An <code>OutputFile</code> represents a single file in a
 *  {@link ConverterResult}, which is output from a {@link Converter}
 *  implementation.
 */
public interface OutputFile {
    
    /** Writes the <code>OutputFile</code> to an <code>OutputStream</code>.
     * 
     *  @param  os  <code>OutputStream</code> to which the content should be written
     *  @throws  IOException  if any I/O error occurs
     */
    public void write(OutputStream os) throws IOException;

    /** Returns the file name of the <code>OutputFile</code>. This includes
     *  the file extension and may also include a relative path, always using
     *  / as separator.
     *
     *  @return  the file name of this <code>OutputFile</code>
     */
    public String getFileName();
    
    /** Get the MIME type of the <code>OutputFile</code>.
     * 
     *  @return string representation of the MIME type
     */
    public String getMIMEType();
    
    /** Test whether this document is part of the main document flow (master documents) or
     *  an auxiliary document  
     * 
     *  @return true if this document is a master document
     */
    public boolean isMasterDocument();
    
    /** Test whether this document contains mathematical formulas
     * 
     * @return true if the document contains formulas
     */
    public boolean containsMath();
}
