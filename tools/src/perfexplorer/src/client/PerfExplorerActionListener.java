package client;

import javax.swing.*;

import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import edu.uoregon.tau.perfdmf.*;
import constants.Constants;
import common.AnalysisType;
import common.RMIView;
import common.RMISortableIntervalEvent;
import common.RMIPerfExplorerModel;
import common.TransformationType;

public class PerfExplorerActionListener implements ActionListener {

	public final static String QUIT = "Quit PerfExplorer";
	public final static String QUIT_SERVER = "Quit PerfExplorer (Shutdown Server)";
	public final static String ABOUT = "About PerfExplorer";
	public final static String SEARCH = "Search for help on...";
	// clustering menu items
	public final static String CLUSTERING_METHOD = "Select Clustering Method";
	public final static String DIMENSION_REDUCTION = "Select Dimension Reduction";
	public final static String NORMALIZATION = "Select Normalization Method";
	public final static String NUM_CLUSTERS = "Set Maximum Number of Clusters";
	public final static String DO_CLUSTERING = "Do Clustering";
	public final static String DO_CORRELATION_ANALYSIS = "Do Correlation Analysis";
	public final static String DO_CORRELATION_CUBE = "Do 3D Correlation Cube";
	public final static String DO_VARIATION_ANALYSIS = "Show Data Summary";
	// chart menu items
	public final static String SET_GROUPNAME = "Set Group Name";
	public final static String SET_PROBLEM_SIZE = "Set Problem Size (Scaling)";
	public final static String SET_METRICNAME = "Set Metric of Interest";
	public final static String SET_TIMESTEPS = "Set Total Number of Timesteps";
	public final static String SET_EVENTNAME = "Set Event of Interest";
	public final static String TIMESTEPS_CHART = "Timesteps Per Second";
	public final static String TOTAL_TIME_CHART = "Total Execution Time";
	public final static String EFFICIENCY_CHART = "Relative Efficiency";
	public final static String EFFICIENCY_EVENTS_CHART = "Relative Efficiency by Event";
	public final static String EFFICIENCY_ONE_EVENT_CHART = "Relative Efficiency for One Event";
	public final static String SPEEDUP_CHART = "Relative Speedup";
	public final static String SPEEDUP_EVENTS_CHART = "Relative Speedup by Event";
	public final static String SPEEDUP_ONE_EVENT_CHART = "Relative Speedup for One Event";
	public final static String COMMUNICATION_CHART = "Group % of Total Runtime";
	public final static String FRACTION_CHART = "Runtime Breakdown";
	public final static String CORRELATION_CHART = "Correlate Events with Total Runtime";
	// phase chart menu items
	public final static String EFFICIENCY_PHASE_CHART = "Relative Efficiency per Phase";
	public final static String SPEEDUP_PHASE_CHART = "Relative Speedup per Phase";
	public final static String FRACTION_PHASE_CHART = "Phase Fraction of Total Runtime";
	// arbitrary view menu items
	public final static String CREATE_NEW_VIEW = "Create New View";
	public final static String CREATE_NEW_SUB_VIEW = "Create New Sub-view";
	public final static String DO_IQR_BOXCHART = "Create BoxChart";
	public final static String DO_HISTOGRAM = "Create Histograms";
	public final static String DO_PROBABILITY_PLOT = "Create Normal Probability Plot";

	private PerfExplorerClient mainFrame;

	public PerfExplorerActionListener (PerfExplorerClient mainFrame) {
		super();
		this.mainFrame = mainFrame;
	}

