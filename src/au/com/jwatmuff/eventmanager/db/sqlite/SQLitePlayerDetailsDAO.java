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

import au.com.jwatmuff.eventmanager.db.PlayerDetailsDAO;
import au.com.jwatmuff.eventmanager.model.vo.PlayerDetails;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 *
 * @author James
 */
public class SQLitePlayerDetailsDAO implements PlayerDetailsDAO {
    private final SimpleJdbcTemplate template;
    
    private static final String
            ID_FIELD = "id",
            HOME_NUMBER_FIELD = "home_phone",
            WORK_NUMBER_FIELD = "work_phone",
            MOBILE_NUMBER_FIELD = "mobile",
            STREET_FIELD = "addr_street",
            CITY_FIELD = "addr_city",
            POSTCODE_FIELD = "addr_postcode",
            STATE_FIELD = "addr_state",
            EMAIL_FIELD = "email",
            EMERGENCY_NAME_FIELD = "emergency_name",
            EMERGENCY_PHONE_FIELD = "emergency_phone",
            EMERGENCY_MOBILE_FIELD = "emergency_mobile",
            MEDICAL_CONDITIONS_FIELD = "medical_conditions",
            MEDICAL_INFO_FIELD = "medical_info",
            INJURY_INFO_FIELD = "injury_info",
            VALID_FIELD = "is_valid",
            TIMESTAMP_FIELD = "last_updated";
    
    /** Creates a new instance of SQLitePlayerDAO */
    public SQLitePlayerDetailsDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }

    private static final ParameterizedRowMapper<PlayerDetails> mapper = new ParameterizedRowMapper<PlayerDetails>() {
        @Override
        public PlayerDetails mapRow(ResultSet data, int rowNum) throws SQLException {
            PlayerDetails p = new PlayerDetails();

            p.setID(data.getInt(ID_FIELD));
            p.setHomeNumber(data.getString(HOME_NUMBER_FIELD));
            p.setWorkNumber(data.getString(WORK_NUMBER_FIELD));
            p.setMobileNumber(data.getString(MOBILE_NUMBER_FIELD));
            p.setStreet(data.getString(STREET_FIELD));
            p.setCity(data.getString(CITY_FIELD));
            p.setPostcode(data.getString(POSTCODE_FIELD));
            p.setState(data.getString(STATE_FIELD));
            p.setEmail(data.getString(EMAIL_FIELD));
            p.setEmergencyName(data.getString(EMERGENCY_NAME_FIELD));
            p.setEmergencyPhone(data.getString(EMERGENCY_PHONE_FIELD));
            p.setEmergencyMobile(data.getString(EMERGENCY_MOBILE_FIELD));
            p.setMedicalConditions(data.getString(MEDICAL_CONDITIONS_FIELD));
            p.setMedicalInfo(data.getString(MEDICAL_INFO_FIELD));
            p.setInjuryInfo(data.getString(INJURY_INFO_FIELD));
            p.setValid(data.getBoolean(VALID_FIELD));
            p.setTimestamp(new Timestamp(data.getDate(TIMESTAMP_FIELD).getTime()));

            return p;
        }
    };
        
    @Override
    public PlayerDetails get(Object id)
    {
        try {
            final String sql = "SELECT * FROM player_details WHERE id = ?";
            return template.queryForObject(sql, mapper, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }
            
    @Override
    public void add(PlayerDetails p)
    {
        final String sql = "INSERT INTO player_details (" +
                      "id, " +
                      "home_phone, work_phone, mobile, " +
                      "addr_street, addr_city, addr_postcode, email, " +
                      "emergency_name, emergency_phone, emergency_mobile, " +
                      "medical_conditions, medical_info, injury_info, " +
                      "is_valid, last_updated " + 
                      ") VALUES ( " +
                      ":ID, " +
                      ":homeNumber, :workNumber, :mobileNumber, " +
                      ":street, :city, :postcode, :email, " +
                      ":emergencyName, :emergencyPhone, :emergencyMobile, " +
                      ":medicalConditions, :medicalInfo, :injuryInfo, " +
                      ":valid, :timestamp );";
        SqlParameterSource params = new BeanPropertySqlParameterSource(p);
        template.update(sql, params);
    }

    @Override
    public void update(PlayerDetails p)
    {
        final String sql = "UPDATE player_details SET " +
                      "home_phone=:homeNumber, " +
                      "work_phone=:workNumber, " +
                      "mobile=:mobileNumber, " +
                      "addr_street=:street, " +
                      "addr_city=:city, " +
                      "addr_postcode=:postcode, " +
                      "email=:email, " +
                      "emergency_name=:emergencyName, " +
                      "emergency_phone=:emergencyPhone, " +
                      "emergency_mobile=:emergencyMobile, " +
                      "medical_info=:medicalInfo, " +
                      "injury_info=:injuryInfo, " +
                      "is_valid=:valid, " +
                      "last_updated=:timestamp " +
                      "WHERE id=:ID;";        
        SqlParameterSource params = new BeanPropertySqlParameterSource(p);        
        template.update(sql, params);
    }

    @Override
    public void delete(PlayerDetails item) {
        throw new RuntimeException("Delete not allowed");
    }

    @Override
    public Class<PlayerDetails> getDataClass() {
        return PlayerDetails.class;
    }
}
