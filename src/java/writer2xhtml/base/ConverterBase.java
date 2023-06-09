/************************************************************************
 *
 *  ConverterBase.java
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
 *  Version 1.6.1 (2018-08-10)
 *
 */

package writer2xhtml.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import org.w3c.dom.Element;

import writer2xhtml.api.Converter;
import writer2xhtml.api.ConverterResult;
import writer2xhtml.api.GraphicConverter;
import writer2xhtml.api.OutputFile;
import writer2xhtml.office.EmbeddedObject;
import writer2xhtml.office.MetaData;
import writer2xhtml.office.OfficeDocument;
import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Misc;

/**<p>Abstract base implementation of <code>writer2xhtml.api.Converter</code></p>
 */
public abstract class ConverterBase implements Converter {
	
	public enum TexMathsStyle {inline, display, latex};

    // Helper	
    protected GraphicConverter graphicConverter;

    // The source document
    protected OfficeDocument odDoc;
    protected OfficeReader ofr;
    protected MetaData metaData;
    protected ImageConverter imageConverter;

    // The output file(s)
    protected String sTargetFileName;
    protected ConverterResultImpl converterResult;
    
    // Constructor
    public ConverterBase() {
        graphicConverter = null;
        converterResult = new ConverterResultImpl();
    }
	
    // Implement the interface
    public void setGraphicConverter(GraphicConverter graphicConverter) {
        this.graphicConverter = graphicConverter;
    }
	
    // Provide a do noting fallback method
    public void readTemplate(InputStream is) throws IOException { }
	
    // Provide a do noting fallback method
    public void readTemplate(File file) throws IOException { }

    // Provide a do noting fallback method
    public void readStyleSheet(InputStream is) throws IOException { }
	
    // Provide a do noting fallback method
    public void readStyleSheet(File file) throws IOException { }

    // Provide a do noting fallback method
    public void readResource(InputStream is, String sFileName, String sMediaType) throws IOException { }
    
    // Provide a do noting fallback method
    public void readResource(File file, String sFileName, String sMediaType) throws IOException { }

    public ConverterResult convert(File source, String sTargetFileName) throws FileNotFoundException,IOException {
        return convert(new FileInputStream(source), sTargetFileName);
    }

    public ConverterResult convert(InputStream is, String sTargetFileName) throws IOException {
        // Read document
        odDoc = new OfficeDocument();
        odDoc.read(is);
        return convert(sTargetFileName,true);
    }
    
    public ConverterResult convert(org.w3c.dom.Document dom, String sTargetFileName, boolean bDestructive) throws IOException {
    	// Read document
    	odDoc = new OfficeDocument();
    	odDoc.read(dom);
    	return convert(sTargetFileName,bDestructive);
    }
    
    private ConverterResult convert(String sTargetFileName, boolean bDestructive) throws IOException {
        ofr = new OfficeReader(odDoc,false,bDestructive);
        metaData = new MetaData(odDoc);
        imageConverter = new ImageConverter(ofr,bDestructive,true);
        imageConverter.setGraphicConverter(graphicConverter);

        // Prepare output
        this.sTargetFileName = sTargetFileName;
        converterResult.reset();
        
        converterResult.setMetaData(metaData);
        if (metaData.getLanguage()==null || metaData.getLanguage().length()==0) {
        	metaData.setLanguage(ofr.getMajorityLanguage());
        }
		
        convertInner();
        
        return converterResult;
    }
	
    // The subclass must provide the implementation
    public abstract void convertInner() throws IOException;

    public MetaData getMetaData() { return metaData; }
    
    public ImageConverter getImageCv() { return imageConverter; }
	
    public void addDocument(OutputFile doc) { converterResult.addDocument(doc); }
	
    public EmbeddedObject getEmbeddedObject(String sHref) {
        return odDoc.getEmbeddedObject(sHref);
    }
    
    /** Get a TexMaths equation from a draw:frame (PNG formula) or draw:g element (SVG)
     *  Such an element is a TexMaths equation if it contains an svg:title element with content "TexMaths"
     *  The actual formula is the content of an svg:desc element
     * 
     * @param node the draw:frame or draw:g element to check
     * @return the TexMaths equation, or null if this is not a TexMaths equation
     */
    public Element getTexMathsEquation(Element node) {
    	Element svgTitle = Misc.getChildByTagName(node, XMLString.SVG_TITLE);
    	if (svgTitle!=null && "TexMaths".equals(Misc.getPCDATA(svgTitle))) {
    		return Misc.getChildByTagName(node, XMLString.SVG_DESC);
    	}
    	return null;
    }
        
    public TexMathsStyle getTexMathsStyle(String s) {
   		String[] sContent = s.split("\u00a7");
   		if (sContent.length>=3) { // we only need 3 items of 6
   			if ("display".equals(sContent[1])) {
   				return TexMathsStyle.display;
   			}
   			else if ("latex".equals(sContent[1]) || "text".equals(sContent[1])) { // text is for OOoLaTeX
   				return TexMathsStyle.latex;
   			}
   		}
   		return TexMathsStyle.inline;
    }
    
    public String getTexMathsEquation(String s) {
   		String[] sContent = s.split("\u00a7");
   		if (sContent.length>=3) { // we only need 3 items of 6
   			return sContent[2];
   		}
   		else {
   			return "";
   		}
    }

}