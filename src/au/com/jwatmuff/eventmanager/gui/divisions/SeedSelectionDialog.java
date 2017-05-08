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
