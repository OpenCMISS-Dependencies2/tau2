package edu.uoregon.tau.perfdmf;

import java.util.*;

/**
 * This class represents a single function profile on a single thread.
 *
 * <P>CVS $Id: FunctionProfile.java,v 1.7 2007/01/21 23:16:13 amorris Exp $</P>
 * @author	Robert Bell, Alan Morris
 * @version	$Revision: 1.7 $
 * @see		Function
 */
public class FunctionProfile {

    // this is a private static class to save memory when callpath data is not needed
    // we need only one empty pointer instead of four
    private static class CallPathData {
        public Set childProfiles;
        public Set parentProfiles;
        public Map childProfileCallPathSets;
        public Map parentProfileCallPathSets;
    }

    private static final int METRIC_SIZE = 2;

    private static final int CALLS = 0;
    private static final int SUBR = 1;
    private static final int INCLUSIVE = 2;
    private static final int EXCLUSIVE = 3;

    private Function function;
    private Thread thread;
    private double[] data;
    private CallPathData callPathData;

    public FunctionProfile(Function function) {
        this(function, 1);
    }

    public FunctionProfile(Function function, int numMetrics) {
        this(function, numMetrics, 1);
    }

    public FunctionProfile(Function function, int numMetrics, int snapshots) {
        numMetrics = Math.max(numMetrics, 1);
        data = new double[((numMetrics + 1) * METRIC_SIZE) * snapshots];
        this.function = function;
    }

    public Function getFunction() {
        return function;
    }

    public String getName() {
        return function.getName();
    }

    public void setInclusive(int metric, double value) {
        this.putDouble(metric, INCLUSIVE, value);
    }

    public void setInclusive(int snapshot, int metric, double value) {
        this.putDouble(snapshot, metric, INCLUSIVE, value);
    }

    public double getInclusive(int metric) {
        return this.getDouble(metric, INCLUSIVE);
    }

    public double getInclusive(int metric, int snapshot) {
        return this.getDouble(snapshot, metric, INCLUSIVE);
    }

    public void setExclusive(int metric, double value) {
        this.putDouble(metric, EXCLUSIVE, value);
    }

    public void setExclusive(int snapshot, int metric, double value) {
        this.putDouble(snapshot, metric, EXCLUSIVE, value);
    }

    public double getExclusive(int snapshot, int metric) {
        return this.getDouble(snapshot, metric, EXCLUSIVE);
    }

    public double getExclusive(int metric) {
        //        if (function.isPhase()) {
        //            return this.getDouble(metric, 0);
        //        } else {
        //            return this.getDouble(metric, 1);
        //        }
        return this.getDouble(metric, EXCLUSIVE);
    }

    public void setInclusivePercent(int metric, double value) {
        //this.putDouble(thread.getNumSnapshots() - 1, metric, INCLUSIVE_PERCENT, value);
    }

    public double getInclusivePercent(int metric) {
        if (thread.getNodeID() >= 0) {
            return getInclusive(metric) / thread.getPercentDivider(metric);
        } else if (thread.getNodeID() == Thread.TOTAL || thread.getNodeID() == Thread.MEAN) {
            return function.getTotalInclusive(metric) / thread.getPercentDivider(metric);
        } else if (thread.getNodeID() == Thread.STDDEV) {
            return getInclusive(metric) / function.getMeanInclusive(metric) * 100.0;
        }
        throw new RuntimeException("Bad Thread ID = " + thread);
    }

    public void setExclusivePercent(int metric, double value) {
        //this.putDouble(thread.getNumSnapshots() - 1, metric, EXCLUSIVE_PERCENT, value);
    }

    public double getExclusivePercent(int metric) {
        if (thread.getNodeID() >= 0) {
            return getExclusive(metric) / thread.getPercentDivider(metric);
        } else if (thread.getNodeID() == Thread.TOTAL || thread.getNodeID() == Thread.MEAN) {
            return function.getTotalExclusive(metric) / thread.getPercentDivider(metric);
        } else if (thread.getNodeID() == Thread.STDDEV) {
            return getExclusive(metric) / function.getMeanExclusive(metric) * 100.0;
        }
        throw new RuntimeException("Bad Thread ID = " + thread);
    }

    public void setNumCalls(double value) {
        this.putDouble(0, CALLS, value);
    }

    public double getNumCalls() {
        return getDouble(0, CALLS);
    }

    public void setNumSubr(double value) {
        this.putDouble(0, SUBR, value);
    }

    public double getNumSubr() {
        return getDouble(0, SUBR);
    }

    public double getInclusivePerCall(int metric) {
        if (this.getNumCalls() == 0) {
            return 0;
        }
        return this.getInclusive(metric) / this.getNumCalls();
    }

    public double getExclusivePerCall(int metric) {
        if (this.getNumCalls() == 0) {
            return 0;
        }
        return this.getExclusive(metric) / this.getNumCalls();
    }

    public void addMetric() {
        int numMetrics = thread.getNumMetrics() - 1;
        int numSnapshots = thread.getNumSnapshots();
        int newMetricSize = numMetrics + 1;

        double[] newArray = new double[(newMetricSize + 1) * METRIC_SIZE * numSnapshots];

        for (int s = 0; s < numSnapshots; s++) {

            int source = (s * METRIC_SIZE * (numMetrics + 1));
            int dest = (s * METRIC_SIZE * (newMetricSize + 1));
            for (int m = 0; m < METRIC_SIZE * (numMetrics + 1); m++) {
                newArray[dest + m] = data[source + m];
            }

        }

        data = newArray;
    }

