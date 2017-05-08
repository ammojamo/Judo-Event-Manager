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

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.eventmanager.db.CompetitionDAO;
import au.com.jwatmuff.eventmanager.model.vo.CompetitionInfo;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 *
 * @author James
 */
public class SQLiteCompetitionDAO implements CompetitionDAO {
    private static final Logger log = Logger.getLogger(SQLiteCompetitionDAO.class);
    private final SimpleJdbcTemplate template;
    
    private static final String
            ID_FIELD = "id",
            NAME_FIELD = "name",
            LOCATION_FIELD = "location",
            START_DATE_FIELD = "start_date",
            END_DATE_FIELD = "end_date",
            AGE_THRESHOLD_DATE = "age_threshold_date",
            MATS_FIELD = "mats",
            PASSWORD_HASH_FIELD = "password_hash",
            WEIGH_IN_PASSWORD_HASH_FIELD = "wi_password_hash",
            PERSONAL_DETAILS_PASSWORD_HASH_FIELD = "pd_password_hash",
            SCOREBOARD_PASSWORD_HASH_FIELD = "sb_password_hash",
            LICENSE_NAME_FIELD = "license_name",
            LICENSE_TYPE_FIELD = "license_type",
            LICENSE_CONTACT_FIELD = "license_contact",
            DIRECTOR_NAME_FIELD = "director_name",
            DIRECTOR_CONTACT_FIELD = "director_contact",
            DRAW_CONFIGURATION_FIELD = "draw_configuration",
            CLOSED_FIELD = "closed",
            TIMESTAMP_FIELD = "last_updated";


    /** Creates a new instance of SQLiteCompetitionDAO */
    public SQLiteCompetitionDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }
    
    private static final ParameterizedRowMapper<CompetitionInfo> mapper =
            new ParameterizedRowMapper<CompetitionInfo>() {
        @Override
        public CompetitionInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            CompetitionInfo ci = new CompetitionInfo();

            ci.setID(rs.getInt(ID_FIELD));
            ci.setName(rs.getString(NAME_FIELD));
            ci.setLocation(rs.getString(LOCATION_FIELD));
            ci.setStartDate(rs.getDate(START_DATE_FIELD));
            ci.setEndDate(rs.getDate(END_DATE_FIELD));
            ci.setAgeThresholdDate(rs.getDate(AGE_THRESHOLD_DATE));
            ci.setMats(rs.getInt(MATS_FIELD));
            ci.setPasswordHash(rs.getInt(PASSWORD_HASH_FIELD));
            ci.setWeighInPasswordHash(rs.getInt(WEIGH_IN_PASSWORD_HASH_FIELD));
            ci.setPersonalDetailsPasswordHash(rs.getInt(PERSONAL_DETAILS_PASSWORD_HASH_FIELD));
            ci.setScoreboardPasswordHash(rs.getInt(SCOREBOARD_PASSWORD_HASH_FIELD));
            ci.setLicenseName(rs.getString(LICENSE_NAME_FIELD));
            ci.setLicenseType(rs.getString(LICENSE_TYPE_FIELD));
            ci.setLicenseContact(rs.getString(LICENSE_CONTACT_FIELD));
            ci.setDirectorName(rs.getString(DIRECTOR_NAME_FIELD));
            ci.setDirectorContact(rs.getString(DIRECTOR_CONTACT_FIELD));
            ci.setDrawConfiguration(rs.getString(DRAW_CONFIGURATION_FIELD));
            ci.setClosed(rs.getBoolean(CLOSED_FIELD));
            ci.setTimestamp(new Timestamp(rs.getDate(TIMESTAMP_FIELD).getTime()));
            return ci;
        }
    };
    
    @Override
    public void add(CompetitionInfo ci) {
        final String sql = "INSERT INTO competition (id, name, location, start_date, end_date, age_threshold_date, mats, password_hash, wi_password_hash, pd_password_hash, sb_password_hash, license_name, license_type, license_contact, director_name, director_contact, draw_configuration, closed, last_updated) VALUES " +
                "(:ID, :name, :location, :startDate, :endDate, :ageThresholdDate, :mats, :passwordHash, :weighInPasswordHash, :personalDetailsPasswordHash, :scoreboardPasswordHash, :licenseName, :licenseType, :licenseContact, :directorName, :directorContact, :drawConfiguration, :closed, :timestamp)";
        
        if(get(null) != null) {
            log.error("Cannot open a competition - a competition is already open.");
            throw new RuntimeException("Cannot open a competition - a competition is already open.");
        }
        
        SqlParameterSource params = new BeanPropertySqlParameterSource(ci);
        template.update(sql, params);
    }
    
    @Override
    public void delete(CompetitionInfo ci) {
        throw new RuntimeException("delete not supported");
    }
    
    private void delete() {
        template.update("DELETE FROM competition");
    }

    @Override
    public CompetitionInfo get(Object id) {
        final String sql = "SELECT * FROM competition";
        try {
            return template.queryForObject(sql, mapper);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    @Override
    public void update(CompetitionInfo ci) {
        CompetitionInfo ciCurr = get(null);
        if(ciCurr != null) {
            if(!ciCurr.getID().equals(ci.getID())) {
                log.error("Cannot update competition - competition IDs do not match. (" + ciCurr.getID() + ", " + ci.getID() +")");
                throw new RuntimeException("Cannot update competition - competition IDs do not match.");
            }
            delete();
        }
        add(ci);
    }

    @Override
    public Class<CompetitionInfo> getDataClass() {
        return CompetitionInfo.class;
    }
}
