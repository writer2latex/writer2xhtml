/************************************************************************
 *
 *  MIMETypes.java
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

/** This class provides definitions of MIME types used by LibreOffice and Writer2xhtml (some are not genuine MIME types, but are used internally in Writer2xhtml)
 */
public class MIMETypes {
    // Various graphics formats, see
    // http://api.openoffice.org/docs/common/ref/com/sun/star/graphic/MediaProperties.html#MimeType
	
	/** Constant defining the MIME type for PNG-files
	 */
    public static final String PNG="image/png";
	
	/** Constant defining the MIME type for JPEG-files
	 */
    public static final String JPEG="image/jpeg";
	
	/** Constant defining the MIME type for GIF-files
	 */
    public static final String GIF="image/gif";
	
	/** Constant defining the MIME type for TIFF-files
	 */
    public static final String TIFF="image/tiff";
	
	/** Constant defining the MIME type for BMP-files
	 */
    public static final String BMP="image/bmp";
	
	/** Constant defining the MIME type for EMF-files
	 */
    public static final String EMF="image/x-emf";
	
	/** Constant defining the MIME type for WMF-files
	 */
    public static final String WMF="image/x-wmf";
	
	/** Constant defining the MIME type for EPS-files
	 */
    public static final String EPS="image/x-eps";
	
	/** Constant defining the MIME type for SVG-files
	 */
    public static final String SVG="image/svg+xml";
	
	/** Constant defining the MIME type for SVM-files
	 */
    // MIME type for SVM has changed
    //public static final String SVM="image/x-svm";
    public static final String SVM="application/x-openoffice-gdimetafile;windows_formatname=\"GDIMetaFile\"";
	
	/** Constant defining the MIME type for PDF-files
	 */
    public static final String PDF="application/pdf";
	
    // Destination formats
	
	/** Constant defining the MIME type for XHTML-files
	 */    
    public static final String XHTML="text/html";
	
	/** Constant defining the MIME type for XHTML 1.1-files (This is a fake MIME type, for internal use only)
    */
    public static final String XHTML11="application/xhtml11";

	/** Constant defining the MIME type for XHTML+MathML-files
	 */    
    public static final String XHTML_MATHML="application/xhtml+xml";
	/** Constant defining the MIME type for HTML5-files (This is a fake MIME type, for internal use only)
     */
    public static final String HTML5="text/html5";

    /** Constant defining the MIME type for EPUB-files
	 */    
    public static final String EPUB="application/epub+zip";

    /** Constant defining the MIME type for EPUB 3-files (This is a fake MIME type, for internal use only)
     */
    public static final String EPUB3="epub3";
	
}