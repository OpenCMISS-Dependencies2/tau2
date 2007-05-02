package edu.uoregon.tau.perfdmf;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import edu.uoregon.tau.perfdmf.database.DB;

/**
 * Reads a single trial from the database
 *  
 * <P>CVS $Id: DBDataSource.java,v 1.6 2007/05/02 17:18:04 amorris Exp $</P>
 * @author  Robert Bell, Alan Morris
 * @version $Revision: 1.6 $
 */
public class DBDataSource extends DataSource {

    private DatabaseAPI databaseAPI;
    private volatile boolean abort = false;
    private volatile int totalItems = 0;
    private volatile int itemsDone = 0;

    public DBDataSource(DatabaseAPI dbAPI) {
        super();
        this.setMetrics(new Vector());
        this.databaseAPI = dbAPI;
    }

    public int getProgress() {
        return 0;
        //return DatabaseAPI.getProgress();
    }

    public void cancelLoad() {
        abort = true;
        return;
    }

    private void getIntervalEventData(Map ieMap) throws SQLException {
        int numMetrics = getNumberOfMetrics();
        // get all the interval event data for all thread
        ListIterator l = databaseAPI.getIntervalEventData().listIterator();
        while (l.hasNext()) {
            IntervalLocationProfile ilp = (IntervalLocationProfile) l.next();
            Thread thread = addThread(ilp.getNode(), ilp.getContext(), ilp.getThread());

            //Function function = this.getFunction(databaseAPI.getIntervalEvent(fdo.getIntervalEventID()).getName());
            Function function = (Function) ieMap.get(new Integer(ilp.getIntervalEventID()));
            FunctionProfile functionProfile = thread.getFunctionProfile(function);

            if (functionProfile == null) {
                functionProfile = new FunctionProfile(function, numMetrics);
                thread.addFunctionProfile(functionProfile);
            }

            for (int i = 0; i < numMetrics; i++) {
                functionProfile.setExclusive(i, ilp.getExclusive(i));
                functionProfile.setInclusive(i, ilp.getInclusive(i));
                //functionProfile.setExclusivePercent(i, ilp.getExclusivePercentage(i));
                //functionProfile.setInclusivePercent(i, ilp.getInclusivePercentage(i));
                // we don't store this as a value, it is derived
                //functionProfile.setInclusivePerCall(i, fdo.getInclusivePerCall(i));
                functionProfile.setNumCalls(ilp.getNumCalls());
                functionProfile.setNumSubr(ilp.getNumSubroutines());
            }
        }
    }

    private void fastGetIntervalEventData(Map ieMap, Map metricMap) throws SQLException {
        int numMetrics = getNumberOfMetrics();
        DB db = databaseAPI.getDb();

        
        StringBuffer where = new StringBuffer();

        where.append(" WHERE p.metric in (");
        for (Iterator it = metricMap.keySet().iterator(); it.hasNext();) {
            int metricID = ((Integer) it.next()).intValue();
            where.append(metricID);
            if (it.hasNext()) {
                where.append(", ");
            } else {
                where.append(") ");
            }
        }
        
        // the much slower way
//        where.append(" WHERE p.interval_event in (");
//        for (Iterator it = ieMap.keySet().iterator(); it.hasNext();) {
//            int id = ((Integer) it.next()).intValue();
//            where.append(id);
//            if (it.hasNext()) {
//                where.append(", ");
//            } else {
//                where.append(") ");
//            }
//        }

        StringBuffer buf = new StringBuffer();
        buf.append("select p.interval_event, p.metric, p.node, p.context, p.thread, ");

        if (db.getDBType().compareTo("oracle") == 0) {
            buf.append("p.inclusive, p.excl, ");
        } else {
            buf.append("p.inclusive, p.exclusive, ");
        }
        if (db.getDBType().compareTo("derby") == 0) {
            buf.append("p.num_calls, ");
        } else {
            buf.append("p.call, ");
        }
        buf.append("p.subroutines ");
        buf.append("from interval_location_profile p ");
        buf.append(where);
        //buf.append(" order by p.interval_event, p.node, p.context, p.thread, p.metric ");
        //System.out.println(buf.toString());

        /*
         1 - interval_event
         2 - metric
         3 - node
         4 - context
         5 - thread
         6 - inclusive
         7 - exclusive
         8 - num_calls
         9 - num_subrs
         */
        
        // get the results
        long time = System.currentTimeMillis();
        ResultSet resultSet = db.executeQuery(buf.toString());
        time = (System.currentTimeMillis()) - time;
        //System.out.println("Query : " + time);

        time = System.currentTimeMillis();
        while (resultSet.next() != false) {

            int intervalEventID = resultSet.getInt(1);
            Function function = (Function) ieMap.get(new Integer(intervalEventID));

            int nodeID = resultSet.getInt(3);
            int contextID = resultSet.getInt(4);
            int threadID = resultSet.getInt(5);

            Thread thread = addThread(nodeID, contextID, threadID);
            FunctionProfile functionProfile = thread.getFunctionProfile(function);

            if (functionProfile == null) {
                functionProfile = new FunctionProfile(function, numMetrics);
                thread.addFunctionProfile(functionProfile);
            }

            int metricIndex = ((Metric)metricMap.get(new Integer(resultSet.getInt(2)))).getID();
            double inclusive, exclusive;

            inclusive = resultSet.getDouble(6);
            exclusive = resultSet.getDouble(7);
            double numcalls = resultSet.getDouble(8);
            double numsubr = resultSet.getDouble(9);

            functionProfile.setNumCalls(numcalls);
            functionProfile.setNumSubr(numsubr);
            functionProfile.setExclusive(metricIndex, exclusive);
            functionProfile.setInclusive(metricIndex, inclusive);
        }
        time = (System.currentTimeMillis()) - time;
        //System.out.println("Processing : " + time);

        resultSet.close();
    }

