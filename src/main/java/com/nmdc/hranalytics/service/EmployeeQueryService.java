package com.nmdc.hranalytics.service;

import com.nmdc.hranalytics.model.Employee;
import com.nmdc.hranalytics.util.AnalyticsUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmployeeQueryService {

    private final DataStore dataStore;

    public EmployeeQueryService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /** Mirrors /api/departments. */
    public Map<String, Object> departments(String categoryFilter) {
        List<Employee> df = dataStore.snapshot();

        if ("Production".equals(categoryFilter)) {
            df = AnalyticsUtil.filter(df, r -> "Production".equals(r.getCategory()));
        } else if ("Non Production".equals(categoryFilter)) {
            df = AnalyticsUtil.filter(df, r -> "Non Production".equals(r.getCategory()));
        } else if ("Others".equals(categoryFilter)) {
            df = AnalyticsUtil.filter(df, r -> "Others".equals(r.getCategory()));
        }

        Map<String, Integer> deptCounts = new LinkedHashMap<>();
        for (Employee r : df) {
            String dept = r.getDepartment() != null && !r.getDepartment().isEmpty() ? r.getDepartment() : "Unknown";
            deptCounts.merge(dept, 1, Integer::sum);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(deptCounts.entrySet());
        entries.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            if (cmp != 0) return cmp;
            return a.getKey().compareTo(b.getKey());
        });
        for (var e : entries) {
            rows.add(Map.of("department", e.getKey(), "employees", e.getValue()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("category", categoryFilter);
        result.put("rows", rows);
        result.put("total", df.size());
        return result;
    }

    /** Mirrors /api/summary. */
    public Map<String, Object> summary() {
        List<Employee> all = dataStore.snapshot();
        Set<String> files = new HashSet<>();
        for (Employee r : all) files.add(r.getSourceFile());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("files", all.isEmpty() ? 0 : files.size());
        result.put("summary_enabled", dataStore.isSummaryEnabled());
        return result;
    }

    /** Mirrors /api/employees-by-attribute. */
    public Map<String, Object> employeesByAttribute(String attrType, String attrValue, String catFilter, String deptFilter, String sectionFilter) {
        List<Employee> df = dataStore.snapshot();

        if (!"All".equals(catFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getCategory(), catFilter));
        }
        df = AnalyticsUtil.applyDeptFilter(df, deptFilter);

        if (attrValue != null && !attrValue.isEmpty() && !attrValue.equalsIgnoreCase("all")) {
            switch (attrType) {
                case "deposit" -> df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getDeposit(), attrValue));
                case "skill" -> {
                    String av = attrValue.trim().toLowerCase();
                    df = AnalyticsUtil.filter(df, r -> r.getSkillsList().stream()
                            .anyMatch(s -> av.equals(s == null ? "" : s.trim().toLowerCase())));
                    if (sectionFilter != null && !sectionFilter.isEmpty() && !sectionFilter.equalsIgnoreCase("all")) {
                        String finalSectionFilter = sectionFilter;
                        df = AnalyticsUtil.filter(df, r -> finalSectionFilter.equals(
                                r.getSection() != null ? r.getSection().trim() : ""));
                    }
                }
                case "gender" -> df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getGender(), attrValue));
                case "grade" -> df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getGrade(), attrValue));
                case "department" -> df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getDepartment(), attrValue));
                case "desig" -> df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getDesignation(), attrValue));
                default -> { /* no-op, mirrors Python's silent fallthrough */ }
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Employee r : df) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", dashIfEmpty(r.getName()));
            row.put("department", dashIfEmpty(r.getDepartment()));
            row.put("designation", dashIfEmpty(r.getDesignation()));
            row.put("grade", dashIfEmpty(r.getGrade()));
            row.put("deposit", dashIfEmpty(r.getDeposit()));
            row.put("gender", dashIfEmpty(r.getGender()));
            row.put("skills", !r.getSkillsList().isEmpty() ? String.join(", ", r.getSkillsList()) : dashIfEmpty(r.getSkills()));
            row.put("emp_no", dashIfEmpty(r.getEmpNo()));
            row.put("section", dashIfEmpty(r.getSection()));
            row.put("dor", r.getDor() != null ? r.getDor() : "");
            rows.add(row);
        }

        rows.sort(Comparator
                .comparing((Map<String, Object> r) -> String.valueOf(r.get("department")))
                .thenComparing(r -> String.valueOf(r.get("name"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", attrType);
        result.put("value", attrValue);
        result.put("total", rows.size());
        result.put("rows", rows);
        return result;
    }

    /** Mirrors /api/skill-sections. */
    public Map<String, Object> skillSections(String skillVal, String catFilter, String deptFilter) {
        List<Employee> df = dataStore.snapshot();

        if (!"All".equals(catFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getCategory(), catFilter));
        }
        df = AnalyticsUtil.applyDeptFilter(df, deptFilter);

        String skillLower = skillVal.trim().toLowerCase();
        df = AnalyticsUtil.filter(df, r -> r.getSkillsList().stream()
                .anyMatch(s -> skillLower.equals(s == null ? "" : s.trim().toLowerCase())));

        Set<String> sections = new TreeSet<>();
        for (Employee r : df) {
            String sec = r.getSection() != null ? r.getSection().trim() : "";
            if (!sec.isEmpty()) sections.add(sec);
        }

        Map<String, Integer> employeesBySection = new LinkedHashMap<>();
        for (String sec : sections) {
            int count = 0;
            for (Employee r : df) {
                String s = r.getSection() != null ? r.getSection().trim() : "";
                if (s.equals(sec)) count++;
            }
            employeesBySection.put(sec, count);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("skill", skillVal);
        result.put("sections", new ArrayList<>(sections));
        result.put("total_employees", df.size());
        result.put("employees_by_section", employeesBySection);
        return result;
    }

    private String dashIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? "\u2014" : s;
    }
}
