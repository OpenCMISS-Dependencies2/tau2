package common;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * This class represents the data to be used to create a scalability or
 * runtime breakdown chart.  
 *
 * <P>CVS $Id: RMIGeneralChartData.java,v 1.3 2007/02/07 06:37:17 khuck Exp $</P>
 * @author khuck
 * @version 0.2
 * @since   0.2
 *
 */
public class RMIGeneralChartData implements Serializable {

	protected ChartDataType dataType = ChartDataType.FRACTION_OF_TOTAL;

	protected List data = null;
	protected Class categoryType = Integer.class;
	protected int xMinimum = -1;
	protected int xMaximum = -1;

	/**
	 * Constructor.
	 * 
	 * @param dataType
	 */
	public RMIGeneralChartData (ChartDataType dataType) {
		this.dataType = dataType;
		this.data = new ArrayList();
	}

	/**
	 * Add a row of data fields (a new line on the chart)
	 * 
	 * @param label
	 */
	public void addRow(String series, String category, double value) {
		CategoryDataRow row = new CategoryDataRow (series, category, value);
		data.add(row);
		if (row.categoryType == String.class) {
			this.categoryType = String.class;
		} else {
			if (this.xMaximum == -1 || row.categoryInteger.intValue() > this.xMaximum) {
				this.xMaximum = row.categoryInteger.intValue();
			}
			if (this.xMinimum == -1 || row.categoryInteger.intValue() < this.xMinimum) {
				this.xMinimum = row.categoryInteger.intValue();
			}
		}
	}

	/**
	 * Get the number of rows in the chart data.
	 *
	 * @return
	 */
	public int getRows() { return data.size(); }

	/**
      * Get the List of values for a particular row (series) in the chart data.
      * 
      * @param index
      * @return
    */
	public CategoryDataRow getRowData(int index) { 
		return (CategoryDataRow)data.get(index); 
	}

	public int getMinimum() {
		return xMinimum;
	}

	public int getMaximum() {
		return xMaximum;
	}

	/**
	 * Get the category type of the data
	 *
	 * @return
	 */
	public Class getCategoryType() { return this.categoryType; }

	public class CategoryDataRow implements Serializable {
		public String series = null;
		public String categoryString = null;
		public Integer categoryInteger = null;
		public double value = 0.0;
		public Class categoryType = Integer.class;

		public CategoryDataRow (String series, String category, double value) {
			this.series = series;
			this.categoryString = category;
			try {
				int tmp = Integer.parseInt(category);
				this.categoryInteger = new Integer(tmp);
			} catch (NumberFormatException e) {
				categoryType = String.class;
			}
			this.value = value;
		}
	}
}
