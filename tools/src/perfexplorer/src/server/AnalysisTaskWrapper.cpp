/**
 * 
 */
package server;

import clustering.*;
#ifdef USE_WEKA_ENGINE
import clustering.weka.WekaAnalysisFactory;
#endif
#ifdef USE_R_ENGINE
import clustering.r.RAnalysisFactory;
#endif
#ifdef USE_OCTAVE_ENGINE
import clustering.octave.RAnalysisFactory;
#endif
import common.*;
import edu.uoregon.tau.perfdmf.*;
import edu.uoregon.tau.perfdmf.database.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.TimerTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.xy.XYDataset;

/**
 * This is a rewrite of the AnalysisTask class.
 * This class is intended to be a wrapper around data mining operations
 * available in Weka, R and Octave.  The orignal AnalysisTask class
 * only supported R directly.  This is intended to be an improvement...
 * 
 * <P>CVS $Id: AnalysisTaskWrapper.cpp,v 1.3 2005/10/05 23:03:01 khuck Exp $</P>
 * @author  Kevin Huck
 * @version 0.1
 * @since   0.1
 */
public class AnalysisTaskWrapper extends TimerTask {

	public static final int RPROJECT_ENGINE = 0;
	public static final int WEKA_ENGINE = 1;
	public static final int OCTAVE_ENGINE = 2;
	
	public static final int DENDROGRAM = 0;
	public static final int HISTOGRAM = 1;
	public static final int VIRTUAL_TOPOLOGY = 2;
	public static final int CLUSTER_AVERAGES = 3;
	public static final int CLUSTER_MAXIMUMS = 4;
	public static final int CLUSTER_MINIMUMS = 5;
	public static final int PCA_SCATTERPLOT = 6;
	public static final int CORRELATION_SCATTERPLOT = 7;
	
	private int chartType = 0;
	private AnalysisFactory factory = null;
	
	private RMIPerfExplorerModel modelData = null;
	private PerfExplorerServer server = null;
	private int analysisID = 0;
	private int numRows = 0;
	private int numCenterRows = 0;
	private int numTotalThreads = 0;
	private int numEvents = 0;
	private int nodes = 0;
	private int contexts = 0;
	private int threads = 0;
	private RawDataInterface rawData = null;
	private double maximum = 0.0;
	private List eventIDs = null;

	/**
	 * Constructor.  The engine parameter passed in specifies which analysis
	 * engine to use. 
	 * 
	 * @param server
	 * @param engine
	 */
	public AnalysisTaskWrapper (PerfExplorerServer server, int engine) {
		super();
		this.server = server;
		switch (engine) {
#ifdef USE_WEKA_ENGINE
			case WEKA_ENGINE:
				factory = WekaAnalysisFactory.getFactory();
				break;
#endif
#ifdef USE_R_ENGINE
			case RPROJECT_ENGINE:
				//rInterpreter = server.getRInterpreter();
				//rEvaluator = server.getREvaluator();
				factory = RAnalysisFactory.getFactory();
				break;
#endif
#ifdef USE_OCTAVE_ENGINE
			case OCTAVE_ENGINE:
				factory = OctaveAnalysisFactory.getFactory();
				break;
#endif
			default:
				System.out.println("Undefined analysis engine type.");
				System.exit(1);
				break;
		}
	}

