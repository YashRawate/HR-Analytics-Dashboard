package com.nmdc.hranalytics.service;

import com.nmdc.hranalytics.model.Employee;
import com.nmdc.hranalytics.model.RequiredSummary;
import com.nmdc.hranalytics.util.AnalyticsUtil;
import com.nmdc.hranalytics.util.AnalyticsUtil.Count;
import com.nmdc.hranalytics.util.Constants;
import com.nmdc.hranalytics.util.Normalizers;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Mirrors /api/analytics in app.py: builds the full aggregated analytics payload
 * (KPIs, grade pareto, gender, deposit distribution/cards, departments, designations,
 * skills, workforce summary, insights, sidebar counts).
 */
@Service
public class AnalyticsService {

    private final DataStore dataStore;

    public AnalyticsService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Map<String, Object> buildAnalytics(String depositFilter, String catFilter, String deptFilter, String showMode) {
        List<Employee> df = dataStore.snapshot();

        if (!"All".equals(depositFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getDeposit(), depositFilter));
        }
        if (!"All".equals(catFilter)) {
            df = AnalyticsUtil.filter(df, r -> Objects.equals(r.getCategory(), catFilter));
        }
        df = AnalyticsUtil.applyDeptFilter(df, deptFilter);

        int n = df.size();

        // ── KPIs ──────────────────────────────────────────────────────────
        Set<String> grades = new LinkedHashSet<>();
        Set<String> desigs = new LinkedHashSet<>();
        Set<String> deptsSet = new LinkedHashSet<>();
        int femaleCnt = 0;
        for (Employee r : df) {
            if (notBlank(r.getGrade())) grades.add(r.getGrade());
            if (notBlank(r.getDesignation())) desigs.add(r.getDesignation());
            if (notBlank(r.getDepartment())) deptsSet.add(r.getDepartment());
            if ("Female".equals(r.getGender())) femaleCnt++;
        }
        int femalePct = n > 0 ? Math.round(femaleCnt * 100f / n) : 0;

        RequiredSummary requiredSummary = dataStore.getRequiredSummary();
        boolean useSummary = "Required".equals(showMode) && requiredSummary != null && requiredSummary.isEnabled();

        // ── Grade distribution (Pareto) ──────────────────────────────────
        List<Map<String, Object>> gradePareto;
        if (useSummary) {
            gradePareto = buildGradeParetoFromSummary(requiredSummary, deptFilter, catFilter);
        } else {
            List<Count> gradeCounts = AnalyticsUtil.countBy(df, Employee::getGrade);
            int totalGrades = gradeCounts.stream().mapToInt(Count::count).sum();
            int cum = 0;
            gradePareto = new ArrayList<>();
            for (Count c : gradeCounts) {
                cum += c.count();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("grade", c.label());
                entry.put("count", c.count());
                entry.put("cumulative_pct", totalGrades > 0 ? Math.round(cum * 100f / totalGrades) : 0);
                gradePareto.add(entry);
            }
        }

        // ── Gender ────────────────────────────────────────────────────────
        List<Count> genderCounts = AnalyticsUtil.countBy(df, Employee::getGender);

        // ── Deposit distribution ────────────────────────────────────────
        List<Count> depositCounts = AnalyticsUtil.countBy(df, Employee::getDeposit);

        // ── Deposit performance cards ───────────────────────────────────
        List<String> depsToShow = "All".equals(depositFilter) ? Constants.DEPOSITS : List.of(depositFilter);
        List<Map<String, Object>> depositCards = new ArrayList<>();
        List<Employee> master = dataStore.snapshot();
        for (String dep : depsToShow) {
            List<Employee> sub = AnalyticsUtil.filter(df, r -> Objects.equals(r.getDeposit(), dep));
            List<Employee> allSub = AnalyticsUtil.filter(master, r -> Objects.equals(r.getDeposit(), dep));
            int emp = sub.size();
            int totalEmp = allSub.size();
            int femCnt = 0;
            Set<String> gSet = new LinkedHashSet<>();
            Set<String> dSet = new LinkedHashSet<>();
            Set<String> skSet = new LinkedHashSet<>();
            for (Employee r : sub) {
                if ("Female".equals(r.getGender())) femCnt++;
                if (notBlank(r.getGrade())) gSet.add(r.getGrade());
                if (notBlank(r.getDesignation())) dSet.add(r.getDesignation());
                skSet.addAll(r.getSkillsList());
            }
            int femPct = emp > 0 ? Math.round(femCnt * 100f / emp) : 0;
            int pct = n > 0 ? Math.round(emp * 100f / n) : 0;
            int empBarPct = totalEmp > 0 ? Math.round(emp * 100f / totalEmp) : 0;

            Map<String, Object> card = new LinkedHashMap<>();
            card.put("deposit", dep);
            card.put("employees", emp);
            card.put("total_employees", totalEmp);
            card.put("female_pct", femPct);
            card.put("grade_count", gSet.size());
            card.put("desig_count", dSet.size());
            card.put("skill_count", skSet.size());
            card.put("pct", pct);
            card.put("emp_bar_pct", empBarPct);
            depositCards.add(card);
        }

