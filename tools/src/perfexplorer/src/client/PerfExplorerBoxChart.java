/* ----------------------------
 * BoxAndWhiskerChartDemo1.java
 * ----------------------------
 * (C) Copyright 2005, by Object Refinery Limited.
 *
 */

package client;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JFrame;
import java.util.List;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import common.RMIChartData;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * A simple demonstration application showing how to create a box-and-whisker
 * chart.
 */
public class PerfExplorerBoxChart extends PerfExplorerChartWindow {

	public PerfExplorerBoxChart (JFreeChart chart, String name) {
		super (chart, name);
	}

	public static void doIQRBoxChart() {
		// for each event, get the variation across all threads.
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		// get the data
		RMIChartData data = server.requestChartData(PerfExplorerModel.getModel(), RMIChartData.IQR_DATA);

		// build the chart
		BoxAndWhiskerCategoryDataset dataset = createDataset(data);
        JFreeChart chart = createChart(dataset);
		JFrame frame = new PerfExplorerBoxChart(chart, "Distributions of Significant Events");
	}

    /**
     * Returns a sample dataset.
     *
     * @return The dataset.
     */
    private static BoxAndWhiskerCategoryDataset createDataset(RMIChartData data) {
        int SERIES_COUNT = data.getRows();
        int CATEGORY_COUNT = 1;
        DefaultBoxAndWhiskerCategoryDataset result
            = new DefaultBoxAndWhiskerCategoryDataset();
        List names = data.getRowLabels();
        for (int s = 0; s < SERIES_COUNT; s++) {
            for (int c = 0; c < CATEGORY_COUNT; c++) {
                List values = createValueList(data.getRowData(s));
                //result.add(values, (String)names.get(s), "Category " + c);
                result.add(values, (String)names.get(s), "");
            }
        }
        return result;
    }
   
    private static List createValueList(List inData) {
    		List result = new java.util.ArrayList();
    		double min = ((double[])(inData.get(0)))[1];
     		double max = min;  
    		for (int i = 1; i < inData.size(); i++) {
    			if (min > ((double[])(inData.get(i)))[1])
    				min = ((double[])(inData.get(i)))[1];
    			if (max < ((double[])(inData.get(i)))[1])
    				max = ((double[])(inData.get(i)))[1];
    		}
    		double range = max - min;
    		//System.out.println("Min: " + min + ", Max: " + max + ", Range: " + range);
    		for (int i = 0; i < inData.size(); i++) {
    			result.add(new Double((((double[])(inData.get(i)))[1]-min)/range));   
    		}
    		return result;
    }

    private static List createValueList(double lowerBound, double upperBound,
                                        int count) {
        List result = new java.util.ArrayList();
        for (int i = 0; i < count; i++) {
            double v = lowerBound + (Math.random() * (upperBound - lowerBound));
            result.add(new Double(v));   
        }
        return result;
    }
   
    /**
     * Creates a sample chart.
     *
     * @param dataset  the dataset.
     *
     * @return The chart.
     */
    private static JFreeChart createChart(BoxAndWhiskerCategoryDataset dataset) {
       
        CategoryAxis domainAxis = new CategoryAxis(null);
        NumberAxis rangeAxis = new NumberAxis("Value");
        CategoryItemRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setToolTipGenerator(new CategoryToolTipGenerator() {
            public String generateToolTip(CategoryDataset inDataset, int arg1, int arg2) {
				BoxAndWhiskerCategoryDataset dataset = 
					(BoxAndWhiskerCategoryDataset) inDataset;
                return "<html>Min Outlier: " + dataset.getMinOutlier(arg1, arg2) + 
                "<BR>Min Regular Value: " + dataset.getMinRegularValue(arg1, arg2) + 
                "<BR>Q1 Value: " + dataset.getQ1Value(arg1, arg2) + 
                "<BR>Mean Value: " + dataset.getMeanValue(arg1, arg2) + 
                "<BR>Q3 Value: " + dataset.getQ3Value(arg1, arg2) + 
                "<BR>Max Regular Value: " + dataset.getMaxRegularValue(arg1, arg2) + 
                "<BR>Max Outlier: " + dataset.getMaxOutlier(arg1, arg2) + "</html>";
            }
        });

        CategoryPlot plot = new CategoryPlot(
            dataset, domainAxis, rangeAxis, renderer
        );
        JFreeChart chart = new JFreeChart("Significant (>2.0% of runtime) Event IQR Boxplot with Outliers", plot);
       
        chart.setBackgroundPaint(Color.white);

        //plot.setBackgroundPaint(Color.lightGray);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.white);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.white);

        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // OPTIONAL CUSTOMISATION COMPLETED.
       
        return chart;
       
    }
  
}
