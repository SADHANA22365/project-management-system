package com.company.pms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 500)
    private String bio;

    @Column(length = 100)
    private String location;

    protected UserProfile() {
    }

    public UserProfile(String phoneNumber, String bio, String location) {
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.location = location;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}