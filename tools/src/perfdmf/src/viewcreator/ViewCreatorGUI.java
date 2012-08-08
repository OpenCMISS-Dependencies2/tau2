package edu.uoregon.tau.perfdmf.viewcreator;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 



/*
 * CardLayoutDemo.java
 *
 */
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.text.NumberFormatter;



public class ViewCreatorGUI extends JFrame implements ActionListener{
    public class ViewCreatorListner implements ItemListener, ActionListener {
    	JPanel cards;

		public ViewCreatorListner(JPanel comparators) {
			super();
			cards = comparators;
		}


		public void itemStateChanged(ItemEvent evt) {
	        CardLayout cl = (CardLayout)(cards.getLayout());
	        cl.show(cards, (String)evt.getItem());
	    }


		public void actionPerformed(ActionEvent evt) {
			if(evt.getSource() instanceof JButton){
				JButton button = (JButton) evt.getSource();
				if(button.getText().equals("+")){
			    	createNewRule();
			    	panel.validate();
				}else if(button.getText().equals("-")){
					rulePane.remove(button.getParent());
					rulePane.getParent().validate();
				}
			}
			
		}

	}


	 static final String STRING_ENDS = "ends with";
	 static final String STRING_CONTAINS = "contains";
	 static final String STRING_EXACTLY = "is exactly";
	 static final String STRING_BEGINS = "beings with";
	
	 static final String NUMBER_EQUAL = "is equal to";
	 static final String NUMBER_LESS = "is less than";
	 static final String NUMBER_RANGE = "is in the range";
	 static final String NUMBER_GREATER = "is greater than";
	 
	 static final String DATE_IS = "is";
	 static final String DATE_RANGE = "is between";
	 static final String DATE_BEFORE = "is before";
	 static final String DATE_AFTER = "is after";
	 
	 static final String STRING = "read as a string";
	 static final String NUMBER = "read as a number";
	 static final String DATE = "read as a date";
	 
	 static final String ANY="any";
	 static final String ALL="all";
	 
	private JPanel panel;
	private JPanel rulePane;
	private ViewCreator viewCreator;

	
    
    public ViewCreatorGUI(ViewCreator vc) {
    	super();
    	 try {
    		 UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
             //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (Exception e) {}
    	this.viewCreator = vc;
    	this.setTitle("TAUdb View Creator");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            	
    	panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    	rulePane = new JPanel();

    	rulePane.setLayout(new BoxLayout(rulePane, BoxLayout.Y_AXIS));


    	JScrollPane scrollRule = new JScrollPane(rulePane);
    	scrollRule.setPreferredSize(new Dimension(800, 200));

    	
    	createNewRule();
    	rulePane.validate();
        //Display the window.
        rulePane.setVisible(true);


    	panel.add(getMatch());
    	panel.add(scrollRule);
    	panel.add(getSaveButtons());
    	panel.validate();
    	
    	this.getContentPane().add(panel);
	}
	private JPanel getSaveButtons() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		
		JButton save = new JButton("Save");
		save.setActionCommand("Save"); 
		save.addActionListener(this);
		
		JButton cancel = new JButton("Cancel");
		cancel.setActionCommand("Cancel");
		cancel.addActionListener(this);
		
		buttonPanel.add(cancel);
		buttonPanel.add(save);
		
    	return buttonPanel;
	}
	public void close(){
		this.dispose();
	}
	public void actionPerformed(ActionEvent e) {
		if("Save".equals(e.getActionCommand())){
			String saveName = (String)JOptionPane.showInputDialog(
			                    this,
			                    "Please enter the name of this TAUdb View",
			                    "Save TAUdb View",
			                    JOptionPane.PLAIN_MESSAGE);

			//If a string was returned, say so.
			if ((saveName != null) && (saveName.length() > 0)) {
				saveView(saveName);
				close();
			}
			
		}else if ("Cancel".equals(e.getActionCommand())){
			close();
		}
		
	}

