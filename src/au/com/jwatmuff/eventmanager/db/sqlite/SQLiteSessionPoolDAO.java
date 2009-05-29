/*
 * SQLiteSessionPoolDAO.java
 *
 * Created on 19 August 2008, 04:24
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.eventmanager.db.SessionPoolDAO;
import au.com.jwatmuff.eventmanager.model.vo.SessionPool;
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
public class SQLiteSessionPoolDAO implements SessionPoolDAO {
    private SimpleJdbcTemplate template;
    
    /** Creates a new instance of SQLiteSessionPoolDAO */
    public SQLiteSessionPoolDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }

    private static ParameterizedRowMapper<SessionPool> mapper =
            new ParameterizedRowMapper<SessionPool>() {
        @Override
        public SessionPool mapRow(ResultSet rs, int rowNum) throws SQLException {
            SessionPool sp = new SessionPool();

            sp.setID(new SessionPool.Key(rs.getInt("session_id"), rs.getInt("pool_id")));
            sp.setValid(rs.getString("is_valid").equals("true"));
            sp.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            return sp;
        }
    };  
    
    @Override
    public Collection<SessionPool> findAll() {
        String sql = "SELECT * FROM session_has_pool WHERE is_valid = 'true'";
        return template.query(sql, mapper);
    }
    
    @Override
    public Collection<SessionPool> findForSession(int sessionID) {
        String sql = "SELECT * FROM session_has_pool WHERE is_valid = 'true' AND session_id = ?";
        return template.query(sql, mapper, sessionID);
    }

    @Override
    public Collection<SessionPool> findForPool(int poolID) {
        String sql = "SELECT * FROM session_has_pool WHERE is_valid = 'true' AND pool_id = ?";
        return template.query(sql, mapper, poolID);
    }
    
    @Override
    public void add(SessionPool item) {
        String sql = "INSERT INTO session_has_pool (session_id, pool_id, is_valid, last_updated) VALUES (:sessionID, :poolID, :valid, :timestamp);";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public SessionPool get(Object id) {
        assert(id instanceof SessionPool.Key);
        try {
            SessionPool.Key key = (SessionPool.Key)id;
            String sql = "SELECT * FROM session_has_pool WHERE session_id = ? AND pool_id = ?";
            return template.queryForObject(sql, mapper, key.sessionID, key.poolID);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void update(SessionPool item) {
        String sql = "UPDATE session_has_pool SET is_valid=:valid, last_updated=:timestamp WHERE session_id = :sessionID AND pool_id = :poolID";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public void delete(SessionPool item) {
        throw new RuntimeException("Delete not supported");
    }

    @Override
    public Class<SessionPool> getDataClass() {
        return SessionPool.class;
    }
}
