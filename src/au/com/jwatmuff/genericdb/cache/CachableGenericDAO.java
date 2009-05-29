/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.cache;

import au.com.jwatmuff.genericdb.GenericDAO;
import au.com.jwatmuff.genericdb.distributed.DataEventListener;
import java.io.Serializable;

/**
 *
 * @author James
 */

public interface CachableGenericDAO extends GenericDAO, DataEventListener {
    void addedToCache(QueryInfo query, Serializable result);
}
