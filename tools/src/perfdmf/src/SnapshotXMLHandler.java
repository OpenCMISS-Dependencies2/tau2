package edu.uoregon.tau.perfdmf;

import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML Handler for snapshot profiles, this is where all the work is done
 *
 * <P>CVS $Id: SnapshotXMLHandler.java,v 1.10 2007/05/02 17:18:04 amorris Exp $</P>
 * @author  Alan Morris
 * @version $Revision: 1.10 $
 */
public class SnapshotXMLHandler extends DefaultHandler {

    private SnapshotDataSource dataSource;

    private Map threadMap = new HashMap();

    private ThreadData currentThread;
    private Snapshot currentSnapshot;
    private Date currentDate;

    private int currentMetrics[];

    private StringBuffer accumulator = new StringBuffer();

    private int currentId;
    private String currentName;
    private String currentValue;
    private String currentGroup;
    private long currentTimestamp;

    private boolean firstSnapshotFound;

    private static class ThreadData {
        public Thread thread;
        public Map metricMap = new HashMap();
        public Map eventMap = new HashMap();
    }

    public SnapshotXMLHandler(SnapshotDataSource source) {
        this.dataSource = source;
    }

    public void startDocument() throws SAXException {
    //System.out.println("startDocument");
    }

    public void endDocument() throws SAXException {
    //System.out.println("endDocument");
    }

    private void handleMetric(String name) {
        Metric metric = dataSource.addMetric(name);
        currentThread.metricMap.put(new Integer(currentId), metric);
    }

    private void handleEvent(String name, String groups) {
        int id = currentId;

        Function function = dataSource.addFunction(name);
        dataSource.addGroups(groups, function);
        currentThread.eventMap.put(new Integer(id), function);
    }

    private void handleThread(Attributes attributes) {
        String threadName = attributes.getValue("id");
        int nodeID = Integer.parseInt(attributes.getValue("node"));
        int contextID = Integer.parseInt(attributes.getValue("context"));
        int threadID = Integer.parseInt(attributes.getValue("thread"));

        ThreadData data = new ThreadData();
        data.thread = dataSource.addThread(nodeID, contextID, threadID);
        threadMap.put(threadName, data);
        currentThread = data;
    }

    private void handleDefinitions(Attributes attributes) {
        String threadID = attributes.getValue("thread");
        currentThread = (ThreadData) threadMap.get(threadID);
    }

    private void handleProfile(Attributes attributes) {
        String threadID = attributes.getValue("thread");
        currentThread = (ThreadData) threadMap.get(threadID);
        if (!firstSnapshotFound) {
            currentSnapshot = (Snapshot) currentThread.thread.getSnapshots().get(0);
            firstSnapshotFound = true;
        } else {
            currentSnapshot = currentThread.thread.addSnapshot("");
        }
    }

    private void handleIntervalData(Attributes attributes) {
        String metrics = attributes.getValue("metrics");

        StringTokenizer tokenizer = new StringTokenizer(metrics, " \t\n\r");

        currentMetrics = new int[tokenizer.countTokens()];
        int index = 0;
        while (tokenizer.hasMoreTokens()) {
            int metricID = Integer.parseInt(tokenizer.nextToken());
            currentMetrics[index++] = metricID;
        }
    }

    private void handleIntervalDataEnd() {
        String data = accumulator.toString();

        StringTokenizer tokenizer = new StringTokenizer(data, " \t\n\r");

        while (tokenizer.hasMoreTokens()) {
            int eventID = Integer.parseInt(tokenizer.nextToken());

            Function function = (Function) currentThread.eventMap.get(new Integer(eventID));

            FunctionProfile fp = currentThread.thread.getFunctionProfile(function);
            if (fp == null) {
                fp = new FunctionProfile(function, dataSource.getNumberOfMetrics(), currentThread.thread.getSnapshots().size());
                currentThread.thread.addFunctionProfile(fp);
            }

            double numcalls = Double.parseDouble(tokenizer.nextToken());
            double numsubr = Double.parseDouble(tokenizer.nextToken());

            for (int i = 0; i < currentMetrics.length; i++) {
                int metricID = currentMetrics[i];
                Metric metric = (Metric) currentThread.metricMap.get(new Integer(metricID));
                double exclusive = Double.parseDouble(tokenizer.nextToken());
                double inclusive = Double.parseDouble(tokenizer.nextToken());
                fp.setExclusive(metric.getID(), exclusive);
                fp.setInclusive(metric.getID(), inclusive);
            }

            fp.setNumCalls(numcalls);
            fp.setNumSubr(numsubr);

            //System.out.println("item = "+ item);
        }

    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //System.out.println("startElement: uri:" + uri + ", localName:"+localName+", qName:"+qName);

        if (localName.equals("thread")) {
            handleThread(attributes);
        } else if (localName.equals("name")) {
            accumulator = new StringBuffer();
        } else if (localName.equals("value")) {
            accumulator = new StringBuffer();
        } else if (localName.equals("group")) {
            accumulator = new StringBuffer();
        } else if (localName.equals("utc_date")) {
            accumulator = new StringBuffer();
        } else if (localName.equals("timestamp")) {
            accumulator = new StringBuffer();
        } else if (localName.equals("definitions")) {
            handleDefinitions(attributes);
        } else if (localName.equals("metric")) {
            currentId = Integer.parseInt(attributes.getValue("id"));
        } else if (localName.equals("event")) {
            currentId = Integer.parseInt(attributes.getValue("id"));
        } else if (localName.equals("profile")) {
            handleProfile(attributes);
        } else if (localName.equals("interval_data")) {
            handleIntervalData(attributes);
            accumulator = new StringBuffer();
        }

    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        //System.out.println("endElement: uri:" + uri + ", localName:"+localName+", qName:"+qName);
        if (localName.equals("thread_definition")) {
            currentThread = null;
        } else if (localName.equals("name")) {
            currentName = accumulator.toString();
        } else if (localName.equals("value")) {
            currentValue = accumulator.toString();
        } else if (localName.equals("utc_date")) {
            try {
                currentDate = DataSource.dateTime.parse(accumulator.toString());
            } catch (java.text.ParseException e) {}
        } else if (localName.equals("timestamp")) {
            currentTimestamp = Long.parseLong(accumulator.toString());
        } else if (localName.equals("group")) {
            currentGroup = accumulator.toString();
        } else if (localName.equals("profile")) {
            currentSnapshot.setName(currentName);
            currentSnapshot.setTimestamp(currentTimestamp);
        } else if (localName.equals("metric")) {
            handleMetric(currentName);
        } else if (localName.equals("event")) {
            handleEvent(currentName, currentGroup);
        } else if (localName.equals("attribute")) {
            currentThread.thread.getMetaData().put(currentName, currentValue);
        } else if (localName.equals("interval_data")) {
            handleIntervalDataEnd();
        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        accumulator.append(ch, start, length);
    }
}
