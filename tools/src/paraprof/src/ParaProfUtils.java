package edu.uoregon.tau.paraprof;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import edu.uoregon.tau.common.ImageExport;
import edu.uoregon.tau.common.Utility;
import edu.uoregon.tau.common.VectorExport;
import edu.uoregon.tau.paraprof.enums.SortType;
import edu.uoregon.tau.paraprof.enums.ValueType;
import edu.uoregon.tau.paraprof.interfaces.ParaProfWindow;
import edu.uoregon.tau.paraprof.interfaces.SortListener;
import edu.uoregon.tau.paraprof.interfaces.UnitListener;
import edu.uoregon.tau.paraprof.script.ParaProfFunctionScript;
import edu.uoregon.tau.paraprof.script.ParaProfScript;
import edu.uoregon.tau.paraprof.script.ParaProfTrialScript;
import edu.uoregon.tau.paraprof.treetable.TreeTableWindow;
import edu.uoregon.tau.perfdmf.*;
import edu.uoregon.tau.perfdmf.Thread;
import edu.uoregon.tau.perfdmf.Thread;

/**
 * Utility class for ParaProf
 * 
 * <P>
 * CVS $Id: ParaProfUtils.java,v 1.23 2007/03/02 20:10:05 amorris Exp $
 * </P>
 * 
 * @author Alan Morris
 * @version $Revision: 1.23 $
 */
public class ParaProfUtils {

    static boolean verbose;
    static boolean verboseSet;

    // Suppress default constructor for noninstantiability
    private ParaProfUtils() {
    // This constructor will never be invoked
    }

    public static FunctionBarChartWindow createFunctionBarChartWindow(ParaProfTrial ppTrial, Function function, Component parent) {
        return new FunctionBarChartWindow(ppTrial, function, parent);
    }

    public static FunctionBarChartWindow createFunctionBarChartWindow(ParaProfTrial ppTrial, Thread thread, Function phase,
            Component parent) {
        return new FunctionBarChartWindow(ppTrial, thread, phase, parent);
    }

    public static LedgerWindow createLedgerWindow(ParaProfTrial ppTrial, int windowType) {
        return new LedgerWindow(ppTrial, windowType, null);
    }

    public static LedgerWindow createLedgerWindow(ParaProfTrial ppTrial, int windowType, Component parent) {
        return new LedgerWindow(ppTrial, windowType, parent);
    }

    private static void checkVerbose() {
        if (!verboseSet) {
            if (System.getProperty("paraprof.verbose") != null) {
                verbose = true;
            }
            verboseSet = true;
        }
    }

    public static void verr(String string) {
        checkVerbose();

        if (verbose) {
            System.err.println(string);
        }
    }

    public static void vout(String string) {
        checkVerbose();

        if (verbose) {
            System.out.println(string);
        }
    }

