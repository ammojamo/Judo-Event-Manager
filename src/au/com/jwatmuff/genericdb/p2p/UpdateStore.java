/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.jwatmuff.genericdb.p2p;

import java.io.IOException;

/**
 * An UpdateStore manages how data events are persisted on disk. It also keeps
 * track of a "committed position", so that in the event of a crash, events
 * that have not been committed to the database can be replayed.
 * 
 * Example implementations include writing events out to a file on disk,
 * or writing them to a table in the database.
 * 
 * @author james
 */
public interface UpdateStore {
    Update loadUpdate() throws IOException;
    void writePartialUpdate(Update update) throws IOException;
    void writeCommitedPosition(UpdatePosition position) throws IOException;
    UpdatePosition getCommittedPosition();
}
