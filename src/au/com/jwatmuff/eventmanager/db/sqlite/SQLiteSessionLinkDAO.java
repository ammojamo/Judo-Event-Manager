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
            sl.setValid(rs.getBoolean("is_valid"));
            sl.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            
            return sl;
        }
    };


    @Override
    public Collection<SessionLink> findForSession(int sessionID) {
        String sql = "SELECT * FROM session_link WHERE (session_id = ? OR following_id = ?) AND is_valid";
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