    public static void vout(Object obj, String string) {
        checkVerbose();

        if (verbose) {

            String className = obj.getClass().getName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }

            System.out.println(className + ": " + string);
        }
    }

    public static void verr(Object obj, String string) {
        checkVerbose();

        if (verbose) {

            String className = obj.getClass().getName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(lastDot + 1);
            }

            System.err.println(className + ": " + string);
        }
    }

    public static void helperAddRadioMenuItem(String name, String command, boolean on, ButtonGroup group, JMenu menu,
            ActionListener act) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, on);
        item.addActionListener(act);
        item.setActionCommand(command);
        group.add(item);
        menu.add(item);
    }

    public static void addCompItem(JFrame frame, Component c, GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        frame.getContentPane().add(c, gbc);
    }

    public static void addCompItem(Container jPanel, Component c, GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        jPanel.add(c, gbc);
    }

    public static void print(Printable printable) {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat defaultFormat = job.defaultPage();
        PageFormat selectedFormat = job.pageDialog(defaultFormat);
        if (defaultFormat != selectedFormat) { // only proceed if the user did not select cancel
            job.setPrintable(printable, selectedFormat);
            //if (job.getPrintService() != null) {
            if (job.printDialog()) { // only proceed if the user did not select cancel
                try {
                    job.print();
                } catch (PrinterException e) {
                    ParaProfUtils.handleException(e);
                }
            }
            //}
        }

    }

    public static JMenu createHelpMenu(final JFrame owner, final ParaProfWindow ppWindow) {

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {

                try {
                    Object EventSrc = evt.getSource();

                    if (EventSrc instanceof JMenuItem) {
                        String arg = evt.getActionCommand();

                        if (arg.equals("About ParaProf")) {
                            ImageIcon icon = Utility.getImageIconResource("tau-medium.png");
                            JOptionPane.showMessageDialog(owner, ParaProf.getInfoString(), "About ParaProf",
                                    JOptionPane.INFORMATION_MESSAGE, icon);
                        } else if (arg.equals("Show Help Window")) {
                            ppWindow.help(true);
                        }
                    }
                } catch (Exception e) {
                    ParaProfUtils.handleException(e);
                }
            }

        };

        JMenu helpMenu = new JMenu("Help");

        JMenuItem menuItem = new JMenuItem("Show Help Window");
        menuItem.addActionListener(actionListener);
        helpMenu.add(menuItem);

        menuItem = new JMenuItem("About ParaProf");
        menuItem.addActionListener(actionListener);
        helpMenu.add(menuItem);

        return helpMenu;
    }

    public static JMenu createFileMenu(final ParaProfWindow window, final Printable printable, final Object panel) {

        if (printable == null) {
            throw new ParaProfException("File menu created with null panel!");
        }

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {

                try {

                    String arg = evt.getActionCommand();

                    if (arg.equals("Print")) {
                        ParaProfUtils.print(printable);
                    } else if (arg.equals("Preferences...")) {
                        // this is just in case there is ever a ParaProfWindow that is not a JFrame (there shouldn't be)
                        ParaProf.preferencesWindow.showPreferencesWindow(window instanceof JFrame ? (JFrame) window : null);
                    } else if (arg.equals("Save as Bitmap Image")) {

                        if (panel instanceof ImageExport) {
                            ParaProfImageOutput.saveImage((ImageExport) panel);
                        } else if (panel instanceof ThreeDeeWindow) {
                            ThreeDeeWindow threeDeeWindow = (ThreeDeeWindow) panel;
                            ParaProfImageOutput.save3dImage(threeDeeWindow);
                        } else {
                            throw new ParaProfException("Don't know how to \"Save Image\" for " + panel.getClass());
                        }

                    } else if (arg.equals("Save as Vector Graphics")) {
                        if (panel instanceof ImageExport) {

                            if (JVMDependent.version.equals("1.4")) {
                                VectorExport.promptForVectorExport((ImageExport) panel, "ParaProf");
                            } else {
                                JOptionPane.showMessageDialog((Component) panel,
                                        "Can't save Vector Graphics with Java 1.3.  Reconfigure TAU with Java 1.4 or higher in the path.");
                            }
                        } else if (panel instanceof ThreeDeeWindow) {
                            JOptionPane.showMessageDialog((JFrame) window, "Can't save 3D visualization as vector graphics");
                        } else {
                            throw new ParaProfException("Don't know how to \"Save as Vector Graphics\" for " + panel.getClass());
                        }
                    } else if (arg.equals("Close This Window")) {
                        window.closeThisWindow();
                    } else if (arg.equals("Exit ParaProf!")) {
                        ParaProf.exitParaProf(0);
                    }
                } catch (Exception e) {
                    ParaProfUtils.handleException(e);
                }
            }

        };

        JMenu fileMenu = new JMenu("File");

        JMenu subMenu = new JMenu("Save ...");
        subMenu.getPopupMenu().setLightWeightPopupEnabled(false);

        JMenuItem menuItem = new JMenuItem("Save as Bitmap Image");
        menuItem.addActionListener(actionListener);
        subMenu.add(menuItem);
        menuItem = new JMenuItem("Save as Vector Graphics");
        menuItem.addActionListener(actionListener);
        subMenu.add(menuItem);
        fileMenu.add(subMenu);

        menuItem = new JMenuItem("Preferences...");
        menuItem.addActionListener(actionListener);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Print");
        menuItem.addActionListener(actionListener);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Close This Window");
        menuItem.addActionListener(actionListener);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Exit ParaProf!");
        menuItem.addActionListener(actionListener);
        fileMenu.add(menuItem);

        return fileMenu;
    }

    private static JMenuItem createMenuItem(String text, ActionListener actionListener, boolean enabled) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setEnabled(enabled);
        menuItem.addActionListener(actionListener);
        return menuItem;
    }

    public static JMenu createScriptMenu(final ParaProfTrial ppTrial, final JFrame owner) {

        final JMenu menu = new JMenu("PyScript");

        menu.addMenuListener(new MenuListener() {

            public void menuCanceled(MenuEvent e) {
            // TODO Auto-generated method stub

            }

            public void menuDeselected(MenuEvent e) {
            // TODO Auto-generated method stub

            }

            public void menuSelected(MenuEvent e) {
                menu.removeAll();
                JMenuItem menuitem = new JMenuItem("Reload scripts");
                menuitem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        ParaProf.loadScripts();
                    }
                });
                menu.add(menuitem);
                //menu.add
                for (int i = 0; i < ParaProf.scripts.size(); i++) {
                    final ParaProfScript pps = (ParaProfScript) ParaProf.scripts.get(i);
                    if (pps instanceof ParaProfTrialScript) {
                        JMenuItem menuItem = new JMenuItem("[Script] " + pps.getName());
                        menuItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    ((ParaProfTrialScript) pps).run(ppTrial);
                                } catch (Exception ex) {
                                    new ParaProfErrorDialog("Exception while executing script:", ex);
                                }
                            }
                        });
                        menu.add(menuItem);
                    }
                }

            }
        });

        return menu;
    }

    public static JMenu createWindowsMenu(final ParaProfTrial ppTrial, final JFrame owner) {

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {

                try {

                    String arg = evt.getActionCommand();

                    if (arg.equals("ParaProf Manager")) {
                        (new ParaProfManagerWindow()).setVisible(true);
                    } else if (arg.equals("Function Legend")) {
                        (new LedgerWindow(ppTrial, 0, owner)).setVisible(true);
                    } else if (arg.equals("Group Legend")) {
                        (new LedgerWindow(ppTrial, 1, owner)).setVisible(true);
                    } else if (arg.equals("User Event Legend")) {
                        (new LedgerWindow(ppTrial, 2, owner)).setVisible(true);
                    } else if (arg.equals("Phase Legend")) {
                        (new LedgerWindow(ppTrial, 3, owner)).setVisible(true);
                    } else if (arg.equals("3D Visualization")) {

                        if (JVMDependent.version.equals("1.3")) {
                            JOptionPane.showMessageDialog(owner, "3D Visualization requires Java 1.4 or above\n"
                                    + "Please make sure Java 1.4 is in your path, then reconfigure TAU and re-run ParaProf");
                            return;
                        }

                        //Gears.main(null);
                        //(new Gears()).show();

                        try {

                            (new ThreeDeeWindow(ppTrial, owner)).setVisible(true);
                            //(new ThreeDeeWindow()).show();
                        } catch (UnsatisfiedLinkError e) {
                            JOptionPane.showMessageDialog(owner, "Unable to load jogl library.  Possible reasons:\n"
                                    + "libjogl.so is not in your LD_LIBRARY_PATH.\n"
                                    + "Jogl is not built for this platform.\nOpenGL is not installed\n\n"
                                    + "Jogl is available at jogl.dev.java.net");
                        } catch (UnsupportedClassVersionError e) {
                            JOptionPane.showMessageDialog(owner,
                                    "Unsupported class version.  Are you sure you're using Java 1.4 or above?");
                        } catch (Exception gle) {
                            new ParaProfErrorDialog("Unable to initialize OpenGL: ", gle);
                        }

                    } else if (arg.equals("Close All Sub-Windows")) {
                        ppTrial.updateRegisteredObjects("subWindowCloseEvent");
                    }

                } catch (Exception e) {
                    ParaProfUtils.handleException(e);
                }
            }

        };

        JMenu windowsMenu = new JMenu("Windows");

        JMenuItem menuItem;

        menuItem = new JMenuItem("ParaProf Manager");
        menuItem.addActionListener(actionListener);
        windowsMenu.add(menuItem);

        windowsMenu.add(new JSeparator());

        menuItem = new JMenuItem("3D Visualization");
        menuItem.addActionListener(actionListener);
        windowsMenu.add(menuItem);

        //menuItem = new JMenuItem("Call Path Relations");
        //menuItem.addActionListener(actionListener);
        //windowsMenu.add(menuItem);

        windowsMenu.add(new JSeparator());

        ActionListener fActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                FunctionSelectorDialog fSelector = new FunctionSelectorDialog(owner, true,
                        ppTrial.getDataSource().getFunctions(), null, false, false);
                if (fSelector.choose()) {
                    Function selectedFunction = (Function) fSelector.getSelectedObject();

                    String arg = evt.getActionCommand();

                    if (arg.equals("Bar Chart")) {
                        FunctionBarChartWindow w = new FunctionBarChartWindow(ppTrial, selectedFunction, owner);
                        w.setVisible(true);
                    } else if (arg.equals("Histogram")) {
                        HistogramWindow w = new HistogramWindow(ppTrial, selectedFunction, owner);
                        w.setVisible(true);
                    }
                }
            }
        };

        final JMenu functionWindows = new JMenu("Function");
        functionWindows.getPopupMenu().setLightWeightPopupEnabled(false);

        functionWindows.add(createMenuItem("Bar Chart", fActionListener, true));
        functionWindows.add(createMenuItem("Histogram", fActionListener, true));
        windowsMenu.add(functionWindows);

        ActionListener tActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                String arg = evt.getActionCommand();

                List list = new ArrayList(ppTrial.getDataSource().getAllThreads());
                if (ppTrial.getDataSource().getAllThreads().size() > 1 && arg.equals("User Event Statistics") == false) {
                    list.add(0, ppTrial.getDataSource().getStdDevData());
                    list.add(1, ppTrial.getDataSource().getMeanData());
                }

                FunctionSelectorDialog fSelector = new FunctionSelectorDialog(owner, true, list.iterator(), null, false, false);
                fSelector.setTitle("Select a Thread");
                if (fSelector.choose()) {
                    Thread selectedThread = (Thread) fSelector.getSelectedObject();

                    if (arg.equals("Bar Chart")) {
                        FunctionBarChartWindow w = new FunctionBarChartWindow(ppTrial, selectedThread, null, owner);
                        w.setVisible(true);
                    } else if (arg.equals("Statistics Text")) {
                        (new StatWindow(ppTrial, selectedThread, false, null, owner)).setVisible(true);
                    } else if (arg.equals("Statistics Table")) {
                        (new TreeTableWindow(ppTrial, selectedThread, owner)).setVisible(true);
                    } else if (arg.equals("Call Graph")) {
                        (new CallGraphWindow(ppTrial, selectedThread, owner)).setVisible(true);
                    } else if (arg.equals("Call Path Relations")) {
                        (new CallPathTextWindow(ppTrial, selectedThread, owner)).setVisible(true);
                    } else if (arg.equals("User Event Bar Chart")) {
                        (new UserEventWindow(ppTrial, selectedThread, owner)).setVisible(true);
                    } else if (arg.equals("User Event Statistics")) {
                        (new StatWindow(ppTrial, selectedThread, true, null, owner)).setVisible(true);
                    }
                }
            }
        };

        final JMenu threadWindows = new JMenu("Thread");
        threadWindows.getPopupMenu().setLightWeightPopupEnabled(false);
        threadWindows.add(createMenuItem("Bar Chart", tActionListener, true));
        threadWindows.add(createMenuItem("Statistics Text", tActionListener, true));
        threadWindows.add(createMenuItem("Statistics Table", tActionListener, true));
        threadWindows.add(createMenuItem("Call Graph", tActionListener, ppTrial.callPathDataPresent()));
        threadWindows.add(createMenuItem("Call Path Relations", tActionListener, ppTrial.callPathDataPresent()));
        threadWindows.add(createMenuItem("User Event Bar Chart", tActionListener, ppTrial.userEventsPresent()));
        threadWindows.add(createMenuItem("User Event Statistics", tActionListener, ppTrial.userEventsPresent()));

        windowsMenu.add(threadWindows);

        windowsMenu.add(new JSeparator());

        menuItem = new JMenuItem("Function Legend");
        menuItem.addActionListener(actionListener);
        windowsMenu.add(menuItem);

        final JMenuItem groupLedger = new JMenuItem("Group Legend");
        groupLedger.addActionListener(actionListener);
        windowsMenu.add(groupLedger);

        final JMenuItem userEventLedger = new JMenuItem("User Event Legend");
        userEventLedger.addActionListener(actionListener);
        windowsMenu.add(userEventLedger);

        if (ppTrial.getDataSource().getPhasesPresent()) {
            final JMenuItem phaseLedger = new JMenuItem("Phase Legend");
            phaseLedger.addActionListener(actionListener);
            windowsMenu.add(phaseLedger);
        }

        windowsMenu.add(new JSeparator());

        menuItem = new JMenuItem("Close All Sub-Windows");
        menuItem.addActionListener(actionListener);
        windowsMenu.add(menuItem);

        MenuListener menuListener = new MenuListener() {
            public void menuSelected(MenuEvent evt) {
                try {
                    groupLedger.setEnabled(ppTrial.groupNamesPresent());
                    userEventLedger.setEnabled(ppTrial.userEventsPresent());
                } catch (Exception e) {
                    ParaProfUtils.handleException(e);
                }
            }

            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}
        };

        windowsMenu.addMenuListener(menuListener);

        return windowsMenu;
    }

    public static void scaleForPrint(Graphics g, PageFormat pageFormat, int width, int height) {
        double pageWidth = pageFormat.getImageableWidth();
        double pageHeight = pageFormat.getImageableHeight();
        int cols = (int) (width / pageWidth) + 1;
        int rows = (int) (height / pageHeight) + 1;
        double xScale = pageWidth / width;
        double yScale = pageHeight / height;
        double scale = Math.min(xScale, yScale);

        double tx = 0.0;
        double ty = 0.0;
        if (xScale > scale) {
            tx = 0.5 * (xScale - scale) * width;
        } else {
            ty = 0.5 * (yScale - scale) * height;
        }

        Graphics2D g2 = (Graphics2D) g;

        g2.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());
        g2.translate(tx, ty);
        g2.scale(scale, scale);
    }

    public FunctionBarChartWindow createFunctionAcrossPhasesWindow(ParaProfTrial ppTrial, Thread thread, Function function,
            Component owner) {
        FunctionBarChartWindow functionDataWindow = new FunctionBarChartWindow(ppTrial, function, owner);
        functionDataWindow.changeToPhaseDisplay(thread);
        return functionDataWindow;
    }

    public static JPopupMenu createFunctionClickPopUp(final ParaProfTrial ppTrial, final Function function, final Thread thread,
            final Component owner) {
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {

                    String arg = evt.getActionCommand();

                    if (arg.equals("Show Function Bar Chart")) {
                        FunctionBarChartWindow functionDataWindow = new FunctionBarChartWindow(ppTrial, function, owner);
                        functionDataWindow.setVisible(true);
                    } else if (arg.equals("Show Function Data over Phases")) {
                        FunctionBarChartWindow functionDataWindow = new FunctionBarChartWindow(ppTrial, function, owner);
                        functionDataWindow.changeToPhaseDisplay(thread);
                        functionDataWindow.setVisible(true);
                    } else if (arg.equals("Show Function Histogram")) {
                        HistogramWindow hw = new HistogramWindow(ppTrial, function, owner);
                        hw.setVisible(true);
                    } else if (arg.equals("Assign Function Color")) {
                        ParaProf.colorMap.assignColor(owner, function);
                    } else if (arg.equals("Reset to Default Color")) {
                        ParaProf.colorMap.removeColor(function);
                        ParaProf.colorMap.reassignColors();
                    } else if (arg.equals("Open Profile for this Phase")) {
                        GlobalDataWindow fdw = new GlobalDataWindow(ppTrial, function.getActualPhase());
                        fdw.setVisible(true);
                        ParaProf.incrementNumWindows();
                    } else if (arg.equals("Show Source Code")) {

                        if (ParaProf.insideEclipse) {
                            ParaProf.eclipseHandler.openSourceLocation(ppTrial, function);
                        } else {
                            // use internal viewer
                            ParaProf.getDirectoryManager().showSourceCode(function.getSourceLink());
                        }
                    }

                } catch (Exception e) {
                    ParaProfUtils.handleException(e);
                }
            }

        };

        JPopupMenu functionPopup = new JPopupMenu();

        //if (ParaProf.insideEclipse && function.getSourceLink().getFilename() != null) {
        if (function.getSourceLink().getFilename() != null) {
            JMenuItem functionDetailsItem = new JMenuItem("Show Source Code");
            functionDetailsItem.addActionListener(actionListener);
            functionPopup.add(functionDetailsItem);
        }

        if (function.isPhase()) {
            JMenuItem functionDetailsItem = new JMenuItem("Open Profile for this Phase");
            functionDetailsItem.addActionListener(actionListener);
            functionPopup.add(functionDetailsItem);
        }

        JMenuItem functionDetailsItem = new JMenuItem("Show Function Bar Chart");
        functionDetailsItem.addActionListener(actionListener);
        functionPopup.add(functionDetailsItem);

        JMenuItem functionHistogramItem = new JMenuItem("Show Function Histogram");
        functionHistogramItem.addActionListener(actionListener);
        functionPopup.add(functionHistogramItem);

        if (ppTrial.getDataSource().getPhasesPresent()) {
            JMenuItem jMenuItem = new JMenuItem("Show Function Data over Phases");
            jMenuItem.addActionListener(actionListener);
            functionPopup.add(jMenuItem);
        }

        JMenuItem jMenuItem = new JMenuItem("Assign Function Color");
        jMenuItem.addActionListener(actionListener);
        functionPopup.add(jMenuItem);

        jMenuItem = new JMenuItem("Reset to Default Color");
        jMenuItem.addActionListener(actionListener);
        functionPopup.add(jMenuItem);

        // count function scripts
        int functionScripts = 0;
        for (int i = 0; i < ParaProf.scripts.size(); i++) {
            ParaProfScript pps = (ParaProfScript) ParaProf.scripts.get(i);
            if (pps instanceof ParaProfFunctionScript) {
                functionScripts++;
            }
        }
        if (functionScripts > 1) {
            functionPopup.add(new JSeparator());
        }
        for (int i = 0; i < ParaProf.scripts.size(); i++) {
            final ParaProfScript pps = (ParaProfScript) ParaProf.scripts.get(i);
            if (pps instanceof ParaProfFunctionScript) {
                JMenuItem menuItem = new JMenuItem("[Script] " + pps.getName());
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ((ParaProfFunctionScript) pps).runFunction(ppTrial, function);
                        } catch (Exception ex) {
                            new ParaProfErrorDialog("Exception while executing script: ", ex);
                        }

                    }
                });
                functionPopup.add(menuItem);
            }
        }

        return functionPopup;

    }

    public static JMenuItem createStatisticsMenuItem(String text, final ParaProfTrial ppTrial, final Function phase,
            final Thread thread, final boolean userEvent, final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StatWindow statWindow = new StatWindow(ppTrial, thread, userEvent, phase, owner);
                statWindow.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static JMenuItem createStatisticsTableMenuItem(String text, final ParaProfTrial ppTrial, final Function phase,
            final Thread thread, final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TreeTableWindow ttWindow = new TreeTableWindow(ppTrial, thread, owner);
                ttWindow.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static JMenuItem createCallGraphMenuItem(String text, final ParaProfTrial ppTrial, final Thread thread,
            final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CallGraphWindow tmpRef = new CallGraphWindow(ppTrial, thread, owner);
                tmpRef.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static JMenuItem createCallPathThreadRelationMenuItem(String text, final ParaProfTrial ppTrial, final Thread thread,
            final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CallPathTextWindow callPathTextWindow = new CallPathTextWindow(ppTrial, thread, owner);
                callPathTextWindow.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static JMenuItem createThreadDataMenuItem(String text, final ParaProfTrial ppTrial, final Function phase,
            final Thread thread, final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FunctionBarChartWindow w = new FunctionBarChartWindow(ppTrial, thread, phase, owner);
                w.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static JMenuItem createComparisonMenuItem(String text, final ParaProfTrial ppTrial, final Thread thread,
            final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ParaProf.theComparisonWindow == null) {
                    ParaProf.theComparisonWindow = FunctionBarChartWindow.CreateComparisonWindow(ppTrial, thread, owner);
                } else {
                    ParaProf.theComparisonWindow.addThread(ppTrial, thread);
                }
                ParaProf.theComparisonWindow.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static JMenuItem createSnapShotMenuItem(String text, final ParaProfTrial ppTrial, final Thread thread,
            final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //SnapshotThreadWindow w = new SnapshotThreadWindow(ppTrial, thread, owner);
                Frame w = new SnapshotBreakdownWindow(ppTrial, thread, owner);
                w.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static JMenuItem createUserEventBarChartMenuItem(String text, final ParaProfTrial ppTrial, final Thread thread,
            final Component owner) {
        JMenuItem jMenuItem = new JMenuItem(text);
        jMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UserEventWindow w = new UserEventWindow(ppTrial, thread, owner);
                w.setVisible(true);
            }

        });
        return jMenuItem;
    }

    public static void handleUserEventClick(final ParaProfTrial ppTrial, final UserEvent userEvent, final JComponent owner,
            MouseEvent evt) {

        ActionListener act = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Object EventSrc = e.getSource();
                if (EventSrc instanceof JMenuItem) {
                    String arg = e.getActionCommand();
                    if (arg.equals("Show User Event Bar Chart")) {
                        UserEventWindow tmpRef = new UserEventWindow(ppTrial, userEvent, owner);
                        tmpRef.setVisible(true);
                    } else if (arg.equals("Change User Event Color")) {

                        Color tmpCol = userEvent.getColor();
                        tmpCol = JColorChooser.showDialog(owner, "Please select a new color", tmpCol);
                        if (tmpCol != null) {
                            userEvent.setSpecificColor(tmpCol);
                            userEvent.setColorFlag(true);
                            ppTrial.updateRegisteredObjects("colorEvent");
                        }
                    } else if (arg.equals("Reset to Generic Color")) {

                        userEvent.setColorFlag(false);
                        ppTrial.updateRegisteredObjects("colorEvent");
                    }
                }

            }
        };

        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        menuItem = new JMenuItem("Show User Event Bar Chart");
        menuItem.addActionListener(act);
        popup.add(menuItem);

        menuItem = new JMenuItem("Change User Event Color");
        menuItem.addActionListener(act);
        popup.add(menuItem);

        menuItem = new JMenuItem("Reset to Generic Color");
        menuItem.addActionListener(act);
        popup.add(menuItem);

        popup.show(owner, evt.getX(), evt.getY());

    }

    public static void handleThreadClick(final ParaProfTrial ppTrial, final Function phase, final Thread thread,
            JComponent owner, MouseEvent evt) {

        String ident;

        if (thread.getNodeID() == -1) {
            ident = "Mean";
        } else if (thread.getNodeID() == -2) {
            ident = "Total";
        } else if (thread.getNodeID() == -3) {
            ident = "Standard Deviation";
        } else {
            ident = "Thread";
        }

        JPopupMenu threadPopup = new JPopupMenu();
        threadPopup.add(createThreadDataMenuItem("Show " + ident + " Bar Chart", ppTrial, phase, thread, owner));
        threadPopup.add(createStatisticsMenuItem("Show " + ident + " Statistics Text Window", ppTrial, phase, thread, false,
                owner));
        threadPopup.add(createStatisticsTableMenuItem("Show " + ident + " Statistics Table", ppTrial, phase, thread, owner));
        threadPopup.add(createCallGraphMenuItem("Show " + ident + " Call Graph", ppTrial, thread, owner));
        threadPopup.add(createCallPathThreadRelationMenuItem("Show " + ident + " Call Path Relations", ppTrial, thread, owner));
        if (ppTrial.userEventsPresent()) {
            threadPopup.add(createUserEventBarChartMenuItem("Show User Event Bar Chart", ppTrial, thread, owner));
            threadPopup.add(createStatisticsMenuItem("Show User Event Statistics Window", ppTrial, null, thread, true, owner));
        }

        // snapshots are disabled for this release
        if (thread.getNumSnapshots() > 1) {
            threadPopup.add(createSnapShotMenuItem("Show Snapshots for " + ident, ppTrial, thread, owner));
        }

        threadPopup.add(createComparisonMenuItem("Add " + ident + " to Comparison Window", ppTrial, thread, owner));
        threadPopup.show(owner, evt.getX(), evt.getY());

    }

    public static void handleSnapshotClick(final ParaProfTrial ppTrial, final Thread thread, final Snapshot snapshot,
            JComponent owner, MouseEvent evt) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Hide this snapshot (" + snapshot + ")");
        popup.add(menuItem);

        menuItem = new JMenuItem("Show all snapshots");
        popup.add(menuItem);

        popup.show(owner, evt.getX(), evt.getY());

    }

    public static int[] computeClipping(Rectangle clipRect, Rectangle viewRect, boolean toScreen, boolean fullWindow, int size,
            int rowHeight, int yCoord) {

        int startElement, endElement;
        if (!fullWindow) {
            int yBeg = 0;
            int yEnd = 0;

            if (toScreen) {
                yBeg = (int) clipRect.getY();
                yEnd = (int) (yBeg + clipRect.getHeight());
            } else {
                yBeg = (int) viewRect.getY();
                yEnd = (int) (yBeg + viewRect.getHeight());
            }
            startElement = ((yBeg - yCoord) / rowHeight) - 1;
            endElement = ((yEnd - yCoord) / rowHeight) + 1;

            if (startElement < 0)
                startElement = 0;

            if (endElement < 0)
                endElement = 0;

            if (startElement > (size - 1))
                startElement = (size - 1);

            if (endElement > (size - 1))
                endElement = (size - 1);

            if (toScreen) {
                yCoord = yCoord + (startElement * rowHeight);
            }
        } else {
            startElement = 0;
            endElement = (size - 1);
        }

        if (startElement < 0) {
            startElement = 0;
        }

        int[] clips = new int[3];
        clips[0] = startElement;
        clips[1] = endElement;
        clips[2] = yCoord;
        return clips;
    }

    public static JMenu createUnitsMenu(final UnitListener unitListener, int initialUnits, boolean doHours) {

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    String arg = evt.getActionCommand();
                    if (arg.equals("Microseconds")) {
                        unitListener.setUnits(0);
                    } else if (arg.equals("Milliseconds")) {
                        unitListener.setUnits(1);
                    } else if (arg.equals("Seconds")) {
                        unitListener.setUnits(2);
                    } else if (arg.equals("hr:min:sec")) {
                        unitListener.setUnits(3);
                    }

                } catch (Exception e) {
                    ParaProfUtils.handleException(e);
                }
            }

        };

        JMenu unitsSubMenu = new JMenu("Select Units...");
        ButtonGroup group = new ButtonGroup();

        JRadioButtonMenuItem button = new JRadioButtonMenuItem("Microseconds", initialUnits == 0);
        button.addActionListener(actionListener);
        group.add(button);
        unitsSubMenu.add(button);

        button = new JRadioButtonMenuItem("Milliseconds", initialUnits == 1);
        button.addActionListener(actionListener);
        group.add(button);
        unitsSubMenu.add(button);

        button = new JRadioButtonMenuItem("Seconds", initialUnits == 2);
        button.addActionListener(actionListener);
        group.add(button);
        unitsSubMenu.add(button);

        button = new JRadioButtonMenuItem("hr:min:sec", initialUnits == 3);
        button.addActionListener(actionListener);
        group.add(button);
        unitsSubMenu.add(button);

        return unitsSubMenu;
    }

    public static void phaseConvertTrial(ParaProfTrial srcPpTrial, JFrame owner) {

        if (srcPpTrial.getDataSource().getCallPathDataPresent() == false) {
            JOptionPane.showMessageDialog(owner, "Can't phase convert non-callpath profiles");
            return;
        }

        FunctionSelectorDialog fSelector = new FunctionSelectorDialog(owner, true, srcPpTrial.getDataSource().getFunctions(),
                null, false, true);
        fSelector.setTitle("Choose Phases");

        if (fSelector.choose()) {
            List phases = fSelector.getSelectedObjects();

            List phaseStrings = new ArrayList();
            for (Iterator it = phases.iterator(); it.hasNext();) {
                Function f = (Function) it.next();
                phaseStrings.add(f.getName());
            }

            DataSource phaseDataSource = new PhaseConvertedDataSource(srcPpTrial.getDataSource(), phaseStrings);

            ParaProfApplication application = ParaProf.applicationManager.addApplication();
            application.setName("New Application");

            ParaProfExperiment experiment = application.addExperiment();
            experiment.setName("New Experiment");

            ParaProf.paraProfManagerWindow.expandApplicationType(0, application.getID(), application);
            ParaProf.paraProfManagerWindow.expandApplication(0, application.getID(), experiment.getID(), null, experiment);

            final ParaProfTrial ppTrial = new ParaProfTrial();
            ppTrial.getTrial().setDataSource(phaseDataSource);

            ppTrial.setExperiment(experiment);
            ppTrial.setApplicationID(experiment.getApplicationID());
            ppTrial.setExperimentID(experiment.getID());

            ppTrial.getTrial().setName("Phase Converted from " + srcPpTrial.getName());

            experiment.addTrial(ppTrial);
            ppTrial.finishLoad();
            ParaProf.paraProfManagerWindow.populateTrialMetrics(ppTrial);
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    ppTrial.showMainWindow();
                }
            });

        }

    }

    public static void exportTrial(ParaProfTrial ppTrial, Component owner) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Trial");
        //Set the directory.
        //fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        javax.swing.filechooser.FileFilter fileFilters[] = fileChooser.getChoosableFileFilters();

        for (int i = 0; i < fileFilters.length; i++) {
            fileChooser.removeChoosableFileFilter(fileFilters[i]);
        }
        fileChooser.addChoosableFileFilter(new ParaProfFileFilter(ParaProfFileFilter.TXT));
        fileChooser.addChoosableFileFilter(new ParaProfFileFilter(ParaProfFileFilter.PPK));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int resultValue = fileChooser.showSaveDialog(owner);
        if (resultValue != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {

            File file = fileChooser.getSelectedFile();
            String path = file.getCanonicalPath();

            String extension = ParaProfFileFilter.getExtension(file);
            if (extension == null) {
                javax.swing.filechooser.FileFilter fileFilter = fileChooser.getFileFilter();
                if (fileFilter instanceof ParaProfFileFilter) {
                    ParaProfFileFilter paraProfImageFormatFileFilter = (ParaProfFileFilter) fileFilter;
                    path = path + "." + paraProfImageFormatFileFilter.getExtension();
                }
                file = new File(path);
            }

            if (file.exists()) {
                int response = JOptionPane.showConfirmDialog(owner, file + " already exists\nOverwrite existing file?",
                        "Confirm Overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.CANCEL_OPTION)
                    return;
            }

            extension = ParaProfFileFilter.getExtension(file).toLowerCase();

            if (extension.compareTo("txt") == 0) {
                DataSourceExport.writeDelimited(ppTrial.getDataSource(), file);
            } else {
                DataSourceExport.writePacked(ppTrial.getDataSource(), file);
            }

        } catch (Exception e) {
            ParaProfUtils.handleException(e);
        }

    }

    public static boolean rightClick(MouseEvent evt) {
        if ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
            return true;
        }
        return false;
    }

    // remove the source code location, if preferences are set for it
    private static String removeSource(String str) {
        if (!ParaProf.preferences.getShowSourceLocation()) {
            while (str.indexOf("[{") != -1) {
                int a = str.indexOf("[{");
                int b = str.indexOf("}]");
                str = str.substring(0, a) + str.substring(b + 2);
            }
        }
        return str;
    }

    // handles reversed callpaths
    public static String getDisplayName(Function function) {
        if (ParaProf.preferences.getReversedCallPaths()) {
            return removeSource(function.getReversedName());
        } else {
            return removeSource(function.getName());
        }
    }

    public static String getThreadIdentifier(Thread thread) {

        if (thread.getNodeID() == -1) {
            return "Mean";
        } else if (thread.getNodeID() == -2) {
            return "Total";
        } else if (thread.getNodeID() == -3) {
            return "Std. Dev.";
        } else {
            return "n,c,t " + thread.getNodeID() + "," + thread.getContextID() + "," + thread.getThreadID();
        }

    }

    public static void logException(Exception e) {
        try {
            String file = ParaProf.paraProfHomeDirectory + "/ParaProf.errors";
            FileOutputStream out = new FileOutputStream(file, true);
            PrintStream p = new PrintStream(out);
            p.println("ParaProf Build (" + ParaProf.getVersionString() + ") encountered the following error on ("
                    + new java.util.Date() + ") : ");
            e.printStackTrace(p);
            p.println("");
            p.close();
            out.close();
        } catch (Exception ex) {
            // oh well...
        }
    }

    public static void handleException(Exception e) {
        logException(e);
        new ParaProfErrorDialog(null, e);
    }

    public static Dimension checkSize(Dimension d) {
        if (!ParaProf.demoMode) {
            return d;
        }

        int width = d.width;
        int height = d.height;

        width = Math.min(width, 640);
        height = Math.min(height, 480);
        return new Dimension(width, height);
    }

    public static NumberFormat createNumberFormatter(final int units) {
        return new NumberFormat() {

            public Number parse(String source, ParsePosition parsePosition) {
                // TODO Auto-generated method stub
                return null;
            }

            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(UtilFncs.getOutputString(units, number, 5));
            }

            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return toAppendTo.append(UtilFncs.getOutputString(units, number, 5));
            }
        };
    }

    public static void setFrameIcon(Frame frame) {
        URL url = Utility.getResource("tau16x16.gif");
        if (url != null) {
            frame.setIconImage(Toolkit.getDefaultToolkit().getImage(url));
        }
    }

    private static Component createMetricMenu(ParaProfTrial ppTrial, final ValueType valueType, boolean enabled,
            ButtonGroup group, final boolean sort, final DataSorter dataSorter, final SortListener sortListener) {
        JRadioButtonMenuItem button = null;

        if (ppTrial.getNumberOfMetrics() == 1) {
            button = new JRadioButtonMenuItem(valueType.toString(), enabled);

            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (sort) {
                        dataSorter.setSortByVisible(false);
                        dataSorter.setSortType(SortType.VALUE);
                        dataSorter.setSortValueType(valueType);
                    } else {
                        dataSorter.setValueType(valueType);
                    }
                    sortListener.resort();
                }
            });
            group.add(button);
            return button;
        } else {
            JMenu subSubMenu = new JMenu(valueType.toString() + "...");
            subSubMenu.getPopupMenu().setLightWeightPopupEnabled(false);
            for (int i = 0; i < ppTrial.getNumberOfMetrics(); i++) {

                if (i == dataSorter.getSelectedMetricID() && enabled) {
                    button = new JRadioButtonMenuItem(ppTrial.getMetric(i).getName(), true);
                } else {
                    button = new JRadioButtonMenuItem(ppTrial.getMetric(i).getName());
                }
                final int metricID = i;

                button.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {
                        if (sort) {
                            dataSorter.setSortByVisible(false);
                            dataSorter.setSortType(SortType.VALUE);
                            dataSorter.setSortMetric(metricID);
                            dataSorter.setSortValueType(valueType);
                        } else {
                            dataSorter.setSelectedMetricID(metricID);
                            dataSorter.setValueType(valueType);
                        }
                        sortListener.resort();
                    }
                });
                group.add(button);
                subSubMenu.add(button);
            }
            return subSubMenu;
        }
    }

    public static JMenu createMetricSelectionMenu(ParaProfTrial ppTrial, String name, final boolean sort, boolean nct,
            final DataSorter dataSorter, final SortListener sortListener, boolean visibleMetric) {
        JRadioButtonMenuItem button;
        JMenu subMenu = new JMenu(name);
        ButtonGroup group = new ButtonGroup();

        if (sort) {

            if (visibleMetric) {
                button = new JRadioButtonMenuItem("Same as Visible Metric", dataSorter.getSortByVisible());
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        dataSorter.setSortType(SortType.VALUE);
                        dataSorter.setSortByVisible(true);
                        sortListener.resort();
                    }
                });
                group.add(button);
                subMenu.add(button);
            }

            if (nct) {
                button = new JRadioButtonMenuItem("Sort By N,C,T", dataSorter.getSortType() == SortType.NCT);
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dataSorter.setSortType(SortType.NCT);
                        sortListener.resort();
                    }
                });
                group.add(button);
                subMenu.add(button);
            } else {
                button = new JRadioButtonMenuItem("Name", dataSorter.getSortType() == SortType.NAME);
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dataSorter.setSortType(SortType.NAME);
                        sortListener.resort();
                    }
                });
                group.add(button);
                subMenu.add(button);
            }

        }

        subMenu.add(createMetricMenu(ppTrial, ValueType.EXCLUSIVE, dataSorter.getValueType() == ValueType.EXCLUSIVE
                || dataSorter.getValueType() == ValueType.EXCLUSIVE_PERCENT, group, sort, dataSorter, sortListener));
        subMenu.add(createMetricMenu(ppTrial, ValueType.INCLUSIVE, dataSorter.getValueType() == ValueType.INCLUSIVE
                || dataSorter.getValueType() == ValueType.INCLUSIVE_PERCENT, group, sort, dataSorter, sortListener));
        subMenu.add(createMetricMenu(ppTrial, ValueType.EXCLUSIVE_PER_CALL,
                dataSorter.getValueType() == ValueType.EXCLUSIVE_PER_CALL, group, sort, dataSorter, sortListener));
        subMenu.add(createMetricMenu(ppTrial, ValueType.INCLUSIVE_PER_CALL,
                dataSorter.getValueType() == ValueType.INCLUSIVE_PER_CALL, group, sort, dataSorter, sortListener));

        button = new JRadioButtonMenuItem("Number of Calls", dataSorter.getValueType() == ValueType.NUMCALLS);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (sort) {
                    dataSorter.setSortByVisible(false);
                    dataSorter.setSortType(SortType.VALUE);
                    dataSorter.setSortValueType(ValueType.NUMCALLS);
                } else {
                    dataSorter.setValueType(ValueType.NUMCALLS);
                }
                sortListener.resort();
            }
        });
        group.add(button);
        subMenu.add(button);

        button = new JRadioButtonMenuItem("Number of Child Calls", dataSorter.getValueType() == ValueType.NUMSUBR);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (sort) {
                    dataSorter.setSortByVisible(false);
                    dataSorter.setSortType(SortType.VALUE);
                    dataSorter.setSortValueType(ValueType.NUMSUBR);
                } else {
                    dataSorter.setValueType(ValueType.NUMSUBR);
                }
                sortListener.resort();
            }
        });
        group.add(button);
        subMenu.add(button);
        return subMenu;
    }

}
