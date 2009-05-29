/*
 * SQLiteFightDAO.java
 *
 * Created on 14 August 2008, 21:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.eventmanager.db.FightDAO;
import au.com.jwatmuff.eventmanager.model.vo.Fight;
import au.com.jwatmuff.genericdb.distributed.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 *
 * @author James
 */
public class SQLiteFightDAO implements FightDAO {
    private final SimpleJdbcTemplate template;
    
    /** Creates a new instance of SQLiteFightDAO */
    public SQLiteFightDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }
    
    private static final String
            ID_FIELD = "id",
            POOL_ID_FIELD = "pool_id",
            PLAYER_CODE1_FIELD = "player_code1",
            PLAYER_CODE2_FIELD = "player_code2",
            POSITION_FIELD = "pos_in_pool",
            VALID_FIELD = "is_valid",
            TIMESTAMP_FIELD = "last_updated";

    private static final ParameterizedRowMapper<Fight> mapper =
            new ParameterizedRowMapper<Fight>() {
        @Override
        public Fight mapRow(ResultSet rs, int rowNum) throws SQLException {
            boolean locked = rs.getString("is_locked").equals("true");
            Fight f = new Fight(locked);

            f.setID(rs.getInt(ID_FIELD));
            f.setPoolID(rs.getInt(POOL_ID_FIELD));
            f.setPlayerCodes(new String[] { rs.getString(PLAYER_CODE1_FIELD), rs.getString(PLAYER_CODE2_FIELD) });
            f.setPosition(rs.getInt(POSITION_FIELD));
            f.setPoints(new int[] {
                rs.getInt("i_points"),
                rs.getInt("w_points"),
                rs.getInt("y_points"),
                rs.getInt("k_points"),
                rs.getInt("l_points")
            });
            f.setValid(rs.getString(VALID_FIELD).equals("true"));
            f.setTimestamp(new Timestamp(rs.getDate(TIMESTAMP_FIELD).getTime()));
            return f;
        }
    };
    
    @Override
    public Collection<Fight> findForPool(int poolID) {
        final String sql = "SELECT * FROM fight WHERE is_valid = 'true' AND pool_id = ? ORDER BY pos_in_pool";
        return template.query(sql, mapper, poolID);
    }
    
    @Override
    public Collection<Fight> findUnplayedInSession(int sessionID) {
        final String sql = "SELECT fight.* FROM fight, session_has_fight WHERE session_has_fight.session_id = ? AND fight.id = session_has_fight.fight_id AND fight.is_valid = 'true' AND session_has_fight.is_valid = 'true' AND fight.id NOT IN (SELECT fight_id FROM fight_result WHERE is_valid = 'true') ORDER BY pos_in_session";
        return template.query(sql, mapper, sessionID);
    }

    @Override
    public Collection<Fight> findWithResult() {
        final String sql =
                "SELECT fight.* FROM fight " +
                "WHERE fight.id IN (SELECT DISTINCT fight_id FROM fight_result) " +
                "AND fight.is_valid = 'true'";
        return template.query(sql, mapper);
    }

    @Override
    public List<Fight> findInLockedSession() {
        final String sql =
                "SELECT fight.* FROM fight, session_fight, session " +
                "WHERE fight.id = session_fight.fight_id " +
                "AND session.id = session_fight.session_id " +
                "AND session.locked_status = 'FIGHTS_LOCKED' " +
                "AND fight.is_valid = 'true' " +
                "AND session_fight.is_valid = 'true' " +
                "AND session.is_valid = 'true'";
        return template.query(sql, mapper);
    }

    @Override
    public void add(Fight item) {
        final String sql = "INSERT INTO fight (" +
                      "id, pool_id, " +
                      "player_code1, player_code2, " +
                      "pos_in_pool, " +
                      "i_points, w_points, y_points, k_points, l_points, " +
                      "is_locked, is_valid, last_updated " + 
                      ") VALUES ( " +
                      ":ID, :poolID, " +
                      ":playerCodes[0], :playerCodes[1], " +
                      ":position, " +
                      ":points[0], :points[1], :points[2], :points[3], :points[4], " +
                      ":locked, :valid, :timestamp );";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public Fight get(Object id) {
        try {
            final String sql = "SELECT * FROM fight WHERE id = ?";
            return template.queryForObject(sql, mapper, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void update(Fight item) {
        final String sql = "UPDATE fight SET " +
                      "player_code1=:playerCodes[0], " +
                      "player_code2=:playerCodes[1], " +
                      "pos_in_pool=:position, " +
                      "i_points=:points[0]" +
                      "w_points=:points[1]" +
                      "y_points=:points[2]" +
                      "k_points=:points[3]" +
                      "l_points=:points[4]" +
                      "is_locked=:locked, " +
                      "is_valid=:valid, " +
                      "last_updated=:timestamp " +
                      "WHERE id=:ID;";        
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public void delete(Fight item) {
        throw new RuntimeException("Delete not allowed");
    }

    @Override
    public Class<Fight> getDataClass() {
        return Fight.class;
    }
    
}
