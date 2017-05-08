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

package au.com.jwatmuff.eventmanager.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author James
 */
public class ListUtils {
    public static List<Integer> getIntegerSequence(int from, int to) {
        assert(from <= to);
        List<Integer> list = new ArrayList<Integer>(to - from + 1);
        for(int i = from; i <= to; i++) {
            list.add(i);
        }
        return list;
    }
}
