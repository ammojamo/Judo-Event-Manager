package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score;
import static au.com.jwatmuff.eventmanager.gui.scoreboard.ScoreboardModel.Score.*;
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
        if(get(SHIDO) + get(LEG_SHIDO) > 3) return false;
        if(get(LEG_SHIDO) > 2) return false;
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
        
        int shido = get(SHIDO) + get(LEG_SHIDO);
        int otherShido = o.get(SHIDO) + o.get(LEG_SHIDO);
        if(shido < otherShido) return SHIDO;
        if(shido > otherShido) return null;

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
