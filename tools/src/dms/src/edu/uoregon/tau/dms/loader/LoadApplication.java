package edu.uoregon.tau.dms.loader;

import edu.uoregon.tau.dms.database.*;
import jargs.gnu.CmdLineParser;

public class LoadApplication {
    private Load load = null;
    private DB db = null;
    
    private static String APP_USAGE = 
        "usage: perfdmf_loadapp [{-h,--help}] {-x,--xmlfile} filename\n";

    private ConnectionManager connector;

    public LoadApplication(String configFileName) {
	super();
	connector = new ConnectionManager(configFileName);
    }

    public ConnectionManager getConnector(){
	return connector;
    }

    public Load getLoad() {
	if (load == null) {
	    if (connector.getDB() == null) {
		load = new Load(connector.getParserClass());
	    } else {
		load = new Load(connector.getDB(), connector.getParserClass());
	    }
	}
	return load;
    }

    /*** Parse and load an application. ***/   

    public String storeApp(String appFile) {
	String appid = null;

	try {	
	    appid = getLoad().parseApp(appFile);
	} catch (Throwable ex) {
	    System.out.println("Error: " + ex.getMessage());
	    return null;
	}

	if ((appid==null) || (appid.trim().length()==0)) {
	    System.out.println("Loadding application failed");
	    return null;
	}
	return appid;
    }

    /*** Beginning of main program. ***/

    public static void main(java.lang.String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
        CmdLineParser.Option configfileOpt = parser.addStringOption('g', "configfile");
        CmdLineParser.Option xmlfileOpt = parser.addStringOption('x', "xmlfile");

        try {
            parser.parse(args);
        }
        catch ( CmdLineParser.OptionException e ) {
            System.err.println(e.getMessage());
	    System.err.println(APP_USAGE);
	    System.exit(-1);
        }

        Boolean help = (Boolean)parser.getOptionValue(helpOpt);
        String configFile = (String)parser.getOptionValue(configfileOpt);
        String xmlFile = (String)parser.getOptionValue(xmlfileOpt);

    	if (help != null && help.booleanValue()) {
	    System.err.println(APP_USAGE);
	    System.exit(-1);
    	}

	if (configFile == null) {
            System.err.println("Please enter a valid config file.");
	    System.err.println(APP_USAGE);
	    System.exit(-1);
	}

	// validate the command line options...
	if (xmlFile == null) {
	    System.err.println("Please enter a valid application XML file.");
	    System.err.println(APP_USAGE);
	    System.exit(-1);
	}

	// create a new LoadApplication object, pass in the configuration file name
	LoadApplication loadApp = new LoadApplication(configFile);
	try {
	    loadApp.getConnector().connect();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(0);
	}

	int exitval = 0;
	
    	/***** Load appliation into PerfDMF *********/
	String appid = loadApp.storeApp(xmlFile);
	if (appid != null)
	    exitval = Integer.parseInt(appid);

	loadApp.getConnector().dbclose();
	// System.exit(exitval);
    }

}

