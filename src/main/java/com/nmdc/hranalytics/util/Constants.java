package com.nmdc.hranalytics.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static constants ported from app.py — department buckets, deposits,
 * and the NMDC Excel column mapping.
 */
public final class Constants {

    private Constants() { }

    public static final List<String> PROD_DEPTS = List.of(
            "Mining", "Services (Mech.)", "Services (Elect.)", "Plant (Mech.)",
            "Plant (Elect.)", "Geology & QC", "Chemical Lab"
    );

    public static final List<String> NONPROD_DEPTS = List.of(
            "Civil", "Materials", "T&S and Environment", "Finance", "Human Resource",
            "M&S", "Commercial", "Contracts Dept.", "Works", "Vigilance", "CSR",
            "ED Sectt.", "GM (P) Sectt.", "IE Dept."
    );

    public static final List<String> OTHER_DEPTS = List.of("School", "Hospital");

    public static final List<String> ALL_DEPTS;
    static {
        java.util.ArrayList<String> all = new java.util.ArrayList<>();
        all.addAll(PROD_DEPTS);
        all.addAll(NONPROD_DEPTS);
        all.addAll(OTHER_DEPTS);
        ALL_DEPTS = List.copyOf(all);
    }

    public static final List<String> DEPOSITS = List.of("14", "11C", "11B");

    public static final Set<String> PROD_SET = Set.copyOf(PROD_DEPTS);
    public static final Set<String> NONPROD_SET = Set.copyOf(NONPROD_DEPTS);
    public static final Set<String> OTHER_SET = Set.copyOf(OTHER_DEPTS);

    /** Virtual "C & IT" sidebar bucket — non-production departments grouped together. */
    public static final List<String> CIT_DEPTS = List.of(
            "Commercial", "Contracts Dept.", "Works", "Vigilance", "CSR", "IE Dept.",
            "ED Sectt.", "GM (P) Sectt.", "M&S", "T&S and Environment"
    );

    /** Column mapping for the specific NMDC test Excel file (exact header names, incl. leading spaces). */
    public static final Map<String, String> NMDC_EXCEL_COLUMN_MAP;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", "  N A M E");
        m.put("department", "Department");
        m.put("designation", "Designation");
        m.put("grade", "GRADE");
        m.put("deposit", " DC");
        m.put("skills", "Original Skill");
        m.put("gender", "Gender");
        m.put("section", "Section");
        m.put("emp_no", "UEC No.");
        m.put("sap_no", "SAP UEC No.");
        m.put("dob", "DOB");
        m.put("dor", "DOR");
        m.put("qualification", "Qualification");
        m.put("category_col", "Prod./Non-Prod./S&H");
        NMDC_EXCEL_COLUMN_MAP = Map.copyOf(m);
    }
}
