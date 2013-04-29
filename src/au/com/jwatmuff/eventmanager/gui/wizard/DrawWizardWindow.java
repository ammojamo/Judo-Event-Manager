/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DrawWizardWindow.java
 *
 * Created on 24/08/2010, 10:31:39 PM
 */

package au.com.jwatmuff.eventmanager.gui.wizard;

import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.PlayerPool;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.transaction.TransactionListener;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericdb.transaction.TransactionalDatabase;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class DrawWizardWindow extends javax.swing.JFrame {
    private static final Logger log = Logger.getLogger(DrawWizardWindow.class);

    public class Context {
        public Pool pool;
        public List<Player> players;
        public List<Player> unapprovedPlayers;
        public Map<Integer, Integer> seeds;
        public boolean detectExternalChanges = true;
        public DrawWizardWindow wizardWindow = DrawWizardWindow.this;
    }

    public interface Panel {
        /*
         * All panels of the wizard must implement this interface
         *
         * If a panel returns false from any of these methods, it tells the
         * wizard window not to perform any action in response to the button
         * press
         */
        boolean nextButtonPressed();
        boolean backButtonPressed();
        boolean closedButtonPressed();
        void beforeShow();
        void afterHide();
    }

    private CardLayout layout = new CardLayout();
    final private Panel[] panels;
    private int currentIndex;
    private Context context = new Context();
    private boolean navigationEnabled = true;

    /** Creates new form DrawWizardWindow */
    public DrawWizardWindow(final TransactionalDatabase database, final TransactionNotifier notifier, final Pool pool) {
        context.pool = pool;
        context.seeds = new HashMap<>();

        panels = new Panel[] {
            new PlayerSelectionPanel(database, notifier, context),
            new SeedingPanel(database, context),
            new ReviewDrawPanel(database, notifier, context)
        };

        initComponents();

        currentIndex = 0;
        panels[0].beforeShow();

        contentPanel.setLayout(layout);
        int index = 0;
        for(Panel panel : panels) {
            contentPanel.add((Component) panel, String.valueOf(index++));
        }

        updateButtons();

        setLocationRelativeTo(null);
        pack();

        // Set up listener to close the wizard if the Pool or any related Players are modified externally
        final TransactionListener listener = new TransactionListener() {
            @Override
            public void handleTransactionEvents(List<DataEvent> events, Collection<Class> dataClasses) {
                if(!context.detectExternalChanges) return;
                for(DataEvent event : events) {
                    boolean changeDetected = false;
                    if(event.getData() instanceof Pool) {
                        if(((Pool)event.getData()).getID() == pool.getID()) changeDetected = true;
                    } else if(event.getData() instanceof PlayerPool) {
                        if(((PlayerPool)event.getData()).getPoolID() == pool.getID()) changeDetected = true;
                    } else if(event.getData() instanceof Player) {
                        Player p = (Player)event.getData();
                        if(database.get(PlayerPool.class, new PlayerPool.Key(p.getID(), pool.getID())) != null) changeDetected = true;
                    }
                    if(changeDetected) {
                        GUIUtils.displayMessage(DrawWizardWindow.this, "This division has been modified by another user on the network.\nThis wizard will now close to prevent any conflicts.", "Change detected");
                        close();
                        return;
                    }
                }
            }
        };
        notifier.addListener(listener, Pool.class, Player.class, PlayerPool.class);

        // clean up listeners
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                notifier.removeListener(listener);
                DrawWizardWindow.this.removeWindowListener(this);
            }
        });
    }

    private void close() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dispatchEvent(new WindowEvent(DrawWizardWindow.this, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    private Panel getCurrentPanel() {
        return panels[currentIndex];
    }

    private void next() {
        currentIndex++;
        if(currentIndex < panels.length) {
            panels[currentIndex].beforeShow();
            layout.next(contentPanel);
            panels[currentIndex - 1].afterHide();
        } else {
            log.warn("Next tried to go past end of available panels");
            currentIndex--;
        }
        updateButtons();
    }

    private void back() {
        currentIndex--;
        if(currentIndex >= 0) {
            panels[currentIndex].beforeShow();
            layout.previous(contentPanel);
            panels[currentIndex + 1].afterHide();
        } else {
            log.warn("Back tried to go past beginning of available panels");
            currentIndex++;
        }
    }

    private void updateButtons() {
        boolean last = currentIndex == panels.length - 1;
        boolean first = currentIndex == 0;
        //nextButton.setText(last ? "Finish" : "Next");
        nextButton.setEnabled(!last && navigationEnabled);
        backButton.setEnabled(!first && navigationEnabled);
        closeButton.setEnabled(navigationEnabled);
        
        if(last) {
            if(nextButton.getParent() != null) nextButton.getParent().remove(nextButton);
            if(backButton.getParent() != null) backButton.getParent().remove(backButton);
            closeButton.setText("Finish");
            pack();
        }
    }
    
    public void disableNavigation() {
        navigationEnabled = false;
        updateButtons();
    }
    
    public void enableNavigation() {
        navigationEnabled = true;
        updateButtons();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        contentPanel = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        closeButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        backButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Draw Wizard");

        javax.swing.GroupLayout contentPanelLayout = new javax.swing.GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
            contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 505, Short.MAX_VALUE)
        );
        contentPanelLayout.setVerticalGroup(
            contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 319, Short.MAX_VALUE)
        );

        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        nextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/resultset_next.png"))); // NOI18N
        nextButton.setText("Next");
        nextButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        backButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/famfamfam/icons/silk/resultset_previous.png"))); // NOI18N
        backButton.setText("Back");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(closeButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(backButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nextButton))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(contentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        mainPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {backButton, closeButton, nextButton});

        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addComponent(contentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nextButton)
                    .addComponent(closeButton)
                    .addComponent(backButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        if(getCurrentPanel().nextButtonPressed()) {
            // if this is the last panel, close wizard
            if(currentIndex == panels.length - 1) {
                close();
                getCurrentPanel().afterHide();
            } else {
                next();
            }
        }
}//GEN-LAST:event_nextButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        if(getCurrentPanel().closedButtonPressed()) {
            close();
            getCurrentPanel().afterHide();
        }
    }//GEN-LAST:event_closeButtonActionPerformed

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        if(getCurrentPanel().backButtonPressed()) {
            back();
        }
    }//GEN-LAST:event_backButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel contentPanel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton nextButton;
    // End of variables declaration//GEN-END:variables

}
