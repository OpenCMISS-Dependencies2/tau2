/* 
	jRacy.java

	Title:			Racy
	Author:			Robert Bell
	Description:	
*/

package jRacy;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.colorchooser.*;
import javax.swing.event.*;

//The colouring class maintains all colouring info for Racy.  There should only be one instance
//of this class present for consistant colouring across windows.



public class ColorChooser implements WindowListener
{
	public ColorChooser(SavedPreferences inSavedPreferences)
	{
		
		if(inSavedPreferences != null)
		{
			globalColors = inSavedPreferences.getGlobalColors();
			highlightColor = inSavedPreferences.getHighlightColor();
			miscMappingsColor = inSavedPreferences.getMiscMappingsColor();
		}
		else
		{
			//Set the default colours.
			this.setDefaultColors();
		}
	}
	
	public void showColorChooser()
	{
		if(!clrChooserFrameShowing)
		{
			//Bring up the color chooser frame.
			clrChooserFrame = new ColorChooserFrame(this);
			clrChooserFrame.addWindowListener(this);
			clrChooserFrame.show();
			clrChooserFrameShowing = true;
		}
		else
		{
		//Just bring it to the foreground.
		clrChooserFrame.show();
		}
	}
	
	public void setSavedColors()
	{
		jRacy.savedPreferences.setGlobalColors(globalColors);
		jRacy.savedPreferences.setHighlightColor(highlightColor);
		jRacy.savedPreferences.setMiscMappingsColor(miscMappingsColor);
	}
	
	public int getNumberOfColors()
	{
		return globalColors.size();
	}
	
	public void setColorInLocation(Color inColor, int inLocation)
	{
		try
		{
			globalColors.setElementAt(inColor, inLocation);
		}
		catch(Exception e)
		{
			if(e instanceof ArrayIndexOutOfBoundsException)
			{
				System.out.println("An out of bounds exception occurred while trying to set a color!");
				System.out.println("The value of the index is: " + inLocation);
			}
			else
			{
				System.out.println("An error occurs whilst setting a color!");
			}
		}
	}
	
	public Color getColorInLocation(int inLocation)
	{
		
		try
		{
			return (Color) globalColors.elementAt(inLocation);
		}
		catch(Exception e)
		{
			if(e instanceof ArrayIndexOutOfBoundsException)
			{
				System.out.println("An out of bounds exception occurred while trying to get a color!");
				System.out.println("The value of the index is: " + inLocation);
			}
			else
			{
				System.out.println("An error occurs whilst getting a color!");
			}
		}
		
		//Return null if the above did not work.
		return null;
	}
	
	public void addColor(Color inColor)
	{
		globalColors.add(inColor);
	}
	
	public Vector getColorAllColors()
	{
		return globalColors;
	}
	
	//***
	//Highlight color functions.
	//***
	public void setHighlightColor(Color inColor)
	{
		highlightColor = inColor;
	}
	
	public Color getHighlightColor()
	{
		return highlightColor;
	}
	
	public void setHighlightColorMappingID(int inInt)
	{
		highlightColorMappingID = inInt;
		
		jRacy.systemEvents.updateRegisteredObjects("colorEvent");
	}
	
	public int getHighlightColorMappingID()
	{
		return highlightColorMappingID;
	}
	//***
	//End - Highlight color functions.
	//***
	
	
	//***
	//Group highlight color functions.
	//***
	public void setGroupHighlightColor(Color inColor)
	{
		groupHighlightColor = inColor;
	}
	
	public Color getGroupHighlightColor()
	{
		return groupHighlightColor;
	}
	
	public void setGroupHighlightColorMappingID(int inInt)
	{
		groupHighlightColorMappingID = inInt;
		
		jRacy.systemEvents.updateRegisteredObjects("colorEvent");
	}
	
