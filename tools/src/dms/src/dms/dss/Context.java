/* 
   Name:       Context.java
   Author:     Robert Bell
   Description:  
*/

package dms.dss;

import java.util.*;

public class Context implements Comparable{

    public Context(){
	threads = new Vector();}

    public Context(int nodeID, int contextID){
	this.nodeID = nodeID;
	this.contextID = contextID;
	threads = new Vector();
    }

    public void setThreadId(int nodeID){
	this.nodeID = nodeID;}

    public int getThreadID(){
	return nodeID;}

    public void setContextID(int contextID){
	this.contextID = contextID;}

    public int getContextID(){
	return contextID;}
    
    
    //Adds the specified thread to the list of threads.
    public void addThread(Thread thread){
	try{
	    if(thread.getThreadID()<0){
		System.out.println("Error - Invalid thread id (id less than zero). Thread not added!");
		return;
	    }

	    int pos = this.getThreadPosition(thread);
	    if(pos>=0)
		System.out.println("Error - Thread already present. Thread not added!");
	    else
		threads.insertElementAt(thread, (-(pos+1)));
	}
	catch(Exception e){
	    UtilFncs.systemError(e, null, "N1");
	}
    }

    //Creates a thread with the specified thread id and adds it to the list of threads.
    public Thread addThread(int threadID){
	Thread thread = null;
	try{
	    if(threadID<0){
		System.out.println("Error - Invalid thread id (id less than zero). Thread not added!");
		return null;
	    }

	    int pos = this.getThreadPosition(new Integer(threadID));
	    if(pos>=0)
		System.out.println("Error - Thread already present. Thread not added!");
	    else{
		thread = new Thread(nodeID, contextID, threadID);
		threads.insertElementAt(thread, (-(pos+1)));
	    }
	    return thread;
	}
	catch(Exception e){
	    UtilFncs.systemError(e, null, "N2");
	}
	return thread;
    }

    //Creates a thread with the specified thread id and capacity and adds it to the list of threads.
    public Thread addThread(int threadID, int capacity){
	Thread thread = null;
	try{
	    if(threadID<0){
		System.out.println("Error - Invalid thread id (id less than zero). Thread not added!");
		return null;
	    }

	    int pos = this.getThreadPosition(new Integer(threadID));
	    if(pos>=0)
		System.out.println("Error - Thread already present. Thread not added!");
	    else{
		thread = new Thread(nodeID, contextID, threadID, capacity);
		threads.insertElementAt(thread, (-(pos+1)));
	    }
	    return thread;
	}
	catch(Exception e){
	    UtilFncs.systemError(e, null, "N2");
	}
	return thread;
    }
    
    public Vector getThreads(){
	return threads;}


    //Gets the thread with the specified thread id.  If the thread is not found, the function returns
    //null.
    public Thread getThread(int threadID){
	Thread thread = null;
	try{
	    int pos = getThreadPosition(new Integer(threadID));
	    if(pos>=0)
		thread = (Thread) threads.elementAt(pos);
	}
	catch(Exception e){
	    UtilFncs.systemError(e, null, "N3");
	}
	return thread;
    }

    public int getNumberOfThreads(){
	return threads.size();}

    private int getThreadPosition(Integer integer){
	return Collections.binarySearch(threads, integer);}

    private int getThreadPosition(Thread thread){
	return Collections.binarySearch(threads, thread);}

    public int compareTo(Object obj){
	if(obj instanceof Integer)
	    return contextID - ((Integer)obj).intValue();
	else
	    return contextID - ((Context)obj).getContextID();
    }
    
    //Instance data.
    int nodeID = -1;
    int contextID = -1;
    Vector threads;
}
