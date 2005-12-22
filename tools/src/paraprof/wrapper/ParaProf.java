package edu.uoregon.tau.paraprof;

import jargs.gnu.CmdLineParser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;

import javax.swing.ToolTipManager;

import edu.uoregon.tau.perfdmf.DataSource;
import edu.uoregon.tau.perfdmf.FileList;
import edu.uoregon.tau.perfdmf.UtilFncs;

/**
 * ParaProf This is the 'main' for paraprof
 * 
 * <P>
 * CVS $Id: ParaProf.java,v 1.3 2005/12/22 00:37:43 amorris Exp $
 * </P>
 * 
 * @author Robert Bell, Alan Morris
 * @version $Revision: 1.3 $
 */
public class ParaProf implements ActionListener {

    // This class handles uncaught throwables on the AWT-EventQueue thread
    static public class XThrowableHandler {

        public XThrowableHandler() {
        }

        public void handle(Throwable t) throws Throwable {
            if (t instanceof Exception) {
                ParaProfUtils.handleException((Exception) t);
            } else {
                System.err.println("Uncaught Throwable: " + t.fillInStackTrace());
            }
        }
    }

    private final static String VERSION = "Wed Dec 21 16:36:30 PST 2005";

    static ColorMap colorMap = new ColorMap();

    //System wide stuff.
    static File paraProfHomeDirectory = null;
    public static int defaultNumberPrecision = 6;
    //static ParaProfLisp paraProfLisp = null;
    public static Preferences preferences = null;
    public static ColorChooser colorChooser;

    public static ParaProfManagerWindow paraProfManagerWindow = null;
    public static ApplicationManager applicationManager = new ApplicationManager();
    public static HelpWindow helpWindow = null;
    public static PreferencesWindow preferencesWindow;
    public static Runtime runtime = null;
    private static int numWindowsOpen = 0;
    //End - System wide stuff.

    //Command line options related.
    private static int fileType = 0; //0:profile, 1:pprof, 2:dynaprof, 3:mpip, 4:hpmtoolkit, 5:gprof, 6:psrun, 7:ppk, 8:cube
    private static File sourceFiles[] = new File[0];
    private static boolean fixNames = false;
    private static boolean monitorProfiles;
    //End - Command line options related.

    public static boolean demoMode;
    
    public static FunctionBarChartWindow theComparisonWindow;
    
    public static boolean JNLP = false;
    
    public ParaProf() {

        try {
            //            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            //            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            //
            //            
            //            UIManager.setLookAndFeel(
            //                                     "com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            //     
            //            UIManager.setLookAndFeel(
            //            "javax.swing.plaf.metal.MetalLookAndFeel");
            //
            //            UIManager.setLookAndFeel(
            //            "com.sun.java.swing.plaf.motif.MotifLookAndFeel");

        } catch (Exception e) {

        }

    }

    private static void usage() {
        System.err.println("Usage: paraprof [--pack <file>] [--dump] [-p] [-m] [-i] [-f <filetype>] <files/directory>\n\n"
                + "try `paraprof --help` for more information");
    }

    private static void outputHelp() {
        System.err.println("Usage: paraprof [options] <files/directory> \n\n" + "Options:\n\n"
                + "  -f, --filetype <filetype>       Specify type of performance data, options are:\n"
                + "                                    profiles (default), pprof, dynaprof, mpip,\n"
                + "                                    gprof, psrun, hpm, packed, cube, hpc\n" + "\n"
                + "  -h, --help                      Display this help message\n"
                + "  -p                              Use `pprof` to compute derived data\n"
                + "  -i, --fixnames                  Use the fixnames option for gprof\n" + "\n"
                + "  --pack <file>                   Pack the data into packed (.ppk) format\n"
                + "                                    (does not launch ParaProf GUI)\n"
                + "  --dump                          Dump profile data to TAU profile format\n"
                + "                                    (does not launch ParaProf GUI)\n" + "\n" + "Notes:\n"
                + "  -m, --monitor                   Perform runtime monitoring of profile data\n"
                + "\n"
                + "  For the TAU profiles type, you can specify either a specific set of profile\n"
                + "files on the commandline, or you can specify a directory (by default the current\n"
                + "directory).  The specified directory will be searched for profile.*.*.* files,\n"
                + "or, in the case of multiple counters, directories named MULTI_* containing\n" + "profile data.\n\n");
    }

