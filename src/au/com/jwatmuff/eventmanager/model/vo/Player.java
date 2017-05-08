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
        MALE ("M"),
        FEMALE ("F"),
        UNSPECIFIED ("U");

        public final String shortGender;
        Gender(String shortGender) {
            this.shortGender = shortGender;
        }
                
        public static Gender fromString(String name) {
            return (name == null) ? UNSPECIFIED : valueOf(name);
        }
    }
    
    public static enum Grade {
        WHITE ("6 Kyu", "WHITE", "6K"),
        YELLOW ("5 Kyu", "YELLOW", "5K"),
        ORANGE ("4 Kyu", "ORANGE", "4K"),
        GREEN ("3 Kyu", "GREEN", "3K"),
        BLUE ("2 Kyu", "BLUE", "2K"),
        BROWN ("1 Kyu", "BROWN", "1K"),
        BLACK_1ST_DAN ("1 Dan", "1st Dan", "1D"),
        BLACK_2ND_DAN ("2 Dan", "2nd Dan", "2D"),
        BLACK_3RD_DAN ("3 Dan", "3rd Dan", "3D"),
        BLACK_4TH_DAN ("4 Dan", "4th Dan", "4D"),
        BLACK_5TH_DAN ("5 Dan", "5th Dan", "5D"),
        BLACK_6TH_DAN ("6 Dan", "6th Dan", "6D"),
        BLACK_7TH_DAN ("7 Dan", "7th Dan", "7D"),
        BLACK_8TH_DAN ("8 Dan", "8th Dan", "8D"),
        BLACK_9TH_DAN ("9 Dan", "9th Dan", "9D"),
        UNSPECIFIED ("None", "None", "N");

        public final String shortGrade;
        public final String belt;
        public final String veryShortGrade;
        Grade(String shortGrade, String belt, String veryShortGrade) {
            this.shortGrade = shortGrade;
            this.belt = belt;
            this.veryShortGrade = veryShortGrade;
        }
        
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

    private String team = "";
    
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

    public String getShortGender() {
        return gender.shortGender;
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

    public String getShortGrade() {
        return grade.shortGrade;
    }

    public String getVeryShortGrade() {
        return grade.veryShortGrade;
    }

    public String getBelt() {
        return grade.belt;
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

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
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
