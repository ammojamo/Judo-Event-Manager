/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.draw;

import au.com.jwatmuff.eventmanager.model.vo.Fight;
import java.util.List;

/**
 *
 * @author James
 */
public interface DrawSystem {
    public List<Fight> getFights();
}
