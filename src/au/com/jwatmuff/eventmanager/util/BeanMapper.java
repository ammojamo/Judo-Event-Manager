/*
 * BeanMapper.java
 *
 * Created on 2 May 2008, 02:29
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.util;

import java.util.Map;

/**
 *
 * @author James
 */
public interface BeanMapper<T> {
    Map<String, Object> mapBean(T bean);
}