    public static void incrementNumWindows() {
        //        System.out.println ("incrementing");
        numWindowsOpen++;
    }

    public static void decrementNumWindows() {
        //        System.out.println ("decrementing");
        numWindowsOpen--;
        if (numWindowsOpen <= 0) {
            exitParaProf(0);
        }
    }

    public void loadDefaultTrial() {

        // Create a default application.
        ParaProfApplication app = ParaProf.applicationManager.addApplication();
        app.setName("Default App");

        // Create a default experiment.
        ParaProfExperiment experiment = app.addExperiment();
        experiment.setName("Default Exp");

        ParaProf.helpWindow = new HelpWindow();
        ParaProf.paraProfManagerWindow = new ParaProfManagerWindow();

        try {
            paraProfManagerWindow.addTrial(app, experiment, sourceFiles, fileType, fixNames, monitorProfiles);
        } catch (java.security.AccessControlException ace) {
            // running as Java Web Start without permission
        }
    }

    public void startSystem() {
        try {
            // Initialization of static objects takes place on a need basis.
            // This helps prevent the creation of a graphical system unless it is absolutely
            // necessary. Static initializations are marked with "Static Initialization" 
            // to make them easy to find.

            ParaProf.preferences = new Preferences();

            //Establish the presence of a .ParaProf directory. This is located
            // by default in the user's home directory.

            try {
                if (System.getProperty("jnlp.running") != null) {
                    ParaProf.JNLP = true;
                }
            } catch (java.security.AccessControlException ace) {
                ParaProf.JNLP = true;
            }

            if (ParaProf.JNLP == false) {
                ParaProf.paraProfHomeDirectory = new File(System.getProperty("user.home") + "/.ParaProf");
                if (paraProfHomeDirectory.exists()) {

                    //Try and load a preference file ... ParaProfPreferences.dat
                    try {
                        FileInputStream savedPreferenceFIS = new FileInputStream(ParaProf.paraProfHomeDirectory.getPath()
                                + "/ParaProf.conf");

                        //If here, means that no exception was thrown, and there is a preference file present.
                        //Create ObjectInputStream and try to read it in.
                        ObjectInputStream inSavedPreferencesOIS = new ObjectInputStream(savedPreferenceFIS);
                        ParaProf.preferences = (Preferences) inSavedPreferencesOIS.readObject();
                        ParaProf.preferences.setLoaded(true);
                        colorChooser = new ColorChooser(ParaProf.preferences);
                    } catch (Exception e) {
                        if (e instanceof FileNotFoundException) {
                            //System.out.println("No preference file present, using defaults!");
                        } else {
                            //Print some kind of error message, and quit the system.
                            System.out.println("Error while trying to read the ParaProf preferences file, using defaults");
                            //                        System.out.println("Please delete this file, or replace it with a valid one!");
                            //                        System.out.println("Note: Deleting the file will cause ParaProf to restore the default preferences");
                        }
                    }

                    ParaProf.colorMap.setMap(preferences.getAssignedColors());
                    ParaProf.preferences.setDatabasePassword(null);

                    //Try and find perfdmf.cfg.
                    File perfDMFcfg = new File(ParaProf.paraProfHomeDirectory.getPath() + "/perfdmf.cfg");
                    if (perfDMFcfg.exists()) {
                        //System.out.println("Found db configuration file: "
                        //        + ParaProf.paraProfHomeDirectory.getPath() + "/perfdmf.cfg");
                        ParaProf.preferences.setDatabaseConfigurationFile(ParaProf.paraProfHomeDirectory.getPath()
                                + "/perfdmf.cfg");
                    } else {
                        System.out.println("Did not find db configuration file ... load manually");
                    }

                } else {
                    System.out.println("Did not find ParaProf home directory ... creating ...");
                    paraProfHomeDirectory.mkdir();
                    System.out.println("Done creating ParaProf home directory!");
                }
            } else {
                // Java Web Start
                //URL url = ParaProf.class.getResource("/perfdmf.cfg");
                //throw new ParaProfException("URL = " + url);
                
                URL url = ParaProf.class.getResource("/perfdmf.cfg");
                
                String path = URLDecoder.decode(url.getPath());
                
                ParaProf.preferences.setDatabaseConfigurationFile(path);

            }

            if (colorChooser == null) {
                ParaProf.colorChooser = new ColorChooser(null);
            }

            ParaProf.preferencesWindow = new PreferencesWindow(preferences);

            DataSource.setMeanIncludeNulls(!preferences.getComputeMeanWithoutNulls());

            //            javax.swing.SwingUtilities.invokeLater(new Runnable() {
            //              public void run() {
            try {
                System.setProperty("sun.awt.exception.handler", XThrowableHandler.class.getName());
            } catch (java.security.AccessControlException ace) {
                // running as Java Web Start without permission
            }
            loadDefaultTrial();

            //            }
            //      });

        } catch (Exception e) {
            ParaProfUtils.handleException(e);
        }
    }

