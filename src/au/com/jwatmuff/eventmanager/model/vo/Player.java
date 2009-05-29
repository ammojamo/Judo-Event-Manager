/*
 * Player.java
 *
 * Created on 25 February 2008, 16:15
 *
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;
import java.sql.Date;

/**
 *
 * @author James
 */
public class Player extends DistributableObject<Integer> implements Serializable {
    public static enum Gender {
        MALE, FEMALE, UNSPECIFIED;
                
        public static Gender fromString(String name) {
            return (name == null) ? UNSPECIFIED : valueOf(name);
        }
    }
    
    public static enum Grade {
        WHITE, YELLOW, ORANGE, GREEN, BLUE, BROWN, BLACK_1ST_DAN, BLACK_2ND_DAN,
        BLACK_3RD_DAN, BLACK_4TH_DAN, BLACK_5TH_DAN, BLACK_6TH_DAN,
        BLACK_7TH_DAN, BLACK_8TH_DAN, BLACK_9TH_DAN, UNSPECIFIED;
        
        public static Grade fromString(String name) {
            return (name == null) ? UNSPECIFIED : valueOf(name);
        }
    }
    
    public static enum LockedStatus {
        LOCKED, UNLOCKED
    }

    private int detailsID;
    
    private String visibleID;
    private String lastName;
    private String firstName;
    private Gender gender = Gender.UNSPECIFIED;
    private Date dob;
    
    private Grade grade = Grade.UNSPECIFIED;
    private double weight;
    
    private LockedStatus lockedStatus = LockedStatus.UNLOCKED;
        
    /** Creates a new instance of Player */
    public Player() {
        setID(IDGenerator.generate());
    }
    
    public Player(LockedStatus lockedStatus) {
        this();
        this.lockedStatus = lockedStatus;
    }
    
    public int getDetailsID() {
        return detailsID;
    }

    public void setDetailsID(int detailsID) {
        this.detailsID = detailsID;
    }
    
    public String getVisibleID() {
        return visibleID;
    }

    public void setVisibleID(String visibleID) {
        this.visibleID = visibleID;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        if(gender == null) gender = Gender.UNSPECIFIED;
        this.gender = gender;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        if(grade == null) grade = Grade.UNSPECIFIED;
        this.grade = grade;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public static Player getLockedCopy(Player p, LockedStatus lockedStatus) {
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
    
    @Override
    public String toString() {
        return firstName + " " + lastName + " (" + getID() + ")";
    }
}
