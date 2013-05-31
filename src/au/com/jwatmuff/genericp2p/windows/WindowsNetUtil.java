/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.genericp2p.windows;

import com.sun.jna.platform.win32.Shell32;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class WindowsNetUtil {
    private static final Logger log = Logger.getLogger(WindowsNetUtil.class);
    
    public static void logNetworkInfo() {
        log.info("Beginning network log");
        log.info(ExecUtil.getStdoutForCommand("NET VIEW"));
        log.info(ExecUtil.getStdoutForCommand("IPCONFIG"));
        log.info(ExecUtil.getStdoutForCommand("netsh advfirewall firewall show rule name=all"));
    }
    
    public static List<String> getNetworkComputerNames() {
        String output = ExecUtil.getStdoutForCommand("NET VIEW");

        List<String> names = new ArrayList<>();

        for(String line : output.split("\\r?\\n")) {
            line = line.trim();
            if(line.startsWith("\\\\")) {
                names.add(line.substring(2));
            }
        }

        return names;
    }
    
    public static boolean firewallRuleExists(String ruleName) {
         String output = ExecUtil.getStdoutForCommand("netsh advfirewall firewall show rule name=\"" + ruleName + "\"");
         return output.contains(ruleName);
    }
    
    public static void openInboundFirewallPort(String ruleName, int port) {
        if(firewallRuleExists(ruleName)) {
            log.info("Firewall rule " + ruleName + " already exists");
            return;
        }
        Shell32.INSTANCE.ShellExecute(
                null,
                "runas",
                "netsh",
                "advfirewall firewall add rule name=\"" + ruleName + "\" dir=in action=allow protocol=TCP localport=" + port,
                null, 1);
    }
}
