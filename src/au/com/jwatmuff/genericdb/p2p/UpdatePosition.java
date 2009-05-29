/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import java.util.HashMap;
import java.util.UUID;

/**
 *
 * @author James
 */
public class UpdatePosition extends HashMap<UUID, Integer> {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Update Table Position: \n");
        for(UUID id : keySet()) {
            sb.append("  " + id + ": " + get(id) + "\n");
        }
        if(keySet().size() == 0) {
            sb.append("  empty\n");
        }
        return sb.toString();
    }
}
