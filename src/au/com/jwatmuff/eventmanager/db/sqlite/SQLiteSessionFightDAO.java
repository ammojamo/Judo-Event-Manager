/*
 * SQLiteSessionFightDAO.java
 *
 * Created on 20 August 2008, 15:24
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.eventmanager.db.SessionFightDAO;
import au.com.jwatmuff.eventmanager.model.vo.SessionFight;
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
public class SQLiteSessionFightDAO implements SessionFightDAO {
    private SimpleJdbcTemplate template;

    /** Creates a new instance of SQLiteSessionFightDAO */
    public SQLiteSessionFightDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }
    
    private static ParameterizedRowMapper<SessionFight> mapper =
            new ParameterizedRowMapper<SessionFight>() {
        @Override
        public SessionFight mapRow(ResultSet rs, int rowNum) throws SQLException {
            SessionFight sf = new SessionFight();

            sf.setID(new SessionFight.Key(rs.getInt("session_id"), rs.getInt("fight_id")));
            sf.setPosition(rs.getInt("pos_in_session"));
            sf.setValid(rs.getString("is_valid").equals("true"));
            sf.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            return sf;
        }
    };  

    @Override
    public Collection<SessionFight> findForSession(int sessionID) {
        String  sql = "SELECT * FROM session_has_fight WHERE is_valid = 'true' AND session_id = ? ORDER BY pos_in_session";
        return template.query(sql, mapper, sessionID);
    }
    
    @Override
    public SessionFight findForFight(int fightID) {
        String  sql = "SELECT session_has_fight.* FROM session_has_fight, session WHERE session_has_fight.is_valid = 'true' AND session.id = session_has_fight.session_id AND session.is_valid = 'true' AND fight_id = ?";
        return template.queryForObject(sql, mapper, fightID);
    }

    @Override
    public void add(SessionFight item) {
        String sql = "INSERT INTO session_has_fight (session_id, fight_id, pos_in_session, is_valid, last_updated) VALUES (:sessionID, :fightID, :position, :valid, :timestamp);";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public SessionFight get(Object id) {
        assert(id instanceof SessionFight.Key);
        try {
            SessionFight.Key key = (SessionFight.Key)id;
            
            String sql = "SELECT * FROM session_has_fight WHERE session_id = ? AND fight_id = ?";
            return template.queryForObject(sql, mapper, key.sessionID, key.fightID);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void update(SessionFight item) {
        String sql = "UPDATE session_has_fight SET pos_in_session=:position, is_valid=:valid, last_updated=:timestamp WHERE session_id = :sessionID AND fight_id = :fightID";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public void delete(SessionFight item) {
        throw new RuntimeException("Delete not supported");
    }

    @Override
    public Class<SessionFight> getDataClass() {
        return SessionFight.class;
    }
}
