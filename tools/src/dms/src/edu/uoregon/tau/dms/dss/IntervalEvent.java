package edu.uoregon.tau.dms.dss;

import edu.uoregon.tau.dms.database.DB;
import java.util.Vector;
import java.util.Hashtable;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Holds all the data for an interval_event in the database.
 * This object is returned by the DataSession class and all of its subtypes.
 * The IntervalEvent object contains all the information associated with
 * an intervalEvent from which the TAU performance data has been generated.
 * A intervalEvent is associated with one trial, experiment and application, and has one or more
 * IntervalLocationProfile objects (one for each node/context/thread location in the trial) associated with it.  
 * <p>
 * An interval event has information
 * related to one particular interval event in the application, including the name of the interval event,
 * the TAU group it belongs to, and all of the total and mean data for the interval event. 
 * In order to see particular measurements for a node/context/thread/metric instance,
 * get the IntervalLocationProfile(s) for this IntervalEvent.  In order to access the total
 * or mean data, getTotalSummary() and getMeanSummary() methods are provided.  The
 * index of the metric in the Trial object should be used to indicate which total/mean
 * summary object to return.
 *
 * <P>CVS $Id: IntervalEvent.java,v 1.10 2004/12/29 00:00:44 amorris Exp $</P>
 * @author	Kevin Huck, Robert Bell
 * @version	0.1
 * @since	0.1
 * @see		DataSession#getIntervalEvents
 * @see		DataSession#setIntervalEvent
 * @see		Application
 * @see		Experiment
 * @see		Trial
 * @see		IntervalLocationProfile
 */
public class IntervalEvent {
    private int intervalEventID;
    private String name;
    private String group;
    private int trialID;
    private IntervalLocationProfile meanSummary = null;
    private IntervalLocationProfile totalSummary = null;
    private DatabaseAPI dataSession = null;

    public IntervalEvent(DatabaseAPI dataSession) {
        this.dataSession = dataSession;
    }

    /**
     * Gets the unique identifier of this intervalEvent object.
     *
     * @return	the unique identifier of the intervalEvent
     */
    public int getID() {
        return this.intervalEventID;
    }

    /**
     * Gets the intervalEvent name.
     *
     * @return	the name of the intervalEvent
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the TAU group of this intervalEvent object.
     *
     * @return	the TAU group the intervalEvent is in.
     */
    public String getGroup() {
        return this.group;
    }

    /**
     * Gets the trial ID of this intervalEvent object.
     *
     * @return	the trial ID for the intervalEvent.
     */
    public int getTrialID() {
        return this.trialID;
    }

    /**
     * Gets mean summary data for the intervalEvent object.
     * The Trial object associated with this intervalEvent has a vector of metric
     * names that represent the metric data stored for each intervalEvent in the
     * application.  Multiple metrics can be recorded for each trial.  If the
     * user has selected a metric before calling getIntervalEvents(), then this method
     * can be used to return the mean metric data for this 
     * intervalEvent/trial/experiment/application combination.
     * The mean data is averaged across all locations, defined as any combination
     * of node/context/thread.
     *
     * @return	the IntervalLocationProfile containing the mean data for this intervalEvent/metric combination.
     * @see		Trial
     * @see		IntervalLocationProfile
     * @see		DataSession#getIntervalEvents
     * @see		DataSession#setMetric(Metric)
     */
    public IntervalLocationProfile getMeanSummary() throws SQLException {
        if (this.meanSummary == null)
            dataSession.getIntervalEventDetail(this);
        return (this.meanSummary);
    }

    /**
     * Gets total summary data for the intervalEvent object.
     * The Trial object associated with this intervalEvent has a vector of metric
     * names that represent the metric data stored for each intervalEvent in the
     * application.  Multiple metrics can be recorded for each trial.  If the
     * user has selected a metric before calling getIntervalEvents(), then this method
     * can be used to return the total metric data for this 
     * intervalEvent/trial/experiment/application combination.
     * The total data is summed across all locations, defined as any combination
     * of node/context/thread.
     *
     * @return	the IntervalLocationProfile containing the total data for this intervalEvent/metric combination.
     * @see		Trial
     * @see		IntervalLocationProfile
     * @see		DataSession#getIntervalEvents
     */
    public IntervalLocationProfile getTotalSummary() throws SQLException {
        if (this.totalSummary == null)
            dataSession.getIntervalEventDetail(this);
        return (this.totalSummary);
    }

    /**
     * Sets the unique ID associated with this interval event.
     * <i> NOTE: This method is used by the DataSession object to initialize
     * the object.  Not currently intended for use by any other code.</i>
     *
     * @param	id unique ID associated with this intervalEvent
     */
    public void setID(int id) {
        this.intervalEventID = id;
    }

