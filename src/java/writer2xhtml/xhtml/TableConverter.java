/************************************************************************
 *
 *  TableConverter.java
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
 *  Version 1.7 (2022-06-16)
 *
 */

package writer2xhtml.xhtml;

import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2xhtml.office.OfficeReader;
import writer2xhtml.office.StyleWithProperties;
import writer2xhtml.office.TableRange;
import writer2xhtml.office.TableReader;
import writer2xhtml.office.TableView;
import writer2xhtml.office.XMLString;
import writer2xhtml.util.Calc;
import writer2xhtml.util.Misc;
import org.w3c.dom.Element;

public class TableConverter extends ConverterHelper {

    // The collection of all table names
    // TODO: Navigation should be handled here rather than in Converter.java
    protected Vector<String> sheetNames = new Vector<String>();
	
    public TableConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
    }
	
    /** Converts an office node as a complete table (spreadsheet) document
     *
     *  @param onode the Office node containing the content to convert
     */
    public void convertTableContent(Element onode) {
        Element hnode = null;
        if (!onode.hasChildNodes()) { return; }
        if (!config.xhtmlCalcSplit()) { hnode = nextOutFile(); }
        NodeList nList = onode.getChildNodes();
        int nLen = nList.getLength();
        for (int i=0; i<nLen; i++) {
            Node child = nList.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String sNodeName = child.getNodeName();
                if (sNodeName.equals(XMLString.TABLE_TABLE)) {
                    StyleWithProperties style = ofr.getTableStyle(
                        Misc.getAttribute(child,XMLString.TABLE_STYLE_NAME));
                    if ((config.xhtmlDisplayHiddenSheets() || style==null
                            || !"false".equals(style.getProperty(XMLString.TABLE_DISPLAY)))
                            && (!config.applyPrintRanges() || ofr.getTableReader((Element)child).getPrintRangeCount()>0)) {
                        if (config.xhtmlCalcSplit()) { hnode = nextOutFile(); }
                        // Collect name
                        String sName = Misc.getAttribute(child,XMLString.TABLE_NAME);
                        sheetNames.add(sName);

                        // Add sheet name as heading, if required
                        if (config.xhtmlUseSheetNamesAsHeadings()) {
                            Element heading = converter.createElement("h2");
                            hnode.appendChild(heading);
                            heading.setAttribute("id","tableheading"+(sheetNames.size()-1));
                            heading.appendChild(converter.createTextNode(sName));
                        }
    
                        // Handle the table
                        handleTable(child,hnode);
    
                        // Add frames belonging to this table
                        Element div = converter.createElement("div");
                        Element shapes = Misc.getChildByTagName(child,XMLString.TABLE_SHAPES);
                        if (shapes!=null) {
                            Node shape = shapes.getFirstChild();
                            while (shape!=null) {
                                if (OfficeReader.isDrawElement(shape)) {
                                    // Actually only the first parameter is used
                                    getDrawCv().handleDrawElement((Element)shape,div,null,DrawConverter.CENTERED);
                                }
                                shape = shape.getNextSibling();
                            }
                        }
                        getDrawCv().flushFrames(div);
                        if (div.hasChildNodes()) { hnode.appendChild(div); }
                    }
                }
            }
        }
    	if (converter.getOutFileIndex()<0) {
    		// No files, add an empty one (This may happen if apply_print_ranges=true
    		// and the document does not contain any print ranges)
    		nextOutFile();
    	}
    }
	
    private Element nextOutFile() {
        Element hnode = converter.nextOutFile();
        // Add title, if required by config
        if (config.xhtmlUseTitleAsHeading()) {
            String sTitle = converter.getMetaData().getTitle();
            if (sTitle!=null) {
                Element title = converter.createElement("h1");
                hnode.appendChild(title);
                title.appendChild(converter.createTextNode(sTitle));
            }
        }
        return hnode;
    }
	
    /** Process a table:table tag 
     * 
     *  @param onode the Office node containing the table element 
     *  @param hnode the XHTML node to which the table should be attached
     */
    public void handleTable(Node onode, Node hnode) {
        TableReader tblr = ofr.getTableReader((Element)onode);
        if (config.applyPrintRanges()) {
            if (tblr.getPrintRangeCount()>0) {
                Element div = converter.createElement("div");
                if (!tblr.isSubTable()) {
                    converter.addTarget(div,tblr.getTableName()+"|table");
                }
                hnode.appendChild(div);
                int nCount = tblr.getPrintRangeCount();
                for (int nRange=0; nRange<nCount; nRange++) {
                    Element table = converter.createElement("table");
                    div.appendChild(table);
                    TableRange range = tblr.getPrintRange(nRange);
                    range.setIncludeHidden(config.displayHiddenRowsCols());
                    range.setIncludeFiltered(config.displayFilteredRowsCols());
                    traverseTable(tblr, range.createTableView(), table);
                }
            }
        }
        else {
            // Create table
            Element table = converter.createElement("table");
            if (!tblr.isSubTable()) {
                converter.addTarget(table,tblr.getTableName()+"|table");
            }
            hnode.appendChild(table);

            // Create view (full table)
            TableRange range = new TableRange(tblr);
            if (ofr.isSpreadsheet()) {
                // skip trailing empty rows and columns
                range.setLastRow(tblr.getMaxRowCount()-1);
                range.setLastCol(tblr.getMaxColCount()-1);	
            }
            range.setIncludeHidden(config.displayHiddenRowsCols());
            range.setIncludeFiltered(config.displayFilteredRowsCols());
            traverseTable(tblr, range.createTableView(),table);
        }
    }
    
    private void traverseTable(TableReader tblr, TableView view, Element hnode) {
        int nRowCount = view.getRowCount();
        int nColCount = view.getColCount();
        
        // Style the table
        StyleInfo info = new StyleInfo();
        applyTableStyle(tblr.getTableStyleName(), info, tblr.isSubTable());        
        if (ofr.isSpreadsheet()) { // For spreadsheets we need a fixed layout
	        String sTotalWidth = "0";
			for (int nCol=0; nCol<nColCount; nCol++) {
				String sColWidth = view.getColumnWidth(nCol);
				if (sColWidth!=null) {
					sTotalWidth = Calc.add(sColWidth, sTotalWidth);
				}
			}
			info.props.addValue("width", this.colScale(sTotalWidth));
			info.props.addValue("table-layout", "fixed");
        }
        applyStyle(info, hnode);
        // Older versions of IE needs the cellspacing attribute, as it doesn't understand the css border-spacing attribute
        // We cannot do this with HTML5
        if (!converter.isHTML5()) {
        	hnode.setAttribute("cellspacing","0");
        }
        
        // Create columns
        if (config.tableSize()!=XhtmlConfig.NONE) {
    		Element colgroup = hnode;
    		if (converter.isHTML5()) {
    			// Polyglot HTML5 documents must use an explicit colgroup
    			colgroup = converter.createElement("colgroup");
    			hnode.appendChild(colgroup);
    		}
    		if (view.getRelTableWidth()!=null || config.tableSize()==XhtmlConfig.RELATIVE) {
    			// Relative column width as required by the source document or the configuration
    			for (int nCol=0; nCol<nColCount; nCol++) {
    				Element col = converter.createElement("col");
    				colgroup.appendChild(col);
    				col.setAttribute("style","width:"+view.getRelColumnWidth(nCol));
    			}
    		}
    		else {
    			// Absolute column width
    			for (int nCol=0; nCol<nColCount; nCol++) {
    				Element col = converter.createElement("col");
    				colgroup.appendChild(col);
    				col.setAttribute("style","width:"+colScale(view.getColumnWidth(nCol)));
    			}
    		}
        }

        // Indentify head
        int nBodyStart = 0;
        while (nBodyStart<nRowCount && view.getRow(nBodyStart).isHeader()) {
            nBodyStart++;
        }
        if (nBodyStart==0 || nBodyStart==nRowCount) {
            // all body or all head
        	Element tbody = hnode;
        	if (converter.isHTML5()) {
        		// Polyglot HTML5 documents must use an explicit tbody
        		tbody = converter.createElement("tbody");
        		hnode.appendChild(tbody);
        	}
            traverseRows(view,0,nRowCount,tbody);
        }
        else {
            // Create thead
            Element thead = converter.createElement("thead");
            hnode.appendChild(thead);
            traverseRows(view,0,nBodyStart,thead);
            // Create tbody
            Element tbody = converter.createElement("tbody");
            hnode.appendChild(tbody);
            traverseRows(view,nBodyStart,nRowCount,tbody);
        }
       
    }
    
    private void traverseRows(TableView view, int nFirstRow, int nLastRow, Element hnode) {
        for (int nRow=nFirstRow; nRow<nLastRow; nRow++) {
            // Create row and apply row style
            Element tr = converter.createElement("tr");
            hnode.appendChild(tr);
            applyRowStyle(view.getRow(nRow).getStyleName(),tr);

            for (int nCol=0; nCol<view.getColCount(); nCol++) {
                Node cell = view.getCell(nRow,nCol);
                if (cell!=null) {
                	if (XMLString.TABLE_TABLE_CELL.equals(cell.getNodeName())) {
                		// Create cell
                		Element td = converter.createElement("td");
                		tr.appendChild(td);
                		int nRowSpan = view.getRowSpan(nRow,nCol);
                		if (nRowSpan>1) {
                			td.setAttribute("rowspan",Integer.toString(nRowSpan));
                		}
                		int nColSpan = view.getColSpan(nRow,nCol);
                		if (nColSpan>1) {
                			td.setAttribute("colspan",Integer.toString(nColSpan));
                		}

                		// Handle content
                		if (!isEmptyCell(cell)) {
                			String sWidth = view.getCellWidth(nRow, nCol);
                			if (sWidth!=null) {
                				converter.pushContentWidth(sWidth);
                			}
                			getTextCv().traverseBlockText(cell,td);
                			if (sWidth!=null) {
                				converter.popContentWidth();
                			}
                		}
                		else {
                			// Hack to display empty cells even in msie...
                			Element par = converter.createElement("p");
                			td.appendChild(par);
                			par.setAttribute("style","margin:0;font-size:1px");
                			par.appendChild(converter.createTextNode("\u00A0"));
                		}

                		// Is this a subtable?
                		Node subTable = Misc.getChildByTagName(cell,XMLString.TABLE_SUB_TABLE);
                		String sValueType = ofr.isOpenDocument() ?
                				Misc.getAttribute(cell,XMLString.OFFICE_VALUE_TYPE) :
                					Misc.getAttribute(cell,XMLString.TABLE_VALUE_TYPE);
                				applyCellStyle(view.getCellStyleName(nRow,nCol), sValueType, td, subTable!=null);
                	}
                	else if (XMLString.TABLE_COVERED_TABLE_CELL.equals(cell.getNodeName())) {
                		// covered table cells are not part of xhtml table model
                	}
                }
                else {
                	// non-existing cell, not needed in the xhtml table model (it will probably be a trailing cell)
                }
            }
        }
    }
	
    private boolean isEmptyCell(Node cell) {
        if (!cell.hasChildNodes()) {
            return true;
        }
        else if (OfficeReader.isSingleParagraph(cell)) {
            Element par = Misc.getChildByTagName(cell,XMLString.TEXT_P);
            return par==null || !par.hasChildNodes();
        }
        return false;
    }
	
    private void applyTableStyle(String sStyleName, StyleInfo info, boolean bIsSubTable) {
        getTableSc().applyStyle(sStyleName,info);

        if (config.tableSize()!=XhtmlConfig.NONE) {
            StyleWithProperties style = ofr.getTableStyle(sStyleName);
            if (style!=null) {
                // Set table width
                String sWidth = style.getProperty(XMLString.STYLE_REL_WIDTH);
                if (sWidth!=null) {
                    info.props.addValue("width",sWidth);
                }
                else {
                    sWidth = style.getProperty(XMLString.STYLE_WIDTH);
                    if (sWidth!=null && config.tableSize()==XhtmlConfig.RELATIVE) { // Force relative width
                        sWidth=Calc.divide(sWidth, converter.getContentWidth(), true);
                        info.props.addValue("width",sWidth);
                    }
                    // Do not export absolute width, which would be
                    // info.props.addValue("width",colScale(sWidth));
                }
            }
        }

        // info.props.addValue("empty-cells","show"); use &nbsp; instead...

        if (ofr.isSpreadsheet()) { info.props.addValue("white-space","nowrap"); }

        if (bIsSubTable) {
            // Should try to fill the cell; hence:
            info.props.addValue("width","100%");
            info.props.addValue("margin","0");
        }
    }

    private void applyRowStyle(String sStyleName, Element row) {
        StyleInfo info = new StyleInfo();
        getRowSc().applyStyle(sStyleName,info);

        if (config.tableSize()!=XhtmlConfig.NONE) {
            StyleWithProperties style = ofr.getRowStyle(sStyleName);
            if (style!=null) {
                // Translates row style properties
                // OOo offers style:row-height and style:min-row-height
                // In css row heights are always minimal, so both are exported as height
                // If neither is specified, the tallest cell rules; this fits with css.
                String s = style.getAbsoluteProperty(XMLString.STYLE_ROW_HEIGHT);
                // Do not export minimal row height; causes trouble with ie
                //if (s==null) { s = style.getAbsoluteProperty(XMLString.STYLE_MIN_ROW_HEIGHT); }
                if (s!=null) { info.props.addValue("height",getRowSc().scale(s)); }
            }
        }

        applyStyle(info,row);
    }
	
    private void applyCellStyle(String sStyleName, String sValueType, Element cell, boolean bIsSubTable) {
        StyleInfo info = new StyleInfo();
        getCellSc().applyStyle(sStyleName,info);

        StyleWithProperties style = ofr.getCellStyle(sStyleName);
        if (style!=null) {
            // Automatic horizontal alignment (calc only)
            if (ofr.isSpreadsheet() && !"fix".equals(style.getProperty(XMLString.STYLE_TEXT_ALIGN_SOURCE))) {
                // Strings go left, other types (float, time, date, percentage, currency, boolean) go right
                // The default is string
                info.props.addValue("text-align", sValueType==null || "string".equals(sValueType) ? "left" : "right");
            }
        }
		
        if (!cell.hasChildNodes()) { // hack to handle empty cells even in msie
            // info.props.addValue("line-height","1px"); TODO: Reenable this...
            cell.appendChild( converter.createTextNode("\u00A0") );
        }
		
        if (bIsSubTable) {
            // Cannot set height of a subtable, if the subtable does not fill
            // the entire cell it is placed at the top
            info.props.addValue("vertical-align","top");
            // Don't add padding if there's a subtable in the cell!
            info.props.addValue("padding","0");
        }

        applyStyle(info,cell);
    }
	
}