    public void load() throws SQLException {

        // System.out.println("Processing data, please wait ......");
        long time = System.currentTimeMillis();

       
        DB db = databaseAPI.getDb();
        StringBuffer joe = new StringBuffer();
        joe.append("SELECT id, name ");
        joe.append("FROM " + db.getSchemaPrefix() + "metric ");
        joe.append("WHERE trial = ");
        joe.append(databaseAPI.getTrial().getID());
        joe.append(" ORDER BY id ");

        Map metricMap = new HashMap();
        
        ResultSet resultSet = db.executeQuery(joe.toString());
        int numberOfMetrics = 0;
        while (resultSet.next() != false) {
            int id = resultSet.getInt(1);
            String name = resultSet.getString(2);
            Metric metric = this.addMetric(name);
            metricMap.put(new Integer(id), metric);
            numberOfMetrics++;
        }
        resultSet.close();

        // map Interval Event ID's to Function objects
        Map ieMap = new HashMap();

        // iterate over interval events (functions), create the function objects and add them to the map
        List intervalEvents = databaseAPI.getIntervalEvents();
        ListIterator l = intervalEvents.listIterator();
        while (l.hasNext()) {
            IntervalEvent ie = (IntervalEvent) l.next();
            Function function = this.addFunction(ie.getName(), numberOfMetrics);
            addGroups(ie.getGroup(), function);
            ieMap.put(new Integer(ie.getID()), function);
        }

        //getIntervalEventData(ieMap);
        fastGetIntervalEventData(ieMap, metricMap);

        // map Interval Event ID's to Function objects
        Map aeMap = new HashMap();

        l = databaseAPI.getAtomicEvents().listIterator();
        while (l.hasNext()) {
            AtomicEvent atomicEvent = (AtomicEvent) l.next();
            UserEvent userEvent = addUserEvent(atomicEvent.getName());
            aeMap.put(new Integer(atomicEvent.getID()), userEvent);
        }

        l = databaseAPI.getAtomicEventData().listIterator();
        while (l.hasNext()) {
            AtomicLocationProfile alp = (AtomicLocationProfile) l.next();
            Thread thread = addThread(alp.getNode(), alp.getContext(), alp.getThread());
            UserEvent userEvent = (UserEvent) aeMap.get(new Integer(alp.getAtomicEventID()));
            UserEventProfile userEventProfile = thread.getUserEventProfile(userEvent);

            if (userEventProfile == null) {
                userEventProfile = new UserEventProfile(userEvent);
                thread.addUserEventProfile(userEventProfile);
            }

            userEventProfile.setNumSamples(alp.getSampleCount());
            userEventProfile.setMaxValue(alp.getMaximumValue());
            userEventProfile.setMinValue(alp.getMinimumValue());
            userEventProfile.setMeanValue(alp.getMeanValue());
            userEventProfile.setSumSquared(alp.getSumSquared());
            userEventProfile.updateMax();
        }

        time = (System.currentTimeMillis()) - time;
        //System.out.println("Time to download file (in milliseconds): " + time);

        // We actually discard the mean and total values by calling this
        // But, we need to compute other statistics anyway
        generateDerivedData();
    }
}