        // ── Top designations ─────────────────────────────────────────────
        List<Count> desigCountsAll = AnalyticsUtil.countBy(df, Employee::getDesignation);
        List<Count> desigCounts = desigCountsAll.subList(0, Math.min(10, desigCountsAll.size()));

        // ── Skills treemap ───────────────────────────────────────────────
        Map<String, Integer> skillCounter = new LinkedHashMap<>();
        for (Employee r : df) {
            for (String sk : r.getSkillsList()) {
                if (sk != null && !sk.equals("undefined") && !sk.equals("null")) {
                    skillCounter.merge(sk, 1, Integer::sum);
                }
            }
        }
        List<Count> skillsData = new ArrayList<>();
        for (var e : skillCounter.entrySet()) skillsData.add(new Count(e.getKey(), e.getValue()));
        skillsData.sort((a, b) -> Integer.compare(b.count(), a.count()));
        skillsData = skillsData.subList(0, Math.min(20, skillsData.size()));

        // ── Workforce summary panel ──────────────────────────────────────
        List<Count> deptCounts = AnalyticsUtil.countBy(df, Employee::getDepartment);
        String topDeptName = deptCounts.isEmpty() ? "\u2014" : deptCounts.get(0).label();
        String topDepName = depositCounts.isEmpty() ? "\u2014" : depositCounts.get(0).label();
        String topSkillName = skillsData.isEmpty() ? "\u2014" : skillsData.get(0).label();
        String topDesigName = desigCounts.isEmpty() ? "\u2014" : desigCounts.get(0).label();

        // ── Insights ──────────────────────────────────────────────────────
        int avgPerDept = deptCounts.isEmpty() ? 0 : Math.round(n * 1f / deptCounts.size());

        // ── Sidebar counts ───────────────────────────────────────────────
        List<Employee> base = "All".equals(depositFilter)
                ? master
                : AnalyticsUtil.filter(master, r -> Objects.equals(r.getDeposit(), depositFilter));
        Map<String, Integer> deptCountMap = new LinkedHashMap<>();
        for (Employee r : base) deptCountMap.merge(r.getDepartment(), 1, Integer::sum);

        Map<String, Object> summaryInfo = null;
        if (useSummary) {
            summaryInfo = new LinkedHashMap<>();
            summaryInfo.put("mode", "Required");
            summaryInfo.put("total_current", requiredSummary.getTotalCurrent());
            summaryInfo.put("total_required", requiredSummary.getTotalRequired());

            Map<String, Object> categories = new LinkedHashMap<>();
            for (var e : requiredSummary.getCategories().entrySet()) {
                categories.put(e.getKey(), Map.of("current", e.getValue().current, "required", e.getValue().required));
            }
            summaryInfo.put("categories", categories);

            Map<String, Object> departments = new LinkedHashMap<>();
            for (var e : requiredSummary.getDepartments().entrySet()) {
                departments.put(e.getKey(), Map.of("current", e.getValue().current, "required", e.getValue().required));
            }
            summaryInfo.put("departments", departments);

            Map<String, Object> gradesOut = new LinkedHashMap<>();
            for (var e : requiredSummary.getGrades().entrySet()) {
                Map<String, Object> gradeMap = new LinkedHashMap<>();
                gradeMap.put("current", e.getValue().current);
                gradeMap.put("required", e.getValue().required);
                Map<String, Object> deptMap = new LinkedHashMap<>();
                for (var de : e.getValue().departments.entrySet()) {
                    deptMap.put(de.getKey(), Map.of("current", de.getValue().current, "required", de.getValue().required));
                }
                gradeMap.put("departments", deptMap);
                gradesOut.put(e.getKey(), gradeMap);
            }
            summaryInfo.put("grades", gradesOut);
        }

        Map<String, Object> sidebar = new LinkedHashMap<>();
        sidebar.put("all", base.size());
        sidebar.put("prod_all", Constants.PROD_DEPTS.stream().mapToInt(d -> deptCountMap.getOrDefault(d, 0)).sum());
        sidebar.put("np_all", Constants.NONPROD_DEPTS.stream().mapToInt(d -> deptCountMap.getOrDefault(d, 0)).sum());
        sidebar.put("oth_all", Constants.OTHER_DEPTS.stream().mapToInt(d -> deptCountMap.getOrDefault(d, 0)).sum());
        sidebar.put("by_dept", deptCountMap);

        // ── Assemble response ────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", n);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("total", n);
        kpis.put("departments", deptsSet.size());
        kpis.put("grades", grades.size());
        kpis.put("female_pct", femalePct);
        kpis.put("designations", desigs.size());
        result.put("kpis", kpis);

