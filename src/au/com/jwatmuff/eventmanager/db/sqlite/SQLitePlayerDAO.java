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

import au.com.jwatmuff.eventmanager.db.PlayerDAO;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 *
 * @author James
 */
public class SQLitePlayerDAO implements PlayerDAO {
    private final SimpleJdbcTemplate template;
    
    private static final String
            ID_FIELD = "id",
            DETAILS_ID_FIELD = "details_id",
            VISIBLE_ID_FIELD = "visible_id",
            FIRST_NAME_FIELD = "first_name",
            LAST_NAME_FIELD = "last_name",
            GENDER_FIELD = "gender",
            DOB_FIELD = "dob",
            GRADE_FIELD = "grade",
            WEIGHT_FIELD = "weight",
            TEAM_FIELD = "team",
            LOCKED_STATUS_FIELD = "locked_status",
            VALID_FIELD = "is_valid",
            TIMESTAMP_FIELD = "last_updated";
    
    /** Creates a new instance of SQLitePlayerDAO */
    public SQLitePlayerDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }

    private static final ParameterizedRowMapper<Player> mapper = new ParameterizedRowMapper<Player>() {
        @Override
        public Player mapRow(ResultSet data, int rowNum) throws SQLException {
            Player p = new Player(Player.LockedStatus.valueOf(data.getString(LOCKED_STATUS_FIELD)));

            p.setID(data.getInt(ID_FIELD));
            p.setDetailsID(data.getInt(DETAILS_ID_FIELD));
            p.setVisibleID(data.getString(VISIBLE_ID_FIELD));
            p.setFirstName(data.getString(FIRST_NAME_FIELD));
            p.setLastName(data.getString(LAST_NAME_FIELD));
            p.setGender(Player.Gender.fromString(data.getString(GENDER_FIELD)));
            p.setDob(data.getDate(DOB_FIELD));
            p.setGrade(Player.Grade.fromString(data.getString(GRADE_FIELD)));
            p.setWeight(data.getDouble(WEIGHT_FIELD));
            p.setTeam(data.getString(TEAM_FIELD));
            p.setValid(data.getBoolean(VALID_FIELD));
            p.setTimestamp(new Timestamp(data.getDate(TIMESTAMP_FIELD).getTime()));

            return p;
        }
    };
        
    @Override
    public Collection<Player> findAll()
    {
        final String sql = "SELECT * FROM player WHERE is_valid";
        return template.query(sql, mapper);
    }

    @Override
    public Player get(Object id)
    {
        try {
            final String sql = "SELECT * FROM player WHERE id = ?";
            return template.queryForObject(sql, mapper, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    @Override
    public Player findForVisibleID(String id) {
        try {
            final String sql = "SELECT * FROM player WHERE visible_id = ? AND is_valid";
            return template.queryForObject(sql, mapper, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    @Override
    public Collection<Player> findForPool(int poolID, boolean approved) {
        final String sql = "SELECT player.* FROM player, player_has_pool WHERE player.is_valid AND player.id = player_id AND pool_id = ? AND approved = ? AND player_has_pool.is_valid AND player.is_valid";
        return template.query(sql, mapper, poolID, approved);
    }
    
    @Override
    public Collection<Player> findWithoutPool() {
        final String sql = "SELECT player.* FROM player WHERE player.id NOT IN (SELECT player_id FROM player_has_pool WHERE is_valid) AND player.is_valid";
        return template.query(sql, mapper);
    }
        
    @Override
    public void add(Player p)
    {
        final String sql = "INSERT INTO player (" +
                      "id, details_id, " +
                      "visible_id, first_name, last_name, gender, dob, " +
                      "grade, weight, team, " +
                      "locked_status, " +
                      "is_valid, last_updated " + 
                      ") VALUES ( " +
                      ":ID, :detailsID, " +
                      ":visibleID, :firstName, :lastName, :gender, :dob, " +
                      ":grade, :weight, :team, " +
                      ":lockedStatus, " +
                      ":valid, :timestamp );";
        SqlParameterSource params = new BeanPropertySqlParameterSource(p);
        template.update(sql, params);
    }

    @Override
    public void update(Player p)
    {
        final String sql = "UPDATE player SET " +
                      "details_id = :detailsID, " +
                      "visible_id = :visibleID, " +
                      "first_name=:firstName, " +
                      "last_name=:lastName, " +
                      "gender=:gender, " +
                      "dob=:dob, " +
                      "grade=:grade, " +
                      "weight=:weight, " +
                      "team=:team, " +
                      "locked_status=:lockedStatus, " +
                      "is_valid=:valid, " +
                      "last_updated=:timestamp " +
                      "WHERE id=:ID;";        
        SqlParameterSource params = new BeanPropertySqlParameterSource(p);        
        template.update(sql, params);
    }

    @Override
    public void delete(Player item) {
        throw new RuntimeException("Delete not allowed");
    }

    @Override
    public Class<Player> getDataClass() {
        return Player.class;
    }
}
