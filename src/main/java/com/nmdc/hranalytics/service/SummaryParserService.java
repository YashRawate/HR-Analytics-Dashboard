package com.nmdc.hranalytics.service;

import com.nmdc.hranalytics.model.RequiredSummary;
import com.nmdc.hranalytics.util.Constants;
import com.nmdc.hranalytics.util.Normalizers;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Ports parse_summary_workbook() from app.py: parses Employee_Grade_Dept_Summary.xlsx
 * (grade rows, department subtotal rows, grand total row) and allocates "required"
 * headcount across grades-per-department proportionally to "current" headcount.
 */
@Service
public class SummaryParserService {

    private static class GradeRow {
        String grade;
        String dept;
        int current;
    }

    public RequiredSummary parse(Workbook wb) {
        RequiredSummary summary = new RequiredSummary();
        DataFormatter formatter = new DataFormatter();

        try {
            Sheet sheet = wb.getSheetAt(0);
            List<GradeRow> gradeRows = new ArrayList<>();
            Map<String, RequiredSummary.CurrentRequired> deptTotals = new LinkedHashMap<>();

            int lastRow = sheet.getLastRowNum(); // 0-indexed; Python used 1-indexed range(3, max_row+1)
            // Python: for r in range(3, sheet.max_row + 1) with sheet.cell(row=r, ...) 1-indexed
            // => rows 3..max_row inclusive, 1-indexed. In POI 0-indexed that's rows 2..lastRow.
            for (int r = 2; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String gradeVal = cellStr(row, 0, formatter);
                if (gradeVal == null) continue;
                String grade = gradeVal.trim();
                if (grade.isEmpty()) continue;

                String deptRaw = Normalizers.safeStr(cellStr(row, 1, formatter));
                String dept = Normalizers.normalizeDepartment(deptRaw);
                if (dept == null) dept = "Unknown";

                int current = parseInt(cellStr(row, 3, formatter));
                int required = parseInt(cellStr(row, 4, formatter));

                String gradeLower = grade.toLowerCase();
                if (gradeLower.equals("grade") || grade.startsWith("\u25B6")) {
                    continue;
                }
                if (gradeLower.contains("grand total")) {
                    summary.setTotalCurrent(current);
                    summary.setTotalRequired(required);
                    continue;
                }
                if (gradeLower.contains("subtotal")) {
                    RequiredSummary.CurrentRequired entry =
                            deptTotals.computeIfAbsent(dept, k -> new RequiredSummary.CurrentRequired());
                    entry.current += current;
                    entry.required += required;
                    continue;
                }

                GradeRow gr = new GradeRow();
                gr.grade = grade;
                gr.dept = dept;
                gr.current = current;
                gradeRows.add(gr);
            }

            // Build departments and categories from subtotal rows
            for (Map.Entry<String, RequiredSummary.CurrentRequired> e : deptTotals.entrySet()) {
                String dept = e.getKey();
                RequiredSummary.CurrentRequired totals = e.getValue();
                String category = Normalizers.getCategory(dept);
                summary.getCategories().computeIfAbsent(category, k -> new RequiredSummary.CurrentRequired());

                summary.getDepartments().put(dept, new RequiredSummary.CurrentRequired(totals.current, totals.required));

                RequiredSummary.CurrentRequired catEntry = summary.getCategories().get(category);
                catEntry.current += totals.current;
                catEntry.required += totals.required;
            }

            // Allocate required counts across grades per department using current proportions
            Map<String, Map<String, Integer>> deptGradeCurrent = new LinkedHashMap<>();
            Map<String, Integer> gradeCurrent = new LinkedHashMap<>();
            for (GradeRow row : gradeRows) {
                deptGradeCurrent
                        .computeIfAbsent(row.dept, k -> new LinkedHashMap<>())
                        .merge(row.grade, row.current, Integer::sum);
                gradeCurrent.merge(row.grade, row.current, Integer::sum);
            }

            Map<String, Map<String, Integer>> gradeRequiredByDept = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Integer>> e : deptGradeCurrent.entrySet()) {
                String dept = e.getKey();
                Map<String, Integer> gradeCounts = e.getValue();
                int requiredTotal = deptTotals.containsKey(dept) ? deptTotals.get(dept).required : 0;
                Map<String, Integer> allocations = allocateCounts(gradeCounts, requiredTotal);
                for (Map.Entry<String, Integer> a : allocations.entrySet()) {
                    gradeRequiredByDept
                            .computeIfAbsent(a.getKey(), k -> new LinkedHashMap<>())
                            .put(dept, a.getValue());
                }
            }

            // Build grade summary using current counts and allocated required counts
            for (Map.Entry<String, Integer> e : gradeCurrent.entrySet()) {
                RequiredSummary.GradeEntry ge = new RequiredSummary.GradeEntry();
                ge.current = e.getValue();
                ge.required = 0;
                summary.getGrades().put(e.getKey(), ge);
            }

            for (Map.Entry<String, Map<String, Integer>> e : gradeRequiredByDept.entrySet()) {
                String grade = e.getKey();
                RequiredSummary.GradeEntry gradeEntry = summary.getGrades().get(grade);
                for (Map.Entry<String, Integer> deptAlloc : e.getValue().entrySet()) {
                    String dept = deptAlloc.getKey();
                    int alloc = deptAlloc.getValue();
                    gradeEntry.required += alloc;
                    int curForDept = deptGradeCurrent.getOrDefault(dept, Map.of()).getOrDefault(grade, 0);
                    gradeEntry.departments.put(dept, new RequiredSummary.CurrentRequired(curForDept, alloc));
                }
            }

            // Fallback totals if grand total row was missing/empty
            if (summary.getTotalCurrent() == 0 && summary.getTotalRequired() == 0) {
                int tc = summary.getDepartments().values().stream().mapToInt(v -> v.current).sum();
                int tr = summary.getDepartments().values().stream().mapToInt(v -> v.required).sum();
                summary.setTotalCurrent(tc);
                summary.setTotalRequired(tr);
            }

            summary.setEnabled(true);
        } catch (Exception e) {
            summary.setEnabled(false);
        }