        List<Map<String, Object>> gradeParetoOut = new ArrayList<>();
        for (Map<String, Object> g : gradePareto) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("label", g.get("grade"));
            e.put("count", g.get("count"));
            e.put("cum_pct", g.get("cumulative_pct"));
            gradeParetoOut.add(e);
        }
        result.put("grade_pareto", gradeParetoOut);

        result.put("gender", toLabelCountList(genderCounts));
        result.put("deposit_dist", toLabelCountList(depositCounts));
        result.put("deposit_cards", depositCards);

        List<Map<String, Object>> departmentsDist;
        if (useSummary) {
            departmentsDist = buildDepartmentsDistFromSummary(requiredSummary, deptFilter, catFilter);
        } else {
            departmentsDist = toLabelCountList(deptCounts);
        }
        result.put("departments_dist", departmentsDist);

        result.put("designations", toLabelCountList(desigCounts));
        result.put("designations_dist", toLabelCountList(desigCountsAll));
        result.put("skills", toLabelCountList(skillsData));

        Map<String, Object> workforceSummary = new LinkedHashMap<>();
        workforceSummary.put("total", n);
        workforceSummary.put("top_deposit", topDepName);
        workforceSummary.put("top_dept", topDeptName);
        workforceSummary.put("top_skill", topSkillName);
        workforceSummary.put("top_desig", topDesigName);
        workforceSummary.put("female_pct", femalePct);
        result.put("workforce_summary", workforceSummary);

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("avg_per_dept", avgPerDept);
        insights.put("top_grade", gradePareto.isEmpty() ? null
                : Map.of("label", gradePareto.get(0).get("grade"), "count", gradePareto.get(0).get("count")));
        insights.put("largest_dept", deptCounts.isEmpty() ? null
                : Map.of("label", deptCounts.get(0).label(), "count", deptCounts.get(0).count()));
        insights.put("largest_deposit", depositCounts.isEmpty() ? null
                : Map.of("label", depositCounts.get(0).label(), "count", depositCounts.get(0).count()));
        insights.put("top_desig", desigCounts.isEmpty() ? null
                : Map.of("label", desigCounts.get(0).label(), "count", desigCounts.get(0).count()));
        insights.put("top_skill", skillsData.isEmpty() ? null
                : Map.of("label", skillsData.get(0).label(), "count", skillsData.get(0).count()));
        insights.put("dept_count", deptCounts.size());
        result.put("insights", insights);

        result.put("sidebar", sidebar);
        result.put("summary", summaryInfo);

        return result;
    }

    /** Mirrors build_grade_pareto_from_summary() in app.py. */
    private List<Map<String, Object>> buildGradeParetoFromSummary(RequiredSummary summary, String deptFilter, String catFilter) {
        List<Map<String, Object>> gradePareto = new ArrayList<>();
        int totalRequired = 0;

        List<Map.Entry<String, Integer>> counts = new ArrayList<>();
        for (var e : summary.getGrades().entrySet()) {
            String grade = e.getKey();
            RequiredSummary.GradeEntry gradeData = e.getValue();
            int count;
            if (!"All".equals(deptFilter)) {
                RequiredSummary.CurrentRequired deptInfo = gradeData.departments.get(deptFilter);
                count = deptInfo != null ? deptInfo.required : 0;
            } else if (!"All".equals(catFilter)) {
                count = 0;
                for (var de : gradeData.departments.entrySet()) {
                    if (Normalizers.getCategory(de.getKey()).equals(catFilter)) {
                        count += de.getValue().required;
                    }
                }
            } else {
                count = gradeData.required;
            }
            if (count > 0) {
                totalRequired += count;
                counts.add(Map.entry(grade, count));
            }
        }
        counts.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int cum = 0;
        for (var e : counts) {
            cum += e.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("grade", e.getKey());
            item.put("count", e.getValue());
            item.put("cumulative_pct", totalRequired > 0 ? Math.round(cum * 100f / totalRequired) : 0);
            gradePareto.add(item);
        }
        return gradePareto;
    }

    /** Mirrors build_departments_dist_from_summary() in app.py. */
    private List<Map<String, Object>> buildDepartmentsDistFromSummary(RequiredSummary summary, String deptFilter, String catFilter) {
        List<Map<String, Object>> deptDist = new ArrayList<>();
        for (String dept : Constants.ALL_DEPTS) {
            if (!"All".equals(catFilter) && !Normalizers.getCategory(dept).equals(catFilter)) continue;
            RequiredSummary.CurrentRequired deptData = summary.getDepartments().get(dept);
            int count = deptData != null ? deptData.required : 0;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", dept);
            entry.put("count", count);
            deptDist.add(entry);
        }
        deptDist.sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));
        return deptDist;
    }

    private List<Map<String, Object>> toLabelCountList(List<Count> counts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Count c : counts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", c.label());
            m.put("count", c.count());
            out.add(m);
        }
        return out;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
