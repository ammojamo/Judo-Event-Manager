/*
 * SQLiteSessionDAO.java
 *
 * Created on 19 August 2008, 00:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.eventmanager.db.SessionDAO;
import au.com.jwatmuff.eventmanager.model.vo.Session;
import au.com.jwatmuff.eventmanager.model.vo.SessionLink;
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
public class SQLiteSessionDAO implements SessionDAO {
    private SimpleJdbcTemplate template;
    
    /** Creates a new instance of SQLiteSessionDAO */
    public SQLiteSessionDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }

    private static ParameterizedRowMapper<Session> mapper =
            new ParameterizedRowMapper<Session>() {
        @Override
        public Session mapRow(ResultSet rs, int rowNum) throws SQLException {
            Session s = new Session();

            s.setID(rs.getInt("id"));
            s.setName(rs.getString("name"));
            s.setType(Session.SessionType.valueOf(rs.getString("session_type")));
            s.setMat(rs.getString("mat"));
            s.setLockedStatus(Session.LockedStatus.valueOf(rs.getString("locked_status")));
            s.setValid(rs.getString("is_valid").equals("true"));
            s.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            return s;
        }
    };    

    @Override
    public Collection<Session> findAll() {
        String sql = "SELECT * FROM session WHERE is_valid = 'true'";
        return template.query(sql, mapper);
    }
    
    @Override
    public Collection<Session> findFollowing(int sessionID, SessionLink.LinkType linkType) {
        String sql = "SELECT session.* FROM session, session_link WHERE session_link.session_id = ? AND session_link.link_type = ? AND session.id = session_link.following_id AND session.is_valid = 'true' AND session_link.is_valid = 'true'";
        return template.query(sql, mapper, sessionID, linkType);
    }

    @Override
    public Collection<Session> findPreceding(int sessionID, SessionLink.LinkType linkType) {
        String sql = "SELECT session.* FROM session, session_link WHERE session_link.following_id = ? AND session_link.link_type = ? AND session.id = session_link.session_id AND session.is_valid = 'true' AND session_link.is_valid = 'true'";
        return template.query(sql, mapper, sessionID, linkType);
    }
    
    @Override
    public Collection<Session> findAllMats() {
        String sql = "SELECT * FROM session WHERE session_type='MAT' AND is_valid = 'true'";
        return template.query(sql, mapper);
    }

    @Override
    public Collection<Session> findAllNormal() {
        String sql = "SELECT * FROM session WHERE session_type='NORMAL' AND is_valid = 'true'";
        return template.query(sql, mapper);
    }
    
    @Override
    public Collection<Session> findWithLockedStatus(Session.LockedStatus lockedStatus) {
        String sql = "SELECT * FROM session WHERE locked_status = ? AND is_valid = 'true'";
        return template.query(sql, mapper, lockedStatus);
    }

    @Override
    public Session findForFight(int fightID) {
        String sql =
                "SELECT session.* FROM session, session_has_fight " +
                "WHERE session.id = session_has_fight.session_id " +
                "AND session_has_fight.fight_id = ? " +
                "AND session.is_valid = 'true' " +
                "AND session_has_fight.is_valid = 'true'";
        try {
            return template.queryForObject(sql, mapper, fightID);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Collection<Session> findForPool(int poolID) {
        String sql =
                "SELECT session.* FROM session, session_has_pool " +
                "WHERE session.id = session_has_pool.session_id " +
                "AND session_has_pool.pool_id = ? " +
                "AND session.is_valid = 'true' " +
                "AND session_has_pool.is_valid = 'true'";
        return template.query(sql, mapper, poolID);
    }

    @Override
    public void add(Session item) {
        String sql = "INSERT INTO session (id, name, session_type, mat, locked_status, is_valid, last_updated) VALUES (:ID, :name, :type, :mat, :lockedStatus, :valid, :timestamp);";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public Session get(Object id) {
        String sql = "SELECT * FROM session WHERE id = ?";
        try {
            return template.queryForObject(sql, mapper, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void update(Session item) {
        String sql = "UPDATE session SET name=:name, session_type=:type, mat=:mat, locked_status=:lockedStatus, is_valid=:valid, last_updated=:timestamp WHERE id=:ID;";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public void delete(Session item) {
        throw new RuntimeException("Delete not supported");
    }

    @Override
    public Class<Session> getDataClass() {
        return Session.class;
    }
}
