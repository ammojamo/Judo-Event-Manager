/*
 * Player.java
 *
 * Created on 25 February 2008, 16:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.eventmanager.model.vo;

import au.com.jwatmuff.eventmanager.util.IDGenerator;
import au.com.jwatmuff.genericdb.distributed.DistributableObject;
import java.io.Serializable;

/**
 *
 * @author James
 */
public class PlayerDetails extends DistributableObject<Integer> implements Serializable {    
    private String homeNumber;
    private String workNumber;
    private String mobileNumber;
    
    private String street;
    private String city;
    private String postcode; 
    private String state;
    private String email;

    private String emergencyName;
    private String emergencyPhone;
    private String emergencyMobile;

    private String medicalConditions;
    private String medicalInfo;
    private String injuryInfo;
        
    /** Creates a new instance of Player */
    public PlayerDetails() {
        setID(IDGenerator.generate());
    }

    public String getHomeNumber() {
        return homeNumber;
    }

    public void setHomeNumber(String homeNumber) {
        this.homeNumber = homeNumber;
    }

    public String getWorkNumber() {
        return workNumber;
    }

    public void setWorkNumber(String workNumber) {
        this.workNumber = workNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmergencyName() {
        return emergencyName;
    }

    public void setEmergencyName(String emergencyName) {
        this.emergencyName = emergencyName;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public String getEmergencyMobile() {
        return emergencyMobile;
    }

    public void setEmergencyMobile(String emergencyMobile) {
        this.emergencyMobile = emergencyMobile;
    }

    public String getMedicalInfo() {
        return medicalInfo;
    }

    public void setMedicalInfo(String medicalInfo) {
        this.medicalInfo = medicalInfo;
    }

    public String getInjuryInfo() {
        return injuryInfo;
    }

    public void setInjuryInfo(String injuryInfo) {
        this.injuryInfo = injuryInfo;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getMedicalConditions() {
        return medicalConditions;
    }

    public void setMedicalConditions(String medicalConditions) {
        this.medicalConditions = medicalConditions;
    }    
}
