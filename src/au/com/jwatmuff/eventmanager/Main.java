/*
 * Main.java
 *
 * Created on 30 April 2008, 17:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager;

import au.com.jwatmuff.eventmanager.db.sqlite.SQLiteDatabaseManager;
import au.com.jwatmuff.eventmanager.gui.main.LoadCompetitionWindow;
import au.com.jwatmuff.eventmanager.gui.main.LoadWindow;
import au.com.jwatmuff.eventmanager.gui.main.MainWindow;
import au.com.jwatmuff.eventmanager.gui.main.SynchronizingWindow;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.permissions.License;
import au.com.jwatmuff.eventmanager.permissions.LicenseManager;
import au.com.jwatmuff.eventmanager.util.GUIUtils;
import au.com.jwatmuff.genericdb.p2p.DatabaseInfo;
import au.com.jwatmuff.genericdb.p2p.DatabaseManager;
import au.com.jwatmuff.genericdb.p2p.DistributedDatabase;
import au.com.jwatmuff.genericdb.transaction.TransactionNotifier;
import au.com.jwatmuff.genericp2p.BonjourRMIPeerManager;
import com.apple.dnssd.BrowseListener;
import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDService;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 *
 * @author James
 */
public class Main {
    private static Logger log = Logger.getLogger(Main.class);
    
    public static final String VERSION = "0.0.2";

    /**
     * Creates a run lock file if it does not already exist. Returns false
     * if a run lock is already present and the force argument is false.
     */
    private static boolean obtainRunLock(boolean force) {
        File f = new File("eventmanager.run.lock");
        if(f.exists() && !force) {
            log.info("Run lock is present");
            return false;
        } else {
            try {
                if(f.exists() && force)
                    f.delete();
                f.createNewFile();
                f.deleteOnExit();
            } catch(IOException e) {
                log.warn("Unable to create lock file", e);
                return false;
            }
        }
        
        return true;
    }

    private static boolean checkBonjour() {
        try {
            DNSSD.browse("_ftp._tcp", new BrowseListener() {
                @Override
                public void serviceFound(DNSSDService arg0, int arg1, int arg2, String arg3, String arg4, String arg5) {
                }
                @Override
                public void serviceLost(DNSSDService arg0, int arg1, int arg2, String arg3, String arg4, String arg5) {
                }
                @Override
                public void operationFailed(DNSSDService arg0, int arg1) {
                }
            }).stop();
        } catch(Throwable t) {
            return false;
        }

        return true;
    }
    
    /**
     * Main method.
     */
    public static void main(String args[]) {
        /* Set timeout for RMI connections - TODO: move to external file */
        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "2000");
        try {
            System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostName());
        } catch(Exception e) {}

        /*
         * Set look and feel to 'system' style
         */
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } 
        catch (Exception e) {
           log.info("Failed to set system look and feel");
        }

        /*
         * Check if run lock exists, if so ask user if it is ok to continue
         */
        if(!obtainRunLock(false)) {
            int response = JOptionPane.showConfirmDialog(
                    null,
                    "Unable to obtain run-lock.\nPlease ensure that no other instances of EventManager are running before continuing.\nDo you wish to continue?",
                    "Run-lock detected",
                    JOptionPane.YES_NO_OPTION);
            if(response == JOptionPane.YES_OPTION)
                obtainRunLock(true);
            else
                System.exit(0);
        }

        if(!checkBonjour()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Unable to initialize Bonjour. Please ensure that Bonjour is installed.",
                    "Event Manager",
                    JOptionPane.OK_OPTION);
            System.exit(0);
        }

        try {
            LoadWindow loadWindow = new LoadWindow();
            loadWindow.setVisible(true);

            loadWindow.addMessage("Reading settings..");

            /*
             * Read properties from file
             */
            final Properties props = new Properties();
            try {
                Properties defaultProps = PropertiesLoaderUtils.loadProperties(new ClassPathResource("eventmanager.properties"));
                props.putAll(defaultProps);
            } catch (IOException ex) {
                log.error(ex);
            }

            props.putAll(System.getProperties());

            File databaseStore = new File("comps");
            int rmiPort = Integer.parseInt(props.getProperty("eventmanager.rmi.port"));

            loadWindow.addMessage("Loading Peer Manager..");
            log.info("Loading Peer Manager");

            BonjourRMIPeerManager peerManager = new BonjourRMIPeerManager(rmiPort);

            loadWindow.addMessage("Loading Database Manager..");
            log.info("Loading Database Manager");

            DatabaseManager databaseManager = new SQLiteDatabaseManager(databaseStore, peerManager);
            LicenseManager licenseManager = new LicenseManager(new File("."));

            loadWindow.addMessage("Loading Load Competition Dialog..");
            log.info("Loading Load Competition Dialog");

            LoadCompetitionWindow loadCompetitionWindow
                    = new LoadCompetitionWindow(databaseManager, licenseManager, peerManager);

            loadWindow.dispose();
            log.info("Starting Load Competition Dialog");

            while(true) {

                GUIUtils.runModalJFrame(loadCompetitionWindow);

                if(loadCompetitionWindow.getSuccess()) {
                    DatabaseInfo info = loadCompetitionWindow.getSelectedDatabaseInfo();

                    SynchronizingWindow syncWindow = new SynchronizingWindow();
                    syncWindow.setVisible(true);
                    DistributedDatabase database = databaseManager.activateDatabase(info.id, info.passwordHash);
                    syncWindow.dispose();

                    if(loadCompetitionWindow.isNewDatabase()) {
                        CompetitionInfo ci = new CompetitionInfo();
                        ci.setName(info.name);
                        ci.setStartDate(new Date());
                        ci.setEndDate(new Date());
                        //ci.setPasswordHash(info.passwordHash);
                        License license = licenseManager.getLicense();
                        if(license != null) {
                            ci.setLicenseName(license.getName());
                            ci.setLicenseType(license.getType().toString());
                            ci.setLicenseContact(license.getContactPhoneNumber());
                        }
                        database.add(ci);
                    }

                    //DataEventNotifier notifier = new DataEventNotifier();
                    TransactionNotifier notifier = new TransactionNotifier();
                    database.setListener(notifier);

                    MainWindow mainWindow = new MainWindow();
                    mainWindow.setDatabase(database);
                    mainWindow.setNotifier(notifier);
                    mainWindow.setPeerManager(peerManager);
                    mainWindow.setLicenseManager(licenseManager);
                    mainWindow.afterPropertiesSet();

                    // show main window (modally)
                    GUIUtils.runModalJFrame(mainWindow);

                    // shutdown procedures

                    // System.exit();

                    database.shutdown();
                    databaseManager.deactivateDatabase(1500);

                    if(mainWindow.getDeleteOnExit()) {
                        for(File file : info.localDirectory.listFiles())
                            if(!file.isDirectory())
                                file.delete();
                        info.localDirectory.deleteOnExit();
                    }
                } else {
                    System.exit(0);
                }
            }
        } catch(Throwable e) {
            log.error("Error in main function", e);
            String message = e.getMessage();
            if(message == null) message = "";
            if(message.length() > 100)
                message = message.substring(0,97) + "...";
            GUIUtils.displayError(null, "An unexpected error has occured.\n\n" + e.getClass().getSimpleName() + "\n" + message);
            System.exit(0);
        }
    }        
}
