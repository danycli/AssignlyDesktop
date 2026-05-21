package com.assignly.model;

public class User {
    private final long id;
    private final String registrationNo;
    private final String encryptedPassword;
    private final boolean rememberMe;

    public User(long id, String registrationNo, String encryptedPassword, boolean rememberMe) {
        this.id = id;
        this.registrationNo = registrationNo;
        this.encryptedPassword = encryptedPassword;
        this.rememberMe = rememberMe;
    }

    public long getId() {
        return id;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }
}
