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

import au.com.jwatmuff.eventmanager.model.vo.Player.Grade;
import au.com.jwatmuff.eventmanager.model.vo.Player.Gender;
import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author James
 */
public class Pool extends DistributableObject<Integer> implements Serializable {
    private String description;
    private String shortName;
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
    private List<Place> places = new ArrayList<Place>();
    // This desribes the pools for seeding players in the draw
    // It is a map from pool numbers to player numbers
    // The way of representing the pools may be revised to make it more
    // readable, but for now it stores information we need.
    private Map<Integer, Integer> drawPools = new HashMap<Integer, Integer>();

    public static class Place implements Serializable {
        public String name;
        public String code;
    }

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

    public String getShortName() {
        if(StringUtils.isEmpty(shortName) && !StringUtils.isEmpty(description)) {
            return description.replaceAll("[a-z\\s]", "").replaceAll("[^0-9][0-9]+$", " $0");
        } else {
            return shortName;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
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

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }

    public Map<Integer, Integer> getDrawPools() {
        return drawPools;
    }

    public void setDrawPools(Map<Integer, Integer> drawPools) {
        this.drawPools = drawPools;
    }
    public LockedStatus getLockedStatus() {
        return lockedStatus;
    }

    private void setLockedStatus(LockedStatus lockedStatus) {
        this.lockedStatus = lockedStatus;
    }

    public static Pool getLockedCopy(Pool p, LockedStatus lockedStatus) {
        p = ObjectCopier.copy(p);
        p.setID(IDGenerator.generate());
        p.setLockedStatus(lockedStatus);
        return p;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Pool && ((Pool) o).getID().equals(this.getID()));
    }
}
