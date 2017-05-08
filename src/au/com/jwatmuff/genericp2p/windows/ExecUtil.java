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