	public int getGHCMID()
	{
		return groupHighlightColorMappingID;
	}
	//***
	//End - Group highlight color functions.
	//***
	
	
	//***
	//Misc. color functions.
	//***
	public void setMiscMappingsColor(Color inColor)
	{
		miscMappingsColor = inColor;
	}
	
	public Color getMiscMappingsColor()
	{
		return miscMappingsColor;
	}
	//***
	//End - Misc. color functions.
	//***
	
	//A Mapping which sets the globalColors vector to be the default set.
	public void setDefaultColors()
	{
		//Clear the globalColors vector.
		globalColors.clear();
		
		//Add the default colours.
		addColor(new Color(61,104,63));
		addColor(new Color(102,0,51));
		addColor(new Color(0,102,102));
		addColor(new Color(0,51,255));
		addColor(new Color(102,132,25));
		addColor(new Color(119,71,145));
		addColor(new Color(221,232,30));
		addColor(new Color(70,156,168));
		addColor(new Color(255,153,0));
		addColor(new Color(0,255,0));
		addColor(new Color(121,196,144));
		addColor(new Color(86,88,112));
	}
	
	//Window Listener code.
	public void windowClosed(WindowEvent winevt){}
	public void windowIconified(WindowEvent winevt){}
	public void windowOpened(WindowEvent winevt){}
	public void windowClosing(WindowEvent winevt)
	{
		if(winevt.getSource() == clrChooserFrame)
		{
			clrChooserFrameShowing = false;
		}
	}
	public void windowDeiconified(WindowEvent winevt){}
	public void windowActivated(WindowEvent winevt){}
	public void windowDeactivated(WindowEvent winevt){}


	//Instance Data.
	Vector globalColors = new Vector();
	
	private Color highlightColor = Color.red;
	int highlightColorMappingID = -1;
	private Color groupHighlightColor = new Color(0,255,255);
	int groupHighlightColorMappingID = -1;
	private Color miscMappingsColor = Color.black;
	private boolean clrChooserFrameShowing = false;	//For determining whether the clrChooserFrame is showing.
	private ColorChooserFrame clrChooserFrame;
}
	
	
	
class ColorChooserFrame extends JFrame implements ActionListener, MouseListener
{
	//******************************
	//Instance data!
	//******************************
	ColorChooser colorChooserRef;
	private ColorSelectionModel clrModel;
	JColorChooser clrChooser;
	DefaultListModel listModel;
	JList colorList;
	JButton addColorButton;
	JButton deleteColorButton;
	JButton updateColorButton;
	JButton restoreDefaultsButton;
	
	int numberOfColors = jRacy.clrChooser.getNumberOfColors();
	//******************************
	//End - Instance data!
	//******************************
	
