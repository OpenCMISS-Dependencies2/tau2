package server;

import common.EngineType;
import common.InterpreterDriver.InterpreterException;
import common.PerfExplorerOutput;
import common.RMIPerfExplorerModel;
import common.TransformationType;
import common.InterpreterDriverManager;

import edu.uoregon.tau.perfdmf.Application;
import edu.uoregon.tau.perfdmf.Experiment;
import edu.uoregon.tau.perfdmf.Metric;
import edu.uoregon.tau.perfdmf.Trial;

import java.io.FileInputStream;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class exists as a unit test of the PerfExplorerServer class.
 *
 * <P>CVS $Id: TestServer.java,v 1.9 2007/01/05 19:24:19 khuck Exp $</P>
 * @author  Kevin Huck
 * @version 0.1
 * @since   0.1
 */
public class TestServer {
	PerfExplorerServer server = null;
	
    public TestServer(String configFile, EngineType engine) {
        server = PerfExplorerServer.getServer(configFile, engine);
        PerfExplorerOutput.println(server.sayHello());
    }

    public void testScripting() throws Exception {
	/*
        Properties _scriptProp = new Properties();
        try {
            _scriptProp.load(new FileInputStream("script.prop"));

            String scriptDrivers = _scriptProp.getProperty("script.drivers");
            StringTokenizer driverTokenizer
                = new StringTokenizer(scriptDrivers, ":");
            while (driverTokenizer.hasMoreTokens()) {
                Class.forName(driverTokenizer.nextToken());
            }
        } catch (Exception ex) {
            System.err.println("Failed to load interpreter drivers " + ex);
            throw ex;
        }

        try {
            InterpreterDriverManager.executeScriptFile("test.py");
        } catch (InterpreterException e) {
            System.err.println("Failed to execute test script");
            throw e;
        }
		*/
		edu.uoregon.tau.common.TauScripter.execfile("etc/test.py");

    }

    public void testClustering() throws Exception {
        Object[] objects = new Object[4];
        ListIterator apps = server.getApplicationList().listIterator();
        Application app = null;
        while (apps.hasNext()) {
            app = (Application)apps.next();
            if (app.getID() == 12) {
                objects[0] = app;
                break;
            }
        }
        ListIterator exps =
        server.getExperimentList(app.getID()).listIterator();
        Experiment exp = null;
        while (exps.hasNext()) {
            exp = (Experiment)exps.next();
            if (exp.getID() == 66) {
                objects[1] = exp;
                break;
            }
        }
        ListIterator trials =
        server.getTrialList(exp.getID()).listIterator();
        Trial trial = null;
        while (trials.hasNext()) {
            trial = (Trial)trials.next();
            if (trial.getID() == 430) {
                objects[2] = trial;
                break;
            }
        }
        Vector metrics = trial.getMetrics();
        for (int i = 0 ; i < metrics.size() ; i++) {
            Metric metric = (Metric)metrics.elementAt(i);
            if (metric.getID() == 1272) {
                objects[3] = metric;
                break;
            }
        }
        RMIPerfExplorerModel model = new RMIPerfExplorerModel();
        //model.setClusterMethod(RMIPerfExplorerModel.CORRELATION_ANALYSIS);
        model.setDimensionReduction(TransformationType.OVER_X_PERCENT);
        model.setNumberOfClusters("10");
        model.setXPercent("2");
        model.setCurrentSelection(objects);
        String status = server.requestAnalysis(model, true);
        PerfExplorerOutput.println(status);
        if (status.equals("Request already exists"))
            PerfExplorerOutput.println(server.requestAnalysis(model, true));
    }

    public static void main (String[] args) {
		PerfExplorerOutput.println ("LIBRARY PATH: " + System.getProperty ("java.library.path"));
		try {
			//int engine = AnalysisTaskWrapper.RPROJECT_ENGINE;
			EngineType engine = EngineType.WEKA;
			//int engine = AnalysisTaskWrapper.OCTAVE_ENGINE;
			TestServer tester = new TestServer(args[0], engine);
			tester.testScripting();
			
		} catch (Exception e) {
			System.err.println("TestServer exception: " + e.getMessage());
			e.printStackTrace();
		}
        try {
            java.lang.Thread.sleep(300000);
        } catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
        }
        System.exit(0);
	}
}