    /**
     * Sets the intervalEvent name.
     * <i> NOTE: This method is used by the DataSession object to initialize
     * the object.  Not currently intended for use by any other code.</i>
     *
     * @param	name the name of the intervalEvent
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the TAU group of this intervalEvent object.
     * <i> NOTE: This method is used by the DataSession object to initialize
     * the object.  Not currently intended for use by any other code.</i>
     *
     * @param	group the TAU group the intervalEvent is in.
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Sets the trial ID of this intervalEvent object.
     * <i> NOTE: This method is used by the DataSession object to initialize
     * the object.  Not currently intended for use by any other code.</i>
     *
     * @param	id the trial ID for the intervalEvent.
     */
    public void setTrialID(int id) {
        this.trialID = id;
    }

    /**
     * Adds a IntervalLocationProfile to the intervalEvent as a mean summary.
     * <i> NOTE: This method is used by the DataSession object to initialize
     * the object.  Not currently intended for use by any other code.</i>
     *
     * @param	meanSummary the mean summary object for the intervalEvent.
     */
    public void setMeanSummary(IntervalLocationProfile meanSummary) {
        this.meanSummary = meanSummary;
    }

    /**
     * Adds a IntervalLocationProfile to the intervalEvent as a total summary.
     * <i> NOTE: This method is used by the DataSession object to initialize
     * the object.  Not currently intended for use by any other code.</i>
     *
     * @param	totalSummary the total summary object for the intervalEvent.
     */
    public void setTotalSummary(IntervalLocationProfile totalSummary) {
        this.totalSummary = totalSummary;
    }

    // returns a Vector of IntervalEvents
    public static Vector getIntervalEvents(DatabaseAPI dataSession, DB db, String whereClause) {
        Vector events = new Vector();
        // create a string to hit the database
        StringBuffer buf = new StringBuffer();
        buf.append("select id, name, group_name, trial ");
        buf.append("from " + db.getSchemaPrefix() + "interval_event ");
        buf.append(whereClause);

        if (db.getDBType().compareTo("oracle") == 0) {
            buf.append(" order by dbms_lob.substr(name) asc");
        } else {
            buf.append(" order by name asc ");
        }

        // System.out.println(buf.toString());

        // get the results
        try {
            ResultSet resultSet = db.executeQuery(buf.toString());
            DatabaseAPI.itemsDone++;

            IntervalEvent tmpIntervalEvent = null;
            while (resultSet.next() != false) {
                IntervalEvent event = new IntervalEvent(dataSession);
                event.setID(resultSet.getInt(1));
                event.setName(resultSet.getString(2));
                event.setGroup(resultSet.getString(3));
                event.setTrialID(resultSet.getInt(4));
                events.addElement(event);
            }
            resultSet.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return events;
    }

    public int saveIntervalEvent(DB db, int newTrialID, Hashtable newMetHash, int saveMetricIndex)
            throws SQLException {
        int newIntervalEventID = 0;

        PreparedStatement statement = null;
        if (saveMetricIndex < 0) {
            //		statement = db.prepareStatement("INSERT INTO interval_event (trial, name, group_name) VALUES (?, ?, ?)");
            statement = db.prepareStatement("INSERT INTO " + db.getSchemaPrefix()
                    + "interval_event (trial, name, group_name) VALUES (?, ?, ?)");
            statement.setInt(1, newTrialID);
            statement.setString(2, name);
            statement.setString(3, group);
            statement.executeUpdate();
            statement.close();

            String tmpStr = new String();
            if (db.getDBType().compareTo("mysql") == 0)
                tmpStr = "select LAST_INSERT_ID();";
            else if (db.getDBType().compareTo("db2") == 0)
                tmpStr = "select IDENTITY_VAL_LOCAL() FROM interval_event";
            else if (db.getDBType().compareTo("oracle") == 0)
                tmpStr = "select " + db.getSchemaPrefix() + "interval_event_id_seq.currval FROM dual";
            else
                tmpStr = "select currval('interval_event_id_seq');";
            newIntervalEventID = Integer.parseInt(db.getDataItem(tmpStr));
        } else {

            if (db.getDBType().compareTo("oracle") == 0)
                statement = db.prepareStatement("SELECT id FROM " + db.getSchemaPrefix()
                        + "interval_event where dbms_lob.instr(name, ?) > 0");
            else
                statement = db.prepareStatement("SELECT id FROM " + db.getSchemaPrefix()
                        + "interval_event where name = ?");

            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next() != false) {
                newIntervalEventID = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
        }

        // save the intervalEvent mean summaries
        if (meanSummary != null) {
            meanSummary.saveMeanSummary(db, newIntervalEventID, newMetHash, saveMetricIndex);
        }

        // save the intervalEvent total summaries
        if (totalSummary != null) {
            totalSummary.saveTotalSummary(db, newIntervalEventID, newMetHash, saveMetricIndex);
        }
        return newIntervalEventID;
    }
}
