/* 
	GlobalMapping.java

	Title:			jRacy
	Author:			Robert Bell
	
	
	Description:	This is a rather large class.  It controls the mappings in the system.
					Currently, there are two mapping structures.  These two mappings reflect
					the current use of jRacy with TAU.  However, this class has been defined
					to be as general as possible, and the interpretation of the two (or in the
					future more) mapping structures is able to vary.  The first mapping structure
					is viewed as a runtime defined mapping whereby low level entities are mapped
					into higher level concepts.  With this mapping concept, information is not
					available for these lower level entities.  The second mapping structure consists
					of both runtime, and post runtime mappings.  Its interpretation is to group
					the first set of mappings into higer level mappings without losing the information
					about those mappings.	
*/

package jRacy;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;


public class GlobalMapping implements WindowListener, Serializable 
{
	//Constructors.
	public GlobalMapping()
	{
		mappings = new Vector[2];
		numberOfMappings = new int[2];
		mappingLedgerWindows = new MappingLedgerWindow[2];
		
		mappings[0] = new Vector();
		mappings[1] = new Vector();
		
		numberOfMappings[0] = 0;
		numberOfMappings[1] = 0;
	}
	
	public int addGlobalMapping(String inMappingName, int mappingSelection)
	{
		int tmpInt = getMappingId(inMappingName, mappingSelection);
		
		if(tmpInt == -1){
			//Just adds to the end of the list.  Its position becomes
			//the value of its mapping ID.
			GlobalMappingElement tmpGME = new GlobalMappingElement();
			tmpGME.setMappingName(inMappingName);
			tmpGME.setGlobalID(numberOfMappings[mappingSelection]);
			if(mappingSelection == 0)
				tmpGME.setGenericColor(jRacy.clrChooser.getColorInLocation(numberOfMappings[mappingSelection] % (jRacy.clrChooser.getNumberOfColors())));
			else
				tmpGME.setGenericColor(jRacy.clrChooser.getMappingGroupColorInLocation(numberOfMappings[mappingSelection] % (jRacy.clrChooser.getNumberOfMappingGroupColors())));
			mappings[mappingSelection].addElement(tmpGME);
			
			//Update the number of global mappings present for the selection.  (Example ... first time
			//round, numberOfMappings[mappingSelection] = 0, and thus the new mapping name gets an
			//ID of 0.  The numberOfMappings[mappingSelection] is now updated to 1 and thus returns
			//the correct amount should it be asked for.
			numberOfMappings[mappingSelection]++;
			
			//Return the mapping id of the just added mapping (since we just incremented, knock off one).
			return (numberOfMappings[mappingSelection] -1);
		}
		
		//If here, the mapping was already present.  Therefore, just return the already existing id for this mapping.
		return tmpInt;
	}
	
	public boolean setMappingNameAt(String inMappingName, int inPosition, int mappingSelection)
	{
		//First check to make sure that inPosition is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inPosition > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return false;
		}
		
		//If here, the size of inPosition is ok.
		//Therefore grab the element at that position.
		GlobalMappingElement tmpGME = (GlobalMappingElement) mappings[mappingSelection].elementAt(inPosition);
		
		//Set the name.
		tmpGME.setMappingName(inMappingName);
		
