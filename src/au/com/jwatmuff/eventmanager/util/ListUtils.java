package au.com.jwatmuff.eventmanager.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author James
 */
public class ListUtils {
    public static List<Integer> getIntegerSequence(int from, int to) {
        assert(from <= to);
        List<Integer> list = new ArrayList<Integer>(to - from + 1);
        for(int i = from; i <= to; i++) {
            list.add(i);
        }
        return list;
    }
}
