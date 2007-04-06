package edu.uoregon.tau.paraprof;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.*;
import java.util.List;

import javax.swing.*;

import edu.uoregon.tau.paraprof.enums.SortType;
import edu.uoregon.tau.paraprof.enums.ValueType;
import edu.uoregon.tau.paraprof.enums.VisType;
import edu.uoregon.tau.paraprof.interfaces.ParaProfWindow;
import edu.uoregon.tau.paraprof.interfaces.SortListener;
import edu.uoregon.tau.paraprof.interfaces.UnitListener;
import edu.uoregon.tau.perfdmf.*;
import edu.uoregon.tau.perfdmf.Thread;
import edu.uoregon.tau.vis.*;

public class ThreeDeeWindow extends JFrame implements ActionListener, KeyListener, Observer, Printable, ParaProfWindow,
        UnitListener, SortListener {

    private final int defaultToScatter = 15000;

    private VisCanvas visCanvas;
    private VisRenderer visRenderer = new VisRenderer();

    private Plot plot;
    private Axes axes;
    private ColorScale colorScale = new ColorScale();

    private ParaProfTrial ppTrial;
    private JMenu optionsMenu = null;
    private DataSorter dataSorter;

    private ThreeDeeControlPanel controlPanel;
    private ThreeDeeSettings settings = new ThreeDeeSettings();
    private ThreeDeeSettings oldSettings;
    private javax.swing.Timer fpsTimer;

    private JSplitPane jSplitPane;

    private TriangleMeshPlot triangleMeshPlot;
    private BarPlot barPlot;
    private ScatterPlot scatterPlot;
    private Axes fullDataPlotAxes;
    private Axes scatterPlotAxes;

    private List functionNames;
    private List threadNames;
    private List functions;
    private List threads;
    private List selectedFunctions = new ArrayList();

    private int units = ParaProf.preferences.getUnits();

    float maxHeightValue = 0;
    float maxColorValue = 0;

    float minScatterValues[];
    float maxScatterValues[];

    public ThreeDeeWindow(ParaProfTrial ppTrial, Component invoker) {
        // set the VisTools exception handler
        VisTools.setSwingExceptionHandler(new ExceptionHandler() {
            public void handleException(Exception e) {
                ParaProfUtils.handleException(e);
            }
        });
        this.ppTrial = ppTrial;

        settings.setColorMetricID(ppTrial.getDefaultMetricID());
        settings.setHeightMetricID(ppTrial.getDefaultMetricID());
        settings.setScatterMetricID(ppTrial.getDefaultMetricID(), 0);
        settings.setScatterMetricID(ppTrial.getDefaultMetricID(), 1);
        settings.setScatterMetricID(ppTrial.getDefaultMetricID(), 2);
        settings.setScatterMetricID(ppTrial.getDefaultMetricID(), 3);
        
        dataSorter = new DataSorter(ppTrial);
        dataSorter.setSortType(SortType.NAME);

        this.setTitle("TAU: ParaProf: 3D Visualizer: "
                + ppTrial.getTrialIdentifier(ParaProf.preferences.getShowPathTitleInReverse()));
        ParaProfUtils.setFrameIcon(this);

        //Add some window listener code
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                thisWindowClosing(evt);
            }
        });

        setupMenus();

        ThreeDeeWindow.this.validate();

        // IA64 workaround
        String os = System.getProperty("os.name").toLowerCase();
        String cpu = System.getProperty("os.arch").toLowerCase();
        if (os.startsWith("linux") && cpu.equals("ia64")) {
            this.setVisible(true);
        }

        DataSource dataSource = ppTrial.getDataSource();
        int numThreads = dataSource.getNumThreads();

        // initialize the scatterplot functions to the 4 most varying functions
        // we just get the first four stddev functions
        DataSorter dataSorter = new DataSorter(ppTrial);
        dataSorter.setSelectedMetricID(ppTrial.getDefaultMetricID());
        dataSorter.setDescendingOrder(true);
        List stdDevList = dataSorter.getFunctionProfiles(dataSource.getStdDevData());
        int count = 0;
        for (Iterator it = stdDevList.iterator(); it.hasNext() && count < 4;) {
            PPFunctionProfile fp = (PPFunctionProfile) it.next();
            if (!fp.isCallPathObject()) {
                settings.setScatterFunction(fp.getFunction(), count);
                count++;
            }
        }

        // if the number of threads is above this threshold, we default to the scatterplot
        if (numThreads > defaultToScatter) {
            settings.setVisType(VisType.SCATTER_PLOT);
        }

        generate3dModel(true, settings);

        oldSettings = (ThreeDeeSettings) settings.clone();

        visRenderer.addShape(plot);
        visRenderer.addShape(colorScale);
        visCanvas = new VisCanvas(visRenderer);

        visCanvas.addKeyListener(this);

        JPanel panel = new JPanel() {
            public Dimension getMinimumSize() {
                return new Dimension(10, 10);
            }
        };

        panel.addKeyListener(this);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        panel.add(visCanvas.getActualCanvas(), gbc);
        panel.setPreferredSize(new Dimension(5, 5));

        controlPanel = new ThreeDeeControlPanel(this, settings, ppTrial, visRenderer);
        jSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, controlPanel);
        jSplitPane.setContinuousLayout(true);
        jSplitPane.setResizeWeight(1.0);
        jSplitPane.setOneTouchExpandable(true);
        jSplitPane.addKeyListener(this);
        this.getContentPane().add(jSplitPane);

        setSize(ParaProfUtils.checkSize(new Dimension(1000, 750)));
        this.setLocation(WindowPlacer.getNewLocation(this, invoker));

        //        //Grab the screen size.
        //        Toolkit tk = Toolkit.getDefaultToolkit();
        //        Dimension screenDimension = tk.getScreenSize();
        //        int screenHeight = screenDimension.height;
        //        int screenWidth = screenDimension.width;

        // IA64 workaround
        if (os.startsWith("linux") && cpu.equals("ia64")) {
            this.validate();
        }
        this.setVisible(true);

        if (System.getProperty("vis.fps") != null) {
            fpsTimer = new javax.swing.Timer(1000, this);
            fpsTimer.start();
        }

        ParaProf.incrementNumWindows();
        ppTrial.addObserver(this);

    }

    private void generateScatterPlot(boolean autoSize, ThreeDeeSettings settings) {

        Function[] scatterFunctions = settings.getScatterFunctions();

        ValueType[] scatterValueTypes = settings.getScatterValueTypes();
        int[] scatterMetricIDs = settings.getScatterMetricIDs();

        DataSource dataSource = ppTrial.getDataSource();
        int numThreads = dataSource.getNumThreads();

        float[][] values = new float[numThreads][4];

        int threadIndex = 0;

        minScatterValues = new float[4];
        maxScatterValues = new float[4];
        for (int f = 0; f < 4; f++) {
            minScatterValues[f] = Float.MAX_VALUE;
        }

        for (Iterator it = ppTrial.getDataSource().getAllThreads().iterator(); it.hasNext();) {
            Thread thread = (Thread) it.next();

            for (int f = 0; f < scatterFunctions.length; f++) {
                if (scatterFunctions[f] != null) {
                    FunctionProfile functionProfile = thread.getFunctionProfile(scatterFunctions[f]);

                    if (functionProfile != null) {
                        values[threadIndex][f] = (float) scatterValueTypes[f].getValue(functionProfile, scatterMetricIDs[f]);
                        maxScatterValues[f] = Math.max(maxScatterValues[f], values[threadIndex][f]);
                        minScatterValues[f] = Math.min(minScatterValues[f], values[threadIndex][f]);
                    }
                }
            }
            threadIndex++;
        }

        if (scatterPlotAxes == null) {
            scatterPlotAxes = new Axes();
        }

        setAxisStrings();

        axes = scatterPlotAxes;

        if (scatterPlot == null) {
            scatterPlot = new ScatterPlot();
            if (numThreads > defaultToScatter) {
                scatterPlot.setSphereSize(0);
            }
        }

        scatterPlot.setSize(15, 15, 15);
        scatterPlot.setAxes(axes);
        scatterPlot.setValues(values);
        scatterPlot.setColorScale(colorScale);
        plot = scatterPlot;
    }

    private void generate3dModel(boolean autoSize, ThreeDeeSettings settings) {

        if (plot != null) {
            plot.cleanUp();
        }

        if (settings.getVisType() == VisType.SCATTER_PLOT) {
            generateScatterPlot(autoSize, settings);
            return;
        }

        if (triangleMeshPlot == null && barPlot == null) {
            autoSize = true;
        }

        DataSource dataSource = ppTrial.getDataSource();

        int numThreads = dataSource.getNumThreads();
        int numFunctions = 0;

        // get the 'mean' thread's functions to sort by
        List list = dataSorter.getFunctionProfiles(ppTrial.getDataSource().getMeanData());

        numFunctions = list.size();

        float[][] heightValues = new float[numFunctions][numThreads];
        float[][] colorValues = new float[numFunctions][numThreads];

        boolean addFunctionNames = false;
        if (functionNames == null) {
            functionNames = new ArrayList();
            functions = new ArrayList();
            addFunctionNames = true;
        }

        if (threadNames == null) {
            threadNames = new ArrayList();
            threads = new ArrayList();

            for (Iterator it = ppTrial.getDataSource().getAllThreads().iterator(); it.hasNext();) {
                Thread thread = (Thread) it.next();
                threadNames.add(thread.getNodeID() + ":" + thread.getContextID() + ":" + thread.getThreadID());
                threads.add(thread);
            }
        }

        maxHeightValue = 0;
        maxColorValue = 0;

        int funcIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            PPFunctionProfile ppFunctionProfile = (PPFunctionProfile) list.get(i);
            Function function = ppFunctionProfile.getFunction();

            //for (Iterator funcIter = ppTrial.getDataSource().getFunctions(); funcIter.hasNext();) {
            //  Function function = (Function) funcIter.next();

            //            if (!ppTrial.displayFunction(function)) {
            //                continue;
            //            }

            if (addFunctionNames) {
                functionNames.add(ParaProfUtils.getDisplayName(function));
                functions.add(function);
            }
            int threadIndex = 0;
            for (Iterator it = ppTrial.getDataSource().getAllThreads().iterator(); it.hasNext();) {
                Thread thread = (Thread) it.next();
                FunctionProfile functionProfile = thread.getFunctionProfile(function);

                if (functionProfile != null) {
                    heightValues[funcIndex][threadIndex] = (float) settings.getHeightValue().getValue(functionProfile,
                            settings.getHeightMetricID());
                    colorValues[funcIndex][threadIndex] = (float) settings.getColorValue().getValue(functionProfile,
                            settings.getColorMetricID());

                    maxHeightValue = Math.max(maxHeightValue, heightValues[funcIndex][threadIndex]);
                    maxColorValue = Math.max(maxColorValue, colorValues[funcIndex][threadIndex]);
                }
                threadIndex++;
            }
            funcIndex++;
        }

        if (autoSize) {
            int plotWidth = 20;
            int plotDepth = 20;
            int plotHeight = 20;

            float ratio = (float) threadNames.size() / functionNames.size();

            if (ratio > 2)
                ratio = 2;
            if (ratio < 0.5f)
                ratio = 0.5f;

            if (ratio > 1.0f) {
                plotDepth = (int) (30 * (1 / ratio));
                plotWidth = 30;

            } else if (ratio < 1.0f) {
                plotDepth = 30;
                plotWidth = (int) (30 * (ratio));

            } else {
                plotWidth = 30;
                plotDepth = 30;
            }
            plotHeight = 6;

            settings.setSize(plotWidth, plotDepth, plotHeight);
            visRenderer.setAim(new Vec(settings.getPlotWidth() / 2, settings.getPlotDepth() / 2, 0));
            settings.setRegularAim(visRenderer.getAim());
        }

        if (fullDataPlotAxes == null) {
            fullDataPlotAxes = new Axes();
            fullDataPlotAxes.setHighlightColor(ppTrial.getColorChooser().getHighlightColor());
        }

        setAxisStrings();

        axes = fullDataPlotAxes;

        //axes.setOrientation(settings.getAxisOrientation());

        if (settings.getVisType() == VisType.TRIANGLE_MESH_PLOT) {
            axes.setOnEdge(false);
            if (triangleMeshPlot == null) {
                triangleMeshPlot = new TriangleMeshPlot();
                triangleMeshPlot.initialize(axes, settings.getPlotWidth(), settings.getPlotDepth(), settings.getPlotHeight(),
                        heightValues, colorValues, colorScale);
                plot = triangleMeshPlot;
            } else {
                triangleMeshPlot.setValues(settings.getPlotWidth(), settings.getPlotDepth(), settings.getPlotHeight(),
                        heightValues, colorValues);
                plot = triangleMeshPlot;
            }
        } else {
            axes.setOnEdge(true);

            if (barPlot == null) {

                barPlot = new BarPlot();
                barPlot.initialize(axes, settings.getPlotWidth(), settings.getPlotDepth(), settings.getPlotHeight(),
                        heightValues, colorValues, colorScale);
                plot = barPlot;
            } else {
                barPlot.setValues(settings.getPlotWidth(), settings.getPlotDepth(), settings.getPlotHeight(), heightValues,
                        colorValues);
                plot = barPlot;
            }
        }
    }

    private void updateSettings(ThreeDeeSettings newSettings) {

        if (oldSettings.getAxisOrientation() != newSettings.getAxisOrientation()) {
            axes.setOrientation(newSettings.getAxisOrientation());
        }

        if (oldSettings.getVisType() != newSettings.getVisType()) {
            // I know this is the same as the thing below, but that will probably change, I want this separate for now
            visRenderer.removeShape(plot);
            visRenderer.removeShape(colorScale);
            generate3dModel(false, newSettings);
            visRenderer.addShape(plot);
            visRenderer.addShape(colorScale);

            plot.setSelectedCol(newSettings.getSelections()[1]);
            plot.setSelectedRow(newSettings.getSelections()[0]);

            if (newSettings.getVisType() == VisType.SCATTER_PLOT) {
                visRenderer.setAim(settings.getScatterAim());
            } else if (newSettings.getVisType() == VisType.TRIANGLE_MESH_PLOT || newSettings.getVisType() == VisType.BAR_PLOT) {
                visRenderer.setAim(settings.getRegularAim());
            }

        } else {

            if (newSettings.getVisType() == VisType.SCATTER_PLOT) {
                visRenderer.removeShape(plot);
                visRenderer.removeShape(colorScale);
                generate3dModel(false, newSettings);
                visRenderer.addShape(plot);
                visRenderer.addShape(colorScale);
            } else if (newSettings.getVisType() == VisType.TRIANGLE_MESH_PLOT || newSettings.getVisType() == VisType.BAR_PLOT) {

                settings.setSize((int) plot.getWidth(), (int) plot.getDepth(), (int) plot.getHeight());

                if (oldSettings.getHeightMetricID() != newSettings.getHeightMetricID()
                        || oldSettings.getHeightValue() != newSettings.getHeightValue()
                        || oldSettings.getColorValue() != newSettings.getColorValue()
                        || oldSettings.getColorMetricID() != newSettings.getColorMetricID()) {
                    generate3dModel(false, newSettings);
                } else {

                    //                    plot.setSize(newSettings.getPlotWidth(), newSettings.getPlotDepth(),
                    //                            newSettings.getPlotHeight());
                    //                    axes.setSize(newSettings.getPlotWidth(), newSettings.getPlotDepth(),
                    //                            newSettings.getPlotHeight());

                    plot.setSelectedCol(newSettings.getSelections()[1]);
                    plot.setSelectedRow(newSettings.getSelections()[0]);
                }
            }
        }

        oldSettings = (ThreeDeeSettings) newSettings.clone();
    }

    public void redraw() {
        jSplitPane.revalidate();
        jSplitPane.validate();
        updateSettings(settings);
        visRenderer.redraw();
    }
    
    public void resetSplitPane() {
        // We try to get the JSplitPane to reset the divider since the 
        // different plots have differing widths of controls 
        jSplitPane.revalidate();
        jSplitPane.validate();
        jSplitPane.resetToPreferredSizes();
        updateSettings(settings);
        visRenderer.redraw();
    }

    private void helperAddRadioMenuItem(String name, String command, boolean on, ButtonGroup group, JMenu menu) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, on);
        item.addActionListener(this);
        item.setActionCommand(command);
        group.add(item);
        menu.add(item);
    }

    private void setupMenus() {

        JMenuBar mainMenu = new JMenuBar();

        optionsMenu = new JMenu("Options");
        optionsMenu.getPopupMenu().setLightWeightPopupEnabled(false);

        JMenu unitsSubMenu = ParaProfUtils.createUnitsMenu(this, units, true);
        unitsSubMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        optionsMenu.add(unitsSubMenu);

        JMenu sort = ParaProfUtils.createMetricSelectionMenu(ppTrial, "Sort by...", true, false, dataSorter, this, false);
        sort.getPopupMenu().setLightWeightPopupEnabled(false);
        optionsMenu.add(sort);

        // now add all the menus to the main menu

        JMenu fileMenu = ParaProfUtils.createFileMenu(this, this, this);
        //JMenu trialMenu = ParaProfUtils.createTrialMenu(ppTrial, this);
        JMenu windowsMenu = ParaProfUtils.createWindowsMenu(ppTrial, this);
        JMenu helpMenu = ParaProfUtils.createHelpMenu(this, this);

        fileMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        //trialMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        windowsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        helpMenu.getPopupMenu().setLightWeightPopupEnabled(false);

        mainMenu.add(fileMenu);
        mainMenu.add(optionsMenu);
        //mainMenu.add(trialMenu);
        mainMenu.add(windowsMenu);
        mainMenu.add(helpMenu);

        setJMenuBar(mainMenu);
    }

    public int getUnits() {
        return units;
    }

    public void update(Observable o, Object arg) {
        String tmpString = (String) arg;

        if (tmpString.equals("subWindowCloseEvent")) {
            closeThisWindow();
        } else if (tmpString.equals("prefEvent")) {

        } else if (tmpString.equals("colorEvent")) {

            if (fullDataPlotAxes != null) {
                fullDataPlotAxes.setHighlightColor(ppTrial.getColorChooser().getHighlightColor());
                visRenderer.redraw();
            }
            //
            //            for (Iterator funcIter = ppTrial.getDataSource().getFunctions(); funcIter.hasNext();) {
            //                Function function = (Function) funcIter.next();
            //                if (function == ppTrial.getHighlightedFunction()) {
            //                    int index = functions.indexOf(function);
            //                    
            //                }
            //            }

        } else if (tmpString.equals("dataEvent")) {
            sortLocalData();
        }

    }

    void thisWindowClosing(java.awt.event.WindowEvent e) {
        closeThisWindow();
    }

    public void closeThisWindow() {
        setVisible(false);
        ppTrial.deleteObserver(this);
        ParaProf.decrementNumWindows();

        if (plot != null) {
            plot.cleanUp();
        }

        if (visRenderer != null) {
            visRenderer.cleanUp();
        }
        //animator.stop();
        //jTimer.stop();
        //jTimer = null;
        visRenderer = null;
        plot = null;

        dispose();
        //        System.gc();
    }

    private void sortLocalData() {
        functionNames = null;

        if (settings.getVisType() == VisType.BAR_PLOT || settings.getVisType() == VisType.TRIANGLE_MESH_PLOT) {
            settings.setSize((int) plot.getWidth(), (int) plot.getDepth(), (int) plot.getHeight());
            settings.setRegularAim(visRenderer.getAim());
            settings.setRegularEye(visRenderer.getEye());
        } else if (settings.getVisType() == VisType.SCATTER_PLOT) {
            //                settings.setSize((int) plot.getWidth(), (int) plot.getDepth(), (int) plot.getHeight());
            settings.setScatterAim(visRenderer.getAim());
            settings.setScatterEye(visRenderer.getEye());
        }

        generate3dModel(false, settings);
        visRenderer.redraw();
        controlPanel.dataChanged();
    }

    public void help(boolean display) {
        //Show the ParaProf help window.
        ParaProf.getHelpWindow().clearText();
        if (display)
            ParaProf.getHelpWindow().show();
        ParaProf.getHelpWindow().writeText("This is the 3D Window");
        ParaProf.getHelpWindow().writeText("");
        ParaProf.getHelpWindow().writeText(
                "This window displays profile data in three dimensions through the Triangle Mesh Plot, the Bar Plot, and the ScatterPlot");
        ParaProf.getHelpWindow().writeText("");
        ParaProf.getHelpWindow().writeText(
                "Change between the plots by selecting the desired type from the radio buttons in the upper right.");
        ParaProf.getHelpWindow().writeText("");
        ParaProf.getHelpWindow().writeText("Experiment with the controls at the right.");
        ParaProf.getHelpWindow().writeText("");
    }

    private long lastCall = 0;

    public BufferedImage getImage() {
        return visRenderer.createScreenShot();
    }

    public int print(Graphics g, PageFormat pageFormat, int page) {
        try {
            if (page >= 1) {
                return NO_SUCH_PAGE;
            }

            ParaProfUtils.scaleForPrint(g, pageFormat, visCanvas.getWidth(), visCanvas.getHeight());

            BufferedImage screenShot = visRenderer.createScreenShot();

            ImageObserver imageObserver = new ImageObserver() {
                public boolean imageUpdate(Image image, int a, int b, int c, int d, int e) {
                    return false;
                }
            };

            g.drawImage(screenShot, 0, 0, Color.black, imageObserver);

            //            renderIt((Graphics2D) g, false, true, false);

            return Printable.PAGE_EXISTS;
        } catch (Exception e) {
            ParaProfUtils.handleException(e);
            return NO_SUCH_PAGE;
        }
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            Object EventSrc = evt.getSource();

            if (EventSrc instanceof javax.swing.Timer) {
                // the timer has ticked, get progress and post
                if (visRenderer == null) { // if it's been closed, go away
                    ((javax.swing.Timer) EventSrc).stop();
                    return;
                }

                long time = System.currentTimeMillis();

                int numFrames = visRenderer.getFramesRendered();
                if (numFrames != 0) {
                    visRenderer.setFramesRendered(0);

                    float fps = numFrames / ((time - lastCall) / (float) 1000);

                    visRenderer.setFps(fps);

                    System.out.println("FPS = " + fps);
                    lastCall = time;
                }
                return;
            }

        } catch (Exception e) {
            ParaProfUtils.handleException(e);
        }
    }

    /**
     * @return Returns the colorScale.
     */
    public ColorScale getColorScale() {
        return colorScale;
    }

    /**
     * @return Returns the plot.
     */
    public Plot getPlot() {
        return plot;
    }

    /**
     * @param plot The plot to set.
     */
    public void setPlot(Plot plot) {
        this.plot = plot;
    }

    public List getFunctionNames() {
        return functionNames;
    }

    public List getThreadNames() {
        return threadNames;
    }

    public String getFunctionName(int index) {
        if (functionNames == null)
            return null;
        return (String) functionNames.get(index);
    }

    public String getThreadName(int index) {
        if (threadNames == null)
            return null;
        return (String) threadNames.get(index);
    }

    public String getSelectedHeightValue() {
        if (threads == null || functionNames == null)
            return "";

        if (settings.getSelections()[1] < 0 || settings.getSelections()[0] < 0)
            return "";

        Thread thread = (Thread) threads.get(settings.getSelections()[1]);

        Function function = (Function) functions.get(settings.getSelections()[0]);

        FunctionProfile fp = thread.getFunctionProfile(function);

        if (fp == null) {
            return "no value";
        }

        int units = this.units;
        ParaProfMetric ppMetric = ppTrial.getMetric(settings.getHeightMetricID());
        if (!ppMetric.isTimeMetric() || !ValueType.isTimeUnits(settings.getHeightValue())) {
            units = 0;
        }

        return UtilFncs.getOutputString(units, settings.getHeightValue().getValue(fp, settings.getHeightMetricID()), 6).trim()
                + getUnitsString(units, settings.getHeightValue(), ppMetric);

        //Double.toString(settings.getHeightValue().getValue(fp, settings.getHeightMetricID()));

    }

    public String getSelectedColorValue() {
        if (threads == null || functionNames == null)
            return "";

        if (settings.getSelections()[1] < 0 || settings.getSelections()[0] < 0)
            return "";

        Thread thread = (Thread) threads.get(settings.getSelections()[1]);

        Function function = (Function) functions.get(settings.getSelections()[0]);
        FunctionProfile fp = thread.getFunctionProfile(function);

        if (fp == null) {
            return "no value";
        }

        int units = this.units;
        ParaProfMetric ppMetric = ppTrial.getMetric(settings.getColorMetricID());
        if (!ppMetric.isTimeMetric() || !ValueType.isTimeUnits(settings.getColorValue())) {
            units = 0;
        }

        return UtilFncs.getOutputString(units, settings.getColorValue().getValue(fp, settings.getColorMetricID()), 6).trim()
                + getUnitsString(units, settings.getColorValue(), ppMetric);

        //return Double.toString(settings.getColorValue().getValue(fp, settings.getColorMetricID()));

    }

    private String getUnitsString(int units, ValueType valueType, ParaProfMetric ppMetric) {
        return valueType.getSuffix(units, ppMetric);
    }

    private void setAxisStrings() {

        if (settings.getVisType() == VisType.SCATTER_PLOT) {

            Function[] scatterFunctions = settings.getScatterFunctions();
            ValueType[] scatterValueTypes = settings.getScatterValueTypes();
            int[] scatterMetricIDs = settings.getScatterMetricIDs();

            List axisNames = new ArrayList();
            for (int f = 0; f < scatterFunctions.length; f++) {
                if (scatterFunctions[f] != null) {
                    // e.g. "MPI_Recv()\n(Exclusive, Time)"
                    if (scatterValueTypes[f] == ValueType.NUMCALLS || scatterValueTypes[f] == ValueType.NUMSUBR) {
                        axisNames.add(ParaProfUtils.getDisplayName(scatterFunctions[f]) + "\n(" + scatterValueTypes[f].toString()
                                + ")");
                    } else {
                        axisNames.add(ParaProfUtils.getDisplayName(scatterFunctions[f]) + "\n(" + scatterValueTypes[f].toString()
                                + ", " + ppTrial.getMetricName(scatterMetricIDs[f]) + ")");
                    }
                } else {
                    axisNames.add("none");
                }
            }

            List[] axisStrings = new ArrayList[4];

            for (int i = 0; i < 4; i++) {
                if (minScatterValues[i] == Float.MAX_VALUE) {
                    minScatterValues[i] = 0;
                }

                ParaProfMetric ppMetric = ppTrial.getMetric(scatterMetricIDs[i]);

                int units = scatterValueTypes[i].getUnits(this.units, ppMetric);

                axisStrings[i] = new ArrayList();
                axisStrings[i].add(UtilFncs.getOutputString(units, minScatterValues[i], 6).trim());
                axisStrings[i].add(UtilFncs.getOutputString(units,
                        minScatterValues[i] + (maxScatterValues[i] - minScatterValues[i]) * .25, 6).trim());
                axisStrings[i].add(UtilFncs.getOutputString(units,
                        minScatterValues[i] + (maxScatterValues[i] - minScatterValues[i]) * .50, 6).trim());
                axisStrings[i].add(UtilFncs.getOutputString(units,
                        minScatterValues[i] + (maxScatterValues[i] - minScatterValues[i]) * .75, 6).trim());
                axisStrings[i].add(UtilFncs.getOutputString(units,
                        minScatterValues[i] + (maxScatterValues[i] - minScatterValues[i]), 6).trim());
            }

            ParaProfMetric ppMetric = ppTrial.getMetric(scatterMetricIDs[3]);
            int units = scatterValueTypes[3].getUnits(this.units, ppMetric);

            colorScale.setStrings(UtilFncs.getOutputString(units, minScatterValues[3], 6).trim(), UtilFncs.getOutputString(units,
                    maxScatterValues[3], 6).trim(), (String) axisNames.get(3));

            scatterPlotAxes.setStrings((String) axisNames.get(0), (String) axisNames.get(1), (String) axisNames.get(2),
                    axisStrings[0], axisStrings[1], axisStrings[2]);

        } else {

            List zStrings = new ArrayList();
            zStrings.add("0");

            int units;

            ParaProfMetric ppMetric = ppTrial.getMetric(settings.getHeightMetricID());
            units = settings.getHeightValue().getUnits(this.units, ppMetric);

            zStrings.add(UtilFncs.getOutputString(units, maxHeightValue * 0.25, 6).trim());
            zStrings.add(UtilFncs.getOutputString(units, maxHeightValue * 0.50, 6).trim());
            zStrings.add(UtilFncs.getOutputString(units, maxHeightValue * 0.75, 6).trim());
            zStrings.add(UtilFncs.getOutputString(units, maxHeightValue, 6).trim());

            String zAxisLabel = settings.getHeightValue().getSuffix(units, ppMetric);

            ppMetric = ppTrial.getMetric(settings.getColorMetricID());
            units = settings.getColorValue().getUnits(this.units, ppMetric);

            String colorAxisLabel = settings.getColorValue().getSuffix(units, ppMetric);

            colorScale.setStrings("0", UtilFncs.getOutputString(units, maxColorValue, 6).trim(), colorAxisLabel);

            //String zAxisLabel = settings.getHeightValue().toString() + ", " + ppTrial.getMetricName(settings.getHeightMetricID());
            //String zAxisLabel = "";

            fullDataPlotAxes.setStrings("Threads", "Functions", zAxisLabel, threadNames, functionNames, zStrings);
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        try {
            // zoom in and out on +/-
            if (e.getKeyChar() == '+') {
                visRenderer.zoomIn();
            } else if (e.getKeyChar() == '-') {
                visRenderer.zoomOut();
            }
        } catch (Exception exp) {
            ParaProfUtils.handleException(exp);
        }

    }

    public void setUnits(int units) {
        this.units = units;
        setAxisStrings();
        controlPanel.dataChanged();
        visRenderer.redraw();
    }

    public void resort() {
        sortLocalData();
    }

}
