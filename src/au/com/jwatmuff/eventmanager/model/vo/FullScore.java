package au.com.jwatmuff.eventmanager.model.vo;

import java.io.Serializable;

public class FullScore implements Comparable<FullScore>, Serializable {
    private int ippon;
    private int wazari;
    private int yuko;
    private int shido;
    private int decision;

    public FullScore() {}

    public FullScore(String score) {
        for(String point : score.split(",")) {
            String[] pair = point.split(":");
            if(pair[0].length() != 1)
                throw new IllegalArgumentException("Invalid score format '" + score + "'");
            char p = pair[0].charAt(0);
            int i = Integer.valueOf(pair[1]);
            switch(p) {
                case 'I': ippon = i; break;
                case 'W': wazari = i; break;
                case 'Y': yuko = i; break;
                case 'S': shido = i; break;
                case 'D': decision = i; break;
                default: throw new IllegalArgumentException("Invalid score format '" + score + "'");
            }
        }
    }

    public int getIppon() {
        return ippon;
    }

    public void setIppon(int ippon) {
        this.ippon = ippon;
    }

    public int getWazari() {
        return wazari;
    }

    public void setWazari(int wazari) {
        this.wazari = wazari;
    }

    public int getYuko() {
        return yuko;
    }

    public void setYuko(int yuko) {
        this.yuko = yuko;
    }

    public int getShido() {
        return shido;
    }

    public void setShido(int shido) {
        this.shido = shido;
    }

    public int getDecision() {
        return decision;
    }

    public void setDecision(int decision) {
        this.decision = decision;
    }

    /**
     * Checks whether the score represented by this score is an actual possible
     * Judo score.
     *
     * An example of an invalid score would be multiple Ippons
     */
    public boolean isValid() {
        if(ippon > 1) return false;
        if(wazari > 2) return false;
        if(ippon == 1 && wazari == 2) return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("I:%d,W:%d,Y:%d,S:%d,D:%d", ippon, wazari, yuko, shido, decision);
    }

    public String displayString() {
        if(shido > 0 && decision > 0 ) {
            return String.format("%d%d%ds%dd%d", ippon, wazari, yuko, shido, decision);
        } else if(shido > 0 && decision == 0 ) {
            return String.format("%d%d%ds%d", ippon, wazari, yuko, shido);
        } else if(shido == 0 && decision > 0 ) {
            return String.format("%d%d%dd%d", ippon, wazari, yuko, decision);
        } else {
            return String.format("%d%d%d", ippon, wazari, yuko);
        }
    }

    public int compareTo(FullScore o) {
        if (getIppon() != o.getIppon())
            return getIppon() - o.getIppon();
        if (getWazari() != o.getWazari())
            return getWazari() - o.getWazari();
        if (getYuko() != o.getYuko())
            return getYuko() - o.getYuko();
        return getDecision() - o.getDecision();
    }
}
