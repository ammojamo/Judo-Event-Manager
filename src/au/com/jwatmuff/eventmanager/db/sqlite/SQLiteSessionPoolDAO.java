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
            sp.setValid(rs.getBoolean("is_valid"));
            sp.setTimestamp(new Timestamp(rs.getDate("last_updated").getTime()));
            return sp;
        }
    };  
    
    @Override
    public Collection<SessionPool> findAll() {
        String sql = "SELECT * FROM session_has_pool WHERE is_valid";
        return template.query(sql, mapper);
    }
    
    @Override
    public Collection<SessionPool> findForSession(int sessionID) {
        String sql = "SELECT * FROM session_has_pool WHERE is_valid AND session_id = ?";
        return template.query(sql, mapper, sessionID);
    }

    @Override
    public Collection<SessionPool> findForPool(int poolID) {
        String sql = "SELECT * FROM session_has_pool WHERE is_valid AND pool_id = ?";
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
