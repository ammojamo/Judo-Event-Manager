/*
 * Competition.java
 *
 * Created on 19 March 2008, 20:18
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author James
 */
public class CompetitionInfo extends DistributableObject<Integer> implements Serializable {
    private String name;
    private String location;
    private Date startDate;
    private Date endDate;
    private int mats;
    private int passwordHash;
    private int weighInPasswordHash;
    private int personalDetailsPasswordHash;
    private int scoreboardPasswordHash;


    
    private boolean poolsLocked;
    private boolean closed;
    
    /** Creates a new instance of Competition */
    public CompetitionInfo() {
        setID(IDGenerator.generate());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getMats() {
        return mats;
    }

    public void setMats(int mats) {
        this.mats = mats;
    }

    public int getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(int passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public int getWeighInPasswordHash() {
        return weighInPasswordHash;
    }

    public void setWeighInPasswordHash(int weighInPasswordHash) {
        this.weighInPasswordHash = weighInPasswordHash;
    }

    public int getPersonalDetailsPasswordHash() {
        return personalDetailsPasswordHash;
    }

    public void setPersonalDetailsPasswordHash(int personalDetailsPasswordHash) {
        this.personalDetailsPasswordHash = personalDetailsPasswordHash;
    }

    public int getScoreboardPasswordHash() {
        return scoreboardPasswordHash;
    }

    public void setScoreboardPasswordHash(int scoreboardPasswordHash) {
        this.scoreboardPasswordHash = scoreboardPasswordHash;
    }
}