    public void actionPerformed(ActionEvent evt) {
        Object EventSrc = evt.getSource();
        if (EventSrc instanceof javax.swing.Timer) {
            System.out.println("------------------------");
            System.out.println("The amount of memory used by the system is: " + runtime.totalMemory());
            System.out.println("The amount of memory free to the system is: " + runtime.freeMemory());
        }
    }

    public static String getInfoString() {
        long memUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;

        return new String("ParaProf 3\n" + getVersionString() + "\nJVM Heap Size: " + memUsage + "kb\n");
    }

    public static String getVersionString() {
        return new String(VERSION);
    }

    public static void loadPreferences(File file) throws FileNotFoundException, IOException, ClassNotFoundException {

        FileInputStream savedPreferenceFIS = new FileInputStream(file);

        //If here, means that no exception was thrown, and there is a preference file present.
        //Create ObjectInputStream and try to read it in.
        ObjectInputStream inSavedPreferencesOIS = new ObjectInputStream(savedPreferenceFIS);
        ParaProf.preferences = (Preferences) inSavedPreferencesOIS.readObject();
        ParaProf.preferences.setLoaded(true);
        colorChooser = new ColorChooser(ParaProf.preferences);

        ParaProf.colorMap.setMap(ParaProf.preferences.getAssignedColors());

        List trials = ParaProf.paraProfManagerWindow.getLoadedTrials();
        for (Iterator it = trials.iterator(); it.hasNext();) {
            ParaProfTrial ppTrial = (ParaProfTrial) it.next();
            ParaProf.colorChooser.setColors(ppTrial, -1);
            ppTrial.updateRegisteredObjects("colorEvent");
            ppTrial.updateRegisteredObjects("prefEvent");
        }

    }

    // This method is reponsible for any cleanup required in ParaProf 
    // before an exit takes place.
    public static void exitParaProf(int exitValue) {
        //try {
        //   throw new Exception();
        //} catch (Exception e) {
        //   e.printStackTrace();
        //}

        try {
            savePreferences(new File(ParaProf.paraProfHomeDirectory.getPath() + "/ParaProf.conf"));
        } catch (Exception e) {
            // we'll get an exception here if running under Java Web Start
        }

        System.exit(exitValue);
    }

