/*
 * GUIUtils.java
 *
 * Created on 24 March 2008, 13:44
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

import au.com.jwatmuff.eventmanager.gui.admin.EnterPasswordDialog;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.apache.log4j.Logger;

/**
 * Collection of convenience methods for performing GUI functions, such as
 * displaying message dialogs.
 *
 * @author James
 */
public class GUIUtils {
    public static final Logger log = Logger.getLogger(GUIUtils.class);

    public static File lastDivisionChooserDirectory;
    public static File lastDrawChooserDirectory;
    public static File lastSirenChooserDirectory;
    public static File lastChooserDirectory ;

    static {
        lastChooserDirectory = null; // default to My Documents on windows

        File f = new File("resources/division");
        if(f.exists())
            lastDivisionChooserDirectory = f;

        f = new File("resources/draw");
        if(f.exists())
            lastDrawChooserDirectory = f;

        f = new File("resources/sound");
        if(f.exists())
            lastSirenChooserDirectory = f;
    }

    public static boolean confirmLock(Frame parent, String thingToLock) {
        int status = JOptionPane.showConfirmDialog(
                parent,
                "Are you sure you wish to lock this " + thingToLock + "?",
                "Confirm Lock",
                JOptionPane.YES_NO_OPTION);
        return (status == JOptionPane.YES_OPTION);
    }

    /** Make non-instantiable */
    private GUIUtils() {
    }

    /**
     * Makes a frame visible and blocks the caller until the frame is closed.
     * 
     * @param frame
     */
    public static void runModalJFrame(final JFrame frame) {
        // there may be a much better way of implementing this, i don't know..
        class RunningFlag {
            boolean value = true;
        }

        final RunningFlag flag = new RunningFlag();
        final Thread t = Thread.currentThread();

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent arg0) {
                            synchronized(t) {
                                flag.value = false;
                                t.notifyAll();
                            }
                        }
                    });

                    frame.setVisible(true);

                }   
            });

            synchronized(t) {
                while(flag.value == true) try { t.wait(); } catch(InterruptedException e) { }
            }
        } catch(InterruptedException e) {
            log.error(e);
        } catch(InvocationTargetException e2) {
            log.error(e2);
        }
    }
    
    public static Map<String,Object> getComponentValues(Container parent) {
        Map<String,Object> values = new HashMap<String,Object>();
        
        for(int i=0;i<parent.getComponentCount();i++) {
            Component child = parent.getComponent(i);
            
            if(child instanceof Container)
                values.putAll(getComponentValues((Container)child));
            
            if(child.getName() != null) {
                String name = child.getName();
                
                if(child instanceof JTextField)
                    values.put(name,((JTextField)child).getText());
            }

        }
        
        return values;
    }
    
    public static void displayErrors(Component parent, List<String> errors) {
            StringBuilder sb = new StringBuilder();
            
            for(String error : errors)
                sb.append(" - " + error + "\n");
            
            JOptionPane.showMessageDialog(
                    parent,
                    sb.toString(),
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
    }

    public static void displayError(Component parent, String string) {
            JOptionPane.showMessageDialog(
                    parent,
                    string,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
    }

    public static void displayMessage(Component parent, String string, String title) {
        JOptionPane.showMessageDialog(
                parent,
                string,
                title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Deprecated
    public static boolean checkPassword(Component parent, String string, int passwordHash) {
        if(passwordHash == 0)
            return true;
        else {
            EnterPasswordDialog epd;
            if(parent instanceof JFrame)
                epd = new EnterPasswordDialog((Frame)parent, true);
            else
                epd = new EnterPasswordDialog((Dialog)parent, true);
            
            if(string != null)
                epd.setPromptText(string);    
            epd.setVisible(true);
            if(epd.getSuccess()) {
                if(epd.getPassword().hashCode() == passwordHash)
                    return true;
                else
                    GUIUtils.displayError(parent, "Incorrect password");
            }
        }
        return false;
    }

    /*
     * Updates the cell renderers for all the columns of the tables to be left
     * aligned
     */
    public static void leftAlignTable(JTable table) {
        Enumeration<TableColumn> cols = table.getColumnModel().getColumns();
        while(cols.hasMoreElements()) {
            TableColumn col = cols.nextElement();
            final TableCellRenderer oldRenderer = col.getCellRenderer();
            if(oldRenderer == null) {
                col.setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if(c instanceof JLabel) {
                            ((JLabel)c).setHorizontalAlignment(SwingConstants.LEFT);
                        }
                        return c;
                    }
                });
            } else {
                col.setCellRenderer(new TableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = oldRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if(c instanceof JLabel) {
                            ((JLabel)c).setHorizontalAlignment(SwingConstants.LEFT);
                        }
                        return c;
                    }
                });
            }
        }
    }
    
}
