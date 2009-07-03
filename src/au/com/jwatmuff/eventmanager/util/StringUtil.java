/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

/**
 *
 * @author James
 */
public class StringUtil {
    public static String humanize(String string) {
        if(string == null || string.equals("")) return "";
        string = string.toLowerCase().replace('_', ' ');
        return string.substring(0,1).toUpperCase() + string.substring(1);
    }
}