    public static boolean savePreferences(File file) {

        ParaProf.colorChooser.setSavedColors();
        ParaProf.preferences.setAssignedColors(ParaProf.colorMap.getMap());
        ParaProf.preferences.setManagerWindowPosition(ParaProf.paraProfManagerWindow.getLocation());

        //        System.out.println ("saving manager position = " + preferences.getManagerWindowPosition());

        try {
            ObjectOutputStream prefsOut = new ObjectOutputStream(new FileOutputStream(file));
            prefsOut.writeObject(ParaProf.preferences);
            prefsOut.close();
        } catch (Exception e) {
            System.err.println("An error occured while trying to save ParaProf preferences.");
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    // Main entry point
    static public void main(String[] args) {

        final ParaProf paraProf = new ParaProf();

        // Set the tooltip delay to 20 seconds
        ToolTipManager.sharedInstance().setDismissDelay(20000);

        // Process command line arguments
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
        CmdLineParser.Option configfileOpt = parser.addStringOption('g', "configfile");
        CmdLineParser.Option typeOpt = parser.addStringOption('f', "filetype");
        CmdLineParser.Option fixOpt = parser.addBooleanOption('i', "fixnames");
        CmdLineParser.Option packOpt = parser.addStringOption('a', "pack");
        CmdLineParser.Option unpackOpt = parser.addBooleanOption('u', "dump");
        CmdLineParser.Option monitorOpt = parser.addBooleanOption('m', "monitor");
        CmdLineParser.Option demoOpt = parser.addBooleanOption('z', "demo");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println("paraprof: " + e.getMessage());
            ParaProf.usage();
            System.exit(-1);
        }

        Boolean help = (Boolean) parser.getOptionValue(helpOpt);
        String fileTypeString = (String) parser.getOptionValue(typeOpt);
        Boolean fixNames = (Boolean) parser.getOptionValue(fixOpt);
        String pack = (String) parser.getOptionValue(packOpt);
        Boolean unpack = (Boolean) parser.getOptionValue(unpackOpt);
        Boolean monitor = (Boolean) parser.getOptionValue(monitorOpt);
        Boolean demo = (Boolean) parser.getOptionValue(demoOpt);

        demoMode = demo != null && demo.booleanValue();

        if (monitor != null) {
            monitorProfiles = monitor.booleanValue();
        }
        
        if (pack != null && unpack != null) {
            System.err.println("--pack and --dump are mutually exclusive");
            System.exit(-1);
        }

        if (help != null && help.booleanValue()) {
            ParaProf.outputHelp();
            System.exit(-1);
        }

        String sourceFilenames[] = parser.getRemainingArgs();
        sourceFiles = new File[sourceFilenames.length];
        for (int i = 0; i < sourceFilenames.length; i++) {
            sourceFiles[i] = new File(sourceFilenames[i]);
        }

        if (fixNames != null)
            ParaProf.fixNames = fixNames.booleanValue();

        if (fileTypeString != null) {
            if (fileTypeString.equals("profiles")) {
                ParaProf.fileType = 0;
            } else if (fileTypeString.equals("pprof")) {
                ParaProf.fileType = 1;
            } else if (fileTypeString.equals("dynaprof")) {
                ParaProf.fileType = 2;
            } else if (fileTypeString.equals("mpip")) {
                ParaProf.fileType = 3;
            } else if (fileTypeString.equals("hpm")) {
                ParaProf.fileType = 4;
            } else if (fileTypeString.equals("gprof")) {
                ParaProf.fileType = 5;
            } else if (fileTypeString.equals("psrun")) {
                ParaProf.fileType = 6;
            } else if (fileTypeString.equals("packed")) {
                ParaProf.fileType = 7;
            } else if (fileTypeString.equals("cube")) {
                ParaProf.fileType = 8;
            } else if (fileTypeString.equals("hpc")) {
                ParaProf.fileType = 9;
            } else {
                System.err.println("Please enter a valid file type.");
                ParaProf.usage();
                System.exit(-1);
            }
        } else {
            if (sourceFilenames.length == 1) {
                String filename = sourceFiles[0].getName();
                if (filename.endsWith(".ppk")) {
                    ParaProf.fileType = 7;
                }
                if (filename.endsWith(".cube")) {
                    ParaProf.fileType = 8;
                }
            }
        }

        ParaProf.runtime = Runtime.getRuntime();

        if (pack != null) {
            try {

                DataSource dataSource = UtilFncs.initializeDataSource(sourceFiles, fileType, ParaProf.fixNames);
                System.out.println("Loading data...");
                dataSource.load();
                System.out.println("Packing data...");
                ParaProfUtils.writePacked(dataSource, new File(pack));

            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }

        if (unpack != null && unpack.booleanValue()) {
            try {

                FileList fl = new FileList();
                List v = fl.helperFindProfiles(".");
                if (v.size() != 0) {
                    System.err.println("Error: profiles found in current directory, please remove first");
                    return;
                }

                DataSource dataSource = UtilFncs.initializeDataSource(sourceFiles, fileType, ParaProf.fixNames);
                System.out.println("Loading data...");
                dataSource.load();
                System.out.println("Creating TAU Profile data...");
                UtilFncs.writeProfiles(dataSource, new File("."));

            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                paraProf.startSystem();
            }
        });
    }
}