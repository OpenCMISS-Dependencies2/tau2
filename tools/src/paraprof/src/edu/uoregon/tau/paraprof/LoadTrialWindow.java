/*
 * LoadTrialWindow.java
 * 
 * Title: ParaProf 
 * Author: Robert Bell 
 * Description:
 */

package edu.uoregon.tau.paraprof;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import edu.uoregon.tau.dms.dss.*;

public class LoadTrialWindow extends JFrame implements ActionListener {

    static String lastDirectory;

    private ParaProfManagerWindow paraProfManagerWindow = null;
    private ParaProfApplication application = null;
    private ParaProfExperiment experiment = null;
    private boolean newExperiment;
    private boolean newApplication;
    
    private JTextField dirLocationField = new JTextField(lastDirectory, 30);
    //0:pprof, 1:profile, 2:dynaprof, 3:mpip, 4:hpmtoolkit, 5:gprof, 6:psrun.
    //String trialTypeStrings[] = {"pprof", "tau profiles", "dynaprof", "mpiP",
    // "hpmtoolkit", "gprof", "psrun"};
    private String trialTypeStrings[] = { "Tau profiles", "Tau pprof.dat", "Dynaprof", "MpiP", "HPMToolkit",
            "Gprof", "PSRun", "ParaProf Packed Profile", "Cube" };
    private JComboBox trialTypes = null;
    private File selectedFiles[];
    private JButton selectButton = null;

    public LoadTrialWindow(ParaProfManagerWindow paraProfManager, ParaProfApplication application,
            ParaProfExperiment experiment, boolean newApplication, boolean newExperiment) {
        this.paraProfManagerWindow = paraProfManager;
        this.application = application;
        this.experiment = experiment;
        this.newApplication = newApplication;
        this.newExperiment = newExperiment;

        if (lastDirectory == null) {
            lastDirectory = System.getProperty("user.dir");
            dirLocationField.setText(lastDirectory);
        }

        //Window Stuff.
        int windowWidth = 400;
        int windowHeight = 200;

        //Grab paraProfManager position and size.
        Point parentPosition = paraProfManager.getLocationOnScreen();
        Dimension parentSize = paraProfManager.getSize();
        int parentWidth = parentSize.width;
        int parentHeight = parentSize.height;

        //Set the window to come up in the center of the screen.
        int xPosition = (parentWidth - windowWidth) / 2;
        int yPosition = (parentHeight - windowHeight) / 2;

        xPosition = (int) parentPosition.getX() + xPosition;
        yPosition = (int) parentPosition.getY() + yPosition;

        this.setLocation(xPosition, yPosition);
        setSize(new java.awt.Dimension(windowWidth, windowHeight));
        setTitle("Load Trial");

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                thisWindowClosing(evt);
            }
        });

        trialTypes = new JComboBox(trialTypeStrings);
        trialTypes.setMaximumRowCount(10);
        trialTypes.addActionListener(this);

        Container contentPane = getContentPane();
        GridBagLayout gbl = new GridBagLayout();
        contentPane.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        gbc.weighty = 0;
        addCompItem(new JLabel("Trial Type"), gbc, 0, 0, 1, 1);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 100;
        gbc.weighty = 0;
        addCompItem(trialTypes, gbc, 1, 0, 1, 1);

        selectButton = new JButton("Select Directory");
        selectButton.addActionListener(this);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        gbc.weighty = 0;
        addCompItem(selectButton, gbc, 0, 1, 1, 1);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 100;
        gbc.weighty = 0;
        addCompItem(dirLocationField, gbc, 1, 1, 2, 1);

        JButton jButton = new JButton("Cancel");
        jButton.addActionListener(this);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        gbc.weighty = 0;
        addCompItem(jButton, gbc, 0, 2, 1, 1);

        jButton = new JButton("Ok");
        jButton.addActionListener(this);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        gbc.weighty = 0;
        addCompItem(jButton, gbc, 2, 2, 1, 1);
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            Object EventSrc = evt.getSource();
            String arg = evt.getActionCommand();
            if (arg.equals("Select Directory")) {
                JFileChooser jFileChooser = new JFileChooser(lastDirectory);
                jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jFileChooser.setMultiSelectionEnabled(false);
                jFileChooser.setDialogTitle("Select Directory");
                jFileChooser.setApproveButtonText("Select");
                if ((jFileChooser.showOpenDialog(this)) != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                lastDirectory = jFileChooser.getSelectedFile().getParent();
                dirLocationField.setText(jFileChooser.getSelectedFile().getCanonicalPath());

            } else if (arg.equals("  Select File(s)  ")) {
                JFileChooser jFileChooser = new JFileChooser(lastDirectory);
                jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jFileChooser.setMultiSelectionEnabled(true);
                jFileChooser.setDialogTitle("Select File(s)");
                jFileChooser.setApproveButtonText("Select");
                if ((jFileChooser.showOpenDialog(this)) != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                selectedFiles = jFileChooser.getSelectedFiles();
                lastDirectory = jFileChooser.getSelectedFile().getParent();

                if (selectedFiles.length > 1) {
                    dirLocationField.setText("<Multiple Files Selected>");
                    dirLocationField.setEditable(false);
                } else {
                    dirLocationField.setText(selectedFiles[0].toString());
                    dirLocationField.setEditable(true);
                }
            } else if (arg.equals("Cancel")) {
                // note that these are null if they're not top level (so this won't delete an application that has other experiments)

                if (newExperiment) {
                    paraProfManagerWindow.handleDelete(experiment);
                }
                if (newApplication) {
                    paraProfManagerWindow.handleDelete(application);
                }
                closeThisWindow();
            } else if (arg.equals("Ok")) {
                if (trialTypes.getSelectedIndex() == 0) {
                    File files[] = new File[1];
                    files[0] = new File(dirLocationField.getText().trim());
                    if (!files[0].exists()) {
                        JOptionPane.showMessageDialog(this, dirLocationField.getText().trim()
                                + " does not exist");
                        return;
                    }
                    paraProfManagerWindow.addTrial(application, experiment, files,
                            trialTypes.getSelectedIndex(), false);
                } else {
                    if (selectedFiles == null) {
                        selectedFiles = new File[1];
                        selectedFiles[0] = new File(dirLocationField.getText().trim());
                        if (!selectedFiles[0].exists()) {
                            JOptionPane.showMessageDialog(this, dirLocationField.getText().trim()
                                    + " does not exist");
                            return;
                        }
                    }
                    paraProfManagerWindow.addTrial(application, experiment, selectedFiles,
                            trialTypes.getSelectedIndex(), false);
                }
                closeThisWindow();
            } else if (arg.equals("comboBoxChanged")) {
                if (trialTypes.getSelectedIndex() == 0) {
                    selectButton.setText("Select Directory");
                    dirLocationField.setEditable(true);
                } else {
                    selectButton.setText("  Select File(s)  ");
                }
            }
        } catch (Exception e) {
            ParaProfUtils.handleException(e);
        }
    }

    private void addCompItem(Component c, GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        getContentPane().add(c, gbc);
    }

    //Close the window when the close box is clicked
    private void thisWindowClosing(java.awt.event.WindowEvent e) {
        closeThisWindow();
    }

    void closeThisWindow() {
        this.setVisible(false);
        dispose();
    }

}