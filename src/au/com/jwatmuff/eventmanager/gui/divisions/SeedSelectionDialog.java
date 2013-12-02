/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.gui.divisions;

import au.com.jwatmuff.eventmanager.util.gui.ComboBoxDialog;
import au.com.jwatmuff.eventmanager.util.gui.StringRenderer;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author james
 */
public class SeedSelectionDialog  extends ComboBoxDialog<Integer> {

    public SeedSelectionDialog(Frame parent, boolean modal) {
        super(parent, modal, getSeedChoices(), "Please select a seed", "Player Seed");
        
        setRenderer(new StringRenderer<Integer>() {
            @Override
            public String asString(Integer i) {
                return i == 0 ? "(None)" : i.toString();
            }
        });
    }
    
    private static List<Integer> getSeedChoices() {
        List<Integer> seeds = new ArrayList<>();
        for(int i = 0; i <= 64; i++) {
            seeds.add(i);
        }
        return seeds;
    }
    
}
