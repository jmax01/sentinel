package sentinel.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import sentinel.exceptions.SentinelException;
import sentinel.exceptions.NoSuchColumnException;
import sentinel.exceptions.NoSuchElementException;
import sentinel.exceptions.NoSuchSelectorException;
import sentinel.utils.SelectorType;
import sentinel.utils.StringUtils;

/**
 * Implements a Table WebElement. contains functionality for counting values, finding values inside a table, and other
 * management issues, such as finding a value in the same row.
 */

public class Table extends PageElement {
	private static final Logger log = LogManager.getLogger(Table.class.getName()); // Create a logger.

	protected List<WebElement> headerElements = null; // Table Columns headers using <th> tags
	protected List<String> headers = new ArrayList<String>(); // Column headers as text
	protected List<WebElement> rowElements = null; // Table Rows using <tr> tags
	protected List<ArrayList<String>> rows = new ArrayList<ArrayList<String>>(); // All text values of every row
	protected Map<String, ArrayList<String>> columns = new HashMap<>(); // All text values of every column
	protected Map<Integer, List<ArrayList<String>>> tables = new HashMap<>(); // Way to hold values of the same table on
																				// multiple pages. TODO: Might want to
																				// put this in a multi-page table
																				// object.

	/**
	 * Initializes how the WebElement is going to be found when it is worked on by the WebDriver class. 
	 * Takes the reference to the WebDriver class that will be exercising its functionality.
	 * 
	 * @param selectorType SelectorType the type of selector to use
	 * @param selectorValue String the value to look for with the given selector type
	 */
	public Table(SelectorType selectorType, String selectorValue) {
		super(selectorType, selectorValue);
	}

	/**
	 * Resets table data when comparing multiple pages of the same table.
	 */
	protected void reset() {
		if (headerElements != null) {
			headerElements.clear();
		}
		if (headers != null) {
			headers.clear();
		}
		if (rowElements != null) {
			rowElements.clear();
		}
		if (rows != null) {
			rows.clear();
		}
		if (columns != null) {
			columns.clear();
		}
	}

	/**
	 * Returns the headers in the table as a list of Strings, populates with the
	 * first row if there are no &lt;th&gt; tags. Creates headers if they do not already exist.
	 * 
	 * @return List&lt;String&gt; the headers of the table, populates with the first
	 *         row if there are no &lt;th&gt; tags
	 * @throws SentinelException if there is a problem retrieving the header or rows
	 */
	public List<String> getOrCreateHeaders() throws SentinelException {
		if (headers.isEmpty()) {
			getOrCreateHeadersElements();
			for (WebElement header : headerElements) {
				String headerText = header.getText();
				log.trace("Header Text: {}", headerText);
				headers.add(headerText);
			}
		}
		// If we cannot find headers, then we need to populate this array with the first
		// row
		if (headers.isEmpty()) {
			getOrCreateRows();
			List<String> firstRow = rows.get(0);
			for (String cell : firstRow) {
				headers.add(cell);
			}
		}
		log.trace("Headers: {}", headers);
		return headers;
	}

	/** 
	 *  Returns the header elements in the table as a list of WebElements, 
	 *  creates row elements if no &lt;th$gt; elements are found,
	 *  and logs the number of Header Elements
	 * 
	 * @return List&lt;WebElement&gt;
	 * @throws SentinelException if the header elements cannot be found
	 */
	public List<WebElement> getOrCreateHeadersElements() throws SentinelException {
		if (headerElements == null) {
			headerElements = this.element().findElements(By.tagName("th"));
		}
		if (headerElements == null) {
			headerElements = getOrCreateRowElements().get(0).findElements(By.tagName("td"));
		}
		log.trace("Number of Header Elements: {}", headerElements.size());
		return headerElements;
	}

	/**
	 * Returns true if the table has &lt;th&gt; elements, otherwise returns false
	 * even though the first row will be used to populate the headers list.
	 * 
	 * @see sentinel.elements.Table#getOrCreateHeadersElements()
	 * @return boolean true if the table has &lt;th&gt; elements, otherwise false
	 * @throws SentinelException if the header elements cannot be found
	 */
	public boolean tableHeadersExist() throws SentinelException {
		return getOrCreateHeadersElements() != null;
	}