	/**
	 * This method saves the analysis results in the database.
	 * 
	 * @param description
	 * @param fileName
	 * @param thumbnail
	 * @param big
	 * @throws PerfExplorerException
	 */
	public void saveAnalysisResult (String description, String fileName, String thumbnail, boolean big) throws PerfExplorerException {
		// create the thumbnail
		Thumbnail.createThumbnail(fileName, thumbnail, big);
		// save the image in the database
		try {
			PerfExplorerServer.getServer().getControl().WAIT("saveAnalysisResult");
			DB db = PerfExplorerServer.getServer().getDB();
			//db.setAutoCommit(false);
			PreparedStatement statement = null;
			statement = db.prepareStatement("insert into analysis_result (analysis_settings, description, thumbnail_size, image_size, thumbnail, image, result_type) values (?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, analysisID);
			statement.setString(2, fileName);
       		File v_file = new File(fileName);
       		FileInputStream v_fis = new FileInputStream(v_file);
       		File v_thumb = new File(thumbnail);
       		FileInputStream v_tis = new FileInputStream(v_thumb);
       		statement.setInt(3,(int)v_thumb.length());
       		statement.setInt(4,(int)v_file.length());
       		statement.setBinaryStream(5,v_tis,(int)v_thumb.length());
       		statement.setBinaryStream(6,v_fis,(int)v_file.length());
			statement.setInt(7, chartType);
       		statement.executeUpdate();
			//db.commit();
       		v_fis.close();
       		statement.close();
			v_file.delete();
			v_thumb.delete();
			PerfExplorerServer.getServer().getControl().SIGNAL("saveAnalysisResult");
		} catch (Exception e) {
			String error = "ERROR: Couldn't insert the analysis results into the database!";
			System.out.println(error);
			e.printStackTrace();
			PerfExplorerServer.getServer().getControl().SIGNAL("saveAnalysisResult");
			throw new PerfExplorerException(error, e);
		}
	}

	/**
	 * This method saves the analysis results in the database.
	 * 
	 * @param centroids
	 * @param deviations
	 * @param thumbnail
	 * @param outfile
	 * @throws PerfExplorerException
	 */
	public void saveAnalysisResult (RawDataInterface centroids, RawDataInterface deviations, File thumbnail, File outfile) throws PerfExplorerException {
		// save the image in the database
		try {
			PerfExplorerServer.getServer().getControl().WAIT("saveAnalysisResult");
			DB db = PerfExplorerServer.getServer().getDB();
			db.setAutoCommit(false);
			PreparedStatement statement = null;
			// for each centroid, save the data
			// TODO - MAKE THIS A GENERAL USE LATER!
			StringBuffer buf = new StringBuffer();
			buf.append("insert into analysis_result ");
			buf.append(" (analysis_settings, description, thumbnail_size, thumbnail, image_size, image, result_type) values (?, ?, ?, ?, ?, ?, ?)");
			statement = db.prepareStatement(buf.toString());
			statement.setInt(1, analysisID);
			statement.setString(2, new String("analysis_result"));
       		FileInputStream inStream = new FileInputStream(thumbnail);
       		statement.setInt(3,(int)outfile.length());
       		statement.setBinaryStream(4,inStream,(int)thumbnail.length());
       		FileInputStream inStream2 = new FileInputStream(outfile);
       		statement.setInt(5,(int)outfile.length());
       		statement.setBinaryStream(6,inStream2,(int)outfile.length());
			statement.setInt(7, chartType);
			//System.out.println(statement.toString());
       		statement.executeUpdate();
       		statement.close();
       		// get the new ID
/*			String tmpStr = new String();
			if (db.getDBType().compareTo("mysql") == 0) {
		   	tmpStr = "select LAST_INSERT_ID();";
			} else if (db.getDBType().compareTo("db2") == 0) {
		   	tmpStr = "select IDENTITY_VAL_LOCAL() FROM analysis_result";
			} else if (db.getDBType().compareTo("oracle") == 0) {
		   	tmpStr = "SELECT ar_id_seq.currval FROM DUAL";
			} else { // postgresql 
		   	tmpStr = "select currval('analysis_result_id_seq');";
			}
			int analysisResultID = Integer.parseInt(db.getDataItem(tmpStr));
			
			buf = new StringBuffer();
			buf.append("insert into analysis_result_data ");
			buf.append(" (interval_event, metric, value, data_type, analysis_result, cluster_index)");
			buf.append(" values (?, ?, ?, ?, ?, ?)");
			statement = db.prepareStatement(buf.toString());
			Instances instances = (Instances) centroids.getData();
			for (int i = 0 ; i < instances.numInstances() ; i++) {
				Instance instance = instances.instance(i);
				for (int j = 0 ; j < numEvents ; j++) {
					statement.setString(1, (String) eventIDs.elementAt(j));
					statement.setInt(2, ((Metric)(modelData.getCurrentSelection())).getID());
					statement.setDouble(3, instance.value(j));
					statement.setInt(4, 0);
					statement.setInt(5, analysisResultID);
					statement.setInt(6, i);
		       		statement.executeUpdate();
				}
			}
*/       		statement.close();
       		db.commit();
			PerfExplorerServer.getServer().getControl().SIGNAL("saveAnalysisResult");
		} catch (Exception e) {
			String error = "ERROR: Couldn't insert the analysis results into the database!";
			System.out.println(error);
			e.printStackTrace();
			PerfExplorerServer.getServer().getControl().SIGNAL("saveAnalysisResult");
			throw new PerfExplorerException(error, e);
		}
	}

	/**
	 * This method gets the constant values for the data.  In order to retrieve
	 * the data correctly, it is necessary to get the number of events, metrics, 
	 * threads, and the total number of rows of data we are expecting.
	 * 
	 * @throws PerfExplorerException
	 */
	private void getConstants () throws PerfExplorerException {
		System.out.print("Getting constants...");
		try {
			DB db = PerfExplorerServer.getServer().getDB();
			PreparedStatement statement = null;
			// First, get the total number of rows we are expecting
			StringBuffer sql = new StringBuffer();

            if (db.getDBType().compareTo("oracle") == 0) {
                sql.append("select count(p.excl) ");
            } else {
                sql.append("select count(p.exclusive) ");
            }

			sql.append("from interval_event e ");
			sql.append("left outer join interval_location_profile p ");
			sql.append("on e.id = p.interval_event ");
			if (modelData.getDimensionReduction().equals(RMIPerfExplorerModel.OVER_X_PERCENT)) {
				sql.append("inner join interval_mean_summary s ");
				sql.append("on e.id = s.interval_event and s.metric = p.metric ");
				sql.append("and s.exclusive_percentage > ");
				sql.append("" + modelData.getXPercent() + "");	
			//} else if (modelData.getCurrentSelection() instanceof Metric) {
				//sql.append("inner join interval_mean_summary s ");
				//sql.append("on e.id = s.interval_event and s.metric = p.metric ");
			}
			sql.append("where e.trial = ?");
			sql.append(" and e.group_name not like '%TAU_CALLPATH%' ");
			if (modelData.getCurrentSelection() instanceof Metric) {
				sql.append(" and p.metric = ?");
			}
			PerfExplorerServer.getServer().getControl().WAIT("getConstants");
			statement = db.prepareStatement(sql.toString());
			statement.setInt(1, modelData.getTrial().getID());
			if (modelData.getCurrentSelection() instanceof Metric) {
				statement.setInt(2, ((Metric)(modelData.getCurrentSelection())).getID());
			}
			// System.out.println(statement.toString());
			ResultSet results = statement.executeQuery();
			if (results.next() != false) {
				numRows = results.getInt(1);
			}
			results.close();
			statement.close();
			// give up the connection, if another thread is waiting
			PerfExplorerServer.getServer().getControl().SIGNAL("getConstants");

			if (modelData.getCurrentSelection() instanceof Metric) {
				// Next, get the event names, and count them
				sql = new StringBuffer();
				sql.append("select e.id, e.name from interval_event e ");
				if (modelData.getDimensionReduction().equals(RMIPerfExplorerModel.OVER_X_PERCENT)) {
					sql.append("inner join interval_mean_summary s on ");
					sql.append("e.id = s.interval_event ");
					sql.append("and s.exclusive_percentage > ");
					sql.append("" + modelData.getXPercent() + "");
					sql.append(" where e.trial = ? ");
					if (modelData.getCurrentSelection() instanceof Metric) {
						sql.append(" and s.metric = ? ");
					}
				} else {
					sql.append("where e.trial = ?");
				}
			sql.append(" and e.group_name not like '%TAU_CALLPATH%' ");
				sql.append(" order by 1");
				PerfExplorerServer.getServer().getControl().WAIT("getConstants");
				statement = db.prepareStatement(sql.toString());
				statement.setInt(1, modelData.getTrial().getID());
				if (modelData.getDimensionReduction().equals(RMIPerfExplorerModel.OVER_X_PERCENT)) {
					if (modelData.getCurrentSelection() instanceof Metric) {
						statement.setInt(2, ((Metric)(modelData.getCurrentSelection())).getID());
					}
				}
				//System.out.println(statement.toString());
				results = statement.executeQuery();
				numEvents = 0;
				eventIDs = new ArrayList();
				while (results.next() != false) {
					numEvents++;
					eventIDs.add(results.getString(2));
				}
				results.close();
				statement.close();
				// give up the connection, if another thread is waiting
				PerfExplorerServer.getServer().getControl().SIGNAL("getConstants");
			} else {

				// Next, get the metric names, and count them
				sql = new StringBuffer();
				sql.append("select m.id, m.name from metric m ");
				sql.append("where m.trial = ?");
				sql.append(" order by 1");
				PerfExplorerServer.getServer().getControl().WAIT("getConstants");
				statement = db.prepareStatement(sql.toString());
				statement.setInt(1, modelData.getTrial().getID());
				//System.out.println(statement.toString());
				results = statement.executeQuery();
				numEvents = 0;
				eventIDs = new ArrayList();
				while (results.next() != false) {
					numEvents++;
					eventIDs.add(results.getString(2));
				}
				results.close();
				statement.close();
				// give up the connection, if another thread is waiting
				PerfExplorerServer.getServer().getControl().SIGNAL("getConstants");
			}

			// get the number of threads
			sql = new StringBuffer();
			sql.append("select max(node), max(context), max(thread) ");
			sql.append("from interval_location_profile ");
			sql.append("inner join interval_event ");
			sql.append("on id = interval_event where trial = ? ");
			if (modelData.getCurrentSelection() instanceof Metric) {
				sql.append(" and metric = ? ");
			}
			PerfExplorerServer.getServer().getControl().WAIT("getConstants");
			statement = db.prepareStatement(sql.toString());
			statement.setInt(1, modelData.getTrial().getID());
			if (modelData.getCurrentSelection() instanceof Metric) {
				statement.setInt(2, ((Metric)(modelData.getCurrentSelection())).getID());
			}
			results = statement.executeQuery();
			//System.out.println(statement.toString());
			if (results.next() != false) {
				nodes = results.getInt(1) + 1;
				contexts = results.getInt(2) + 1;
				threads = results.getInt(3) + 1;
				numTotalThreads = nodes * contexts * threads;
			}
			results.close();
			statement.close();
			// give up the connection, if another thread is waiting
			PerfExplorerServer.getServer().getControl().SIGNAL("getConstants");
		} catch (SQLException e) {
			String error = "ERROR: Couldn't the constant settings from the database!";
			System.out.println(error);
			e.printStackTrace();
			PerfExplorerServer.getServer().getControl().SIGNAL("getConstants");
			throw new PerfExplorerException(error, e);
		}
		System.out.println("\nnumRows: " + numRows);
		System.out.println("numCenterRows: " + numCenterRows);
		System.out.println("nodes: " + nodes);
		System.out.println("contexts: " + contexts);
		System.out.println("threads: " + threads);
		System.out.println("numTotalThreads: " + numTotalThreads);
		System.out.println("numEvents: " + numEvents);
		System.out.println(" Done!");
	}

	/**
	 * This method gets the raw performance data from the database.
	 * 
	 * @throws PerfExplorerException
	 */
	private void getRawData () throws PerfExplorerException {
		System.out.print("Getting raw data...");
		rawData = factory.createRawData("Cluster Test", eventIDs, numTotalThreads, numEvents);
		ResultSet results = null;
		int currentFunction = 0;
		int functionIndex = -1;
		int rowIndex = 0;
		int threadIndex = 0;
		maximum = 0.0;
		try {
			PerfExplorerServer.getServer().getControl().WAIT("getRawData");
			DB db = PerfExplorerServer.getServer().getDB();
			PreparedStatement statement = null;
			StringBuffer sql = new StringBuffer();
			if (modelData.getDimensionReduction().equals(RMIPerfExplorerModel.OVER_X_PERCENT)) {
				sql.append("select e.id, (p.node*");
				sql.append(contexts * threads);
				sql.append(") + (p.context*");
				sql.append(threads);
                
                if (db.getDBType().compareTo("oracle") == 0) {
                    sql.append(") + p.thread as thread, p.metric as metric, p.excl ");
                } else {
                    sql.append(") + p.thread as thread, p.metric as metric, p.exclusive ");
                }

				sql.append("from interval_event e ");
				sql.append("inner join interval_mean_summary s ");
				sql.append("on e.id = s.interval_event and s.exclusive_percentage > ");
				sql.append(modelData.getXPercent());
				sql.append(" left outer join interval_location_profile p ");
				sql.append("on e.id = p.interval_event ");
				sql.append("and p.metric = s.metric where e.trial = ? ");
			} else {
				sql.append("select e.id, (p.node*" + (contexts * threads) + "");
				sql.append(") + (p.context*" + threads + "");
                
                if (db.getDBType().compareTo("oracle") == 0) {
                    sql.append(") + p.thread as thread, p.metric as metric, p.excl ");
                } else {
                    sql.append(") + p.thread as thread, p.metric as metric, p.exclusive ");
                }

				sql.append("from interval_event e ");
				sql.append("left outer join interval_location_profile p ");
				sql.append("on e.id = p.interval_event where e.trial = ? ");
			}
			if (modelData.getCurrentSelection() instanceof Metric) {
				sql.append(" and p.metric = ? ");
			}
			sql.append(" and e.group_name not like '%TAU_CALLPATH%' ");
			sql.append(" order by 3,1,2 ");
			statement = db.prepareStatement(sql.toString());
			statement.setInt(1, modelData.getTrial().getID());
			if (modelData.getCurrentSelection() instanceof Metric) {
				statement.setInt(2, ((Metric)(modelData.getCurrentSelection())).getID());
			}
			//System.out.println(statement.toString());
			results = statement.executeQuery();

			// if we are getting data for one metric, we are focussed on the events.
			// if we are getting data for all metrics, we are focussed on the metrics.
			int importantIndex = (modelData.getCurrentSelection() instanceof Metric) ? 1 : 3 ;

			// get the rows
			while (results.next() != false) {
				if (currentFunction != results.getInt(importantIndex)) {
					functionIndex++;
				}
				currentFunction = results.getInt(importantIndex);
				threadIndex = results.getInt(2);
				rawData.addValue(threadIndex, functionIndex, results.getDouble(4));
				if (maximum < results.getDouble(4))
					maximum = results.getDouble(4);
				rowIndex++;
			}
			results.close();
			statement.close();
			PerfExplorerServer.getServer().getControl().SIGNAL("getRawData");
		} catch (SQLException e) {
			String error = "ERROR: Couldn't the raw data from the database!";
			System.out.println(error);
			e.printStackTrace();
			PerfExplorerServer.getServer().getControl().SIGNAL("getRawData");
			throw new PerfExplorerException(error, e);
		} catch (ArrayIndexOutOfBoundsException e2) {
			e2.printStackTrace();
			System.out.println("\ncurrentFunction: " + currentFunction);
			System.out.println("functionIndex: " + functionIndex);
			System.out.println("rowIndex: " + rowIndex);
			System.out.println("threadIndex: " + threadIndex);
			PerfExplorerServer.getServer().getControl().SIGNAL("getRawData");
			System.exit(1);
		}
		System.out.println(" Done!");
	}

	/**
	 * This method creates the dendrogram tree which represents the results
	 * of the hierarchical clustering, if it was done.
	 * 
	 * @param merge
	 * @param height
	 * @return
	 */
	public DendrogramTree createDendrogramTree (int[] merge, double[] height) {
	// public DendrogramTree createDendrogramTree (int[] merge) {
		DendrogramTree leftLeaf = null;
		DendrogramTree rightLeaf = null;
		DendrogramTree newTree = null;
		//Hashtable finder = new Hashtable(height.length);
		Hashtable finder = new Hashtable(merge.length);
		int j = 0;
		for (int i = 0 ; i < height.length ; i++) {
			if (merge[j] < 0)
				leftLeaf = new DendrogramTree(merge[j], 0.0);
			else
				leftLeaf = (DendrogramTree)finder.get(new Integer(merge[j]));
			j++;
			if (merge[j] < 0)
				rightLeaf = new DendrogramTree(merge[j], 0.0);
			else
				rightLeaf = (DendrogramTree)finder.get(new Integer(merge[j]));
			j++;
			newTree = new DendrogramTree(i+1, height[i]);
			newTree.setLeftAndRight(leftLeaf, rightLeaf);
			finder.put(new Integer(i+1), newTree);
		}
		return newTree;
	}

	/**
	 * @return
	 */
	public RawDataInterface doDimensionReduction() {
		// TODO: Implement dimension reduction.
		// for now, just return the original data.
		return rawData;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		modelData = server.getNextRequest();
		if (modelData != null) {
			analysisID = modelData.getAnalysisID();
			try {
				System.out.println("Processing " + modelData.toString());
				// get the number of events, threads, etc.
				getConstants();
				// get the raw data
				getRawData();
				// do dimension reduction, if requested
				RawDataInterface reducedData = doDimensionReduction();
				// find good initial centers
				//makeDendrogram(reducedData);
				// do it!
				if (modelData.getClusterMethod().equals(RMIPerfExplorerModel.K_MEANS)) {
					int maxClusters = (numTotalThreads <= modelData.getNumberOfClusters()) ? (numTotalThreads-1) : modelData.getNumberOfClusters();
					for (int i = 2 ; i <= maxClusters ; i++) {
						System.out.println("Doing " + i + " clusters...");
						// create a cluster engine
						KMeansClusterInterface clusterer = factory.createKMeansEngine();
						clusterer.setInputData(reducedData);
						clusterer.setK(i);
						clusterer.findClusters();
						// get the centroids
						RawDataInterface centroids = clusterer.getClusterCentroids();
						RawDataInterface deviations = clusterer.getClusterStandardDeviations();
						int[] clusterSizes = clusterer.getClusterSizes();
						// do histograms
						File thumbnail = generateThumbnail(clusterSizes, eventIDs);
						File chart = generateImage(clusterSizes, eventIDs);
						// TODO - fix this to save the cluster sizes, too!
						chartType = HISTOGRAM;
						saveAnalysisResult(centroids, deviations, thumbnail, chart);
						
						if (modelData.getCurrentSelection() instanceof Metric) {
						// do PCA breakdown
						PrincipalComponentsAnalysisInterface pca =
						factory.createPCAEngine(server.getCubeData(modelData));
						pca.setInputData(reducedData);
						pca.doPCA();
						// get the components
						RawDataInterface components = pca.getResults();
						pca.setClusterer(clusterer);
						RawDataInterface[] clusters = pca.getClusters();
						// do a scatterplot
						chartType = AnalysisTaskWrapper.PCA_SCATTERPLOT;
						//System.out.println("PCA Dimensions: " + components.numDimensions());
						thumbnail = generateThumbnail(clusters);
						chart = generateImage(components, clusters);
						saveAnalysisResult(components, components, thumbnail, chart);
						}
						
						// do virtual topology
						VirtualTopology vt = new VirtualTopology(modelData, clusterer);
						String filename = vt.getImage();
						String nail = vt.getThumbnail();
						saveAnalysisResult("Virtual Topology", filename, nail, false);
						
						// do maxes
						thumbnail = generateThumbnail(clusterer.getClusterMaximums(), deviations, eventIDs);
						chart = generateImage(clusterer.getClusterMaximums(), deviations, eventIDs);
						chartType = CLUSTER_AVERAGES;
						saveAnalysisResult(clusterer.getClusterMaximums(), deviations, thumbnail, chart);
						// do averages
						thumbnail = generateThumbnail(centroids, deviations, eventIDs);
						chart = generateImage(centroids, deviations, eventIDs);
						chartType = CLUSTER_MAXIMUMS;
						saveAnalysisResult(centroids, deviations, thumbnail, chart);
						// do mins
						thumbnail =
						generateThumbnail(clusterer.getClusterMinimums(), deviations, eventIDs);
						chart = generateImage(clusterer.getClusterMinimums(), deviations, eventIDs);
						chartType = CLUSTER_MINIMUMS;
						saveAnalysisResult(clusterer.getClusterMinimums(), deviations, thumbnail, chart);
					}
				} else {
					System.out.println("Doing PCA...");
					// create a PCA engine
					PrincipalComponentsAnalysisInterface pca = 
						factory.createPCAEngine(server.getCubeData(modelData));
					pca.setInputData(reducedData);
					pca.doPCA();
					// get the components
					RawDataInterface components = pca.getResults();
					//RawDataInterface components = reducedData;
					// do a scatterplot
					chartType = AnalysisTaskWrapper.CORRELATION_SCATTERPLOT;
					System.out.println(components.numDimensions());
					for (int i = 0 ; i < components.numDimensions() ; i++) {
						for (int j = 0 ; j < components.numDimensions() ; j++) {
							File thumbnail = generateThumbnail(components, i, j);
							File chart = generateImage(components, i, j);
							saveAnalysisResult(components, components, thumbnail, chart);			
						}
						System.out.println("Finished: " + (i+1) + " of " + components.numDimensions());
					}
				}
			}catch (PerfExplorerException pee) {
			}catch (ClusterException ce) {
			}
			// let the server (main thread) know we are done
			server.taskFinished();
			modelData = null;
		} // else 
			//System.out.println("nothing to do... ");
			
	}
	
	/**
	 * 
	 * @param centroids
	 * @param deviations
	 * @param rowLabels
	 * @return
	 */
	public File generateThumbnail(RawDataInterface centroids, RawDataInterface deviations, List rowLabels) {
		// create a JFreeChart of this analysis data.  Create a stacked bar chart
		// with standard deviation bars?
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int x = 0 ; x < centroids.numVectors() ; x++) {
			for (int y = 0 ; y < centroids.numDimensions() ; y++) {
				dataset.addValue(centroids.getValue(x,y), (String)rowLabels.get(y), new String(Integer.toString(x)));
			}
		}
        JFreeChart chart = ChartFactory.createStackedBarChart(
        		null, null, null,     // range axis label
            dataset,                         // data
            PlotOrientation.HORIZONTAL,        // the plot orientation
            false,                            // legend
            true,                            // tooltips
            false                            // urls
        );
        File outfile = new File("/tmp/thumbnail." + modelData.toShortString() + ".png");
        try {
        		ChartUtilities.saveChartAsPNG(outfile, chart, 100, 100);
        } catch (IOException e) {}
        return outfile;
	}

