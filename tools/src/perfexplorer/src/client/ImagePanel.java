/*
 * Created on Jun 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package client;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.*;

/**
 * Description
 *
 * <P>CVS $Id: ImagePanel.java,v 1.1 2005/07/05 22:29:47 amorris Exp $</P>
 * @author khuck
 * @version 0.1
 * @since   0.1
 *
 */
public class ImagePanel extends JPanel implements ImageObserver {
	private Image img = null;
	
	public ImagePanel(Image img) {
		super();
		this.img = img;
		int height = java.lang.Math.max(img.getHeight(this), 400);
		int width = java.lang.Math.max(img.getWidth(this), 400); 
		setPreferredSize(new Dimension(height, width));
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		// the size of the component
		Dimension d = getSize();
		// the internal margins of the component
		Insets i = getInsets();
		// draw to fill the entire component
		g.drawImage(img, i.left, i.top, d.width - i.left - i.right, d.height - i.top - i.bottom, this );
		//g.drawImage(img, i.left, i.top, 512, 512, this );
	}

}
