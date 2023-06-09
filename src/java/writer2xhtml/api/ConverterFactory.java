/************************************************************************
 *
 *  ConverterFactory.java
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
 *  Copyright: 2002-2022 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2022-08-03)
 *
 */
 
package writer2xhtml.api;

/** This is a factory class which provides static methods to create converters
 *  for documents in OpenDocument (or OpenOffice.org 1.x) format into a specific MIME type
 */
public class ConverterFactory {

    // Version information
    private static final String VERSION = "1.7";
    private static final String DATE = "2022-08-03";
	
    /** Return the Writer2LaTeX version in the form
     *  (major version).(minor version).(patch level)<br/>
     *  Development versions have an odd minor version number
     *  @return the version number
     */
    public static String getVersion() { return VERSION; }

    /** Return date information
     *  @return the release date for this Writer2LaTeX version
     */
    public static String getDate() { return DATE; }

    /** <p>Create a <code>Converter</code> implementation which supports
     *  conversion into the specified MIME type.</p>
     *  <p>Currently supported MIME types are:</p>
     *  <ul>
     *    <li><code>text/html</code> for XHTML 1.0 strict format</li>
     *    <li><code>application/xhtml11</code> for XHTML 1.1 format
     *    Note that this is <em>not</em> the recommended media type for XHTML 1.1
     *    (see http://www.w3.org/TR/xhtml-media-types/), but it is used internally
     *    by Writer2xhtml to distinguish from XHTML+MathML</li>
     *    <li><code>application/xhtml+xml</code> for XHTML+MathML</li>
     *    <li><code>text/html5</code> for HTML5 documents
     *    Note that this is <em>not</em> the recommended media type for HTML5
     *    (see http://wiki.whatwg.org/), but it is used internally
     *    by Writer2xhtml to distinguish XHTML from HTML5</li>
     *    <li><code>application/epub+zip</code> for EPUB format</li>
     *    <li><code>epub3</code> for EPUB 3 format</li>
     *  </ul>
     *  
     *  @param sMIME the MIME type of the target format
     *  @return the required <code>Converter</code> or null if a converter for
     *  the requested MIME type could not be created
     */
    public static Converter createConverter(String sMIME) {
        if (MIMETypes.XHTML.equals(sMIME)) {
            return new writer2xhtml.xhtml.Xhtml10Converter();
        }
        else if (MIMETypes.XHTML11.equals(sMIME)) {
            return new writer2xhtml.xhtml.Xhtml11Converter();
        }
        else if (MIMETypes.XHTML_MATHML.equals(sMIME)) {
            return new writer2xhtml.xhtml.XhtmlMathMLConverter();
        }
        else if (MIMETypes.HTML5.equals(sMIME)) {
            return new writer2xhtml.xhtml.Html5Converter();
        }
        else if (MIMETypes.EPUB3.equals(sMIME)) {
            return new writer2xhtml.epub.EPUB3Converter();
        }
        else if (MIMETypes.EPUB.equals(sMIME)) {
            return new writer2xhtml.epub.EPUBConverter();
        }
        return null;
    }
	
}
