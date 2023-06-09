/************************************************************************
 *
 *  EPUBWriter.java
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
 *  version 1.7 (2022-06-23)
 *
 */

package writer2xhtml.epub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import writer2xhtml.api.ConverterResult;
import writer2xhtml.api.OutputFile;
import writer2xhtml.util.Misc;
import writer2xhtml.xhtml.XhtmlConfig;

/** This class repackages an XHTML document into EPUB format.
 *  Some filenames are hard wired in this implementation: The main directory is OEBPS and
 *  the OPF and NCX files are book.opf and book.ncx respectively; finally the EPUB 3 navigation
 *  document is nav.xhtml 
 */
public class EPUBWriter implements OutputFile {
	
	private static final byte[] mimeBytes = { 'a', 'p', 'p', 'l', 'i', 'c', 'a', 't', 'i', 'o', 'n', '/',
		'e', 'p', 'u', 'b', '+', 'z', 'i', 'p'};
	
	private ConverterResult xhtmlResult;
	private String sFileName;
	private int nVersion;
	private XhtmlConfig config;
	
	/** Create a new <code>EPUBWriter</code> based on a <code>ConverterResult</code>.
	 * 
	 * @param xhtmlResult the converter result to repackage
	 * @param sFileName the file name to use for the result
	 * @param nVersion the EPUB version number. Can be either 2 or 3, other values are interpreted as 2.
	 * @param config the configuration to use
	 */
	public EPUBWriter(ConverterResult xhtmlResult, String sFileName, int nVersion, XhtmlConfig config) {
		this.xhtmlResult = xhtmlResult;
		this.sFileName = Misc.removeExtension(sFileName);
		this.nVersion = nVersion;
		this.config = config;
	}
	
	// Implement OutputFile

	public String getFileName() {
		return sFileName+".epub";
	}

	public String getMIMEType() {
		return "application/epub+zip";
	}

	public boolean isMasterDocument() {
		return true;
	}
	
	public boolean containsMath() {
		// We don't really care about this
		return nVersion==3;
	}

	public void write(OutputStream os) throws IOException {		
		ZipOutputStream zos = new ZipOutputStream(os);
		
		// Write uncompressed MIME type as first entry
		ZipEntry mimeEntry = new ZipEntry("mimetype");
		mimeEntry.setMethod(ZipEntry.STORED);
		mimeEntry.setCrc(0x2CAB616F);
		mimeEntry.setSize(mimeBytes.length);
		zos.putNextEntry(mimeEntry);
		zos.write(mimeBytes, 0, mimeBytes.length);
		zos.closeEntry();
		
		// Write container entry next
		OutputFile containerWriter = new ContainerWriter();
		ZipEntry containerEntry = new ZipEntry("META-INF/container.xml");
		zos.putNextEntry(containerEntry);
		writeZipEntry(containerWriter,zos);
		zos.closeEntry();
		
		// Then manifest
		OPFWriter manifest = new OPFWriter(xhtmlResult,sFileName,nVersion,config);
		ZipEntry manifestEntry = new ZipEntry("OEBPS/book.opf");
		zos.putNextEntry(manifestEntry);
		writeZipEntry(manifest,zos);
		zos.closeEntry();
		
		// And content table
		if (nVersion==3) {
			OutputFile navigation = new NavigationWriter(xhtmlResult, config.originalPageNumbers());
			ZipEntry navigationEntry = new ZipEntry("OEBPS/nav.xhtml");
			zos.putNextEntry(navigationEntry);
			writeZipEntry(navigation,zos);
			zos.closeEntry();
		}
		if (nVersion!=3 || config.includeNCX()) {
			OutputFile ncx = new NCXWriter(xhtmlResult, manifest.getUid());
			ZipEntry ncxEntry = new ZipEntry("OEBPS/book.ncx");
			zos.putNextEntry(ncxEntry);
			writeZipEntry(ncx,zos);
			zos.closeEntry();
		}
		
		// Finally XHTML content
		Iterator<OutputFile> iter = xhtmlResult.iterator();
		while (iter.hasNext()) {
			OutputFile file = iter.next();
			ZipEntry entry = new ZipEntry("OEBPS/"+file.getFileName());
			zos.putNextEntry(entry);
			writeZipEntry(file, zos);
			zos.closeEntry();
		}
		
		zos.close();
	}
	
	private void writeZipEntry(OutputFile file, ZipOutputStream zos) throws IOException {
		// Unfortunately we cannot simply do file.write(zos) because the write method of OutputFile
		// closes the OutputStream. Hence this suboptimal solution
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		file.write(baos);
		byte[] content = baos.toByteArray();
		zos.write(content, 0, content.length);
	}

}
