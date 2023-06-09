/************************************************************************
*
*  ConverterResultImpl.java
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
*  Version 1.7 (2022-06-24)
*
*/ 

package writer2xhtml.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import writer2xhtml.api.ContentEntry;
import writer2xhtml.api.ConverterResult;
import writer2xhtml.api.MetaData;
import writer2xhtml.api.OutputFile;

import java.util.Iterator;

/** <code>ConverterResultImpl</code> is a straightforward implementation of <code>ConverterResult</code>
 */
public class ConverterResultImpl implements ConverterResult {
	
	private List<OutputFile> files;
	
	private List<ContentEntry> content; 
	private ContentEntry titlePageFile;
	private ContentEntry textFile;
	private ContentEntry tocFile;
	private ContentEntry lofFile;
	private ContentEntry lotFile;
	private ContentEntry indexFile;
	private ContentEntry bibliographyFile;
	private ContentEntry coverFile;
	private ContentEntry coverImageFile;
	private List<ContentEntry> originalPageNumbers; 
	
	private MetaData metaData = null;
	
	private int nMasterCount;
	
	/** Construct a new <code>ConverterResultImpl</code> with empty content
	 */
	public ConverterResultImpl() {
		reset();
	}
	
    /** Resets all data.  This empties all <code>OutputFile</code> and <code>ContentEntry</code> objects
     *  objects from this class.  This allows reuse of a <code>ConvertResult</code> object.
     */
    public void reset() {
        files = new Vector<>();
        content = new Vector<>();
        titlePageFile = null;
        textFile = null;
        tocFile = null;
        lofFile = null;
        lotFile = null;
        indexFile = null;
        bibliographyFile = null;
        coverImageFile = null;
        originalPageNumbers = new Vector<ContentEntry>();
        metaData = null;
        nMasterCount = 0;
	}

    /** Adds an <code>OutputFile</code> to the list
     *
     *  @param  file  The <code>OutputFile</code> to add.
     */
    public void addDocument(OutputFile file) {
    	if (file.isMasterDocument()) {
    		files.add(nMasterCount++, file);
    	}
    	else {
    		files.add(file);
    	}
	}
		
    /** Get the first master document
     * 
     *  @return <code>OutputFile</code> the master document
     */
    public OutputFile getMasterDocument() {
        return files.size()>0 ? files.get(0) : null;
    }
	
    /**
     *  Gets an <code>Iterator</code> to access the <code>List</code>
     *  of <code>OutputFile</code> objects
     *
     *  @return  The <code>Iterator</code> to access the
     *           <code>List</code> of <code>OutputFile</code> objects.
     */
    public Iterator<OutputFile> iterator() {
        return files.iterator();
	}

    /** Add an entry to the "external" table of contents
     * 
     *  @param entry the entry to add
     */
    public void addContentEntry(ContentEntry entry) {
    	content.add(entry);
    }
    
    public List<ContentEntry> getContent() {
    	return Collections.unmodifiableList(content);
    }
    
    /** Define the entry which contains the title page
     * 
     * @param entry the entry
     */
    public void setTitlePageFile(ContentEntry entry) {
    	titlePageFile = entry;
    }
    
    public ContentEntry getTitlePageFile() {
    	return titlePageFile;
    }
    
    /** Define the entry which contains the main text file
     * 
     * @param entry the entry
     */
    public void setTextFile(ContentEntry entry) {
    	textFile = entry;
    }
    
    public ContentEntry getTextFile() {
    	return textFile;
    }
    
    /** Define the entry which contains the table of contents
     * 
     * @param entry the entry
     */
    public void setTocFile(ContentEntry entry) {
    	tocFile = entry;
    }
    
    public ContentEntry getTocFile() {
    	return tocFile;
    }
    
    /** Define the entry which contains the list of figures
     * 
     * @param entry the entry
     */
    public void setLofFile(ContentEntry entry) {
    	lofFile = entry;
    }
    
    public ContentEntry getLofFile() {
    	return lofFile;
    }
    
    /** Define the entry which contains the list of tables
     * 
     * @param entry the entry
     */
    public void setLotFile(ContentEntry entry) {
    	lotFile = entry;
    }
    
    public ContentEntry getLotFile() {
    	return lotFile;
    }
    
    /** Define the entry which contains the alphabetical index
     * 
     * @param entry the entry
     */
    public void setIndexFile(ContentEntry entry) {
    	indexFile = entry;
    }
    
    public ContentEntry getIndexFile() {
    	return indexFile;
    }
    
    /** Define the entry which contains the bibliography
     * 
     * @param entry the entry
     */
    public void setBibliographyFile(ContentEntry entry) {
    	bibliographyFile = entry;
    }
    
    public ContentEntry getBibliographyFile() {
    	return bibliographyFile;
    }
    
    /** Define the entry which contains the cover
     * 
     * @param entry the entry
     */
    public void setCoverFile(ContentEntry entry) {
    	coverFile = entry;
    }
    
    public ContentEntry getCoverFile() {
    	return coverFile;
    }

    /** Define the entry which contains the cover image
     * 
     * @param entry the entry
     */
    public void setCoverImageFile(ContentEntry entry) {
    	coverImageFile = entry;
    }
    
    public ContentEntry getCoverImageFile() {
    	return coverImageFile;
    }
    
    /** Add an entry to the table of original page numbers
     * 
     *  @param entry the entry to add
     */
    public void addOriginalPageNumber(ContentEntry entry) {
    	originalPageNumbers.add(entry);
    }
    
    /** Remove the last entry from the table of original page numbers, if any
     */
    public void removeOriginalPageNumber() {
    	if (!originalPageNumbers.isEmpty()) {
    		originalPageNumbers.remove(originalPageNumbers.size()-1);
    	}
    }
    
    public List<ContentEntry> getOriginalPageNumbers() {
    	return Collections.unmodifiableList(originalPageNumbers);
    }
    
    /** Set the meta data of this <code>ConverterResult</code> 
     * 
     * @param metaData the meta data
     */
	public void setMetaData(MetaData metaData) {
		this.metaData = metaData;
	}
	
	/** Get the meta data of this <code>ConverterResult</code>
	 *  
	 *  @return the meta data
	 */
    public MetaData getMetaData() {
		return metaData;
	}
    
    /** Write all files to a given directory
     * 
     *  @param dir the directory to use
     */
    public void write(File dir) throws IOException {
        if (dir!=null && !dir.exists()) throw new IOException("Directory does not exist");
        Iterator<OutputFile> docEnum = iterator();
        while (docEnum.hasNext()) {
            OutputFile docOut = docEnum.next();
            String sDirName = "";
            String sFileName = docOut.getFileName();
            File subdir = dir;
            int nSlash = sFileName.indexOf("/");
            if (nSlash>-1) {
                sDirName = sFileName.substring(0,nSlash);
                sFileName = sFileName.substring(nSlash+1);
                subdir = new File(dir,sDirName);
                if (!subdir.exists()) { subdir.mkdir(); }
            }
            File outfile = new File (subdir,sFileName);
            FileOutputStream fos = new FileOutputStream(outfile);
            docOut.write(fos);
            fos.flush();
            fos.close();
        }

    }
}

