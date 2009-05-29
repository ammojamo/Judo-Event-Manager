package au.com.jwatmuff.genericdb.cache;

import java.util.Arrays;

public class QueryInfo {

    private Object[] args;
    private Class c;
    private String name;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueryInfo other = (QueryInfo) obj;
        if (this.args != other.args && (this.args == null || !Arrays.deepEquals(this.args, other.args))) {
            return false;
        }
        if (this.c != other.c && (this.c == null || !this.c.equals(other.c))) {
            return false;
        }
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.args != null ? Arrays.deepHashCode(this.args) : 0);
        hash = 13 * hash + (this.c != null ? this.c.hashCode() : 0);
        hash = 13 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    public QueryInfo(Class aClass, String query, Object... args) {
        super();
        this.c = aClass;
        this.name = query;
        this.args = args;
    }
}
