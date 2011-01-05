package au.com.jwatmuff.eventmanager.util;

import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Composite class for SqlParameterSource
 *
 * This allows us to combine bean properties with derived values by combining
 * a BeanPropertySqlParameterSource and a MapSqlParameterSource.
 *
 * @author James
 */
public class CompositeSqlParameterSource implements SqlParameterSource {
    private List<SqlParameterSource> sources;

    public CompositeSqlParameterSource(SqlParameterSource... sources) {
        this.sources = Arrays.asList(sources);
    }

    private SqlParameterSource getSourceForValue(String string) {
        for(SqlParameterSource source : sources)
            if(source.hasValue(string)) return source;
        return null;

    }

    public boolean hasValue(String property) {
        return getSourceForValue(property) != null;
    }

    public Object getValue(String property) throws IllegalArgumentException {
        SqlParameterSource source = getSourceForValue(property);

        if(source == null)
            throw new IllegalArgumentException("No property called '" + property +'"');
        else
            return source.getValue(property);
    }

    public int getSqlType(String property) {
        SqlParameterSource source = getSourceForValue(property);
        return (source == null) ? TYPE_UNKNOWN : source.getSqlType(property);
    }

    public String getTypeName(String string) {
        SqlParameterSource source = getSourceForValue(string);
        return (source == null) ? null : source.getTypeName(string);
    }
}
