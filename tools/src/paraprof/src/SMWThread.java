/* 
   SMWThread.java

   Title:      ParaProf
   Author:     Robert Bell
   Description:  
*/

package paraprof;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class SMWThread{

    public SMWThread(){}

    public SMWThread(Thread thread){
	this.thread = thread;}

    public int getNodeID(){
	return this.thread.getNodeID();}

    public int getContextID(){
	return this.thread.getContextID();}

    public int getThreadID(){
	return this.thread.getThreadID();}
  
    public void addFunction(SMWThreadDataElement sMWThreadDataElement){
	functions.addElement(sMWThreadDataElement);}

    public void addUserevent(SMWThreadDataElement sMWThreadDataElement){
	userevents.addElement(sMWThreadDataElement);}

    public Vector getFunctionList(){
	return functions;}

    public ListIterator getFunctionListIterator(){
	return new ParaProfIterator(functions);}
  
    public Vector getUsereventList(){
	return userevents;}

    public ListIterator getUsereventListIterator(){
	return new ParaProfIterator(userevents);}
  
    //Rest of the public functions.
    public void setYDrawCoord(int yDrawCoord){
	yDrawCoord = this.yDrawCoord;}
  
    public int getYDrawCoord(){
	return yDrawCoord;}
    
    //####################################
    //Instance data.
    //####################################
    Thread thread = null;
    Vector functions = new Vector();
    Vector userevents = new Vector();
    //To aid with drawing searches.
    int yDrawCoord = -1;
    //####################################
    //End - Instance data.
    //####################################
}
