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

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.DECISION;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.HANSAKUMAKE;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.IPPON;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.SHIDO;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.WAZARI;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class FullScore implements Comparable<FullScore>, Serializable {
    private int scores[] = new int[Score.values().length];

    public FullScore() {}

    public FullScore(String score) {
        for(String point : score.split(",")) {
            String[] pair = point.split(":");
            if(pair[0].length() != 1)
                throw new IllegalArgumentException("Invalid score format '" + score + "'");
            char p = pair[0].charAt(0);
            int i = Integer.valueOf(pair[1]);

            boolean found = false;
            for(Score s : Score.values()) {
                if(s.initial == p) {
                    set(s, i);
                    found = true;
                    break;
                }
            }

            if(!found) {
                throw new IllegalArgumentException("Invalid score format '" + score + "'");
            }
        }
    }

    public int get(Score score) {
        return scores[score.ordinal()];
    }

    public final void set(Score score, int s) {
        scores[score.ordinal()] = s;
    }

    /**
     * Checks whether the score represented by this score is an actual possible
     * Judo score.
     *
     * An example of an invalid score would be multiple Ippons
     */
    public boolean isValid() {
        if(get(IPPON) > 1) return false;
        if(get(HANSAKUMAKE) > 1) return false;
        if(get(SHIDO) > 3) return false;
        return true;
    }

    @Override
    public String toString() {
        List<String> elements = new ArrayList<>();
        for(Score score : Score.values()) {
            elements.add(score.initial + ":" + get(score));
        }
        return String.join(",", elements);
    }

    public String displayString() {
        StringBuilder sb = new StringBuilder();
        for(Score s : Score.values()) {
            if(s == IPPON || s == WAZARI) { // Always include ippon and wazari, without prefix
                sb.append(get(s));
            } else if(get(s) > 0) { // Include other non-zero scores, with prefix
                sb.append(Character.toUpperCase(s.initial));
                sb.append(get(s));
            }
        }
        return sb.toString();
    }

    public Score getWinningScore(FullScore o) {
        if(get(IPPON) > o.get(IPPON)) return IPPON;
        if(get(IPPON) < o.get(IPPON)) return null;
        if(get(WAZARI) > o.get(WAZARI)) return WAZARI;
        if(get(WAZARI) < o.get(WAZARI)) return null;
        if(get(SHIDO) < o.get(SHIDO)) return SHIDO;
        if(get(SHIDO) > o.get(SHIDO)) return null;
        if(get(DECISION) > o.get(DECISION)) return DECISION;
        if(get(DECISION) < o.get(DECISION)) return null;
        return null;
    }

    public int compareTo(FullScore o) {
        if(this.getWinningScore(o) != null) return 1;
        if(o.getWinningScore(this) != null) return -1;
        return 0;
    }
}