        return summary;
    }

    public RequiredSummary parseFromBytes(byte[] fileBytes) throws IOException {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            return parse(wb);
        }
    }

    /**
     * Mirrors allocate_counts() in app.py: distribute `total` across `weights` proportionally,
     * using largest-remainder rounding so the allocations sum exactly to `total`.
     */
    private Map<String, Integer> allocateCounts(Map<String, Integer> weights, int total) {
        Map<String, Integer> alloc = new LinkedHashMap<>();
        if (total <= 0 || weights.isEmpty()) {
            for (String k : weights.keySet()) alloc.put(k, 0);
            return alloc;
        }

        long totalWeight = weights.values().stream().mapToLong(Integer::longValue).sum();
        if (totalWeight <= 0) {
            int per = total / weights.size();
            for (String k : weights.keySet()) alloc.put(k, per);
            return alloc;
        }

        int allocated = 0;
        List<Map.Entry<String, Double>> remainders = new ArrayList<>();
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            double exact = total * (double) e.getValue() / (double) totalWeight;
            int floorVal = (int) Math.floor(exact);
            alloc.put(e.getKey(), floorVal);
            allocated += floorVal;
            remainders.add(new AbstractMap.SimpleEntry<>(e.getKey(), exact - floorVal));
        }

        int remainder = total - allocated;
        remainders.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < remainder && i < remainders.size(); i++) {
            String key = remainders.get(i).getKey();
            alloc.merge(key, 1, Integer::sum);
        }
        return alloc;
    }

    private String cellStr(Row row, int col, DataFormatter formatter) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return formatter.formatCellValue(cell);
    }

    private int parseInt(String value) {
        if (value == null) return 0;
        String s = value.replace(",", "").trim();
        if (s.isEmpty()) return 0;
        try {
            // handle values like "12.0" from formatted numeric cells
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