	/**
	 * Returns &lt;tr&gt; elements found in a table. 
	 * 
	 * @return List&lt;WebElement&gt;
	 * @throws SentinelException if the row elements cannot be found
	 */
	public List<WebElement> getOrCreateRowElements() throws SentinelException {
		if (rowElements == null) {
			rowElements = this.element().findElements(By.tagName("tr"));
		}
		return rowElements;
	}
	
	/**
	 * Returns array of cell arrays, with data for each cell, in the table. Initial row of table headers is removed
	 * 
	 * @return List&lt;ArrayList&lt;String&gt;&gt;
	 * @throws SentinelException if the row elements cannot be found
	 */

	public List<ArrayList<String>> getOrCreateRows() throws SentinelException {
		if (rows.isEmpty()) {
			List<WebElement> dataRows = getOrCreateRowElements();
			dataRows.remove(0); // Header row
			for (WebElement row : dataRows) {
				List<WebElement> cellElements = row.findElements(By.tagName("td"));
				ArrayList<String> cells = new ArrayList<String>();
				for (WebElement cell : cellElements) {
					cells.add(cell.getText());
				}
				rows.add(cells);
			}
		}
		log.trace("Rows Data: {}", rows);
		return rows;
	}

	/**
	 * Returns number of row elements from getOrCreateRowElements
	 * 
	 * @see sentinel.elements.Table#getOrCreateRowElements()
	 * @return int the number of row elements
	 * @throws SentinelException if the row elements cannot be found
	 */
	public int getNumberOfRows() throws SentinelException {
		return getOrCreateRowElements().size() - 1;
	}

	/**
	 * Returns the mapping of header strings to cell arrays for each column in the table. 
	 * e.g. { "Date Column": ["1/1/01", "1/2/01", ...] }
	 * 
	 * @return Map&lt;String, ArrayList&lt;String&gt;&gt;
	 * @throws SentinelException if the header or row elements cannot be found
	 */
	public Map<String, ArrayList<String>> getOrCreateColumns() throws SentinelException {
		if (columns.isEmpty()) {
			int index = 0;
			getOrCreateRows(); // We cannot create the columns without Row data
			for (String header : getOrCreateHeaders()) {
				ArrayList<String> cells = new ArrayList<String>();
				for (ArrayList<String> row : rows) {
					cells.add(row.get(index));
				}
				columns.put(header, cells);
				index++;
			}
		}
		log.trace("Columns Data: {}", columns);
		return columns;
	}
	
	/**
	 * Returns the number of columns in the table.
	 * 
	 * @see sentinel.elements.Table#getOrCreateHeaders()
	 * @return int the number of columns
	 * @throws SentinelException if the header elements cannot be found
	 */
	public int getNumberOfColumns() throws SentinelException {
		return getOrCreateHeaders().size();
	}

	/**
	 * Returns list of all cells in the given tableRow, and logs the first cell and then row cells in separate log entries.
	 * 
	 * @param tableRow WebElement a row in the table
	 * @return List&lt;WebElement;&gt; 
	 */
	protected List<WebElement> getCells(WebElement tableRow) {
		List<WebElement> cells = tableRow.findElements(By.tagName("td"));
		log.debug("First Cell: {}", tableRow.findElement(By.tagName("td")));
		log.debug("Row Cells: {}", cells);
		return cells;
	}

	/**
	 * Stores the current table rows on the current page in an index given by the
	 * integer passed in the pageNumber parameter. Used for comparing paginated
	 * table data.
	 * 
	 * @param pageNumber int the page number under which to store the table data for comparison
	 * @throws SentinelException if the rows cannot be found
	 */
	public void storeTable(int pageNumber) throws SentinelException {
		reset();
		tables.put(pageNumber, getOrCreateRows());
	}

