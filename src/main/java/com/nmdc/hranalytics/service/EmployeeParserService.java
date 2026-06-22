package com.nmdc.hranalytics.service;

import com.nmdc.hranalytics.model.Employee;
import com.nmdc.hranalytics.util.Constants;
import com.nmdc.hranalytics.util.Normalizers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Builds Employee records from parsed spreadsheet rows. Two code paths mirror app.py:
 *   - buildWithAutoColumnMap(): used by /api/autoload (NMDC_EXCEL_COLUMN_MAP, exact header match)
 *   - buildWithUserMapping():   used by /api/process (user-supplied column -> field mapping)
 */
@Service
public class EmployeeParserService {

    public static class AutoLoadResult {
        public List<Employee> records = new ArrayList<>();
        public int skipped = 0;
        public String deptCol;
        public String nameCol;
        public String desigCol;
        public String gradeCol;
        public String depCol;
        public String skillCol;
    }

    /** Mirrors the regular-employee-data branch of /api/autoload in app.py. */
    public AutoLoadResult buildWithAutoColumnMap(List<Map<String, String>> rows, Set<String> availableColumns, String sourceFileName) {
        AutoLoadResult result = new AutoLoadResult();
        Map<String, String> colMap = Constants.NMDC_EXCEL_COLUMN_MAP;

        result.deptCol = getCol(colMap, availableColumns, "department");
        result.nameCol = getCol(colMap, availableColumns, "name");
        result.desigCol = getCol(colMap, availableColumns, "designation");
        result.gradeCol = getCol(colMap, availableColumns, "grade");
        result.depCol = getCol(colMap, availableColumns, "deposit");
        result.skillCol = getCol(colMap, availableColumns, "skills");
        String genderCol = getCol(colMap, availableColumns, "gender");
        String empNoCol = getCol(colMap, availableColumns, "emp_no");
        String sectionCol = getCol(colMap, availableColumns, "section");
        String qualCol = getCol(colMap, availableColumns, "qualification");
        String dorCol  = getCol(colMap, availableColumns, "dor");

        if (result.deptCol == null) {
            // Caller is expected to check this and short-circuit with an error response.
            return result;
        }

        for (Map<String, String> row : rows) {
            String deptRaw = Normalizers.safeStr(row.getOrDefault(result.deptCol, ""));
            String dept = Normalizers.normalizeDepartment(deptRaw);
            if (dept == null) {
                result.skipped++;
                continue;
            }

            String skillsRaw = result.skillCol != null ? Normalizers.safeStr(row.getOrDefault(result.skillCol, "")) : "";
            String gradeRaw = result.gradeCol != null ? Normalizers.safeStr(row.getOrDefault(result.gradeCol, "")).trim() : "";
            String depositRaw = result.depCol != null ? Normalizers.safeStr(row.getOrDefault(result.depCol, "")) : "";
            String genderRaw = genderCol != null ? Normalizers.safeStr(row.getOrDefault(genderCol, "")) : "";

            Employee emp = new Employee();
            emp.setName(result.nameCol != null ? Normalizers.safeStr(row.getOrDefault(result.nameCol, "")) : "");
            emp.setDepartment(dept);
            emp.setDesignation(result.desigCol != null ? Normalizers.safeStr(row.getOrDefault(result.desigCol, "")) : "");
            emp.setGrade(gradeRaw);
            emp.setDeposit(Normalizers.normalizeDeposit(depositRaw));
            emp.setGender(Normalizers.normalizeGender(genderRaw));
            emp.setSkills(skillsRaw);
            emp.setSkillsList(Normalizers.parseSkills(skillsRaw));
            emp.setCategory(Normalizers.getCategory(dept));
            emp.setSourceFile(sourceFileName);
            emp.setEmpNo(empNoCol != null ? Normalizers.safeStr(row.getOrDefault(empNoCol, "")) : "");
            emp.setSection(sectionCol != null ? Normalizers.safeStr(row.getOrDefault(sectionCol, "")) : "");
            emp.setQualification(qualCol != null ? Normalizers.safeStr(row.getOrDefault(qualCol, "")) : "");
            emp.setDor(dorCol != null ? Normalizers.safeStr(row.getOrDefault(dorCol, "")) : "");

            result.records.add(emp);
        }

        return result;
    }

    /** Mirrors the regular-employee-data branch of /api/process in app.py (user-supplied mapping). */
    public List<Employee> buildWithUserMapping(List<Map<String, String>> rows, Map<String, String> mapping, String sourceFileName) {
        List<Employee> records = new ArrayList<>();
        String deptCol = mapping.get("department");
        if (deptCol == null || deptCol.isBlank()) return records;

        for (Map<String, String> row : rows) {
            String deptRaw = row.getOrDefault(deptCol, "");
            String dept = Normalizers.normalizeDepartment(deptRaw);
            if (dept == null) continue;

            String skillsRaw = getVal(row, mapping.get("skills"));

            Employee emp = new Employee();
            emp.setName(getVal(row, mapping.get("name")));
            emp.setDepartment(dept);
            emp.setDesignation(getVal(row, mapping.get("designation")));
            emp.setGrade(getVal(row, mapping.get("grade")));
            emp.setDeposit(Normalizers.normalizeDeposit(getVal(row, mapping.get("deposit"))));
            emp.setGender(Normalizers.normalizeGender(getVal(row, mapping.get("gender"))));
            emp.setSkills(skillsRaw);
            emp.setSkillsList(Normalizers.parseSkills(skillsRaw));
            emp.setCategory(Normalizers.getCategory(dept));
            emp.setSourceFile(sourceFileName);
            emp.setDor(getVal(row, mapping.get("dor")));

            records.add(emp);
        }
        return records;
    }

    private String getVal(Map<String, String> row, String colName) {
        if (colName == null || colName.isBlank()) return "";
        return Normalizers.safeStr(row.getOrDefault(colName, ""));
    }

    private String getCol(Map<String, String> colMap, Set<String> available, String key) {
        String col = colMap.get(key);
        return (col != null && available.contains(col)) ? col : null;
    }
}
