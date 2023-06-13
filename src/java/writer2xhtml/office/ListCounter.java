/************************************************************************
 *
 *  ListCounter.java
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
 *  Copyright: 2002-2023 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.7 (2023-06-23)
 *
 */

package writer2xhtml.office;

import writer2xhtml.util.*;

/**
 * This class produces labels for ODF lists/outlines 
 *
 */
public class ListCounter {
    private int[] nCounter = new int[11];
    private String[] sNumFormat = new String[11];
    private int[] nStartValue = new int[11];
    private ListStyle style;
    private int nLevel=1; // current level
	
    public ListCounter() {
        // Create a dummy counter
        this.style = null;
        for (int i=1; i<=10; i++) {
            sNumFormat[i] = null;
        }
    }
	
    public ListCounter(ListStyle style) {
        this();
        if (style!=null) {
            this.style = style;
            for (int i=1; i<=10; i++) {
                sNumFormat[i] = style.getLevelProperty(i,XMLString.STYLE_NUM_FORMAT);
                nStartValue[i] = Misc.getPosInteger(style.getLevelProperty(i, XMLString.TEXT_START_VALUE),1);
            }
        }
        restart(1);
    }
	
    public ListCounter step(int nLevel) {
        // Make sure no higher levels are zero
        // This means that unlike eg. LaTeX, step(1).step(3) does not create
        // the value 1.0.1 but rather 1.1.1
        for (int i=1; i<nLevel; i++) {
            if (nCounter[i]==0) { nCounter[i]=1; }
        }
        // Then step this level
        nCounter[nLevel]++;
        // Finally clear lower levels
        if (nLevel<10) { restart(nLevel+1); }
        this.nLevel = nLevel;
        return this;
    }
	
    public ListCounter restart(int nLevel) {
        restart(nLevel,nStartValue[nLevel]-1);
        return this;
    }
	
    public ListCounter restart(int nLevel, int nValue) {
        nCounter[nLevel] = nValue;
        for (int i=nLevel+1; i<=10; i++) {
            nCounter[i] = 0;
        }
        return this;
    }
	
    public int getValue(int nLevel) {
        return nCounter[nLevel];
    }
	
    public int[] getValues() {
        int[] nCounterSnapshot = new int[11];
        System.arraycopy(nCounter,0,nCounterSnapshot,0,11);
        return nCounterSnapshot;
    }
	
    public String getLabel() {
		return getPrefix()+getLabelAndSuffix(); 
    }
    
    public String getPrefix() {
    	if (style.isNumber(nLevel)) {
    		String sPrefix = style.getLevelProperty(nLevel,XMLString.STYLE_NUM_PREFIX);
    		return sPrefix!=null ? sPrefix : "";
    	}
    	return "";
    }
    
    public String getLabelAndSuffix() {
    	if (style.isNumber(nLevel)) {
    		String sLabel="";
    		if (sNumFormat[nLevel]==null) return "";
    		int nLevels = Misc.getPosInteger(style.getLevelProperty(nLevel,
    				XMLString.TEXT_DISPLAY_LEVELS),1);
    		String sSuffix = style.getLevelProperty(nLevel,XMLString.STYLE_NUM_SUFFIX);
    		String sSpace = "nothing".equals(style.getLevelStyleProperty(nLevel, XMLString.TEXT_LABEL_FOLLOWED_BY)) ? "" : " ";
    		for (int j=nLevel-nLevels+1; j<nLevel; j++) {
    			String sFormat = formatNumber(nCounter[j],sNumFormat[j],true); 
    			sLabel+=sFormat+(sFormat.length()>0?".":"");
    		}
    		// TODO: Lettersync
    		sLabel+=formatNumber(nCounter[nLevel],sNumFormat[nLevel],true);
    		if (sSuffix!=null) { sLabel+=sSuffix; }
    		if (sLabel.length()>0 && sSpace!=null) { sLabel+=sSpace; }
    		return sLabel;
    	}
    	else if (style.isBullet(nLevel)) {
    		return  style.getLevelProperty(nLevel,XMLString.TEXT_BULLET_CHAR);
    	}
    	else {
    		return "";
    	}    	
    }
	
    // Utility method to generate number
    public static String formatNumber(int number,String sStyle,boolean bLetterSync) {
        if ("a".equals(sStyle)) { return Misc.int2alph(number,bLetterSync); }
        else if ("A".equals(sStyle)) { return Misc.int2Alph(number,bLetterSync); }
        else if ("i".equals(sStyle)) { return Misc.int2roman(number); }
        else if ("I".equals(sStyle)) { return Misc.int2Roman(number); }
        else if ("1".equals(sStyle)) { return Misc.int2arabic(number); }
        else { return ""; }
    }


}
