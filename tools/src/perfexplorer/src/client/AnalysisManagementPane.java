package client;

import javax.swing.*;
import java.awt.*;

/**
 Borrowed from http://java.sun.com/docs/books/tutorial/uiswing/components/example-1dot4/TabbedPaneDemo.java
 */
public class AnalysisManagementPane extends JScrollPane {

	private static AnalysisManagementPane thePane = null;

	private JTable dataTable = null;

	public static AnalysisManagementPane getPane () {
		if (thePane == null) {
        	JTable dataTable = null;
        	dataTable = new JTable(new PerfExplorerTableModel(null));
        	dataTable.setPreferredScrollableViewportSize(new Dimension(400, 400));
			thePane = new AnalysisManagementPane(dataTable);
		}
		thePane.repaint();
		return thePane;
	}

	private AnalysisManagementPane (JTable dataTable) {
		super(dataTable);
		this.dataTable = dataTable;
		JScrollBar jScrollBar = this.getVerticalScrollBar();
		jScrollBar.setUnitIncrement(35);
    }

	public JTable getTable () {
		return dataTable;
	}

}
