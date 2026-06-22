package com.nmdc.hranalytics.service;

import com.nmdc.hranalytics.model.Employee;
import com.nmdc.hranalytics.util.AnalyticsUtil;
import com.nmdc.hranalytics.util.Constants;
import com.nmdc.hranalytics.util.Normalizers;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Mirrors the /api/sections and /api/section-details endpoints in app.py:
 * builds a section x deposit count matrix, and returns employee-level detail rows
 * for a specific department + section + deposit combination.
 */
@Service
public class SectionsService {

    private final DataStore dataStore;

    public SectionsService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Map<String, Object> sections(String deptFilter, String catFilter, String depositFilter) {
        List<Employee> df = dataStore.snapshot();

        df = AnalyticsUtil.applyDeptFilter(df, deptFilter);
        if ("All".equals(deptFilter) && !"All".equals(catFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getCategory(), catFilter));
        }
        if (!"All".equals(depositFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getDeposit(), depositFilter));
        }

        // Build section x deposit matrix, grouped by department + base section.
        record Key(String dept, String section) { }
        Map<Key, Map<String, Integer>> sectionMap = new LinkedHashMap<>();

        for (Employee r : df) {
            Map.Entry<String, String> split = Normalizers.splitSection(r.getSection());
            String sec = split.getKey() != null ? split.getKey() : "(No Section)";
            String dep = r.getDeposit() != null ? r.getDeposit() : "Unknown";
            String dept = r.getDepartment() != null ? r.getDepartment() : "";
            Key key = new Key(dept, sec);
            sectionMap.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge(dep, 1, Integer::sum);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (var e : sectionMap.entrySet()) {
            Key key = e.getKey();
            Map<String, Integer> depCounts = e.getValue();
            int total = depCounts.values().stream().mapToInt(Integer::intValue).sum();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("section", key.section());
            row.put("dept", key.dept());
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String d : Constants.DEPOSITS) counts.put(d, depCounts.getOrDefault(d, 0));
            row.put("counts", counts);
            row.put("unknown", depCounts.getOrDefault("Unknown", 0));
            row.put("total", total);
            rows.add(row);
        }

        rows.sort((a, b) -> {
            int deptCmp = String.valueOf(a.get("dept")).compareTo(String.valueOf(b.get("dept")));
            if (deptCmp != 0) return deptCmp;
            int totalCmp = Integer.compare((int) b.get("total"), (int) a.get("total"));
            if (totalCmp != 0) return totalCmp;
            return String.valueOf(a.get("section")).compareTo(String.valueOf(b.get("section")));
        });

        // Determine which deposits are actually present
        List<Employee> dfFinal = df;
        List<String> depositsPresent = new ArrayList<>();
        for (String d : Constants.DEPOSITS) {
            if (dfFinal.stream().anyMatch(r -> Objects.equals(r.getDeposit(), d))) depositsPresent.add(d);
        }

        boolean hideDeposits = false;
        if ("Non Production".equals(catFilter) || "Others".equals(catFilter)) hideDeposits = true;
        if ("C & IT".equals(deptFilter)) hideDeposits = true;
        if (!"All".equals(deptFilter) && !"C & IT".equals(deptFilter)
                && (Constants.NONPROD_SET.contains(deptFilter) || Constants.OTHER_SET.contains(deptFilter))) {
            hideDeposits = true;
        }

        if (hideDeposits) {
            depositsPresent = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                r.put("counts", new LinkedHashMap<>());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dept", deptFilter);
        result.put("cat", catFilter);
        result.put("deposit", depositFilter);
        result.put("deposits", depositsPresent);
        result.put("rows", rows);
        result.put("total", df.size());
        return result;
    }

    public Map<String, Object> sectionDetails(String deptFilter, String catFilter, String depositFilter, String sectionFilter) {
        List<Employee> df = dataStore.snapshot();

        df = AnalyticsUtil.applyDeptFilter(df, deptFilter);
        if ("All".equals(deptFilter) && !"All".equals(catFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getCategory(), catFilter));
        }
        if (!"All".equals(depositFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getDeposit(), depositFilter));
        }

        String sectionKey = sectionFilter == null ? "all" : sectionFilter.trim().toLowerCase();
        if (!sectionKey.equals("all")) {
            List<Employee> filtered = new ArrayList<>();
            for (Employee r : df) {
                Map.Entry<String, String> split = Normalizers.splitSection(r.getSection());
                String base = split.getKey() != null ? split.getKey().trim().toLowerCase() : "";
                if (base.equals(sectionKey)) filtered.add(r);
            }
            df = filtered;
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Employee r : df) {
            Map.Entry<String, String> split = Normalizers.splitSection(r.getSection());
            String baseSection = split.getKey() != null ? split.getKey() : "";
            String sectionDeposit = split.getValue() != null ? split.getValue() : "";

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", orEmpty(r.getName()));
            row.put("department", orEmpty(r.getDepartment()));
            row.put("section", orEmpty(r.getSection()));
            row.put("base_section", baseSection);
            row.put("section_deposit", sectionDeposit);
            row.put("deposit", orEmpty(r.getDeposit()));
            row.put("designation", orEmpty(r.getDesignation()));
            row.put("grade", orEmpty(r.getGrade()));
            row.put("gender", orEmpty(r.getGender()));
            row.put("skills", !r.getSkillsList().isEmpty() ? String.join(", ", r.getSkillsList()) : orEmpty(r.getSkills()));
            row.put("emp_no", orEmpty(r.getEmpNo()));
            row.put("qualification", orEmpty(r.getQualification()));
            rows.add(row);
        }

        rows.sort(Comparator
                .comparing((Map<String, Object> r) -> String.valueOf(r.get("department")))
                .thenComparing(r -> String.valueOf(r.get("section")))
                .thenComparing(r -> String.valueOf(r.get("name"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dept", deptFilter);
        result.put("cat", catFilter);
        result.put("deposit", depositFilter);
        result.put("section", sectionFilter);
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    private String orEmpty(String s) {
        return s != null ? s : "";
    }
}
