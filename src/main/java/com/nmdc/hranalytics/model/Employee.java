package com.nmdc.hranalytics.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors the per-employee dict records stored in MASTER_DATA in app.py.
 */
public class Employee {
    private String name = "";
    private String department = "";
    private String designation = "";
    private String grade = "";
    private String deposit;          // nullable
    private String gender;           // nullable
    private String skills = "";
    private List<String> skillsList = new ArrayList<>();
    private String category = "";
    private String sourceFile = "";  // mirrors '_file'
    private String empNo = "";
    private String section = "";
    private String qualification = "";
    private String dor = "";  // Date of Retirement (raw string from DOR column)

    public Employee() { }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getDeposit() { return deposit; }
    public void setDeposit(String deposit) { this.deposit = deposit; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public List<String> getSkillsList() { return skillsList; }
    public void setSkillsList(List<String> skillsList) { this.skillsList = skillsList; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public String getEmpNo() { return empNo; }
    public void setEmpNo(String empNo) { this.empNo = empNo; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getDor() { return dor; }
    public void setDor(String dor) { this.dor = dor; }
}