	public ColorChooserFrame(ColorChooser inColorChooser)
	{
		
		
		//Window Stuff.
		setLocation(new Point(100, 100));
		setSize(new Dimension(850, 450));
		
		colorChooserRef = inColorChooser;
		
		//******************************
		//Code to generate the menus.
		//******************************
		
		
		JMenuBar mainMenu = new JMenuBar();
		
		//******************************
		//File menu.
		//******************************
		JMenu fileMenu = new JMenu("File");
		
		//Add a menu item.
		JMenuItem closeItem = new JMenuItem("Close This Window");
		closeItem.addActionListener(this);
		fileMenu.add(closeItem);
		
		//Add a menu item.
		JMenuItem exitItem = new JMenuItem("Exit jRacy!");
		exitItem.addActionListener(this);
		fileMenu.add(exitItem);
		//******************************
		//End - File menu.
		//******************************
		
		//******************************
		//Help menu.
		//******************************
		/*JMenu helpMenu = new JMenu("Help");
		
		//Add a menu item.
		JMenuItem aboutItem = new JMenuItem("About Racy");
		helpMenu.add(aboutItem);
		
		//Add a menu item.
		JMenuItem showHelpWindowItem = new JMenuItem("Show Help Window");
		showHelpWindowItem.addActionListener(this);
		helpMenu.add(showHelpWindowItem);*/
		//******************************
		//End - Help menu.
		//******************************
		
		
		//Now, add all the menus to the main menu.
		mainMenu.add(fileMenu);
		//mainMenu.add(helpMenu);
		
		setJMenuBar(mainMenu);
		
		//******************************
		//End - Code to generate the menus.
		//******************************
		
		//******************************
		//Create and add the componants.
		//******************************
		//Setting up the layout system for the main window.
		Container contentPane = getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		contentPane.setLayout(gbl);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		
		//Create some borders.
		Border raisedBev = BorderFactory.createRaisedBevelBorder();
		Border empty = BorderFactory.createEmptyBorder();
		
		
		//Create a new ColorChooser.
		clrChooser = new JColorChooser();
		clrModel = clrChooser.getSelectionModel();
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.weightx = 0;
		gbc.weighty = 0;
		
		//First add the label.
		JLabel titleLabel = new JLabel("jRacy Color Set.");
		titleLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
		addCompItem(titleLabel, gbc, 0, 0, 1, 1);
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 0;
		gbc.weighty = 0;
		
		//Create and add color list.
		listModel = new DefaultListModel();
		colorList = new JList(listModel);
		colorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		colorList.setCellRenderer(new CustomCellRenderer());
		JScrollPane sp = new JScrollPane(colorList);
		sp.setBorder(raisedBev);
		addCompItem(sp, gbc, 0, 1, 1, 4);
		
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.weightx = 0;
		gbc.weighty = 0;
		addColorButton = new JButton("Add Color");
		addColorButton.addActionListener(this);
		addCompItem(addColorButton, gbc, 1, 1, 1, 1);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 0;
		gbc.weighty = 0;
		deleteColorButton = new JButton("Delete Selected Color");
		deleteColorButton.addActionListener(this);
		addCompItem(deleteColorButton, gbc, 1, 2, 1, 1);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 0;
		gbc.weighty = 0;
		updateColorButton = new JButton("Update Selected Color");
		updateColorButton.addActionListener(this);
		addCompItem(updateColorButton, gbc, 1, 3, 1, 1);
		
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 0;
		gbc.weighty = 0;
		restoreDefaultsButton = new JButton("Restore Defaults");
		restoreDefaultsButton.addActionListener(this);
		addCompItem(restoreDefaultsButton, gbc, 1, 4, 1, 1);
		
		//Add the JColorChooser.
		addCompItem(clrChooser, gbc, 2, 0, 1, 5);
		
		//Now populate the colour list.
		populateColorList();
	}	
	
	
	//ActionListener code.
	public void actionPerformed(ActionEvent evt)
	{
		Object EventSrc = evt.getSource();
		String arg = evt.getActionCommand();
		
		if(EventSrc instanceof JMenuItem)
		{
			if(arg.equals("Exit jRacy!"))
			{
				setVisible(false);
				dispose();
				System.exit(0);
			}
			else if(arg.equals("Close This Window"))
			{
				setVisible(false);
			}
		}
		else if(EventSrc instanceof JButton)
		{
			if(arg.equals("Add Color"))
			{
				Color tmpColor = clrModel.getSelectedColor();
				
				listModel.addElement(tmpColor);
				(colorChooserRef.getColorAllColors()).add(tmpColor);
				
				//Update the GlobalMapping.
				GlobalMapping gMRef = jRacy.staticSystemData.getGlobalMapping();
				gMRef.updateGenericColors(0);
				
				//Update the listeners.
				jRacy.systemEvents.updateRegisteredObjects("colorEvent");
			}
			else if(arg.equals("Delete Selected Color"))
			{
				//Get the currently selected items and cycle through them.
				Object [] values = colorList.getSelectedValues();
				for(int i = 0; i < values.length; i++)
				{
					listModel.removeElement(values[i]);
					(colorChooserRef.getColorAllColors()).removeElement(values[i]);
				}
				
				//Update the GlobalMapping.
				GlobalMapping gMRef = jRacy.staticSystemData.getGlobalMapping();
				gMRef.updateGenericColors(0);
				
				//Update the listeners.
				jRacy.systemEvents.updateRegisteredObjects("colorEvent");
			}
			else if(arg.equals("Update Selected Color"))
			{
				Color tmpColor = clrModel.getSelectedColor();
				
				//Get the currently selected items and cycle through them.
				int [] values = colorList.getSelectedIndices();
				for(int i = 0; i < values.length; i++)
				{
					listModel.setElementAt(tmpColor, values[i]);
					
					if((values[i]) == (jRacy.clrChooser.getNumberOfColors())){
						jRacy.clrChooser.setHighlightColor(tmpColor);
					}
					else if((values[i]) == ((jRacy.clrChooser.getNumberOfColors())+1)){
						jRacy.clrChooser.setGroupHighlightColor(tmpColor);
					}
					else if((values[i]) == ((jRacy.clrChooser.getNumberOfColors())+2)){
						jRacy.clrChooser.setMiscMappingsColor(tmpColor);
					}
					else{
						colorChooserRef.setColorInLocation(tmpColor, values[i]);
					}
				}
				
				//Update the GlobalMapping.
				GlobalMapping gMRef = jRacy.staticSystemData.getGlobalMapping();
				gMRef.updateGenericColors(0);
				
				//Update the listeners.
				jRacy.systemEvents.updateRegisteredObjects("colorEvent");
			}
			else if(arg.equals("Restore Defaults"))
			{
				colorChooserRef.setDefaultColors();
				listModel.clear();
				populateColorList();
				
				//Update the GlobalMapping.
				GlobalMapping gMRef = jRacy.staticSystemData.getGlobalMapping();
				gMRef.updateGenericColors(0);
				
				//Update the listeners.
				jRacy.systemEvents.updateRegisteredObjects("colorEvent");
			}
		}
	}
	
