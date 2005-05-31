/* 
  
  ParaProfImageOutput.java
  
  Title:       ParaProfImageOutput.java
  Author:      Robert Bell
  Description: Handles the output of the various panels to image files.
*/

package edu.uoregon.tau.paraprof;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import edu.uoregon.tau.dms.dss.*;
import edu.uoregon.tau.paraprof.interfaces.*;

public class ParaProfImageOutput{

    public ParaProfImageOutput(){
    }

    public static void saveImage(ImageExport ref){
	try{
	    JOptionPane.showMessageDialog(null, "Jar compiled for jdk-ver < 1.4. Image support not available.",
					  "Image Error!",
					  JOptionPane.ERROR_MESSAGE);
	} catch(Exception e){
	}
    }

    
    public static void save3dImage(final ThreeDeeWindow ref) throws IOException {

    }

}
