/*
 * Pool.java
 *
 * Created on 25 April 2008, 17:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.model.vo.Player.Grade;
import au.com.jwatmuff.eventmanager.model.vo.Player.Gender;
import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;

/**
 *
 * @author James
 */
public class Pool extends DistributableObject<Integer> implements Serializable {

    private String description;
    private int minimumAge;
    private int maximumAge;
    private double minimumWeight;
    private double maximumWeight;
    private Grade minimumGrade;
    private Grade maximumGrade;
    private Gender gender;
    private int matchTime;
    private int minimumBreakTime;
    private int goldenScoreTime;
    private String templateName;
    private LockedStatus lockedStatus = LockedStatus.UNLOCKED;

    public enum LockedStatus {

        UNLOCKED, PLAYERS_LOCKED, FIGHTS_LOCKED
    }

    /** Creates a new instance of Pool */
    public Pool() {
        setID(IDGenerator.generate());
    }

    public Pool(LockedStatus lockedStatus) {
        this();
        setLockedStatus(lockedStatus);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMinimumAge() {
        return minimumAge;
    }

    public void setMinimumAge(int minimumAge) {
        this.minimumAge = minimumAge;
    }

    public int getMaximumAge() {
        return maximumAge;
    }

    public void setMaximumAge(int maximumAge) {
        this.maximumAge = maximumAge;
    }

    public double getMinimumWeight() {
        return minimumWeight;
    }

    public void setMinimumWeight(double minimumWeight) {
        this.minimumWeight = minimumWeight;
    }

    public double getMaximumWeight() {
        return maximumWeight;
    }

    public void setMaximumWeight(double maximumWeight) {
        this.maximumWeight = maximumWeight;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Pool && ((Pool) o).getID().equals(this.getID()));
    }

    public Grade getMinimumGrade() {
        return minimumGrade;
    }

    public void setMinimumGrade(Grade minimumGrade) {
        this.minimumGrade = minimumGrade;
    }

    public Grade getMaximumGrade() {
        return maximumGrade;
    }

    public void setMaximumGrade(Grade maximumGrade) {
        this.maximumGrade = maximumGrade;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(int matchTime) {
        this.matchTime = matchTime;
    }

    public int getMinimumBreakTime() {
        return minimumBreakTime;
    }

    public void setMinimumBreakTime(int minimumBreakTime) {
        this.minimumBreakTime = minimumBreakTime;
    }

    public int getGoldenScoreTime() {
        return goldenScoreTime;
    }

    public void setGoldenScoreTime(int goldenScoreTime) {
        this.goldenScoreTime = goldenScoreTime;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public static Pool getLockedCopy(Pool p, LockedStatus lockedStatus) {
        p = ObjectCopier.copy(p);
        p.setID(IDGenerator.generate());
        p.setLockedStatus(lockedStatus);
        return p;
    }

    public LockedStatus getLockedStatus() {
        return lockedStatus;
    }

    private void setLockedStatus(LockedStatus lockedStatus) {
        this.lockedStatus = lockedStatus;
    }
}