		//Successful ... return true.
		return true;
	}
	
	public boolean addGroup(int inPosition, int inGroupID, int mappingSelection)
	{
		//First check to make sure that inPosition is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inPosition > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return false;
		}
		
		//If here, the size of inPosition is ok.
		//Therefore grab the element at that position.
		GlobalMappingElement tmpGME = (GlobalMappingElement) mappings[mappingSelection].elementAt(inPosition);
		
		//Set the name.
		if(tmpGME.addGroup(inGroupID))
			return true;
		else
			return false;
	}
	
	public boolean isGroupMember(int inPosition, int inGroupID, int mappingSelection)
	{
		
		//First check to make sure that inPosition is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inPosition > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return false;
		}
		
		//If here, the size of inPosition is ok.
		//Therefore grab the element at that position.
		GlobalMappingElement tmpGME = (GlobalMappingElement) mappings[mappingSelection].elementAt(inPosition);
		
		//Set the name.
		if(tmpGME.isGroupMember(inGroupID))
			return true;
		else
			return false;
	}
	
	public boolean setMeanExclusiveValueAt(double inMeanExclusiveValue, int inPosition, int mappingSelection)
	{
		//First check to make sure that inPosition is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inPosition > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return false;
		}
		
		//If here, the size of inPosition is ok.
		//Therefore grab the element at that position.
		GlobalMappingElement tmpGME = (GlobalMappingElement) mappings[mappingSelection].elementAt(inPosition);
		
		//Set the mean value.
		tmpGME.setMeanExclusiveValue(inMeanExclusiveValue);
		
		//Successful ... return true.
		return true;
	}
	
	public boolean setMeanInclusiveValueAt(double inMeanInclusiveValue, int inPosition, int mappingSelection)
	{
		//First check to make sure that inPosition is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inPosition > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return false;
		}
		
		//If here, the size of inPosition is ok.
		//Therefore grab the element at that position.
		GlobalMappingElement tmpGME = (GlobalMappingElement) mappings[mappingSelection].elementAt(inPosition);
		
		//Set the mean value.
		tmpGME.setMeanInclusiveValue(inMeanInclusiveValue);
		
		//Successful ... return true.
		return true;
	}
	
	public boolean setTotalExclusiveValueAt(double inTotalExclusiveValue, int inPosition, int mappingSelection)
	{
		//First check to make sure that inPosition is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inPosition > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return false;
		}
		
		//If here, the size of inPosition is ok.
		//Therefore grab the element at that position.
		GlobalMappingElement tmpGME = (GlobalMappingElement) mappings[mappingSelection].elementAt(inPosition);
		
		//Set the mean value.
		tmpGME.setTotalExclusiveValue(inTotalExclusiveValue);
		
		//Successful ... return true.
		return true;
	}
	
	public boolean setTotalInclusiveValueAt(double inTotalInclusiveValue, int inPosition, int mappingSelection)
	{
		//First check to make sure that inPosition is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inPosition > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return false;
		}
		
		//If here, the size of inPosition is ok.
		//Therefore grab the element at that position.
		GlobalMappingElement tmpGME = (GlobalMappingElement) mappings[mappingSelection].elementAt(inPosition);
		
		//Set the mean value.
		tmpGME.setTotalInclusiveValue(inTotalInclusiveValue);
		
		//Successful ... return true.
		return true;
	}
		
	
	public boolean isMappingPresent(String inMappingName, int mappingSelection)
	{
		if(inMappingName == null)
			return false;
		
		GlobalMappingElement tmpElement;
		String tmpString;
		
		for(Enumeration e = mappings[mappingSelection].elements(); e.hasMoreElements() ;)
		{
			tmpElement = (GlobalMappingElement) e.nextElement();
			tmpString = tmpElement.getMappingName();
			if(tmpString != null)
			{
				if(inMappingName.equals(tmpString))
					return true;
			}
		}
		
		//If here, it means that the mapping was not found.
		return false;
	}
	
	public int getNumberOfMappings(int mappingSelection)
	{
		return numberOfMappings[mappingSelection];
	}
	
	public GlobalMappingElement getGlobalMappingElement(int inMappingID, int mappingSelection)
	{
		//Note that by default the elments in nameIDMapping are in mappingID order.
		
		//First check to make sure that mappingID is not greater than the number of
		//mappings present (minus one of course for vector numbering).
		if(inMappingID > (this.getNumberOfMappings(mappingSelection) - 1))
		{
			return null;
		}
		
		//We are ok, therefore, grab the element at that position.
		return (GlobalMappingElement) mappings[mappingSelection].elementAt(inMappingID);
	}
	
	public int getMappingId(String inMappingName, int mappingSelection)
	{
		//Cycle through the list to obtain the mapping id.  Return -1
		//if we cannot find the name.
		int count = 0;
		GlobalMappingElement tmpGlobalMappingElement = null;
		for(Enumeration e1 = mappings[mappingSelection].elements(); e1.hasMoreElements() ;)
		{
			tmpGlobalMappingElement = (GlobalMappingElement) e1.nextElement();
			String tmpString = tmpGlobalMappingElement.getMappingName();
			if(tmpString != null)
			{
				if(tmpString.equals(inMappingName))
					return count;
			}
				
			count++;
		}
		
		//If here,  means that we did not find the mapping name.
		return -1;
	}
	
	public GlobalMappingElement getGlobalMappingElement(String inMappingName, int mappingSelection)
	{
		//Cycle through the list to obtain the mapping id.  Return null
		//if we cannot find the name.
		
		GlobalMappingElement tmpGlobalMappingElement = null;
		for(Enumeration e1 = mappings[mappingSelection].elements(); e1.hasMoreElements() ;)
		{
			tmpGlobalMappingElement = (GlobalMappingElement) e1.nextElement();
			String tmpString = tmpGlobalMappingElement.getMappingName();
			if(tmpString != null)
			{
				if(tmpString.equals(inMappingName))
					return tmpGlobalMappingElement;
			}
		}
		
		//If here,  means that we did not find the mapping name.
		return null;
	}
	
	public Vector getMapping(int mappingSelection)
	{
		return mappings[mappingSelection];
	}
	
	public void updateGenericColors(int mappingSelection)
	{
		if(mappingSelection == 0){
			int tmpInt = jRacy.clrChooser.getNumberOfColors();
			for(Enumeration e = mappings[mappingSelection].elements(); e.hasMoreElements() ;)
			{
				
				GlobalMappingElement tmpGME = (GlobalMappingElement) e.nextElement();
				int mappingID = tmpGME.getGlobalID();
				tmpGME.setGenericColor(jRacy.clrChooser.getColorInLocation(mappingID % tmpInt));
			}
		}
		else{
			int tmpInt = jRacy.clrChooser.getNumberOfMappingGroupColors();
			for(Enumeration e = mappings[mappingSelection].elements(); e.hasMoreElements() ;)
			{
				
				GlobalMappingElement tmpGME = (GlobalMappingElement) e.nextElement();
				int mappingID = tmpGME.getGlobalID();
				tmpGME.setGenericColor(jRacy.clrChooser.getMappingGroupColorInLocation(mappingID % tmpInt));
			}
		}		
	}
	
	public void setGroupMappingHighlight(int inGroupID, int mappingSelectionApply, int mappingSelectionSource)
	{
		GlobalMappingElement tmpGME1 = (GlobalMappingElement) mappings[mappingSelectionSource].elementAt(inGroupID);
		Color tmpColor = tmpGME1.getGenericColor();
		System.out.println("r,g,b: " + tmpColor.getRed() + "," + tmpColor.getGreen()+ "," + tmpColor.getBlue());
		
		for(Enumeration e = mappings[mappingSelectionApply].elements(); e.hasMoreElements() ;)
		{
			GlobalMappingElement tmpGME2 = (GlobalMappingElement) e.nextElement();
			if(tmpGME2.isGroupMember(inGroupID)){
				tmpGME2.setGroupColor(tmpColor);
				tmpGME2.setGroupColorFlag(true);
			}
		}
	}
	
	public void unsetGroupMappingHighlight(int mappingSelection)
	{
		for(Enumeration e = mappings[mappingSelection].elements(); e.hasMoreElements() ;)
		{
			GlobalMappingElement tmpGME = (GlobalMappingElement) e.nextElement();
			tmpGME.setGroupColorFlag(false);
		}
	}
	
	public void setIsSelectedGroupOn(boolean inBool){
		isSelectedGroupOn = inBool;
	}
	
	public boolean getIsSelectedGroupOn(){
		return isSelectedGroupOn;
	}
	
	public void setSelectedGroupID(int inInt){
		selectedGroupID = inInt;
	}
	
	public int getSelectedGroupID(){
		return selectedGroupID;
	}
	
	public void displayMappingLedger(int mappingSelection)
	{
		if(mappingLedgerWindows[mappingSelection] == null)
		{
			
			mappingLedgerWindows[mappingSelection] = new MappingLedgerWindow(mappings[mappingSelection], mappingSelection);
			//Add the main window as a listener as it needs
			//to know when this window closes.
			mappingLedgerWindows[mappingSelection].addWindowListener(this);
			jRacy.systemEvents.addObserver(mappingLedgerWindows[mappingSelection]);
			mappingLedgerWindows[mappingSelection].show();
		}
		else
		{
			//Just bring it to the foreground.
			mappingLedgerWindows[mappingSelection].show();
		}
	}
	
	public void closeMappingLedger(int mappingSelection)
	{
	
		if(jRacy.debugIsOn)
			System.out.println("Inside closeMappingLedger - mapping selection is: " + mappingSelection);
		
		if(mappingLedgerWindows[mappingSelection] != null)
		{
			jRacy.systemEvents.deleteObserver(mappingLedgerWindows[mappingSelection]);
			mappingLedgerWindows[mappingSelection].setVisible(false);
			mappingLedgerWindows[mappingSelection].dispose();
			mappingLedgerWindows[mappingSelection] = null;
		}
		else
		{
			if(jRacy.debugIsOn)
				System.out.println("Inside closeMappingLedger - mapping selection " + mappingSelection + " is null.  Not attemptinga close.");
		}
		
	}
	
	//Window Listener code.
	public void windowClosed(WindowEvent winevt){}
	public void windowIconified(WindowEvent winevt){}
	public void windowOpened(WindowEvent winevt){}
	public void windowClosing(WindowEvent winevt)
	{
		if(winevt.getSource() == mappingLedgerWindows[0])
		{
			mappingLedgerWindows[0] = null;
		}
		
		if(winevt.getSource() == mappingLedgerWindows[1])
		{
			mappingLedgerWindows[1] = null;
		}
	}
	
	public void windowDeiconified(WindowEvent winevt){}
	public void windowActivated(WindowEvent winevt){}
	public void windowDeactivated(WindowEvent winevt){}
	
	
	//Instance element.
	Vector[] mappings;
	int[] numberOfMappings;
	MappingLedgerWindow[] mappingLedgerWindows;
	
	int sizeOfArray = 2;
	
	boolean isSelectedGroupOn = false;
	int selectedGroupID = -1;
	
	private MappingLedgerWindow mappingLedgerWindow;
}