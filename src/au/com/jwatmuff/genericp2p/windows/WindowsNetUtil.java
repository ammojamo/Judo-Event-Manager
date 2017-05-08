/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    
    public static boolean runningOnWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
    
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
