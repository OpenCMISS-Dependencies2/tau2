package common;

import edu.uoregon.tau.perfdmf.DatabaseAPI;
import edu.uoregon.tau.perfdmf.IntervalEvent;
import edu.uoregon.tau.perfdmf.IntervalLocationProfile;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.text.FieldPosition;


/**
 * This class is the RMI class which contains the tree of views to be 
 * constructed in the PerfExplorerClient.
 *
 * <P>CVS $Id: RMISortableIntervalEvent.java,v 1.1 2006/06/14 03:57:02 khuck Exp $</P>
 * @author khuck
 * @version 0.1
 * @since   0.1
 *
 */
public class RMISortableIntervalEvent extends IntervalEvent implements Serializable, Comparable {
	public int metricIndex;
	public RMISortableIntervalEvent (IntervalEvent e, DatabaseAPI dataSession, int metricIndex) {
		super(dataSession);
		this.setID(e.getID());
		this.setName(e.getName());
		this.setGroup(e.getGroup());
		this.setTrialID(e.getTrialID());
        try {
			this.setMeanSummary(e.getMeanSummary());
			this.setTotalSummary(e.getTotalSummary());
        } catch (SQLException exception) {
        }
		this.metricIndex = metricIndex;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
        try {
			DecimalFormat format = new DecimalFormat("00.00");
			FieldPosition f = new FieldPosition(0);
			format.format(this.getMeanSummary().getExclusivePercentage(), buf, f);
			buf.append("%");
        } catch (Exception exception) {}
		buf.append(" : ");
		buf.append(this.getName());
		return buf.toString();
	}

    public int compareTo(Object o) {
        IntervalEvent e = (IntervalEvent) o;
        IntervalLocationProfile ms1 = null;
        IntervalLocationProfile ms2 = null;
        try {
            ms1 = this.getMeanSummary();
            ms2 = e.getMeanSummary();
        } catch (Exception exception) {
            return 0;
        }
		// sort in DESCENDING order!!!!
        if (ms1.getExclusive(metricIndex) < ms2.getExclusive(metricIndex)) {
            return 1;
        } else if (ms1.getExclusive(metricIndex) > ms2.getExclusive(metricIndex)) {
            return -1;
        }else
            return 0;
    }

}
