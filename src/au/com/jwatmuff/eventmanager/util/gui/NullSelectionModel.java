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

package au.com.jwatmuff.eventmanager.util.gui;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author James
 * Copied from http://www.coderanch.com/t/346552/GUI/java/Disable-Selection-JTable
 */
public class NullSelectionModel implements ListSelectionModel {

    public boolean isSelectionEmpty() {
        return true;
    }

    public boolean isSelectedIndex(int index) {
        return false;
    }

    public int getMinSelectionIndex() {
        return -1;
    }

    public int getMaxSelectionIndex() {
        return -1;
    }

    public int getLeadSelectionIndex() {
        return -1;
    }

    public int getAnchorSelectionIndex() {
        return -1;
    }

    public void setSelectionInterval(int index0, int index1) {
    }

    public void setLeadSelectionIndex(int index) {
    }

    public void setAnchorSelectionIndex(int index) {
    }

    public void addSelectionInterval(int index0, int index1) {
    }

    public void insertIndexInterval(int index, int length, boolean before) {
    }

    public void clearSelection() {
    }

    public void removeSelectionInterval(int index0, int index1) {
    }

    public void removeIndexInterval(int index0, int index1) {
    }

    public void setSelectionMode(int selectionMode) {
    }

    public int getSelectionMode() {
        return SINGLE_SELECTION;
    }

    public void addListSelectionListener(ListSelectionListener lsl) {
    }

    public void removeListSelectionListener(ListSelectionListener lsl) {
    }

    public void setValueIsAdjusting(boolean valueIsAdjusting) {
    }

    public boolean getValueIsAdjusting() {
        return false;
    }
}