	public void actionPerformed (ActionEvent event) {
		try {
			Object EventSrc = event.getSource();
			if(EventSrc instanceof JMenuItem) {
				String arg = event.getActionCommand();
				if(arg.equals(QUIT)) {
					System.exit(0);
				} else if(arg.equals(QUIT_SERVER)) {
					//PerfExplorerConnection server = PerfExplorerConnection.getConnection();
					//server.stopServer();
					System.exit(0);
			// help menu items
				} else if (arg.equals(ABOUT)) {
					createAboutWindow();
				} else if (arg.equals(SEARCH)) {
					createHelpWindow();
			// clustering items
				} else if (arg.equals(CLUSTERING_METHOD)) {
					createMethodWindow();
				} else if (arg.equals(DIMENSION_REDUCTION)) {
					createDimensionWindow();
				} else if (arg.equals(NORMALIZATION)) {
					createNormalizationWindow();
				} else if (arg.equals(NUM_CLUSTERS)) {
					createClusterSizeWindow();
				} else if (arg.equals(DO_CLUSTERING)) {
					if (validAnalysisSelection())
						createDoClusteringWindow();
				} else if (arg.equals(DO_CORRELATION_ANALYSIS)) {
					if (validCorrelationSelection())
						createDoCorrelationWindow();
				} else if (arg.equals(DO_CORRELATION_CUBE)) {
					if (valid3DSelection())
						PerfExplorerCube.doCorrelationCube();
				} else if (arg.equals(DO_IQR_BOXCHART)) {
					if (valid3DSelection())
						PerfExplorerBoxChart.doIQRBoxChart();
				} else if (arg.equals(DO_HISTOGRAM)) {
					if (validDistributionSelection())
						PerfExplorerHistogramChart.doHistogram();
				} else if (arg.equals(DO_VARIATION_ANALYSIS)) {
					if (valid3DSelection())
						PerfExplorerVariation.doVariationAnalysis();
				} else if (arg.equals(DO_PROBABILITY_PLOT)) {
					if (validDistributionSelection())
						PerfExplorerProbabilityPlot.doProbabilityPlot();
			// chart items
				} else if (arg.equals(SET_PROBLEM_SIZE)) {
					checkAndSetProblemSize(true);
				} else if (arg.equals(SET_GROUPNAME)) {
					checkAndSetGroupName(true);
				} else if (arg.equals(SET_METRICNAME)) {
					checkAndSetMetricName(true);
				} else if (arg.equals(SET_EVENTNAME)) {
					checkAndSetEventName(true);
				} else if (arg.equals(SET_TIMESTEPS)) {
					checkAndSetTimesteps(true);
				} else if (arg.equals(TIMESTEPS_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetTimesteps(false))
							PerfExplorerChart.doTimestepsChart();
				} else if (arg.equals(TOTAL_TIME_CHART)) {
					if (checkAndSetMetricName(false))
						PerfExplorerChart.doTotalTimeChart();
				} else if (arg.equals(EFFICIENCY_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetProblemSize(false))
							PerfExplorerChart.doEfficiencyChart();
				} else if (arg.equals(EFFICIENCY_EVENTS_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetProblemSize(false))
						PerfExplorerChart.doEfficiencyEventsChart();
				} else if (arg.equals(EFFICIENCY_ONE_EVENT_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetEventName(false))
						if (checkAndSetProblemSize(false))
							PerfExplorerChart.doEfficiencyOneEventChart();
				} else if (arg.equals(SPEEDUP_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetProblemSize(false))
						PerfExplorerChart.doSpeedupChart();
				} else if (arg.equals(SPEEDUP_EVENTS_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetProblemSize(false))
						PerfExplorerChart.doSpeedupEventsChart();
				} else if (arg.equals(SPEEDUP_ONE_EVENT_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetEventName(false))
						if (checkAndSetProblemSize(false))
							PerfExplorerChart.doSpeedupOneEventChart();
				} else if (arg.equals(COMMUNICATION_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetGroupName(false))
							PerfExplorerChart.doCommunicationChart();
				} else if (arg.equals(FRACTION_CHART)) {
					if (checkAndSetMetricName(false))
						PerfExplorerChart.doFractionChart();
				} else if (arg.equals(CORRELATION_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetProblemSize(false))
							PerfExplorerChart.doCorrelationChart();
				} else if (arg.equals(EFFICIENCY_PHASE_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetProblemSize(false))
						PerfExplorerChart.doEfficiencyPhasesChart();
				} else if (arg.equals(SPEEDUP_PHASE_CHART)) {
					if (checkAndSetMetricName(false))
						if (checkAndSetProblemSize(false))
						PerfExplorerChart.doSpeedupPhasesChart();
				} else if (arg.equals(FRACTION_PHASE_CHART)) {
					if (checkAndSetMetricName(false))
						PerfExplorerChart.doFractionPhasesChart();
			// view items
				} else if (arg.equals(CREATE_NEW_VIEW)) {
					int parent = 0; // no parent
					PerfExplorerViews.createNewView(mainFrame, parent);
				} else if (arg.equals(CREATE_NEW_SUB_VIEW)) {
					PerfExplorerViews.createNewSubView(mainFrame);
				} else {
					System.out.println("unknown event! " + arg);
				}
			}
		} catch (Exception e) {
			System.err.println("actionPerformed Exception: " + e.getMessage());
			e.printStackTrace();
		} 
	}

