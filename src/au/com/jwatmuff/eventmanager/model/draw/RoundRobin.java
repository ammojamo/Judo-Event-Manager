package au.com.jwatmuff.eventmanager.model.draw;

import au.com.jwatmuff.eventmanager.model.vo.Fight;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 *
 * @author James
 */
public class RoundRobin implements DrawSystem {
    private int size;

    private class Pair {
        Integer p1, p2;

        public Pair(int p1, int p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    public RoundRobin(int size) {
        if(size < 2)
            throw new IllegalArgumentException("A round robin fight draw requires two or more players (" + size + " players specified)");
        this.size = size;
    }

    @Override
    public List<Fight> getFights() {
        List<Pair> fights = new ArrayList<Pair>();

        Queue<Integer> players = new LinkedList<Integer>();
        for(int i = 0; i < size; i++) players.add(i+1);

        int numFights = 0;
        for(int i = 0; i < size; i++) {
            numFights += size - i - 1;
        }
        //for(int i = 0; i < numFights; i++)
        //int a = players.remove();
        //int b = players.remove();

        return null;
    }
}