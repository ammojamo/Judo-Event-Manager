/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author James
 */
public class UpdatePosition extends HashMap<UUID, Integer> {
    @Override
    public String toString() {
        List<String> strings = new ArrayList<>();
        for(UUID id : keySet()) {
            strings.add(id.toString().substring(0, 8) + ": " + get(id));
        }
        return "UpdatePosition( " + String.join(",", strings) + " )";
    }
}
