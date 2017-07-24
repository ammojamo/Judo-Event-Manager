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

package au.com.jwatmuff.genericdb.p2p;

import au.com.jwatmuff.genericdb.distributed.Clock;
import au.com.jwatmuff.genericdb.distributed.DataEvent;
import au.com.jwatmuff.genericdb.distributed.DataEvent.TransactionStatus;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 * Maintains a list of updates for each of a set of peers
 *
 * @author James
 */
public class Update implements Serializable {
    private static final long serialVersionUID = 8474484567520664967L;

    private static final Logger log = Logger.getLogger(Update.class);

    protected Map<UUID, EventList> updateMap = Collections.synchronizedMap(new HashMap<UUID, EventList>());

    /**
     * Appends events to a given peer's list
     *
     * @param peerID
     * @param events
     */
    public synchronized void addEvents(UUID peerID, List<DataEvent> events) {
        if(!updateMap.containsKey(peerID))
            updateMap.put(peerID, new EventList(events));
        else
            updateMap.get(peerID).addAll(events);
    }

    /**
     * Adds events from the supplied update that are not already present in
     * this update.
     *
     * @param update
     * @return an update object storing just those updates which were added
     */
    public synchronized Update mergeWith(Update update) {
        UpdatePosition position = getPosition();

        for(UUID peerID : update.updateMap.keySet()) {
            if(updateMap.containsKey(peerID))
                updateMap.get(peerID).mergeWith(update.updateMap.get(peerID));
            else
                updateMap.put(peerID, new EventList(update.updateMap.get(peerID)));
        }

        return update.afterPosition(position);
    }

    /**
     * Gets a position object describing the final index of each peers event
     * list
     *
     * @return
     */
    public synchronized UpdatePosition getPosition() {
        UpdatePosition position = new UpdatePosition();

        for(UUID peerID : updateMap.keySet()) {
            EventList list = updateMap.get(peerID);
            position.put(peerID, list.base + list.size());
        }

        return position;
    }


    /**
     * Gets the subset of this update which includes only events after the
     * given update position
     *
     * @param position
     * @return
     */
    public synchronized Update afterPosition(UpdatePosition position) {
        Update update = new Update();

        for(UUID peerID : updateMap.keySet()) {
            EventList list = updateMap.get(peerID);
            EventList afterList;
            if(position.containsKey(peerID))
                afterList = list.after(position.get(peerID));
            else
                afterList = new EventList(list);
            update.updateMap.put(peerID, afterList);
        }

        return update;
    }

    public synchronized Update forPeer(UUID peerID) {
        Update update = new Update();
        if(updateMap.containsKey(peerID)) {
            update.updateMap.put(peerID, updateMap.get(peerID));
        }
        return update;
    }

    public synchronized List<DataEvent> getAllEventsOrdered() {
        return getAllEventsOrdered(null);
    }

    /**
     * Orders all events for all peers by timestamp and returns them in one
     * unified list
     *
     * @return
     */
    public synchronized List<DataEvent> getAllEventsOrdered(Map<DataEvent,UUID> uuidMap) {
        if(updateMap.size() == 1 && uuidMap == null) {
            return updateMap.entrySet().iterator().next().getValue();
        }

        List<DataEvent> events = new ArrayList<DataEvent>();

        /* keep track of our position in each list of the table, starting at
         * the first entry for each list */
        UpdatePosition pos = getBasePosition();

        /* continue as long as we are still keeping track of positions in the
         * table (we remove entries from the position object as the lists
         * finish being processed */
        while(!pos.isEmpty()) {

            /* find the ID of the list in the table which has the earliest
             * unprocessed entry */
            UUID earliestID = null;
            DataEvent earliestEvent = null;
            // copy the list of ids rather than directly iterate over it
            // (so we can modify the map inside the loop)
            Set<UUID> ids = new HashSet<UUID>(pos.keySet());
            for(UUID id : ids) {
                int eventPos = pos.get(id);
                EventList eventList = updateMap.get(id);

                //log.debug(id + " : " + eventPos);

                if(eventPos >= eventList.base + eventList.size()) {
                    //log.debug("Removing id");
                    pos.remove(id);
                    continue;
                }
                DataEvent e = eventList.get(eventPos - eventList.base);

                if(earliestEvent == null || e.getTimestamp().before(earliestEvent.getTimestamp())) {
                    earliestEvent = e;
                    earliestID = id;
                }
            }

            if(earliestEvent == null) continue;

            /* output the event and any subsequent events which are in the same
             * transaction */
            int eventPos = pos.get(earliestID);
            EventList eventList = updateMap.get(earliestID);
            DataEvent event;
            do {
                event = eventList.get(eventPos - eventList.base);
                eventPos++;
                events.add(event);
                if(uuidMap != null) uuidMap.put(event, earliestID);
            } while((event.getTransactionStatus() == TransactionStatus.BEGIN) ||
                    (event.getTransactionStatus() == TransactionStatus.CURRENT));

            /* if we have reached the end of the list, remove it's index from
             * the position object */
            if(eventPos == eventList.base + eventList.size())
                pos.remove(earliestID);
            else
                pos.put(earliestID, eventPos);
        }

        return events;
    }

