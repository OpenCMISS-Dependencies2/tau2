package client;

import common.RMICubeData;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLCapabilities;
import net.java.games.jogl.GLDrawableFactory;
import edu.uoregon.tau.vis.*;

public class PerfExplorerCube {

	public static void doCorrelationCube () {
		// get the server
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		// get the data
		RMICubeData data = server.requestCubeData(
			PerfExplorerModel.getModel()); 
		createAndShowGUI(data);
	}

    private static void createAndShowGUI(RMICubeData data) {

        // Create and set up the window.
        JFrame frame = new JFrame("ScatterPlotExample");

		// get the data values
        float values[][] = data.getValues();

        // Create the visRenderer and register it with the canvas
        VisRenderer visRenderer = new VisRenderer();

        // Create the JOGL canvas
        VisCanvas canvas = new VisCanvas(visRenderer);
        canvas.getActualCanvas().setSize(600, 600);

        ColorScale colorScale = new ColorScale();

        // Create the scatterPlot
        ScatterPlot scatterPlot = PlotFactory.createScatterPlot(data.getNames(0), data.getNames(1), data.getNames(2), data.getNames(3), values, true, colorScale);
        
        // Set the size
        scatterPlot.setSize(10, 10, 10);

        // point at the center of the scatterPlot
        visRenderer.setAim(new Vec(5,5,5));
        
        // Add the drawable objects to the visRenderer (the scatterPlot will draw the axes)
        visRenderer.addShape(scatterPlot);
        visRenderer.addShape(colorScale);

        
        // Create the control panel, if desired
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addTab("ScatterPlot", scatterPlot.getControlPanel(visRenderer));
        tabbedPane.addTab("Axes", scatterPlot.getAxes().getControlPanel(visRenderer));
        tabbedPane.addTab("ColorScale", colorScale.getControlPanel(visRenderer));
        tabbedPane.addTab("Render", visRenderer.getControlPanel());
        tabbedPane.setMinimumSize(new Dimension(300, 160));

        // Add everything to a JPanel and add the panel to the frame
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.add(canvas.getActualCanvas(), new GridBagConstraints(0, 0, 1, 1, 0.9, 1.0, GridBagConstraints.WEST,
                GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 1, 1));
        panel.add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 0.1, 1.0, GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 1, 1));

        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

}
