package com.nmdc.hranalytics.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mirrors the REQUIRED_SUMMARY dict structure built by parse_summary_workbook() in app.py.
 */
public class RequiredSummary {

    public static class CurrentRequired {
        public int current = 0;
        public int required = 0;

        public CurrentRequired() { }
        public CurrentRequired(int current, int required) {
            this.current = current;
            this.required = required;
        }
    }

    public static class GradeEntry {
        public int current = 0;
        public int required = 0;
        public Map<String, CurrentRequired> departments = new LinkedHashMap<>();
    }

    private boolean enabled = false;
    private int totalCurrent = 0;
    private int totalRequired = 0;
    private Map<String, CurrentRequired> categories = new LinkedHashMap<>();
    private Map<String, CurrentRequired> departments = new LinkedHashMap<>();
    private Map<String, GradeEntry> grades = new LinkedHashMap<>();

    public RequiredSummary() {
        categories.put("Production", new CurrentRequired());
        categories.put("Non Production", new CurrentRequired());
        categories.put("Others", new CurrentRequired());
        categories.put("Unknown", new CurrentRequired());
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getTotalCurrent() { return totalCurrent; }
    public void setTotalCurrent(int totalCurrent) { this.totalCurrent = totalCurrent; }

    public int getTotalRequired() { return totalRequired; }
    public void setTotalRequired(int totalRequired) { this.totalRequired = totalRequired; }

    public Map<String, CurrentRequired> getCategories() { return categories; }
    public Map<String, CurrentRequired> getDepartments() { return departments; }
    public Map<String, GradeEntry> getGrades() { return grades; }
}
