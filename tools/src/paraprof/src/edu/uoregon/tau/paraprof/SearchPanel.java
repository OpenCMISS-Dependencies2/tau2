package edu.uoregon.tau.paraprof;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import edu.uoregon.tau.paraprof.interfaces.Searchable;
import edu.uoregon.tau.paraprof.interfaces.SearchableOwner;

/**
 * Search panel for ParaProf windows, similar to FireFox search panel
 *    
 * TODO : ...
 *
 * <P>CVS $Id: SearchPanel.java,v 1.3 2005/05/31 23:21:49 amorris Exp $</P>
 * @author  Alan Morris
 * @version $Revision: 1.3 $
 */
public class SearchPanel extends JPanel {

    private Searchable searchable;
    private JTextField searchField;

    public SearchPanel(final SearchableOwner owner, final Searchable searchable) {
        this.searchable = searchable;

        searchable.setSearchHighlight(false);
        searchable.setSearchMatchCase(false);

        searchField = new JTextField();

        searchField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

            }

        });

        searchField.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {

            }

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchable.searchNext();
                } else {
                    if (searchable.setSearchString(searchField.getText())) {
                        searchField.setBackground(Color.white);
                        searchField.setForeground(Color.black);
                    } else {
                        searchField.setBackground(new Color(255, 102, 102));
                        searchField.setForeground(Color.white);
                    }
                }
            }

            public void keyTyped(KeyEvent e) {

            }

        });

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.weightx = 0;
        gbc.weighty = 0;

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchable.setSearchString("");
                owner.showSearchPanel(false);
            }
        });

        final JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchable.searchNext();
            }
        });

        final JButton prevButton = new JButton("Previous");
        prevButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchable.searchPrevious();
            }
        });

        final JCheckBox highlightBox = new JCheckBox("Highlight");
        highlightBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchable.setSearchHighlight(highlightBox.isSelected());
            }
        });

        final JCheckBox matchCaseBox = new JCheckBox("Match Case");
        matchCaseBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchable.setSearchMatchCase(matchCaseBox.isSelected());
                if (searchable.setSearchString(searchField.getText())) {
                    searchField.setBackground(Color.white);
                    searchField.setForeground(Color.black);
                } else {
                    searchField.setBackground(new Color(255, 102, 102));
                    searchField.setForeground(Color.white);
                }
            }
        });

        ParaProfUtils.addCompItem(this, closeButton, gbc, 0, 0, 1, 1);
        ParaProfUtils.addCompItem(this, new JLabel("Find:"), gbc, 1, 0, 1, 1);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.weighty = 1;
        ParaProfUtils.addCompItem(this, searchField, gbc, 2, 0, 1, 1);

        gbc.weightx = 0;
        gbc.weighty = 0;
        ParaProfUtils.addCompItem(this, nextButton, gbc, 3, 0, 1, 1);
        ParaProfUtils.addCompItem(this, prevButton, gbc, 4, 0, 1, 1);
        ParaProfUtils.addCompItem(this, highlightBox, gbc, 5, 0, 1, 1);
        ParaProfUtils.addCompItem(this, matchCaseBox, gbc, 6, 0, 1, 1);

        searchField.requestFocus();
    }

    public void setFocus() {
        searchField.requestFocus();
    }

}