	//Mouse Listener Stuff.
	public void mousePressed(MouseEvent evt){}
	public void mouseClicked(MouseEvent evt)
	{
		//Get the panel that fired the event.
		//JPanel TmpPanelRef = (JPanel) evt.getSource();
		//Set the background for that panel.
		//TmpPanelRef.setBackground(clrModel.getSelectedColor());
		
		//if(highlightPanel == TmpPanelRef)
			//jRacy.clrChooser.setHighlightColor(clrModel.getSelectedColor());
			
		//Repaint that panel.
		//TmpPanelRef.repaint();
		
		//jRacy.systemEvents.updateRegisteredObjects("colorEvent");
	}
	public void mouseReleased(MouseEvent evt){}
	public void mouseEntered(MouseEvent evt){}
	public void mouseExited(MouseEvent evt){}
	
	
	private void addCompItem(Component c, GridBagConstraints gbc, int x, int y, int w, int h)
	{
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		
		getContentPane().add(c, gbc);
	}
	
	void populateColorList()
	{
		Color tmpColor;
		
		for(Enumeration e = (colorChooserRef.getColorAllColors()).elements(); e.hasMoreElements() ;)
		{
			tmpColor = (Color) e.nextElement();
			listModel.addElement(tmpColor);
		}
		
		tmpColor = jRacy.clrChooser.getHighlightColor();
		listModel.addElement(tmpColor);
		
		tmpColor = jRacy.clrChooser.getGroupHighlightColor();
		listModel.addElement(tmpColor);
		
		tmpColor = jRacy.clrChooser.getMiscMappingsColor();
		listModel.addElement(tmpColor);
	}
	
}

