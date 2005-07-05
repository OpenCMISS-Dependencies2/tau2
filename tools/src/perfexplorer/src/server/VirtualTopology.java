/*
 * Created on Jun 29, 2005
 *
 */
package server;

import java.awt.image.*;
import javax.swing.JPanel;
import clustering.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Color;
import common.RMIPerfExplorerModel;

/**
 * This class takes the Weka or R cluster results, and creates a virtual
 * topology image showing which cluster each thread of execution belongs to.
 *
 * <P>CVS $Id: VirtualTopology.java,v 1.1 2005/07/05 22:29:54 amorris Exp $</P>
 * @author khuck
 * @version 0.1
 * @since   0.1
 *
 */
public class VirtualTopology extends JPanel {
	RMIPerfExplorerModel modelData = null;
	KMeansClusterInterface clusterer = null;
	int[] pixels = null;
	BufferedImage img = null;
	StringBuffer description = null;
	private static final int idealSize = 128;
	private static final Color[] colors = PEChartColor.createDefaultColorArray();
	
	
	public VirtualTopology(RMIPerfExplorerModel modelData, KMeansClusterInterface clusterer) {
		this.modelData = modelData;
		this.clusterer = clusterer;
		
		description = new StringBuffer();
		description.append(modelData.toShortString() + ".");
		description.append(clusterer.getK() + "_clusters");
	
		// build the image data from the cluster results
		pixels = new int[clusterer.getNumInstances()];
		
		// get the size of the image...
		int width = 1;
		int height = clusterer.getNumInstances();
		while (width < height) {
			height = height / 2;
			width = width * 2;
		}
		
		// if this is a BIG topology (larger than 128x128), just do one pixel per thread
		if (idealSize <= width || idealSize <= height) {
			img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			int i = 0;
			for (int x = 0 ; x < width ; x++) {
				for (int y = 0 ; y < height ; y++) {
					img.setRGB(x, y, colors[clusterer.clusterInstance(i)].getRGB());
					i++;
				}
			}
		} else {
			img = new BufferedImage(idealSize, idealSize, BufferedImage.TYPE_INT_RGB);

			// otherwise, make each thread "bigger" (multiple pixels) in the final image.
			int cellWidth = idealSize / width;
			int cellHeight = idealSize / height;
			
			int i = 0;
			for (int x = 0 ; x < width ; x++) {
				for (int y = 0 ; y < height ; y++) {
					for (int cellX = x * cellWidth ; cellX < (x+1) * cellWidth ; cellX++) {
						for (int cellY = y * cellHeight ; cellY < (y+1) * cellHeight ; cellY++) {
							img.setRGB(cellX, cellY, colors[clusterer.clusterInstance(i)].getRGB());
						}
					}
					i++;
				}
			}
		}
	}

	public String getImage() {
		String filename = "/tmp/clusterImage." + description + ".png";
		File outFile = new File(filename);
		try {
			ImageIO.write(img, "PNG", outFile);
		} catch (IOException e) {
			String error = "ERROR: Couldn't write the virtual topology image!";
			System.out.println(error);
			e.printStackTrace();
		}
		return filename;
	}

	public String getThumbnail() {
		String filename = "/tmp/clusterImage.thumb." + description + ".png";
		return filename;
	}
}
