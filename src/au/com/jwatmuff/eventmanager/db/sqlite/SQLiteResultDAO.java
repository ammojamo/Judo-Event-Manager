/*
 * SQLiteResultDAO.java
 *
 * Created on 28 August 2008, 03:34
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.eventmanager.db.ResultDAO;
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
            r.setPlayerScores(new int[] { rs.getInt("player_score1"), rs.getInt("player_score2") });
            r.setPlayerIDs(new int[] { rs.getInt("player_id1"), rs.getInt("player_id2") });
            r.setEventLog(rs.getString("event_log"));
            r.setValid(rs.getString("is_valid").equals("true"));
            r.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            
            return r;
        }
    };
    
    @Override
    public Collection<Result> findForSession(int sessionID) {
        String sql = "SELECT fight_result.* FROM fight_result, session_has_fight WHERE fight_result.fight_id = session_has_fight.fight_id AND session_has_fight.session_id = ? AND fight_result.is_valid = 'true' AND session_has_fight.is_valid = 'true'";
        return template.query(sql, mapper, sessionID);
    }
    
    @Override
    public Collection<Result> findForFight(int fightID) {
        String sql = "SELECT fight_result.* FROM fight_result, fight WHERE fight_result.fight_id = fight.id AND fight.id = ? AND fight_result.is_valid = 'true' AND fight.is_valid = 'true' ORDER BY last_updated DESC";
        return template.query(sql, mapper, fightID);
    }

    @Override
    public Collection<Result> findForPlayer(int playerID) {
        String sql = "SELECT fight_result.* FROM fight_result WHERE player_id1 = ? OR player_id2 = ? AND fight_result.is_valid = 'true'";
        return template.query(sql, mapper, playerID, playerID);
    }

    @Override
    public Collection<Result> findAll() {
        String sql = "SELECT * FROM fight_result WHERE is_valid = 'true' ORDER BY last_updated";
        return template.query(sql, mapper);
    }

    @Override
    public void add(Result item) {
        String sql = "INSERT INTO fight_result (id, fight_id, player_score1, player_score2, player_id1, player_id2, event_log, is_valid, last_updated) VALUES (:ID, :fightID, :playerScores[0], :playerScores[1], :playerIDs[0], :playerIDs[1], :eventLog, :valid, :timestamp)";
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
        String sql = "UPDATE fight_result SET fight_id=:fightID, player_score1=:playerScores[0], player_score2=:playerScores[1], player_id1=:playerIDs[0], player_id2=:playerIDs[1], event_log=:eventLog, is_valid=:valid, last_updated=:timestamp WHERE id = ?";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public void delete(Result item) {
        throw new RuntimeException("Delete not supported");
    }

    @Override
    public Class<Result> getDataClass() {
        return Result.class;
    }
    
}