class CustomCellRenderer implements ListCellRenderer
{
	public Component getListCellRendererComponent(final JList list, final Object value,
										 final int index, final boolean isSelected,
										 final boolean cellHasFocus)
	{
		return new JPanel()
			{
				
				public void paintComponent(Graphics g)
				{
					super.paintComponent(g);
					
					Color inColor = (Color) value;
					
					int xSize = 0;
					int ySize = 0;
					int maxXNumFontSize = 0;
					int maxXFontSize = 0;
					int maxYFontSize = 0;
					int thisXFontSize = 0;
					int thisYFontSize = 0;
					int barHeight = 0;
					
					//For this, I will not allow changes in font size.
					barHeight = 12;
					
					
					//Create font.
					Font font = new Font(jRacy.jRacyPreferences.getJRacyFont(), Font.PLAIN, barHeight);
					g.setFont(font);
					FontMetrics fmFont = g.getFontMetrics(font);
					
					maxXFontSize = fmFont.getAscent();
					maxYFontSize = fmFont.stringWidth("0000,0000,0000");
					
					xSize = getWidth();
					ySize = getHeight();
					
					String tmpString1 = new String("00" + (jRacy.clrChooser.getNumberOfColors()));
					maxXNumFontSize = fmFont.stringWidth(tmpString1);
					
					
					String tmpString2 = new String(inColor.getRed() + "," + inColor.getGreen() + "," + inColor.getBlue());
					thisXFontSize = fmFont.stringWidth(tmpString2);
					thisYFontSize = maxYFontSize;
					
					
					g.setColor(isSelected ? list.getSelectionBackground() : list.getBackground());
					g.fillRect(0, 0, (5 + maxXNumFontSize + 5), ySize);
						
					int xStringPos1 = 5;
					int yStringPos1 = (ySize - 5);
					g.setColor(isSelected ? list.getSelectionForeground() : list.getForeground());
					
					if((index) == (jRacy.clrChooser.getNumberOfColors())){
						g.drawString(("" + ("MHC")), xStringPos1, yStringPos1);
					}
					else if((index) == ((jRacy.clrChooser.getNumberOfColors())+1)){
						g.drawString(("" + ("GHC")), xStringPos1, yStringPos1);
					}
					else if((index) == ((jRacy.clrChooser.getNumberOfColors())+2)){
						g.drawString(("" + ("MPC")), xStringPos1, yStringPos1);
					}
					else{
						g.drawString(("" + (index + 1)), xStringPos1, yStringPos1);
					}
					
					g.setColor(inColor);
					g.fillRect((5 + maxXNumFontSize + 5), 0,50, ySize);
					
					//Just a sanity check.
					if((xSize - 50) > 0)
					{
						g.setColor(isSelected ? list.getSelectionBackground() : list.getBackground());
						g.fillRect((5 + maxXNumFontSize + 5 + 50), 0,(xSize - 50), ySize);
					}
					
					int xStringPos2 = 50 + (((xSize - 50) - thisXFontSize) / 2);
					int yStringPos2 = (ySize - 5);
					
					g.setColor(isSelected ? list.getSelectionForeground() : list.getForeground());
					g.drawString(tmpString2, xStringPos2, yStringPos2);
				}
				
				
				public Dimension getPreferredSize()
				{
					int xSize = 0;
					int ySize = 0;
					int maxXNumFontSize = 0;
					int maxXFontSize = 0;
					int maxYFontSize = 0;
					int barHeight = 12;
					
					//Create font.
					Font font = new Font(jRacy.jRacyPreferences.getJRacyFont(), Font.PLAIN, barHeight);
					Graphics g = getGraphics();
					FontMetrics fmFont = g.getFontMetrics(font);
					
					String tmpString = new String("00" + (jRacy.clrChooser.getNumberOfColors()));
					maxXNumFontSize = fmFont.stringWidth(tmpString);
					
					maxXFontSize = fmFont.stringWidth("0000,0000,0000");
					maxYFontSize = fmFont.getAscent();
					
					xSize = (maxXNumFontSize + 10 + 50 + maxXFontSize + 20);
					ySize = (10 + maxYFontSize);
					
					return new Dimension(xSize,ySize);
				}
			};
	}
}