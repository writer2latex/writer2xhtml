/************************************************************************
 *
 *  Misc.java
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
 *  Version 1.7 (2022-08-16)
 *
 */

package writer2xhtml.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.text.Collator;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

// This class contains some usefull, but unrelated static methods 
public class Misc{

    private final static int BUFFERSIZE = 1024;

    public static final int[] doubleIntArray(int[] array) {
        int n = array.length;
        int[] newArray = new int[2*n];
        for (int i=0; i<n; i++) { newArray[i] = array[i]; }
        return newArray;
    }
    
    // Truncate a date+time to the date only
    public static final String dateOnly(String sDate) {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    	try {
			sdf.parse(sDate);
		} catch (ParseException e) {
			// If the date cannot be parsed according to the given pattern, return the original string
			return sDate;
		}
		// Return using a default format for the given locale
		return sDate.substring(0,10);   		    	
    }
    
    public static final String formatDate(String sDate, String sLanguage, String sCountry) {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
   		Date date = null;
    	try {
			date = sdf.parse(sDate);
		} catch (ParseException e) {
			// If the date cannot be parsed according to the given pattern, return the original string
			return sDate;
		}
		// Return using a default format for the given locale
		Locale locale = sCountry!=null ? new Locale(sLanguage,sCountry) : new Locale(sLanguage);
		return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale).format(date);   		
    }
	
    public static final String int2roman(int number) {
    	assert number>0; // Only works for positive numbers!
        StringBuilder roman=new StringBuilder();
        while (number>=1000) { roman.append('m'); number-=1000; }
        if (number>=900) { roman.append("cm"); number-=900; }
        if (number>=500) { roman.append('d'); number-=500; }
        if (number>=400) { roman.append("cd"); number-=400; }
        while (number>=100) { roman.append('c'); number-=100; }
        if (number>=90) { roman.append("xc"); number-=90; }
        if (number>=50) { roman.append('l'); number-=50; }
        if (number>=40) { roman.append("xl"); number-=40; }
        while (number>=10) { roman.append('x'); number-=10; }
        if (number>=9) { roman.append("ix"); number-=9; }
        if (number>=5) { roman.append('v'); number-=5; }
        if (number>=4) { roman.append("iv"); number-=4; }
        while (number>=1) { roman.append('i'); number-=1; }
        return roman.toString();        
    }
	
    public static final String int2Roman(int number) {
        return int2roman(number).toUpperCase();
    }
	
    public static final String int2arabic(int number) {
        return Integer.toString(number);
    }
	
    public static final String int2alph(int number, boolean bLetterSync) {
    	assert number>0; // Only works for positive numbers!
    	if (bLetterSync) {
    		char[] chars = new char[(number-1)/26+1]; // Repeat the character this number of times
    		Arrays.fill(chars, (char) ((number-1) % 26+97)); // Use this character
    		return String.valueOf(chars);
    	}
    	else {
    		int n=number-1;
    		// Least significant digit is special because a is treated as zero here!
    		int m = n % 26;
    		String sNumber = Character.toString((char) (m+97));
    		n = (n-m)/26;
    		// For the more significant digits, a is treated as one!
    		while (n>0) {
    			m = n % 26; // Calculate new least significant digit
   				sNumber = ((char) (m+96))+sNumber;
    			n = (n-m)/26;
    		}
            return sNumber;
    	}
    }
	
    public static final String int2Alph(int number, boolean bLetterSync) {
        return int2alph(number,bLetterSync).toUpperCase();
    }
    
    public static final int getIntegerFromHex(String sHex, int nDefault){
        int n;
        try {
            n=Integer.parseInt(sHex,16);
        }
        catch (NumberFormatException e) {
            return nDefault;
        }
        return n;
    }
	
    /** Make a file name TeX friendly, replacing offending characters
     * 
     * @param sFileName the file name
     * @param sDefault a default name to use if no characters remains after filtering
     * @return the modified file name
     */
    public static String makeTeXFriendly(String sFileName, String sDefault) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<sFileName.length(); i++) {
            char c = sFileName.charAt(i);
            if ((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9')) {
                builder.append(c);
            }
            else {
            	switch (c) {
            	case '.': builder.append('.'); break;
            	case '-': builder.append('-'); break;
            	case ' ' : builder.append('-'); break;
            	case '_' : builder.append('-'); break;
            	// Replace accented and national characters
            	case '\u00c0' : builder.append('A'); break;
            	case '\u00c1' : builder.append('A'); break;
            	case '\u00c2' : builder.append('A'); break;
            	case '\u00c3' : builder.append('A'); break;
            	case '\u00c4' : builder.append("AE"); break;
            	case '\u00c5' : builder.append("AA"); break;
            	case '\u00c6' : builder.append("AE"); break;
            	case '\u00c7' : builder.append('C'); break;
            	case '\u00c8' : builder.append('E'); break;
            	case '\u00c9' : builder.append('E'); break;
            	case '\u00ca' : builder.append('E'); break;
            	case '\u00cb' : builder.append('E'); break;
            	case '\u00cc' : builder.append('I'); break;
            	case '\u00cd' : builder.append('I'); break;
            	case '\u00ce' : builder.append('I'); break;
            	case '\u00cf' : builder.append('I'); break;
            	case '\u00d0' : builder.append('D'); break;
            	case '\u00d1' : builder.append('N'); break;
            	case '\u00d2' : builder.append('O'); break;
            	case '\u00d3' : builder.append('O'); break;
            	case '\u00d4' : builder.append('O'); break;
            	case '\u00d5' : builder.append('O'); break;
            	case '\u00d6' : builder.append("OE"); break;
            	case '\u00d8' : builder.append("OE"); break;
            	case '\u00d9' : builder.append('U'); break;
            	case '\u00da' : builder.append('U'); break;
            	case '\u00db' : builder.append('U'); break;
            	case '\u00dc' : builder.append("UE"); break;
            	case '\u00dd' : builder.append('Y'); break;
            	case '\u00df' : builder.append("sz"); break;
            	case '\u00e0' : builder.append('a'); break;
            	case '\u00e1' : builder.append('a'); break;
            	case '\u00e2' : builder.append('a'); break;
            	case '\u00e3' : builder.append('a'); break;
            	case '\u00e4' : builder.append("ae"); break;
            	case '\u00e5' : builder.append("aa"); break;
            	case '\u00e6' : builder.append("ae"); break;
            	case '\u00e7' : builder.append('c'); break;
            	case '\u00e8' : builder.append('e'); break;
            	case '\u00e9' : builder.append('e'); break;
            	case '\u00ea' : builder.append('e'); break;
            	case '\u00eb' : builder.append('e'); break;
            	case '\u00ec' : builder.append('i'); break;
            	case '\u00ed' : builder.append('i'); break;
            	case '\u00ee' : builder.append('i'); break;
            	case '\u00ef' : builder.append('i'); break;
            	case '\u00f0' : builder.append('d'); break;
            	case '\u00f1' : builder.append('n'); break;
            	case '\u00f2' : builder.append('o'); break;
            	case '\u00f3' : builder.append('o'); break;
            	case '\u00f4' : builder.append('o'); break;
            	case '\u00f5' : builder.append('o'); break;
            	case '\u00f6' : builder.append("oe"); break;
            	case '\u00f8' : builder.append("oe"); break;
            	case '\u00f9' : builder.append('u'); break;
            	case '\u00fa' : builder.append('u'); break;
            	case '\u00fb' : builder.append('u'); break;
            	case '\u00fc' : builder.append("ue"); break;
            	case '\u00fd' : builder.append('y'); break;
            	case '\u00ff' : builder.append('y'); break;
            	}
            }
        }
        return builder.length()>0 ? builder.toString() : sDefault; 
    }

    /*
     * Utility method to make sure the document name is stripped of any file
     * extensions before use.
     * (this is copied verbatim from PocketWord.java in xmerge)
     */
    public static final String trimDocumentName(String name,String extension) {
        String temp = name.toLowerCase();
        
        if (temp.endsWith(extension)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - extension.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }
    
    /** Get the path part of an URL
     * 
     * @param sURL the URL from which the filename should be extracted
     * @return the file name
     */
    public static final String getPath(String sURL) {
    	return sURL.substring(0,sURL.lastIndexOf('/')+1);
    }
    
    /** Get the file name part of an URL
     * 
     * @param sURL the URL from which the filename should be extracted
     * @return the file name
     */
    public static final String getFileName(String sURL) {
    	return sURL.substring(sURL.lastIndexOf('/')+1);
    }
    
    /** Get the file extension from an URL
     * 
     * @param sURL
     * @return the file extension (including dot) or the empty string if there is no file extension
     */
    public static final String getFileExtension(String sURL) {
    	String sFileName = getFileName(sURL);
    	int nDot = sFileName.lastIndexOf('.');
    	if (nDot>=0) {
    		return sFileName.substring(nDot);
    	}
    	else {
    		return "";
    	}
    }
	
    public static final String removeExtension(String sURL) {
    	String sExt = getFileExtension(sURL);
    	return sURL.substring(0, sURL.length()-sExt.length());
    }
	
     /*
     * Utility method to retrieve a Node attribute or null
     */
    public static final String getAttribute (Node node, String attribute) {
        NamedNodeMap attrNodes = node.getAttributes();
        
        if (attrNodes != null) {
            Node attr = attrNodes.getNamedItem(attribute);
            if (attr != null) {
                return attr.getNodeValue();
            }
        }
        
        return null;
    }
	
    public static final boolean isElement(Node node) {
        return node.getNodeType()==Node.ELEMENT_NODE;
    }
	
    /* Utility method to determine if a Node is a specific Element
     */
    public static final boolean isElement(Node node, String sTagName) {
        return node.getNodeType()==Node.ELEMENT_NODE
            && node.getNodeName().equals(sTagName);
    }
	
    public static final boolean isText(Node node) {
        return node.getNodeType()==Node.TEXT_NODE;
    }
	
     /*
     * Utility method to retrieve an element attribute or null
     */
    public static final String getAttribute (Element node, String attribute) {
        if (node.hasAttribute(attribute)) { return node.getAttribute(attribute); }
        else { return null; }
    }

    /* utility method to get the first child with a given tagname */
    public static final Element getChildByTagName(Node node, String sTagName){
        if (node.hasChildNodes()){
            NodeList nl=node.getChildNodes();
            int nLen=nl.getLength();
            for (int i=0; i<nLen; i++){
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNodeName().equals(sTagName)){
                    return (Element) child;
                }
            }
        }
        return null;
    }
	
    /* utility method to get the first <em>element</em> child of a node*/
    public static final Element getFirstChildElement(Node node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }
	
    /* utility method that collects PCDATA content of an element */
    public static String getPCDATA(Node node) {
        StringBuilder buf = new StringBuilder();
        if (node.hasChildNodes()) {
            NodeList nl = node.getChildNodes();
            int nLen = nl.getLength();
            for (int i=0; i<nLen; i++) {
                if (nl.item(i).getNodeType()==Node.TEXT_NODE) {
                    buf.append(nl.item(i).getNodeValue());
                }
            }
        }
        return buf.toString();
    }
    
    // Utility method to return a sorted string array based on a set
    public static String[] sortStringSet(Set<String> theSet) {
    	String[] theArray = theSet.toArray(new String[theSet.size()]);
		Collator collator = Collator.getInstance();
		Arrays.sort(theArray, collator);
    	return theArray;
    }

    
    /* Utility method that url encodes a string */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s,"UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /* Utility method that url decodes a string */
    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s,"UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /* utility method to make a file name valid for a href attribute
       (ie. replace spaces with %20 etc.)
     */
    public static String makeHref(String s) {
        try {
            URI uri = new URI(null, null, s, null);
            return uri.toString();	    
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }
    
    /* utility method to convert a *relative* URL to a file name
    (ie. replace %20 with spaces etc.)
     */
    public static String makeFileName(String sURL) {
    	try {
    		File file = new File(new java.net.URI("file:///"+sURL));
    		return file.getName();	    
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	return "error";
    }
 
    public static File urlToFile(String sUrl) {
        try {
            return new File(new URI(sUrl));
        }
        catch (URISyntaxException e) {
            return new File(".");
        }
    }
	
    /** <p>Read an <code>InputStream</code> into a <code>byte</code>array</p>
     *  @param is   the <code>InputStream</code> to read
     *  @return     a byte array with the contents read from the stream
     *  @throws     IOException  in case of any I/O errors.
     */
    public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
        if (is==null) {
            throw new IOException ("No input stream to read");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int nLen = 0;
        byte buffer[] = new byte[BUFFERSIZE];
        while ((nLen = is.read(buffer)) > 0) {
            baos.write(buffer, 0, nLen);
        }
        return baos.toByteArray();
    }

	public static final int getPosInteger(String sInteger, int nDefault){
	    int n;
	    try {
	        n=Integer.parseInt(sInteger);
	    }
	    catch (NumberFormatException e) {
	        return nDefault;
	    }
	    return n>0 ? n : nDefault;
	}
	


}