    /**
     * Gets a position object describing the final index of each peers event
     * list
     *
     * @return
     */
    private synchronized UpdatePosition getBasePosition() {
        UpdatePosition position = new UpdatePosition();

        for(UUID peerID : updateMap.keySet()) {
            EventList list = updateMap.get(peerID);
            position.put(peerID, list.base);
        }

        return position;
    }

    /**
     * Updates the clock using timestamps all events we ever received
     */
    public void updateClock() {
        for(EventList list : updateMap.values())
            for(DataEvent event : list)
                Clock.setEarliestTime(event.getTimestamp());
    }

    /**
     * For debugging purposes
     *
     * @param events
     * @return
     */
    private static boolean verifyTransactionStates(List<DataEvent> events) {
        boolean inTransaction = false;
        for(DataEvent event : events) {
            switch(event.getTransactionStatus()) {
                case BEGIN:
                case NONE:
                    if(inTransaction) {
                        return false;
                    }
                    break;
                case CURRENT:
                case END:
                    if(!inTransaction) {
                        return false;
                    }
                    break;
            }
            switch(event.getTransactionStatus()) {
                case BEGIN:
                case CURRENT:
                    inTransaction = true;
                    break;
                case NONE:
                case END:
                    inTransaction = false;
                    break;
            }
        }
        return true;
    }

    public void verifyTransactionStates() {
        for(Map.Entry<UUID,EventList> entry : updateMap.entrySet()) {
            if(!verifyTransactionStates(entry.getValue())) {
                log.warn("Invalid transaction state detected for peer: " + entry.getKey());
            }
        }

        if(!verifyTransactionStates(getAllEventsOrdered())) {
            log.warn("Failed to validate transaction states for all ordered events");
        }
    }

    /**
     * @return the total number of events in this update
     */
    public int size() {
        int size = 0;
        for(EventList list : updateMap.values()) {
            size += list.size();
        }
        return size;
    }

    public String toString() {
        List<String> strings = new ArrayList<>();
        for(UUID id : updateMap.keySet()) {
            strings.add(id.toString().substring(0, 8) + ": " + updateMap.get(id));
        }
        return "Update( " + String.join(", ", strings) + " )";
    }

    private String makeWidth(String s, int n) {
        if(s == null)
            s = "";
        if(s.length() > n)
            return s.substring(0,n);
        while(s.length() < n) {
            s = s + " ";
        }
        return s;
    }

    public synchronized String dumpTable() {
        final int COLWIDTH = 35;

        DecimalFormat df = new DecimalFormat("0000");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        int cols = updateMap.keySet().size();

        if(cols == 0) return "empty\n";

        int rows = 0;
        for(EventList el : updateMap.values()) {
            rows = Math.max(rows, el.size());
        }

        rows = rows*2 + 2; /* for title */

        String[][] cells = new String[cols][rows];
        int col = 0;
        int row;

        for(UUID id : updateMap.keySet()) {
            row = 0;

            cells[col][row++] = id.toString();
            cells[col][row++] = "-----------------------------------";

            EventList el = updateMap.get(id);
            int index = el.base;
            for(DataEvent event : updateMap.get(id)) {
                cells[col][row++] = (df.format(index++) + " " + sdf.format(event.getTimestamp()));
                cells[col][row++] = (event.toString());
            }
            col++;
        }

        StringBuilder sb = new StringBuilder();

        for(row = 0; row < rows; row++) {
            for(col = 0; col < cols; col++) {
                if(col != 0)
                    sb.append("|");

                String s = makeWidth(cells[col][row], COLWIDTH);
                sb.append(s);
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
/* Manages a list of events with an arbitrary starting index */
class EventList extends ArrayList<DataEvent> implements Serializable {
    public int base = 0;

    public EventList() { super(); }

    public EventList(EventList list) {
        super(list);
        base = list.base;
    }

    public EventList(List<DataEvent> list) {
        super(list);
    }

    public void mergeWith(EventList list) {
        if((base + size()) < list.base)
            throw new RuntimeException("Merging lists failed (" + base + ", " + size() + ") + (" + list.base + ", " + list.size() + ")");

        if((base + size()) < (list.base + list.size()))
            addAll(list.subList( (base + size() - list.base), list.size()));
    }

    public EventList after(int index) {
        if(index < base)
            throw new IllegalArgumentException("Index out of bounds. [index: " + index + "] < [base:" + base + "]");

        EventList list = new EventList();

        /*
         * This could be a greater than or equal check, but would result in
         * slightly different behaviour because list.base is not always set.
         * I think the base should always be set, however I am worried that
         * something might depend on this strange behaviour. Should be fixed but
         * requires extensive testing.
        */
        if(index > (base + size())) {
            return list;
        }

        list.base = index;
        list.addAll(subList(index-base, size()));
        return list;
    }

    public String toString() {
        return "[" + base + ".." + (base + size()) + "]";
    }
}
