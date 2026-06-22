package com.nmdc.hranalytics.model;

/**
 * Mirrors a row from Users.xlsx: { empid, usertype, user, password }
 */
public class User {
    private String empId = "";
    private String userType = "";
    private String user = "";
    private String password = "";

    public User() { }

    public User(String empId, String userType, String user, String password) {
        this.empId = empId;
        this.userType = userType;
        this.user = user;
        this.password = password;
    }

    public String getEmpId() { return empId; }
    public void setEmpId(String empId) { this.empId = empId; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