    public static String getVersionString() {
		return new String(Constants.VERSION);
	}

	public void createAboutWindow() {
		long memUsage = (Runtime.getRuntime().totalMemory() -
			Runtime.getRuntime().freeMemory()) / 1024;

		String message = new String("PerfExplorer 1.0\n" +
					getVersionString() + "\nJVM Heap Size: " + memUsage
					+ "kb\n");
		JOptionPane.showMessageDialog(mainFrame, message, 
			"About PerfExplorer", JOptionPane.PLAIN_MESSAGE);
	}

	public void createHelpWindow() {
		JOptionPane.showMessageDialog(mainFrame, 
			"Help not implemented.\nFor the most up-to-date documentation, please see\n<html><a href='http://www.cs.uoregon.edu/research/tau/'>http://www.cs.uoregon.edu/research/tau/</a></html>",
			"PerfExplorer Help", JOptionPane.PLAIN_MESSAGE);
	}

	public void createMethodWindow() {
		Object[] options = AnalysisType.getClusterMethods();
		String reply = (String)JOptionPane.showInputDialog (mainFrame,
			"Select a cluster method:",
			"Cluster Method",
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			AnalysisType.K_MEANS);
		PerfExplorerModel.getModel().setClusterMethod(reply);
	}

	public void createDimensionWindow() {
		Object[] options = TransformationType.getDimensionReductions();
		Object obj = (String)JOptionPane.showInputDialog (mainFrame,
			"Select a dimension reduction method:",
			"Dimension Reduction",
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			TransformationType.NONE);
        TransformationType reply = (TransformationType)obj;
		PerfExplorerModel.getModel().setDimensionReduction(reply);
		if (PerfExplorerModel.getModel().getDimensionReduction().equals(reply)) {
			String reply2 = (String)JOptionPane.showInputDialog (mainFrame,
				"Only select events with exclusive time % greater than X:\n(where 0 <= X < 100)",
				"Minimum Percentage", JOptionPane.PLAIN_MESSAGE);
			if (reply != null && !reply.equals(""))
				PerfExplorerModel.getModel().setXPercent(reply2);
		}
	}

	public void createNormalizationWindow() {
		Object[] options = TransformationType.getNormalizations();
		TransformationType reply = (TransformationType)JOptionPane.showInputDialog (mainFrame,
			"Select a normalization method:",
			"Normalization",
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			TransformationType.NONE);
		PerfExplorerModel.getModel().setNormalization(reply);
	}

	public void createClusterSizeWindow() {
		String numClusters = (new Integer(PerfExplorerModel.getModel().getNumberOfClusters())).toString();
		String reply = (String)JOptionPane.showInputDialog (mainFrame,
			"Enter the max number of clusters (<= " + numClusters + "):",
			"Max Clusters", JOptionPane.PLAIN_MESSAGE);
		if (reply != null && !reply.equals(""))
			PerfExplorerModel.getModel().setNumberOfClusters(reply);
	}

