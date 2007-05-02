package edu.uoregon.tau.paraprof.enums;

import edu.uoregon.tau.perfdmf.UserEvent;
import edu.uoregon.tau.perfdmf.UserEventProfile;

/**
 * type-safe enum pattern for value type
 *    
 * TODO : nothing, this class is complete
 *
 * <P>CVS $Id: UserEventValueType.java,v 1.3 2007/05/02 19:45:06 amorris Exp $</P>
 * @author  Alan Morris
 * @version $Revision: 1.3 $
 */
public abstract class UserEventValueType {

    private final String name;

    private UserEventValueType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static final UserEventValueType NUMSAMPLES = new UserEventValueType("Number of Samples") {
        public double getValue(UserEventProfile uep) {
            return uep.getNumSamples();
        }
        public double getMaxValue(UserEvent ue) {
            return ue.getMaxUserEventNumberValue();
        }
    };

    public static final UserEventValueType MAX = new UserEventValueType("Max Value") {
        public double getValue(UserEventProfile uep) {
            return uep.getMaxValue();
        }
        public double getMaxValue(UserEvent ue) {
            return ue.getMaxUserEventMaxValue();
        }
    };

    public static final UserEventValueType MIN = new UserEventValueType("Min Value") {
        public double getValue(UserEventProfile uep) {
            return uep.getMinValue();
        }
        public double getMaxValue(UserEvent ue) {
            return ue.getMaxUserEventMinValue();
        }
    };

    public static final UserEventValueType MEAN = new UserEventValueType("Mean Value") {
        public double getValue(UserEventProfile uep) {
            return uep.getMeanValue();
        }
        public double getMaxValue(UserEvent ue) {
            return ue.getMaxUserEventMeanValue();
        }
    };

    public static final UserEventValueType STDDEV = new UserEventValueType("Standard Deviation") {
        public double getValue(UserEventProfile uep) {
            return uep.getStdDev();
        }
        public double getMaxValue(UserEvent ue) {
            return ue.getMaxUserEventStdDev();
        }
    };

    public abstract double getValue(UserEventProfile uep);

    public abstract double getMaxValue(UserEvent ue);
    
    
}
