package client;

import java.util.ListIterator;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import edu.uoregon.tau.perfdmf.*;
import common.RMIView;;

public class PerfExplorerJTree extends JTree {

	private static PerfExplorerJTree theTree = null;
	private static List leafViews = null;

	private PerfExplorerJTree (DefaultTreeModel model) {
		super(model);
		putClientProperty("JTree.lineStyle", "Angled");
		//getSelectionModel().setSelectionMode (
			//TreeSelectionModel.SINGLE_TREE_SELECTION);
		setShowsRootHandles(true);

		PerfExplorerTreeCellRenderer renderer = 
			new PerfExplorerTreeCellRenderer();
		setCellRenderer(renderer);

		addTreeSelectionListener (
			new PerfExplorerTreeSelectionListener(this));
		addTreeWillExpandListener (
			new PerfExplorerTreeExpansionListener(this));
	}

	public static PerfExplorerJTree getTree() {
		if (theTree == null) {
			DefaultTreeModel model = new DefaultTreeModel(createNodes());
			model.setAsksAllowsChildren(true);
			theTree = new PerfExplorerJTree(model);
			addTrialsForViews();
		}
		return theTree;
	}

	private static DefaultMutableTreeNode createNodes () {
		DefaultMutableTreeNode root = new PerfExplorerTreeNode("Performance Data");
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		DefaultMutableTreeNode top = new PerfExplorerTreeNode(server.getConnectionString());
		DefaultMutableTreeNode viewTop = new PerfExplorerTreeNode("Views");
		//addApplicationNodes(top, true);
		leafViews = new ArrayList();
		//addViewNodes(viewTop, "0");
		root.add(top);
		root.add(viewTop);

		return root;
	}

	public static void addViewNodes (DefaultMutableTreeNode parentNode, String parent) {
		// get the top level views
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		List viewVector = server.getViews(Integer.parseInt(parent));
		Iterator views = viewVector.iterator();
		while (views.hasNext()) {
			RMIView view = (RMIView) views.next();
			DefaultMutableTreeNode node = new PerfExplorerTreeNode (view);
			addViewNodes(node, view.getField("ID"));
			parentNode.add(node);
		}
		if (viewVector.size() == 0) {
			leafViews.add(parentNode);
			addTrialsForView(parentNode);
		}
	}

	public static void addApplicationNodes (DefaultMutableTreeNode parent, boolean getExperiments) {
		System.out.println("application nodes...");
		DefaultMutableTreeNode node = null;
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		if (server != null) {
			// get the applications
			ListIterator applications = server.getApplicationList();
			if (applications != null) {
				Application app = null;

				// loop through all the applications, and print out some info
				while(applications.hasNext())
				{
					app = (Application) applications.next();
					node = new PerfExplorerTreeNode (app);
					//addExperimentNodes(node, app, true);
					parent.add(node);
				}
			}
		}

	}

	public static void addExperimentNodes (DefaultMutableTreeNode node, Application app, boolean getTrials) {
		System.out.println("experiment nodes...");
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		// get the experiments
		ListIterator experiments = server.getExperimentList(app.getID());
		Experiment exp = null;
		DefaultMutableTreeNode expNode = null;
		// loop through all the experiments, and print out some info
		while(experiments.hasNext())
		{
			exp = (Experiment) experiments.next();
			expNode = new PerfExplorerTreeNode (exp);
			//if (getTrials) addTrialNodes(expNode, exp);
			node.add(expNode);
		}
	}

	public static void addTrialNodes (DefaultMutableTreeNode node, Experiment exp) {
		System.out.println("trial nodes...");
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		// get the trials
		ListIterator trials = server.getTrialList(exp.getID());
		Trial trial = null;
		DefaultMutableTreeNode trialNode = null;
		// loop through all the trials, and print out some info
		while(trials.hasNext())
		{
			trial = (Trial) trials.next();
			trialNode = new PerfExplorerTreeNode (trial);
			//addMetricNodes(trialNode, trial);
			node.add(trialNode);
		}
	}

	public static void addTrialsForViews () {
		Iterator e = leafViews.iterator();
		while (e.hasNext()) {
			PerfExplorerTreeNode node = (PerfExplorerTreeNode) e.next();
			addTrialsForView(node);
		}
	}

	public static void addTrialsForView (DefaultMutableTreeNode node) {
		System.out.println("trial nodes...");
		Object[] objects = node.getUserObjectPath();
		List views = new ArrayList();
		for (int i = 0 ; i < objects.length ; i++) {
			if (objects[i] instanceof RMIView) {
				views.add(objects[i]);
			}
		}
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		// get the trials
		if (views.size() > 0) {
			ListIterator trials = server.getTrialsForView(views);
			Trial trial = null;
			DefaultMutableTreeNode trialNode = null;
			// loop through all the trials, and print out some info
			while(trials.hasNext())
			{
				trial = (Trial) trials.next();
				trialNode = new PerfExplorerTreeNode (trial);
				//addMetricNodes(trialNode, trial);
				node.add(trialNode);
			}
		}
	}


	public static void addMetricNodes (DefaultMutableTreeNode node, Trial trial) {
		System.out.println("metric nodes...");
		// get the metrics
		List metricVector = trial.getMetrics();
		int metricIndex = 0;
		if (metricVector != null) {
			ListIterator metrics = metricVector.listIterator();
			Metric metric = null;
			DefaultMutableTreeNode metricNode = null;
			// loop through all the metrics, and print out some info
			while(metrics.hasNext())
			{
				metric = (Metric) metrics.next();
				metricNode = new PerfExplorerTreeNode (metric, true);
				//addEventNodes(metricNode, trial, metricIndex++);
				node.add(metricNode);
			}
		}
	}

	public static void addEventNodes (DefaultMutableTreeNode node, Trial trial, int metricIndex) {
		System.out.println("event nodes...");
		PerfExplorerConnection server = PerfExplorerConnection.getConnection();
		// get the events
		ListIterator events = server.getEventList(trial.getID(), metricIndex);
		IntervalEvent event = null;
		DefaultMutableTreeNode eventNode = null;
		// loop through all the events, and print out some info
		while(events.hasNext())
		{
			event = (IntervalEvent) events.next();
			eventNode = new PerfExplorerTreeNode (event, false);
			node.add(eventNode);
		}
	}
}