	public void createDoClusteringWindow() {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		Object selection = theModel.getCurrentSelection();
		String status = null;
		if (selection instanceof Application) {
			int reply = getConfirmation(theModel);
			if (reply == 1) {
				Application application = (Application)selection;
				PerfExplorerConnection server = PerfExplorerConnection.getConnection();
				ListIterator experiments = server.getExperimentList(application.getID());
				Experiment experiment = null;
				boolean failed = false;
				while (experiments.hasNext() && !failed) {
					experiment = (Experiment) experiments.next();
					theModel.setCurrentSelection(experiment);
					ListIterator trials = server.getTrialList(experiment.getID());
					Trial trial = null;
					while (trials.hasNext() && !failed) {
						trial = (Trial) trials.next();
						theModel.setCurrentSelection(trial);
						List metrics = trial.getMetrics();
						for (int i = 0; i < metrics.size(); i++) {
							Object metric = metrics.get(i);
							theModel.setCurrentSelection(metric);
							// request some analysis!
							RMIPerfExplorerModel modelCopy = theModel.copy();
							status = server.requestAnalysis(modelCopy, true);
							if (!status.endsWith("Request accepted.")) {
								JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
									"Request Status", JOptionPane.ERROR_MESSAGE);
								failed = true;
								break;
							}
						}
					}
                    // set the selection back to experiment
					theModel.setCurrentSelection(experiment);
				}
				// set the selection back to application
				theModel.setCurrentSelection(application);
				if (status.endsWith("Request accepted.")) {
					JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
						"Request Status", JOptionPane.PLAIN_MESSAGE);
				}
			}
		} else if (selection instanceof Experiment) {
			int reply = getConfirmation(theModel);
			if (reply == 1) {
				Experiment experiment = (Experiment)selection;
				PerfExplorerConnection server = PerfExplorerConnection.getConnection();
				ListIterator trials = server.getTrialList(experiment.getID());
				Trial trial = null;
				boolean failed = false;
				while (trials.hasNext() && !failed) {
					trial = (Trial) trials.next();
					theModel.setCurrentSelection(trial);
					List metrics = trial.getMetrics();
					for (int i = 0; i < metrics.size(); i++) {
						Object metric = metrics.get(i);
						theModel.setCurrentSelection(metric);
						// request some analysis!
						RMIPerfExplorerModel modelCopy = theModel.copy();
						status = server.requestAnalysis(modelCopy, true);
						if (!status.endsWith("Request accepted.")) {
							JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
								"Request Status", JOptionPane.ERROR_MESSAGE);
							failed = true;
							break;
						}
					}
				}
				if (status.endsWith("Request accepted.")) {
					JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
						"Request Status", JOptionPane.PLAIN_MESSAGE);
				}
                // set the selection back to experiment
				theModel.setCurrentSelection(experiment);
			}
		} else if (selection instanceof Trial) {
		/*
			JOptionPane.showMessageDialog(mainFrame, "Please select a Metric.",
				"Selection Error", JOptionPane.ERROR_MESSAGE);
				*/
			int reply = getConfirmation(theModel);
			if (reply == 1) {
				PerfExplorerConnection server = PerfExplorerConnection.getConnection();
				RMIPerfExplorerModel modelCopy = theModel.copy();
				status = server.requestAnalysis(modelCopy, true);
				Trial trial = (Trial)selection;
				List metrics = trial.getMetrics();
				for (int i = 0; i < metrics.size(); i++) {
					modelCopy = theModel.copy();
					Object metric = metrics.get(i);
					modelCopy.setCurrentSelection(metric);
					// request some analysis!
					status = server.requestAnalysis(modelCopy, true);
					if (!status.endsWith("Request accepted.")) {
						JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
							"Request Status", JOptionPane.ERROR_MESSAGE);
						break;
					}
				}
				if (status.endsWith("Request accepted.")) {
					JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
						"Request Status", JOptionPane.PLAIN_MESSAGE);
				}
			}
		} else if (selection instanceof Metric) {
			int reply = getConfirmation(theModel);
			if (reply == 1) {
				// request some analysis!
				PerfExplorerConnection server = PerfExplorerConnection.getConnection();
				status = server.requestAnalysis(PerfExplorerModel.getModel(), true);
				if (status.endsWith("Request accepted.")) {
					JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
						"Request Status", JOptionPane.PLAIN_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
						"Request Status", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		return;
	}

	public void createDoCorrelationWindow() {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		Object selection = theModel.getCurrentSelection();
		String status = null;
		if ((selection instanceof Trial) || (selection instanceof Metric)) {
//			String tmp = theModel.getClusterMethod();
			RMIPerfExplorerModel modelCopy = theModel.copy();
			modelCopy.setClusterMethod(AnalysisType.CORRELATION_ANALYSIS);
			int reply = getConfirmation(modelCopy);
			if (reply == 1) {
				// request some analysis!
				PerfExplorerConnection server = PerfExplorerConnection.getConnection();
				status = server.requestAnalysis(modelCopy, true);
				if (status.endsWith("Request accepted.")) {
					JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
						"Request Status", JOptionPane.PLAIN_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(mainFrame, "Request Status: \n" + status,
						"Request Status", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		return;
	}

	private int getConfirmation(RMIPerfExplorerModel theModel) {
		Object [] options = { "No, not yet" , "Yes, do analysis" };
		StringBuffer buf = new StringBuffer();
		buf.append("Analysis method: " + theModel.getClusterMethod());
		buf.append("\nDimension Reduction: " + theModel.getDimensionReduction());
		if (theModel.getDimensionReduction().equals(TransformationType.OVER_X_PERCENT)) 
			buf.append("\n\t\t Minimum percentage: " + theModel.getXPercent());
		buf.append("\nNormalization: " + theModel.getNormalization());
		if (!theModel.getClusterMethod().equals(AnalysisType.CORRELATION_ANALYSIS))
			buf.append("\nMax Clusters: " + theModel.getNumberOfClusters());
		buf.append("\nTrial: " + theModel.toString());
		buf.append("\n\nPerform clustering with the these options?");
		int reply = JOptionPane.showOptionDialog(mainFrame, buf.toString(),
			"Confirm Analysis",
			JOptionPane.YES_NO_OPTION, 
			JOptionPane.PLAIN_MESSAGE,
			null, 
			options, 
			options[1]);
		return reply;
	}

	private boolean validCorrelationSelection () {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		Object selection = theModel.getCurrentSelection();
		// allow Experiments or Trials or 1 view
		if (!(selection instanceof Trial) && !(selection instanceof Metric)) {
			JOptionPane.showMessageDialog(mainFrame, "Please select a Trial or Metric.",
				"Selection Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		// check multi-selections, to make sure they are homogeneous
		return true;
	}

	private boolean validAnalysisSelection () {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		Object selection = theModel.getCurrentSelection();
		// allow Experiments or Trials or 1 view
		if (!(selection instanceof Experiment) && !(selection instanceof Trial) && !(selection instanceof Application) && !(selection instanceof Metric)) {
			JOptionPane.showMessageDialog(mainFrame, "Please select an Application, Experiment, Trial or Metric.",
				"Selection Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		// check multi-selections, to make sure they are homogeneous
		return true;
	}

	private boolean validSelection (PerfExplorerModel theModel) {
		Object selection = theModel.getCurrentSelection();
/*		// allow Experiments or Trials or 1 view
		if (!(selection instanceof Experiment) && !(selection instanceof Trial) && !(selection instanceof RMIView)) {
			JOptionPane.showMessageDialog(mainFrame, "Please select one or more Experiments or Trials.",
				"Selection Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
*/		// allow Experiments or 1 view
		if (!(selection instanceof Experiment) && !(selection instanceof RMIView)) {
		// allow Experiments or 1 view
		// if (!(selection instanceof Experiment) && !(selection instanceof RMIView)) {
			JOptionPane.showMessageDialog(mainFrame, "Please select one or more Experiments or one View.",
				"Selection Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		// check multi-selections, to make sure they are homogeneous
		return true;
	}

	private boolean valid3DSelection () {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		Object selection = theModel.getCurrentSelection();
		// allow only Metrics
		if ((selection == null) || !(selection instanceof Metric)) {
			JOptionPane.showMessageDialog(mainFrame, "Please select a Metric.",
				"Selection Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private boolean validDistributionSelection () {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		Object selection = theModel.getCurrentSelection();
		// allow only Metrics or IntervalEvents
		if ((selection == null) || 
			(!(selection instanceof Metric) && 
			 !(selection instanceof RMISortableIntervalEvent))) {
			JOptionPane.showMessageDialog(mainFrame, 
				"Please select an Metrics or one or more Events.",
				"Selection Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private boolean checkAndSetMetricName (boolean forceIt) {
		//TODO - MAKE SURE THE METRIC EXISTS IN THE SELECTED TRIALS!
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		if (!validSelection(theModel))
			return false;
		String metric = theModel.getMetricName();
		if (forceIt || (metric == null)) {
			PerfExplorerConnection server = PerfExplorerConnection.getConnection();
			List metrics = server.getPotentialMetrics(theModel);
			Object[] options = metrics.toArray();
			if (options.length > 0) {
				if (options.length == 1) {
					metric = (String)options[0];
				} else {
					metric = (String)JOptionPane.showInputDialog (mainFrame,
						"Please enter the metric of interest",
						"Metric of interest", JOptionPane.PLAIN_MESSAGE,
						null, options,options[0]);
				}
				theModel.setMetricName(metric);
			}
		}
		return (!forceIt && metric == null) ? false : true;
	}

	private boolean checkAndSetGroupName (boolean forceIt) {
		//TODO - MAKE SURE THE GROUP EXISTS IN THE SELECTED TRIALS!
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		String group = theModel.getGroupName();
		if (forceIt || group == null) {
			PerfExplorerConnection server = PerfExplorerConnection.getConnection();
			List metrics = server.getPotentialGroups(theModel);
			Object[] options = metrics.toArray();
			if (options.length > 0) {
				if (options.length == 1) {
					group = (String)options[0];
				} else {
					group = (String)JOptionPane.showInputDialog (mainFrame,
						"Please enter the group of interest",
						"Group of interest", JOptionPane.PLAIN_MESSAGE,
						null, options, options[0]);
				}
				theModel.setGroupName(group);
			}
		}
		return (!forceIt && group == null) ? false : true;
	}

	private boolean checkAndSetEventName (boolean forceIt) {
		//TODO - MAKE SURE THE EVENT EXISTS IN THE SELECTED TRIALS!
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		String event = theModel.getEventName();
		if (forceIt || event == null) {
			PerfExplorerConnection server = PerfExplorerConnection.getConnection();
			List metrics = server.getPotentialEvents(theModel);
			Object[] options = metrics.toArray();
			if (options.length > 0) {
				if (options.length == 1) {
					event = (String)options[0];
				} else {
					event = (String)JOptionPane.showInputDialog (mainFrame,
						"Please enter the event of interest",
						"Event of interest", JOptionPane.PLAIN_MESSAGE,
						null, options, options[0]);
				}
				theModel.setEventName(event);
			}
		}
		return (!forceIt && event == null) ? false : true;
	}

	private boolean checkAndSetTimesteps (boolean forceIt) {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		String timesteps = theModel.getTotalTimesteps();
		if (forceIt || timesteps == null) {
			timesteps = (String)JOptionPane.showInputDialog (mainFrame,
				"Please enter the total number of timesteps for the experiment",
				"Total Timesteps", JOptionPane.PLAIN_MESSAGE);
			theModel.setTotalTimesteps(timesteps);
		}
		return (!forceIt && timesteps == null) ? false : true;
	}

	private boolean checkAndSetProblemSize (boolean forceIt) {
		PerfExplorerModel theModel = PerfExplorerModel.getModel();
		Boolean constantProblem = theModel.getConstantProblem();
		if (forceIt || constantProblem == null) {
			List answers = new ArrayList();
			answers.add("The problem size remains constant. (strong scaling)");
			answers.add("The problem size increases as the processor count increases. (weak scaling)");
			Object[] options = answers.toArray();
			String response = (String)JOptionPane.showInputDialog (mainFrame,
				"Please select the problem scaling:",
				"Problem Scaling", JOptionPane.PLAIN_MESSAGE,
				null, options, options[0]);
			theModel.setConstantProblem(response.startsWith("The problem size remains") ? true : false);
			constantProblem = theModel.getConstantProblem();
		}
		return (!forceIt && constantProblem == null) ? false : true;
	}
}
