/* 
 Metric.java

 Title:      ParaProf
 Author:     Robert Bell
 Description:  
 */

package edu.uoregon.tau.paraprof;

import edu.uoregon.tau.perfdmf.*;

import javax.swing.tree.*;

public class ParaProfMetric extends Metric implements ParaProfTreeNodeUserObject {
    private ParaProfTrial ppTrial = null;
    private DefaultMutableTreeNode defaultMutableTreeNode = null;
    private TreePath treePath = null;
    private boolean derivedMetric = false;

    public ParaProfMetric() {
    }

    public void setPpTrial(ParaProfTrial ppTrial) {
        this.ppTrial = ppTrial;
    }

    public ParaProfTrial getParaProfTrial() {
        return ppTrial;
    }

    public void setDMTN(DefaultMutableTreeNode defaultMutableTreeNode) {
        this.defaultMutableTreeNode = defaultMutableTreeNode;
    }

    public DefaultMutableTreeNode getDMTN() {
        return defaultMutableTreeNode;
    }

    public void setTreePath(TreePath treePath) {
        this.treePath = treePath;
    }

    public TreePath getTreePath() {
        return treePath;
    }

    public void setDerivedMetric(boolean derivedMetric) {
        this.derivedMetric = derivedMetric;
    }

    public boolean getDerivedMetric() {
        return derivedMetric;
    }

    public int getApplicationID() {
        return ppTrial.getApplicationID();
    }

    public int getExperimentID() {
        return ppTrial.getExperimentID();
    }

    public int getTrialID() {
        return ppTrial.getID();
    }

    public String getIDString() {
        if (ppTrial != null) {
            return ppTrial.getIDString() + ":" + this.getID() + " - " + this.getName();
        } else {
            return ":" + this.getID() + " - " + this.getName();
        }
    }

    public String toString() {
        return super.getName();
    }

    public void clearDefaultMutableTreeNode() {
        this.setDMTN(null);
    }

}