	/**
	 * Stores the current tables rows in index 1. Used for single-page
	 * (un-paginated) tables.
	 * 
	 * @throws SentinelException if the rows cannot be found
	 */
	public void storeTable() throws SentinelException {
		storeTable(1);
	}

	/**
	 * Compares the given number of a page stored in memory and compares it to the one
	 * currently being displayed. If all the rows match, returns true. If any data
	 * is different, returns false.
	 * 
	 * @param pageNumber int the stored page number to compare against the current page
	 * @return boolean Table matches the one in memory.
	 * @throws SentinelException if the rows cannot be found
	 */
	public boolean compareWithStoredTable(int pageNumber) throws SentinelException {
		reset();
		return (tables.get(pageNumber) == getOrCreateRows());
	}

	/**
	 * Finds the link in a row by using text in another cell. For example,
	 * finding an "Edit" link for a specific username in a table. To work, this
	 * method must have a link name and a unique string to match. This method could
	 * also be used to validate text in a cell in the same row Returning
	 * successfully would indicate the expected text was found.
	 * 
	 * @param elementText String the text of the element (link) you are looking to find.
	 * @param textToMatch String the unique text to locate the row in question.
	 * @return WebElement a selenium WebElement object that can be operated on.
	 * @throws NoSuchElementException if the element cannot be found
	 * @throws NoSuchSelectorException if the passed selector type does not exist
	 */
	protected WebElement getElementInRowThatContains(String elementText, String textToMatch) throws NoSuchElementException, NoSuchSelectorException {
		try {
			return this.element().findElement(By.xpath(
					"//td[contains(text(),'" + elementText + "')]/..//*[contains(text(),'" + textToMatch + "')]"));
		} catch (org.openqa.selenium.NoSuchElementException e) {
			String errorMsg = StringUtils.format("{} not found in {} Error: {}", textToMatch, elementText, e.getMessage());
			log.error(errorMsg);
			throw new sentinel.exceptions.NoSuchElementException(errorMsg);
		}
	}

	/**
	 * Clicks a link in a row by using text in another cell. For example,
	 * finding an "Edit" link for a specific username in a table. To work, this
	 * method must have a link name and a unique string to match. This method could
	 * also be used to validate text in a cell in the same row Returning
	 * successfully would indicate the expected text was found.
	 * 
	 * @param elementText String the text of the element (link) you are looking to find
	 * @param textToClick String the unique text to locate the row in question
	 * @throws NoSuchElementException if the element cannot be found
	 * @throws NoSuchSelectorException if the passed selector type does not exist
	 */
	public void clickElementInRowThatContains(String elementText, String textToClick) throws NoSuchElementException, NoSuchSelectorException {
		getElementInRowThatContains(elementText, textToClick).click();
	}

	/**
	 * Returns true if getOrCreateColumns returns a value. Throws error if no column is found 
	 * or wrong column is found.
	 * 
	 * @param columnHeader String text in the given column header
	 * @param textToMatch String text to find in the given column
	 * @return boolean true if the given cell contains the give text, false if duplicates are found
	 * @throws SentinelException if the column or row creation fails
	 */
	public boolean verifyColumnCellsContain(String columnHeader, String textToMatch) throws SentinelException {
		getOrCreateHeaders();
		ArrayList<String> column = getOrCreateColumns().get(columnHeader);
		if (column == null) {
			log.error("Column does not exist. Header text: {} | Text to Match: {}", columnHeader, textToMatch);
			throw new IllegalArgumentException("Column header \"" + columnHeader + "\" does not exist.");
		}
		for (String cell : column) {
			try {
				if (!cell.contains(textToMatch)) {
					log.error("False result returned. Header text: {} | Cell data: {} | Text to Match: {} | Result: {}",
							columnHeader, cell, textToMatch, cell.contains(textToMatch));
					return false;
				}
			} catch (NullPointerException e) {
				log.error("Header text: {} | Cell data: {} | Text to Match: {}", columnHeader, cell, textToMatch);
				throw e;
			}

		}
		return true;
	}

