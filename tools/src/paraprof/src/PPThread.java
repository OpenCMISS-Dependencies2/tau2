/* 
 PPThread.java

 Title:      ParaProf
 Author:     Robert Bell
 Description:  
 */

package edu.uoregon.tau.paraprof;

import java.util.*;
import edu.uoregon.tau.perfdmf.Thread;
import edu.uoregon.tau.perfdmf.*;

public class PPThread {

    private ParaProfTrial ppTrial;
    private Thread thread = null;
    private List functions = new ArrayList();
    private List userevents = new ArrayList();


    public PPThread(Thread thread, ParaProfTrial ppTrial) {
        if (thread == null) {
            throw new ParaProfException("PPThread constructor called with null thread");
        }
        this.ppTrial = ppTrial;
        this.thread = thread;
    }

    public Thread getThread() {
        return thread;
    }

    public int getNodeID() {
        return this.thread.getNodeID();
    }

    public int getContextID() {
        return this.thread.getContextID();
    }

    public int getThreadID() {
        return this.thread.getThreadID();
    }

    public String getName() {
        if (this.getNodeID() == -1) {
            return "mean";
        } else if (this.getNodeID() == -2) {
            return "total";
        } else if (this.getNodeID() == -3) {
            return "std. dev.";
        } else {
            return "n,c,t " + (this.getNodeID()) + "," + (this.getContextID()) + "," + (this.getThreadID());
        }
    }

    public String getFullName() {
        if (thread.getNodeID() == -1) {
            return "Mean Data";
        } else if (thread.getNodeID() == -2) {
            return "Total Data";
        } else if (thread.getNodeID() == -3) {
            return "Standard Deviation Data";
        } else {
            return getName();
        }
    }
       
    
    public void addFunction(PPFunctionProfile ppFunctionProfile) {
        functions.add(ppFunctionProfile);
    }

    public void addUserevent(PPFunctionProfile ppFunctionProfile) {
        userevents.add(ppFunctionProfile);
    }

    public List getFunctionList() {
        return functions;
    }

    public ListIterator getFunctionListIterator() {
        return functions.listIterator();
    }

    public List getUsereventList() {
        return userevents;
    }

    public ListIterator getUsereventListIterator() {
        return userevents.listIterator();
    }


    public List getSortedFunctionProfiles(DataSorter dataSorter, boolean getAll) {
        List newList = null;
      

        List functionList = thread.getFunctionProfiles();
        newList = new ArrayList();

        for (Iterator e1 = functionList.iterator(); e1.hasNext();) {
            FunctionProfile functionProfile = (FunctionProfile) e1.next();
            if (functionProfile != null) {
                if (getAll || ppTrial.displayFunction(functionProfile.getFunction()) && functionProfile.getFunction().isPhaseMember(dataSorter.getPhase())) {
                    PPFunctionProfile ppFunctionProfile = new PPFunctionProfile(dataSorter, thread, functionProfile);
                    newList.add(ppFunctionProfile);
                }
            }
        }
        Collections.sort(newList);
        return newList;
    }

  

}
