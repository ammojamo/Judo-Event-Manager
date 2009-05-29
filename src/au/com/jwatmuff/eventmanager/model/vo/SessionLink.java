/*
 * SessionLink.java
 *
 * Created on 22 August 2008, 02:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;

/**
 *
 * @author James
 */
public class SessionLink extends DistributableObject<Integer> {
    public enum LinkType {
        DEPENDENT, MAT
    }
    
    private LinkType linkType;
    private int sessionID;
    private int followingID;
    
    /** Creates a new instance of SessionDependency */
    public SessionLink() {
        setID(IDGenerator.generate());
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(LinkType linkType) {
        this.linkType = linkType;
    }

    public int getFollowingID() {
        return followingID;
    }

    public void setFollowingID(int followingID) {
        this.followingID = followingID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public void setSessionID(int sessionID) {
        this.sessionID = sessionID;
    }
}