	/**
	 * Returns &lt;code&gt;true&lt;/code&gt; if the column cells are unique
	 * 
	 * @param columnHeader String text of the given column header to search
	 * @return boolean true if column cells are unique, false if duplicates are found, throws error otherwise
	 * @throws SentinelException if the column or row creation fails
	 */
	public boolean verifyColumnCellsAreUnique(String columnHeader) throws SentinelException {
		if (verifyColumnExists(columnHeader) == false) {
			log.error("IllegalArgumentException: Column header \"{}\" does not exist.", columnHeader);
			throw new IllegalArgumentException("Column header \"" + columnHeader + "\" does not exist.");
		}
		getOrCreateHeaders();
		ArrayList<String> column = getOrCreateColumns().get(columnHeader);
		if (column.isEmpty()) {
			log.error("Header text: {}", columnHeader);
			throw new IllegalArgumentException("Column header \"" + columnHeader + "\" does not exist.");
		}
		for (String cell : column) {
			try {
				int count = 0;
				for (String cell2 : column) {
					if (cell2.contains(cell)) {
						count++;
					}
					if (count > 1) {
						log.error("False result returned. Header text: {} | Cell data: {}", columnHeader, cell);
						return false;
					}
				}

			} catch (NullPointerException e) {
				log.error("NullPointerException thrown. Header text: {} | Cell data: {}", columnHeader, cell);
				throw e;
			}

		}
		return true;
	}

	/**
	 * Returns true if column exists, false if column does not exist
	 * 
	 * @param columnName String name of column to find
	 * @return boolean true if column exists, false if column does not exists.
	 * @throws SentinelException if the headers do not exist
	 */
	public boolean verifyColumnExists(String columnName) throws SentinelException {
		for (String header : getOrCreateHeaders()) {
			if (header.contains(columnName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the row cell's values are unique for the given column name
	 * 
	 * @param columnName String comma delimited columns list
	 * @return boolean true if all cells values are unique, false if any duplicates
	 * @throws SentinelException if a column doesn't exist
	 */
	public boolean verifyRowCellsAreUnique(String columnName) throws SentinelException {
		String[] columns = columnName.split(", ");
		return verifyRowCellsAreUnique(columns);
	}

	/**
	 * Returns true if the cell values are unique for the given array of column names
	 * 
	 * @param columnsHeader string[] the array of column name to validate
	 * @return boolean true if all cells values are unique, false if any duplicates
	 * @throws SentinelException if a column doesn't exist
	 */
	public boolean verifyRowCellsAreUnique(String[] columnsHeader) throws SentinelException {

		getOrCreateHeaders();
		getOrCreateRows();
		List<Integer> indexes = new ArrayList<Integer>();
		for (String columnHeader : columnsHeader) {
			if (verifyColumnExists(columnHeader) == false) {
				String errorMessage = StringUtils.format("Column header \"{}\" does not exist.", columnHeader);
				log.error(errorMessage);
				throw new NoSuchColumnException(errorMessage);
			}
			for (int i = 0; i < headers.size(); i++) {

				if (columnHeader.equals(headers.get(i)))
					indexes.add(i);
			}
		}
		for (List<String> cells : rows) {

			String cellValue = getCellsValue(indexes, cells);

			int count = 0;
			for (List<String> cells2 : rows) {
				String cellValue2 = getCellsValue(indexes, cells2);
				if (cellValue2.contains(cellValue)) {
					count++;
				}
				if (count > 1) {
					log.trace("False result returned. Header text: {} | Cell data: {}", indexes, cellValue);
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns all the cells values in the listed columns
	 * 
	 * @param indexes List&lt;Integer&gt; listed column index
	 * @param cells List&lt;String&gt; row cells
	 * @return String of listed columns row values
	 */
	private String getCellsValue(List<Integer> indexes, List<String> cells) {
		String cellValues = "";
		for (int index = 0; index < indexes.size(); index++) {
			cellValues = cellValues.concat(cells.get(indexes.get(index)));
		}
		return cellValues;
	}

}
