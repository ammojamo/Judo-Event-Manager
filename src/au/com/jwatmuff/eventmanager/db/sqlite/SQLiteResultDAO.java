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

import au.com.jwatmuff.eventmanager.db.ResultDAO;
import au.com.jwatmuff.eventmanager.model.vo.FullScore;
import au.com.jwatmuff.eventmanager.model.vo.Result;
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
public class SQLiteResultDAO implements ResultDAO
{
    SimpleJdbcTemplate template;

    /** Creates a new instance of SQLiteResultDAO */
    public SQLiteResultDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }

    private static ParameterizedRowMapper<Result> mapper =
            new ParameterizedRowMapper<Result>() {
        @Override
        public Result mapRow(ResultSet rs, int rowNum) throws SQLException {
            Result r = new Result();

            r.setID(rs.getInt("id"));
            r.setFightID(rs.getInt("fight_id"));
            r.setScores(new FullScore[] { new FullScore(rs.getString("player_score1")), new FullScore(rs.getString("player_score2")) });
            r.setPlayerIDs(new int[] { rs.getInt("player_id1"), rs.getInt("player_id2") });
            r.setDuration(rs.getInt("duration"));
            r.setEventLog(rs.getString("event_log"));
            r.setValid(rs.getBoolean("is_valid"));
            r.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            
            return r;
        }
    };
    
    @Override
    public Collection<Result> findForSession(int sessionID) {
        String sql = "SELECT fight_result.* FROM fight_result, session_has_fight WHERE fight_result.fight_id = session_has_fight.fight_id AND session_has_fight.session_id = ? AND fight_result.is_valid AND session_has_fight.is_valid";
        return template.query(sql, mapper, sessionID);
    }
    
    @Override
    public Collection<Result> findForFight(int fightID) {
        String sql = "SELECT fight_result.* FROM fight_result, fight WHERE fight_result.fight_id = fight.id AND fight.id = ? AND fight_result.is_valid AND fight.is_valid ORDER BY last_updated DESC";
        return template.query(sql, mapper, fightID);
    }

    @Override
    public Collection<Result> findForPlayer(int playerID) {
        String sql = "SELECT fight_result.* FROM fight_result WHERE player_id1 = ? OR player_id2 = ? AND fight_result.is_valid";
        return template.query(sql, mapper, playerID, playerID);
    }

    @Override
    public Collection<Result> findAll() {
        String sql = "SELECT * FROM fight_result WHERE is_valid ORDER BY last_updated";
        return template.query(sql, mapper);
    }

    @Override
    public void add(Result item) {
        String sql = "INSERT INTO fight_result (id, fight_id, player_score1, player_score2, player_id1, player_id2, duration, event_log, is_valid, last_updated) VALUES (:ID, :fightID, :scores[0], :scores[1], :playerIDs[0], :playerIDs[1], :duration, :eventLog, :valid, :timestamp)";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public Result get(Object id) {
        try {
            String sql = "SELECT * FROM fight_result WHERE id = ?";
            return template.queryForObject(sql, mapper, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void update(Result item) {
        throw new RuntimeException("Results should never be updated");
        /*
         * do not uncomment this and expect it to work
         *
        String sql = "UPDATE fight_result SET fight_id=:fightID, player_score1=:scores[0], player_score2=:scores[1], player_id1=:playerIDs[0], player_id2=:playerIDs[1], duration=:duration, event_log=:eventLog, is_valid=:valid, last_updated=:timestamp WHERE id = :ID";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
        */
    }

    @Override
    public void delete(Result item) {
        throw new RuntimeException("Results should never be deleted");
    }

    @Override
    public Class<Result> getDataClass() {
        return Result.class;
    }
    
}
