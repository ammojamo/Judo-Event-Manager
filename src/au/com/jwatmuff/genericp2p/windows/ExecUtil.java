/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.genericp2p.windows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.exec.*;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class ExecUtil {
    private static final Logger log = Logger.getLogger(ExecUtil.class);
    
    public static String getStdoutForCommand(String cmd) {
        CommandLine cmdLine = CommandLine.parse(cmd);
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWatchdog(new ExecuteWatchdog(60000));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(stdout, null));
        try {
            executor.execute(cmdLine);
        } catch (ExecuteException e) {
            log.error("Exception executing '" + cmd + "'", e);
        } catch (IOException e) {
            log.error("IOException executing '" + cmd + "'", e);
        }

        return stdout.toString();
    }
}
