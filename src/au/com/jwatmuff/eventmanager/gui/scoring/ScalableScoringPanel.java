/*
 * ScalableScoringPanel.java
 *
 * Created on 4 November 2008, 17:18
 */

package au.com.jwatmuff.eventmanager.gui.scoring;

import au.com.jwatmuff.eventmanager.util.gui.ScalableAbsoluteLayout;
import au.com.jwatmuff.eventmanager.util.gui.ScalableLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;

/**
 *
 * @author  James
 */
public class ScalableScoringPanel extends javax.swing.JPanel {
    private static final Logger log = Logger.getLogger(ScalableScoringPanel.class);

    private static final String IPPON = "I";
    private static final String WAZA_ARI = "W";
    private static final String DECISION = "D";
    
    ScalableLabel iButton;
    ScalableLabel wButton;
    ScalableLabel dButton;
    ScalableLabel playerLabel;
    ScalableLabel winnerByLabel;
    ScalableLabel confirmButton;
    ScalableLabel cancelButton;
    
    /** Creates new form ScalableScoringPanel */
    public ScalableScoringPanel() {
        initComponents();
        ScalableAbsoluteLayout layout = new ScalableAbsoluteLayout(this);
        
        iButton = new ScalableLabel(IPPON, true);   
        layout.addComponent(iButton, 1.0/10, 1.0/2, 1.0/5, 1.0/4);
        this.add(iButton);

        wButton = new ScalableLabel(WAZA_ARI);
        layout.addComponent(wButton, 3.0/10, 1.0/2, 1.0/5, 1.0/4);
        
//        layout.addComponent(yButton, new Rectangle.Double(5.0/10, 1.0/2, 1.0/5, 1.0/4));

        dButton = new ScalableLabel(DECISION, true);
        layout.addComponent(dButton, 7.0/10, 1.0/2, 1.0/5, 1.0/4);
        
        playerLabel = new ScalableLabel("Player");
        layout.addComponent(playerLabel, 1.0/8, 1.0/6, 6.0/8, 1.0/6);

        winnerByLabel = new ScalableLabel("Winner By");
        winnerByLabel.setBorder(new EmptyBorder(0,0,0,0));
        layout.addComponent(winnerByLabel, 3.0/10, 2.0/6, 4.0/10, 1.0/6);

        confirmButton = new ScalableLabel("OK");
        confirmButton.setVisible(false);
        layout.addComponent(confirmButton, 1.5/10, 5.1/6, 3.0/10, 0.8/6);

        cancelButton = new ScalableLabel("Cancel");
        cancelButton.setVisible(false);
        layout.addComponent(cancelButton, 5.5/10, 5.1/6, 3.0/10, 0.8/6);

        this.setLayout(layout);
    }

    public void showScoreButtons(boolean show) {
        iButton.setVisible(show);
        wButton.setVisible(show);
        dButton.setVisible(show);
    }

    public void showConfirmButtons(boolean show) {
        confirmButton.setVisible(show);
        cancelButton.setVisible(show);
    }
    
    public void setColors(Color fg, Color bg, Color fg2, Color bg2) {
        setForeground(fg);
        setBackground(bg);
        for(Component comp : getComponents()) {
            comp.setForeground(fg2);
            comp.setBackground(bg2);
        }

        winnerByLabel.setForeground(Color.BLACK);
        winnerByLabel.setBackground(null);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setMinimumSize(new java.awt.Dimension(100, 100));
        setPreferredSize(new java.awt.Dimension(100, 100));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
