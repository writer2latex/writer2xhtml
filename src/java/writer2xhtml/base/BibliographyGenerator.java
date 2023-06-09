/************************************************************************
 *
 *  BibliographyGenerator.java
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
 *  Version 1.6 (2018-03-06)
 *
 */

package writer2xhtml.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Misc;
import writer2xhtml.util.StringComparator;

/** This class is used to generate bibliographic references and a bibliography.
 *  Bibliographies are generated from a list of items (text:bibliograpy-mark), a global configuration
 *  (text:bibliography-configuration) and a formatting template (text:bibliography-source)
 */
public abstract class BibliographyGenerator {
	
	// Bibliography configuration data
	private String sPrefix = "[";
	private String sSuffix = "]";
	
	// The sorted list of bibliography marks
	private List<Element> bibMarkList = new ArrayList<Element>();
	
	// Map from key to label
	private Map<String,String> bibMarkLabel = new HashMap<String,String>();
	
	// Flag to identify numbering
	private boolean bNumberedEntries = false;
	
	// Flag to identify truncation of templates
	private boolean bSkipKey = false;
		
	/** Create a new bibliography generator based on a bibliography configuration and a list of bibliography marks
	 * 
	 *  @param ofr the office reader used to access the source document
	 *  @param bSkipKey set to true if the key should be excluded when applying templates
	 */
	protected BibliographyGenerator(OfficeReader ofr, boolean bSkipKey) {
		this.bSkipKey = bSkipKey;
		
		Element bibConfig = ofr.getBibliographyConfiguration();
		if (bibConfig!=null) {
			if (bibConfig.hasAttribute(XMLString.TEXT_PREFIX)) {
				sPrefix = bibConfig.getAttribute(XMLString.TEXT_PREFIX);
			}
			if (bibConfig.hasAttribute(XMLString.TEXT_SUFFIX)) {
				sSuffix = bibConfig.getAttribute(XMLString.TEXT_SUFFIX);
			}
		}

		collectBibMarks(ofr.getBibliographyMarks());
		sortBibMarks(bibConfig);
		createLabels(bibConfig);
	}
	
	// Collect the bibliography marks from the raw list, removing any duplicates
	private void collectBibMarks(List<Element> bibMarks) {
		Set<String> keys = new HashSet<String>();		
		for (Element bibMark : bibMarks) {
			String sKey = bibMark.getAttribute(XMLString.TEXT_IDENTIFIER);
			if (!keys.contains(sKey)) {
				bibMarkList.add(bibMark);
				keys.add(sKey);
			}
		}
	}
	
	// Sort the bibliography marks based on the settings in the bibliography configuration
	private void sortBibMarks(Element bibConfig) {
		if (bibConfig!=null && "false".equals(bibConfig.getAttribute(XMLString.TEXT_SORT_BY_POSITION))) {
			// Get the sort algorithm
			//String sSortAlgorithm = "alphanumeric";
			//if (bibConfig.hasAttribute(XMLString.TEXT_SORT_ALGORITHM)) {
			//	sSortAlgorithm = bibConfig.getAttribute(XMLString.TEXT_SORT_ALGORITHM);
			//}

			// Get the sort keys
			List<String> sortKeys = new ArrayList<String>();
			List<Boolean> sortAscending = new ArrayList<Boolean>(); 
			Node child = bibConfig.getFirstChild();
			while (child!=null) {
				if (child.getNodeType()==Node.ELEMENT_NODE && child.getNodeName().equals(XMLString.TEXT_SORT_KEY)) {
					String sKey = Misc.getAttribute(child, XMLString.TEXT_KEY);
					if (sKey!=null) {
						sortKeys.add(sKey);
						sortAscending.add(!"false".equals(Misc.getAttribute(child, XMLString.TEXT_SORT_ASCENDING)));
					}
				}
				child = child.getNextSibling();
			}
			
			// Sort the list
			Comparator<Element> comparator = new StringComparator<Element>(
					Misc.getAttribute(bibConfig,XMLString.FO_LANGUAGE),
	        		Misc.getAttribute(bibConfig, XMLString.FO_COUNTRY)) {
				private List<String> sortKeys = null;
				private List<Boolean> sortAscending = null;
				
				Comparator<Element> setSortKeys(List<String> sortKeys, List<Boolean> sortAscending) {
					this.sortKeys = sortKeys;
					this.sortAscending = sortAscending;
					return this;
				}
				
				public int compare(Element a, Element b) {
					int nCount = sortKeys.size();
					for (int i=0; i<nCount; i++) {
						String sWorda = a.getAttribute("text:"+sortKeys.get(i));
						String sWordb = b.getAttribute("text:"+sortKeys.get(i));
						int nCompare = getCollator().compare(sWorda, sWordb)*(sortAscending.get(i) ? 1 : -1);
						if (nCompare!=0) { return nCompare; }
					}
					return 0;
				}
			}.setSortKeys(sortKeys, sortAscending);
			
			Collections.sort(bibMarkList, comparator);
		}
	}
	