    public void addSnapshot() {
        //        int newCallsLength = thread.getNumSnapshots() * CALL_SIZE;
        //        if (newCallsLength > calls.length) {
        //            // could only do this with Java 1.6 :(
        //            //calls = Arrays.copyOf(calls, (int)(newCallsLength*1.5));
        //            double[] newCalls = new double[(int) (newCallsLength * 1.5)];
        //            System.arraycopy(calls, 0, newCalls, 0, calls.length);
        //            calls = newCalls;
        //        }

        int numMetrics = thread.getNumMetrics();
        int newLength = thread.getNumSnapshots() * ((METRIC_SIZE * numMetrics) + 2);
        if (newLength > data.length) {
            // could only do this with Java 1.6 :(
            //data = Arrays.copyOf(data, (int)(newLength*1.5));
            double[] newArray = new double[(int) (newLength * 1.5)];
            System.arraycopy(data, 0, newArray, 0, data.length);
            data = newArray;
        }
    }

    // call path section
    public void addChildProfile(FunctionProfile child, FunctionProfile callpath) {
        // example:
        // callpath: a => b => c => d
        // child: d
        // this: c
        CallPathData callPathData = getCallPathData();

        if (callPathData.childProfiles == null)
            callPathData.childProfiles = new HashSet();
        callPathData.childProfiles.add(child);

        if (callPathData.childProfileCallPathSets == null)
            callPathData.childProfileCallPathSets = new HashMap();

        // we maintain a set of callpaths for each child, retrieve the set for this child
        Set callPathSet = (Set) callPathData.childProfileCallPathSets.get(child);

        if (callPathSet == null) {
            callPathSet = new HashSet();
            callPathData.childProfileCallPathSets.put(child, callPathSet);
        }

        callPathSet.add(callpath);
    }

    public void addParentProfile(FunctionProfile parent, FunctionProfile callpath) {
        // example:
        // callpath: a => b => c => d
        // parent: c
        // this: d

        CallPathData callPathData = getCallPathData();

        if (callPathData.parentProfiles == null)
            callPathData.parentProfiles = new HashSet();
        callPathData.parentProfiles.add(parent);

        if (callPathData.parentProfileCallPathSets == null)
            callPathData.parentProfileCallPathSets = new HashMap();

        // we maintain a set of callpaths for each child, retrieve the set for this child
        Set callPathSet = (Set) callPathData.parentProfileCallPathSets.get(parent);

        if (callPathSet == null) {
            callPathSet = new HashSet();
            callPathData.parentProfileCallPathSets.put(parent, callPathSet);
        }

        callPathSet.add(callpath);
    }

    public Iterator getChildProfiles() {
        CallPathData callPathData = getCallPathData();
        if (callPathData.childProfiles != null)
            return callPathData.childProfiles.iterator();
        return new UtilFncs.EmptyIterator();
    }

    public Iterator getParentProfiles() {
        CallPathData callPathData = getCallPathData();
        if (callPathData.parentProfiles != null)
            return callPathData.parentProfiles.iterator();
        return new UtilFncs.EmptyIterator();
    }

    public Iterator getParentProfileCallPathIterator(FunctionProfile parent) {
        CallPathData callPathData = getCallPathData();
        if (callPathData.parentProfileCallPathSets == null)
            return new UtilFncs.EmptyIterator();
        return ((Set) callPathData.parentProfileCallPathSets.get(parent)).iterator();
    }

    public Iterator getChildProfileCallPathIterator(FunctionProfile child) {
        CallPathData callPathData = getCallPathData();
        if (callPathData.childProfileCallPathSets == null)
            return new UtilFncs.EmptyIterator();
        return ((Set) callPathData.childProfileCallPathSets.get(child)).iterator();
    }

    /**
     * Passthrough to the actual function's isCallPathFunction
     * 
     * @return		whether or not this function is a callpath (contains '=>')
     */
    public boolean isCallPathFunction() {
        return function.isCallPathFunction();
    }

    private void putDouble(int snapshot, int metric, int offset, double inDouble) {
        int numMetrics = thread.getNumMetrics();
        int location = (snapshot * (METRIC_SIZE * (numMetrics + 1))) + (metric * METRIC_SIZE) + offset;
        data[location] = inDouble;
    }

    private void putDouble(int metric, int offset, double inDouble) {
        int snapshot = thread.getNumSnapshots() - 1;
        int numMetrics = thread.getNumMetrics();
        int location = (snapshot * (METRIC_SIZE * (numMetrics + 1))) + (metric * METRIC_SIZE) + offset;
        data[location] = inDouble;
    }

    private double getDouble(int snapshot, int metric, int offset) {
        int numMetrics = thread.getNumMetrics();
        int location = (snapshot * (METRIC_SIZE * (numMetrics + 1))) + (metric * METRIC_SIZE) + offset;
        return data[location];
    }

    private double getDouble(int metric, int offset) {
        int snapshot = thread.getNumSnapshots() - 1;
        int numMetrics = thread.getNumMetrics();
        int location = (snapshot * (METRIC_SIZE * (numMetrics + 1))) + (metric * METRIC_SIZE) + offset;
        return data[location];
    }

    public String toString() {
        return thread + " : " + function;
    }

    private CallPathData getCallPathData() {
        if (callPathData == null) {
            callPathData = new CallPathData();
        }
        return callPathData;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
}