	/**
	 * @param clusterSizes
	 * @param rowLabels
	 * @return
	 */
	public File generateThumbnail(int[] clusterSizes, List rowLabels) {
		// create a JFreeChart of this analysis data.  Create a stacked bar chart
		// with standard deviation bars?
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int x = 0 ; x < clusterSizes.length ; x++) {
			dataset.addValue(clusterSizes[x], "Threads in cluster", new String(Integer.toString(x)));
		}
        JFreeChart chart = ChartFactory.createStackedBarChart(
        		null, null, null,     // range axis label
            dataset,                         // data
            PlotOrientation.HORIZONTAL,        // the plot orientation
            false,                            // legend
            true,                            // tooltips
            false                            // urls
        );
        File outfile = new File("/tmp/thumbnail." + modelData.toShortString() + ".png");
        try {
        		ChartUtilities.saveChartAsPNG(outfile, chart, 100, 100);
        } catch (IOException e) {}
        return outfile;
	}

	/**
	 * @param pcaData
	 * @param rawData
	 * @param clusterer
	 * @return
	 */
	public File generateThumbnail(RawDataInterface[] clusters) {
		File outfile = null;
		if (chartType == PCA_SCATTERPLOT) {
	        XYDataset data = new PCAPlotDataset(clusters);
	        JFreeChart chart = ChartFactory.createScatterPlot(
	            null, null, null, data, PlotOrientation.VERTICAL, false, false, false);
	        outfile = new File("/tmp/thumbnail." + modelData.toShortString() + ".png");
	        try {
        			ChartUtilities.saveChartAsPNG(outfile, chart, 100, 100);
	        } catch (IOException e) {}
		}
        return outfile;
	}

	/**
	 * @param pcaData
	 * @param rawData
	 * @param clusterer
	 * @return
	 */
	public File generateImage(RawDataInterface pcaData, RawDataInterface[] clusters) {
		File outfile = null;
		if (chartType == PCA_SCATTERPLOT) {
		/*
			int max = pcaData.numDimensions();
			int x = max - 1;
			int y = max - 2;
			if (max < 2) {
				x = 0;
				y = 0;
			}
			*/
	        XYDataset data = new PCAPlotDataset(clusters);
	        JFreeChart chart = ChartFactory.createScatterPlot(
	            "PCA Results",
	            (String)(pcaData.getEventNames().get(0)),
	            (String)(pcaData.getEventNames().get(1)),
	            data,
	            PlotOrientation.VERTICAL,
	            true,
	            false,
	            false
	        );
	        outfile = new File("/tmp/image." + modelData.toShortString() + ".png");
	        try {
	        		ChartUtilities.saveChartAsPNG(outfile, chart, 800, 800);
	        } catch (IOException e) {}
		}
        return outfile;
	}

	/**
	 * @param pcaData
	 * @param i
	 * @param j
	 * @return
	 */
	public File generateThumbnail(RawDataInterface pcaData, int i, int j) {
		File outfile = null;
		if (chartType == CORRELATION_SCATTERPLOT) {
	        XYDataset data = new ScatterPlotDataset(pcaData, modelData.toString(), i, j);
	        JFreeChart chart = ChartFactory.createScatterPlot(
	            null, null, null, data, PlotOrientation.VERTICAL, false, false, false);
	        outfile = new File("/tmp/thumbnail." + modelData.toShortString() + ".png");
	        try {
        			ChartUtilities.saveChartAsPNG(outfile, chart, 100, 100);
	        } catch (IOException e) {}
		}
        return outfile;
	}

	/**
	 * @param pcaData
	 * @param i
	 * @param j
	 * @return
	 */
	public File generateImage(RawDataInterface pcaData, int i, int j) {
		File outfile = null;
		if (chartType == CORRELATION_SCATTERPLOT) {
	        XYDataset data = new ScatterPlotDataset(pcaData, modelData.toString(), i, j);
	        JFreeChart chart = ChartFactory.createScatterPlot(
	            "PCA Results",
	            (String)(pcaData.getEventNames().get(i)),
	            (String)(pcaData.getEventNames().get(j)),
	            data,
	            PlotOrientation.VERTICAL,
	            true,
	            false,
	            false
	        );
	        outfile = new File("/tmp/image." + modelData.toShortString() + ".png");
	        try {
	        		ChartUtilities.saveChartAsPNG(outfile, chart, 800, 800);
	        } catch (IOException e) {}
		}
        return outfile;
	}

	/**
	 * @param clusterSizes
	 * @param rowLabels
	 * @return
	 */
	public File generateImage(int[] clusterSizes, List rowLabels) {
		// create a JFreeChart of this analysis data.  Create a stacked bar chart
		// with standard deviation bars?
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int x = 0 ; x < clusterSizes.length ; x++) {
			dataset.addValue(clusterSizes[x], "Threads in cluster", new String(Integer.toString(x)));
		}
        JFreeChart chart = ChartFactory.createStackedBarChart(
            modelData.toString(),  // chart title
            "Cluster Number",          // domain axis label
            "Threads in cluster",     // range axis label
            dataset,                         // data
            PlotOrientation.HORIZONTAL,        // the plot orientation
            true,                            // legend
            true,                            // tooltips
            false                            // urls
        );
        File outfile = new File("/tmp/image." + modelData.toShortString() + ".png");
        try {
        		ChartUtilities.saveChartAsPNG(outfile, chart, 500, 500);
        } catch (IOException e) {}
        return outfile;
	}

	/**
	 * @param centroids
	 * @param deviations
	 * @param rowLabels
	 * @return
	 */
	public File generateImage(RawDataInterface centroids, RawDataInterface deviations, List rowLabels) {
		// create a JFreeChart of this analysis data.  Create a stacked bar chart
		// with standard deviation bars?
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (int x = 0 ; x < centroids.numVectors() ; x++) {
			for (int y = 0 ; y < centroids.numDimensions() ; y++) {
				dataset.addValue(centroids.getValue(x,y), (String) rowLabels.get(y), new String(Integer.toString(x)));
			}
		}
        JFreeChart chart = ChartFactory.createStackedBarChart(
            modelData.toString(),  // chart title
            "Cluster Number",          // domain axis label
            "Total Runtime",     // range axis label
            dataset,                         // data
            PlotOrientation.HORIZONTAL,        // the plot orientation
            true,                            // legend
            true,                            // tooltips
            false                            // urls
        );
        File outfile = new File("/tmp/image." + modelData.toShortString() + ".png");
        try {
        		ChartUtilities.saveChartAsPNG(outfile, chart, 500, 500);
        } catch (IOException e) {}
        return outfile;
	}
}