	private void saveView(String saveName) {
		// TODO: Some how collects the information on the screen.
		int numRules = rulePane.getComponentCount();

		for (Component pane : rulePane.getComponents()) {

			JPanel rule = (JPanel) pane;

			JComboBox metadataCB = (JComboBox) rule.getComponent(0);
			String metadataName = metadataCB.getSelectedItem().toString();
			System.out.println("Metadata Name: " + metadataName);
			JComboBox readAsCB = (JComboBox) rule.getComponent(1);
			String type = readAsCB.getSelectedItem().toString();
			System.out.println("Type: " + type);
			JComboBox comparCB = (JComboBox) ((JPanel) ((JPanel) rule
					.getComponent(2)).getComponent(0)).getComponent(0);
			String comparator = comparCB.getSelectedItem().toString();
			System.out.println("Comp: " + comparator);
			
			JPanel valuePanel = (JPanel) ((JPanel) ((JPanel) rule.getComponent(2)).getComponent(0)).getComponent(1);

			 if (comparator.equals(STRING_EXACTLY)) {
					JTextField text = (JTextField) ((JPanel) valuePanel.getComponent(0)).getComponent(0);
					String value = text.getText();
					System.out.println(value);
			 }else if (comparator.equals(STRING_BEGINS)) {
				JTextField text = (JTextField) ((JPanel) valuePanel.getComponent(1)).getComponent(0);
				String value = text.getText();
				System.out.println(value);
		
			} else if (comparator.equals(STRING_ENDS)) {
				JTextField text = (JTextField) ((JPanel) valuePanel.getComponent(2)).getComponent(0);
				String value = text.getText();
				System.out.println(value);
			} else if (comparator.equals(STRING_CONTAINS)) {
				JTextField text = (JTextField) ((JPanel) valuePanel.getComponent(3)).getComponent(0);
				String value = text.getText();
				System.out.println(value);
			}

			else if (comparator.equals(NUMBER_EQUAL)) {
				JFormattedTextField text = (JFormattedTextField) ((JPanel) valuePanel.getComponent(0)).getComponent(0);
				String value = text.getText();
				System.out.println(value);
			} else if (comparator.equals(NUMBER_GREATER)) {
			} else if (comparator.equals(NUMBER_LESS)) {
			} else if (comparator.equals(NUMBER_RANGE)) {
			} 
			
			else if (comparator.equals(DATE_IS)) {
			} else if (comparator.equals(DATE_AFTER)) {
			} else if (comparator.equals(DATE_BEFORE)) {

			} else if (comparator.equals(NUMBER_RANGE)) {

			}

		}

		// and sent it to the DB
		System.out.println("Saving " + saveName);

	}
	private JPanel getMatch() {
		JPanel panel = new JPanel();
		String[] comboBoxItems = {ALL,  ANY};
		JComboBox comboBox = new JComboBox(comboBoxItems);
		JLabel label1 = new JLabel("Match ");
		JLabel label2 = new JLabel(" of the following rules.");

		panel.add(label1);
		panel.add(comboBox);
		panel.add(label2);
		
		return panel;
	}
//	public enum ComparatorType {
//	    STRING("read as a string"), 
//	    NUMBER(""), 
//	    DATE("");
//	    private final String string;   // in kilograms
//	    ComparatorType(String string) {
//	        this.string = string;
//	    }
//	}

	
	public void createNewRule() {

		String[] comparatorTypes = {STRING,NUMBER, DATE};

    	JPanel cards;
        JPanel comboBoxPane = new JPanel(); //use FlowLayout
        String comboBoxItems[] = comparatorTypes ;
        JComboBox cb = new JComboBox(comboBoxItems);
        cb.setEditable(false);
        
        cards = new JPanel(new CardLayout());
        
        cards.add(addStringField(), STRING);
        cards.add(addNumberField(),NUMBER);
        cards.add(addDateField(), DATE);
        
        ViewCreatorListner listner = new ViewCreatorListner(cards);
        cb.addItemListener(listner);

        
        JButton plusButton = new JButton("+");
        JButton minusButton = new JButton("-");
        plusButton.addActionListener(listner);
        minusButton.addActionListener(listner);
        
        String metadataList[] = getMetaDataList();
        JComboBox metadataCB = new JComboBox(metadataList);
        metadataCB.setEditable(false);
        
        comboBoxPane.add(metadataCB, BorderLayout.WEST);
        comboBoxPane.add(cb, BorderLayout.CENTER);
        comboBoxPane.add(cards, BorderLayout.EAST);
        comboBoxPane.add(minusButton);
        comboBoxPane.add(plusButton);
        comboBoxPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        rulePane.add(comboBoxPane);
	}
    private String[] getMetaDataList() {
		//List of names should be looked up in database
    	//String[] returnS = {"Aplication","CPU Cores","Username"};
    	String[] returnS = new String[0];
    	returnS = viewCreator.getMetadataNames().toArray(returnS);
		return returnS;
	}
    private Component addNumberField(){
        //Put the JComboBox in a JPanel to get a nicer look.
        JPanel comboBoxPane = new JPanel(); //use FlowLayout
        String comboBoxItems[] = {NUMBER_EQUAL, NUMBER_GREATER, NUMBER_LESS, NUMBER_RANGE};
        JComboBox cb = new JComboBox(comboBoxItems);
        cb.setEditable(false);
        
        //Create the "cards".
        JPanel greaterCard = new JPanel();
        JFormattedTextField text = new JFormattedTextField(new NumberFormatter());
        text.setValue(0.0);  
        text.setPreferredSize(new Dimension(100, 20));
        greaterCard.add(text);
        
        
        JPanel lessCard = new JPanel();
         text = new JFormattedTextField(new NumberFormatter());
        text.setValue(0.0);  
        text.setPreferredSize(new Dimension(100, 20));
        lessCard.add(text);
        
        JPanel equalCard = new JPanel();
         text = new JFormattedTextField(new NumberFormatter());
        text.setValue(0.0);  
        text.setPreferredSize(new Dimension(100, 20));
        equalCard.add(text);
        
        JPanel rangeCard = new JPanel();
        text = new JFormattedTextField(new NumberFormatter());
        text.setValue(0.0);  
        text.setPreferredSize(new Dimension(100, 20));
        rangeCard.add(text);
         text = new JFormattedTextField(new NumberFormatter());
        text.setValue(0.0);  
        text.setPreferredSize(new Dimension(100, 20));
        rangeCard.add(text);


        
        //Create the panel that contains the "cards".
        JPanel comparators = new JPanel(new CardLayout());
        comparators.add(equalCard, NUMBER_EQUAL);
        comparators.add(greaterCard, NUMBER_GREATER);
        comparators.add(lessCard, NUMBER_LESS);
        comparators.add(rangeCard, NUMBER_RANGE);

        cb.addItemListener(new ViewCreatorListner(comparators));
       
        
        comboBoxPane.add(cb, BorderLayout.WEST);
        comboBoxPane.add(comparators, BorderLayout.EAST);
        
        return comboBoxPane;
    	
    }
    private Component addDateField(){
        //Put the JComboBox in a JPanel to get a nicer look.
        JPanel comboBoxPane = new JPanel(); //use FlowLayout
        String comboBoxItems[] = {DATE_IS, DATE_AFTER, DATE_BEFORE, DATE_RANGE};
        JComboBox cb = new JComboBox(comboBoxItems);
        cb.setEditable(false);
        
        //Create the "cards".
        JPanel greaterCard = new JPanel();
        JFormattedTextField date = new JFormattedTextField(new SimpleDateFormat("MM/dd/yyyy"));
        date.setValue(new Date());
        greaterCard.add(date);
        
        
        JPanel lessCard = new JPanel();
        date = new JFormattedTextField(new SimpleDateFormat("MM/dd/yyyy"));
        date.setValue(new Date());
        lessCard.add(date);
        
        JPanel equalCard = new JPanel();
        date = new JFormattedTextField(new SimpleDateFormat("MM/dd/yyyy"));
        date.setValue(new Date());
        equalCard.add(date);
        
        JPanel rangeCard = new JPanel();
        date = new JFormattedTextField(new SimpleDateFormat("MM/dd/yyyy"));
        date.setValue(new Date());
        rangeCard.add(date);
        date = new JFormattedTextField(new SimpleDateFormat("MM/dd/yyyy"));
        date.setValue(new Date());
        rangeCard.add(date);
        
        //Create the panel that contains the "cards".
        JPanel comparators = new JPanel(new CardLayout());
        comparators.add(equalCard, DATE_IS);
        comparators.add(greaterCard, DATE_AFTER);
        comparators.add(lessCard, DATE_BEFORE);
        comparators.add(rangeCard, DATE_RANGE);

        cb.addItemListener(new ViewCreatorListner(comparators));
       
        
        comboBoxPane.add(cb, BorderLayout.WEST);
        comboBoxPane.add(comparators, BorderLayout.EAST);
        
        return comboBoxPane;
    	
    }
	private Component addStringField(){
        //Put the JComboBox in a JPanel to get a nicer look.
        JPanel comboBoxPane = new JPanel(); //use FlowLayout
        String comboBoxItems[] = {STRING_EXACTLY,STRING_BEGINS, STRING_ENDS, STRING_CONTAINS};
        JComboBox cb = new JComboBox(comboBoxItems);
        cb.setEditable(false);
        
        //Create the "cards".
        JPanel beginCard = new JPanel();
        beginCard.add(new JTextField("", 20));
        
        JPanel containsCard = new JPanel();
        containsCard.add(new JTextField("", 20));
        
        JPanel endCard = new JPanel();
        endCard.add(new JTextField("", 20));
        
        JPanel exactlyCard = new JPanel();
        exactlyCard.add(new JTextField("", 20));
        

        
        //Create the panel that contains the "cards".
        JPanel comparators = new JPanel(new CardLayout());
        comparators.add(exactlyCard, STRING_EXACTLY);
        comparators.add(beginCard, STRING_BEGINS);
        comparators.add(endCard, STRING_ENDS);
        comparators.add(containsCard, STRING_CONTAINS);

        cb.addItemListener(new ViewCreatorListner(comparators));

        comboBoxPane.add(cb, BorderLayout.WEST);
        comboBoxPane.add(comparators, BorderLayout.EAST);
        
        return comboBoxPane;
    	
    }
    


    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        //Create and set up the content pane.
        ViewCreatorGUI frame = new ViewCreatorGUI(null);
       
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
    

	public static void main(String[] args) {
        /* Use an appropriate Look and Feel */
        try {
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        /* Turn off metal's use of bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}

