/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.p2p;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author James
 */
public class UpdateSyncInfo implements Serializable {
    public enum Status {
        OK,
        DATABASE_ID_MISMATCH,
        AUTH_FAILED,
        BAD_INFO}

    public Status status = Status.OK;
    public UUID senderID;
    public UUID databaseID;
    public byte[] authPrefix, authHash;
    public UpdatePosition position;
    public Update update;
    public Date senderTime;
}
