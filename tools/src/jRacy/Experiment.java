/* 	Experiment.java	Title:			jRacy	Author:			Robert Bell	Description:	*/package jRacy;import java.util.*;import java.awt.*;public class Experiment{	//Constructors.		public Experiment(){		experimentRuns = new Vector();	}		public Experiment(String inExperimentName)	{		experimentName = inExperimentName;		experimentRuns = new Vector();	}			public void setExperimentName(String inExperimentName)	{		experimentName = inExperimentName;	}		public String getExperimentName()	{		return experimentName;	}		public Vector getExperimentRuns(){		return experimentRuns;	}		public void addExperimentRun(ExperimentRun inExperimentRun){		experimentRuns.add(inExperimentRun);	}		public boolean isExperimentRunNamePresent(String inString){				for(Enumeration e = experimentRuns.elements(); e.hasMoreElements() ;)		{			ExperimentRun expRun = (ExperimentRun) e.nextElement();			if(inString.equals(expRun.toString()))				return true;		}				//If we make it here, the experiment run name is not present.  Return false.		return false;	}		public String toString()	{			return experimentName;		}			//Data section.	private String experimentName = null;	Vector experimentRuns = null;}