	private void createLabels(Element bibConfig) {
		bNumberedEntries = bibConfig!=null && "true".equals(bibConfig.getAttribute(XMLString.TEXT_NUMBERED_ENTRIES));
		int nCount = bibMarkList.size();
		for (int i=0; i<nCount; i++) {
			Element item = bibMarkList.get(i);
			String sKey = item.getAttribute(XMLString.TEXT_IDENTIFIER);
 			if (bNumberedEntries) {
				bibMarkLabel.put(sKey, Integer.toString(i+1));
			}
			else {
				bibMarkLabel.put(sKey, sKey);
			}	
		}		
	}
	
	/** Get all labels used in the bibliography
	 * 
	 * @return the set of labels
	 */
	protected Collection<String> getLabels() {
		return bibMarkLabel.values();
	}
	
	/** Check whether entries are numbered rather than labeled with the key
	 * 
	 *  @return true if the entries are numbered
	 */
	protected boolean isNumberedEntries() {
		return bNumberedEntries;
	}
	
	/** Get citation text for a reference to the bibliography
	 * 
	 * @param sKey the key of the bibliography item
	 * @return the citation text to be shown in the document
	 */
	public String generateCitation(String sKey) {
		return sPrefix+bibMarkLabel.get(sKey)+sSuffix;
	}
	
	/** Generate a bibliography
	 * 
	 * @param bibSource a text:bibliography-source element
	 */
	protected void generateBibliography(Element bibSource) {
		Map<String,Element> bibEntryTemplate = collectTemplates(bibSource);
		for (Element bibMark : bibMarkList) {
			String sKey = bibMark.getAttribute(XMLString.TEXT_IDENTIFIER);
			String sType = bibMark.getAttribute(XMLString.TEXT_BIBLIOGRAPHY_TYPE);
			if (bibEntryTemplate.containsKey(sType)) {
				Element template = bibEntryTemplate.get(sType);
				String sStyleName = template.getAttribute(XMLString.TEXT_STYLE_NAME);
				insertBibliographyItem(sStyleName,sKey);
				applyTemplate(template,bibMark);
			}
			else { // Use a default template (identical with the default template in LO)
				String sAuthor = bibMark.getAttribute(XMLString.TEXT_AUTHOR);
				String sTitle = bibMark.getAttribute(XMLString.TEXT_TITLE);
				String sYear = bibMark.getAttribute(XMLString.TEXT_YEAR);
				insertBibliographyItem(null,sKey);
				if (!bSkipKey) {
					insertBibliographyItemElement(null,bibMarkLabel.get(sKey));
					insertBibliographyItemElement(null,": ");
				}
				insertBibliographyItemElement(null,sAuthor);
				insertBibliographyItemElement(null,", ");
				insertBibliographyItemElement(null,sTitle);
				insertBibliographyItemElement(null,", ");
				insertBibliographyItemElement(null,sYear);
			}
		}
	}
	
	private Map<String,Element> collectTemplates(Element bibSource) {
		Map<String,Element> bibEntryTemplate = new HashMap<String,Element>(); 
		Node child = bibSource.getFirstChild();
		while (child!=null) {
			if (child.getNodeType()==Node.ELEMENT_NODE
					&& child.getNodeName().equals(XMLString.TEXT_BIBLIOGRAPHY_ENTRY_TEMPLATE)) {
				String sType = Misc.getAttribute(child, XMLString.TEXT_BIBLIOGRAPHY_TYPE);
				if (sType!=null) {
					bibEntryTemplate.put(sType, (Element)child);
				}
			}
			child = child.getNextSibling();
		}
		return bibEntryTemplate;
	}
	
	private void applyTemplate(Element template, Element bibMark) {
		boolean bSkip = bSkipKey;
		Node child = template.getFirstChild();
		while (child!=null) {
			if (child.getNodeType()==Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(XMLString.TEXT_INDEX_ENTRY_BIBLIOGRAPHY)) {
					String sField = Misc.getAttribute(child, XMLString.TEXT_BIBLIOGRAPHY_DATA_FIELD);
					if (sField!=null) {
						String sValue = bibMark.getAttribute("text:"+sField);
						if (sField.equals("identifier")) {
							sValue = bibMarkLabel.get(sValue);
						}
						else {
							bSkip = false;
						}
						if (!bSkip) {
							String sElementStyleName = Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME);
							insertBibliographyItemElement(sElementStyleName,sValue);
						}
					}
				}
				else if (child.getNodeName().equals(XMLString.TEXT_INDEX_ENTRY_SPAN)) {
					if (!bSkip) {
						String sValue = Misc.getPCDATA(child);
						String sElementStyleName = Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME);
						insertBibliographyItemElement(sElementStyleName,sValue);
					}
				}
			}
			child = child.getNextSibling();
		}
	}
	
	/** Insert a new bibliography item
	 * 
	 * @param sStyleName a paragraph style to apply to the item
	 * @param sKey the key of the bibliography item
	 */
	protected abstract void insertBibliographyItem(String sStyleName, String sKey);
	
	/** Insert an element of a bibliography item
	 * 
	 * @param sStyleName a character style to apply to the element
	 * @param sText the element text
	 */
	protected abstract void insertBibliographyItemElement(String sStyleName, String sText);

}
