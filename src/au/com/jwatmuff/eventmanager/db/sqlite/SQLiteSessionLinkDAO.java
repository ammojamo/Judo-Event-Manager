/*
 * SQLiteSessionLinkDAO.java
 *
 * Created on 22 August 2008, 13:18
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.db.sqlite;

import au.com.jwatmuff.eventmanager.db.SessionLinkDAO;
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
public class SQLiteSessionLinkDAO implements SessionLinkDAO {
    private SimpleJdbcTemplate template;
    
    /** Creates a new instance of SQLiteSessionLinkDAO */
    public SQLiteSessionLinkDAO(SimpleJdbcTemplate template) {
        this.template = template;
    }
    
    private static ParameterizedRowMapper<SessionLink> mapper =
            new ParameterizedRowMapper<SessionLink>() {
        @Override
        public SessionLink mapRow(ResultSet rs, int rowNum) throws SQLException {
            SessionLink sl = new SessionLink();

            sl.setID(rs.getInt("id"));
            sl.setSessionID(rs.getInt("session_id"));
            sl.setFollowingID(rs.getInt("following_id"));
            sl.setLinkType(SessionLink.LinkType.valueOf(rs.getString("link_type")));
            sl.setValid("true".equals(rs.getString("is_valid")));
            sl.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            
            return sl;
        }
    };


    @Override
    public Collection<SessionLink> findForSession(int sessionID) {
        String sql = "SELECT * FROM session_link WHERE (session_id = ? OR following_id = ?) AND is_valid = 'true'";
        return template.query(sql, mapper, sessionID, sessionID);
    }
    
    @Override
    public void add(SessionLink item) {
        String sql = "INSERT INTO session_link (id, session_id, following_id, link_type, is_valid, last_updated) VALUES (:ID, :sessionID, :followingID, :linkType, :valid, :timestamp)";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public SessionLink get(Object id) {
        try {
            String sql = "SELECT * FROM session_link WHERE id = ?";
            return template.queryForObject(sql, mapper, id);
        } catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void update(SessionLink item) {
        String sql = "UPDATE session_link SET session_id=:sessionID, following_id=:followingID, link_type=:linkType, is_valid=:valid, last_updated=:timestamp WHERE id=:ID";
        SqlParameterSource params = new BeanPropertySqlParameterSource(item);
        template.update(sql, params);
    }

    @Override
    public void delete(SessionLink item) {
        throw new RuntimeException("Delete not supported");
    }

    @Override
    public Class<SessionLink> getDataClass() {
        return SessionLink.class;
    }
}
