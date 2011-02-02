/*
 * PoolChecker.java
 *
 * Created on 31 July 2008, 18:41
 *
 * To change pool template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package au.com.jwatmuff.eventmanager.model.misc;

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.draw.ConfigurationFile;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Player.Grade;
import au.com.jwatmuff.eventmanager.model.vo.Player.Gender;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.genericdb.Database;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class PoolChecker {
    public static final Logger log = Logger.getLogger(PoolChecker.class);

    /** Creates a new instance of PoolChecker */
    private PoolChecker() {
    }

    public static int calculateAge(Date dob, Date censusDate) {
        if(dob == null) return -1;

        /* calculate age */
        GregorianCalendar birthCal = new GregorianCalendar();
        birthCal.setTime(dob);
        int birthYear = birthCal.get(GregorianCalendar.YEAR);

        GregorianCalendar censusCal = new GregorianCalendar();
        censusCal.setTime(censusDate);
        int censusYear = censusCal.get(GregorianCalendar.YEAR);

        int age = censusYear - birthYear;

        birthCal.set(GregorianCalendar.YEAR, censusYear);
        if (censusCal.before(birthCal)) {
            age--;
        }

        return age;
    }

    private static int getAge(Player p, Date censusDate) {
        return calculateAge(p.getDob(), censusDate);
    }

    private static Grade reduceGrade(Grade g, int amount) {
        assert (g != null);
        return Grade.values()[Math.max(g.ordinal() - amount, 0)];
    }

    public static Grade getEffectiveGrade(Player p, Pool pool, Date censusDate, ConfigurationFile configurationFile) {
        Grade g = p.getGrade();

        if(g == Grade.UNSPECIFIED || g == null)
            return Grade.UNSPECIFIED;

        int age = getAge(p, censusDate);

        int maxAge = pool.getMaximumAge();

        if(configurationFile.getBooleanProperty("defaultAdjustGrade", false)){
            int age1 = configurationFile.getIntegerProperty("defaultAgeThreshold1", 9);
            int age2 = configurationFile.getIntegerProperty("defaultAgeThreshold2", 14);
            int age3 = configurationFile.getIntegerProperty("defaultAgeThreshold3", 16);
            int gradeDrop = configurationFile.getIntegerProperty("defaultBeltDrop", 2);
            if (maxAge > age3 || maxAge == 0) {
                if(age <= age3) g = reduceGrade(g, gradeDrop);
                if(age <= age2) g = reduceGrade(g, gradeDrop);
                if(age <= age1) g = reduceGrade(g, gradeDrop);
            } else if(maxAge > age2) {
                if(age <= age2) g = reduceGrade(g, gradeDrop);
                if(age <= age1) g = reduceGrade(g, gradeDrop);
            } else if(maxAge > age1) {
                if(age <= age1) g = reduceGrade(g, gradeDrop);
            }
        }

        return g;
    }

    /**
     * Checks whether the given player is eligible to be entered into the given
     * pool.
     * 
     * @param p             The player to check
     * @param pool          The pool which the player wants to be entered in
     * @param censusDate    The date at which to calculate the player's age
     * 
     * @return              True if the player is eligible, false otherwise.
     */
    public static boolean checkPlayer(Player p, Pool pool, Date censusDate, ConfigurationFile configurationFile) {
        if (p.getLockedStatus() != Player.LockedStatus.LOCKED) {
            return false;
        }

        // check gender
        if ((pool.getGender() != null) &&
                (pool.getGender() != Gender.UNSPECIFIED) &&
                (p.getGender() != pool.getGender())) {
            return false;
        }

        // check weight
        if (pool.getMaximumWeight() > 0) {
            if (p.getWeight() > pool.getMaximumWeight() || p.getWeight() <= 0) {
                return false;
            }
        }

        if (pool.getMinimumWeight() > 0) {
            if (p.getWeight() < pool.getMinimumWeight() || p.getWeight() <= 0) {
                return false;
            }
        }

        int age = getAge(p, censusDate);
        if(age > 0) {
            if (pool.getMaximumAge() > 0) {
                if (age > pool.getMaximumAge()) {
                    return false;
                }
            }

            if (pool.getMinimumAge() > 0) {
                if (age < pool.getMinimumAge()) {
                    return false;
                }
            }
        } else if(pool.getMaximumAge() > 0 || pool.getMinimumAge() > 0) {
            return false;
        }

        // adjust belt if necessary
        Grade grade = getEffectiveGrade(p, pool, censusDate, configurationFile);

        // check grade/belt
        Grade maxGrade = pool.getMaximumGrade();
        if ((maxGrade != null) &&
                (maxGrade != Grade.UNSPECIFIED) &&
                ((maxGrade.compareTo(grade) < 0) || (grade == Grade.UNSPECIFIED))) {
            return false;
        }
        Grade minGrade = pool.getMinimumGrade();
        if ((minGrade != null) &&
                (minGrade != Grade.UNSPECIFIED) &&
                ((minGrade.compareTo(grade) > 0) || (grade == Grade.UNSPECIFIED))) {
            return false;
        }

        return true;
    }

    public static List<Player> findEligiblePlayers(final Pool pool, Database database) {
        final CompetitionInfo ci = database.get(CompetitionInfo.class, null);
        final ConfigurationFile configurationFile = ConfigurationFile.getConfiguration(ci.getDrawConfiguration());
        List<Player> players = database.findAll(Player.class, PlayerDAO.ALL);
        CollectionUtils.filter(players, new Predicate() {
            public boolean evaluate(Object arg0) {
                return checkPlayer((Player)arg0, pool, ci.getAgeThresholdDate(), configurationFile);
            }
        });
        return players;
    }
}
