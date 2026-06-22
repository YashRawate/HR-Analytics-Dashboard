package com.nmdc.hranalytics.controller;

import com.nmdc.hranalytics.model.Employee;
import com.nmdc.hranalytics.model.RequiredSummary;
import com.nmdc.hranalytics.model.TempFile;
import com.nmdc.hranalytics.service.*;
import com.nmdc.hranalytics.util.Normalizers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Mirrors the data-ingestion routes in app.py:
 *   POST   /api/upload    -> parse file, return headers + preview, store in TEMP_FILES
 *   POST   /api/process   -> apply column mapping to a temp file, add to MASTER_DATA
 *   POST   /api/autoload  -> auto-detect summary vs employee file, load directly
 *   DELETE /api/clear     -> clear all server-side data
 */
@RestController
@RequestMapping("/api")
public class UploadController {

    private final DataStore dataStore;
    private final ExcelService excelService;
    private final SummaryParserService summaryParserService;
    private final EmployeeParserService employeeParserService;

    public UploadController(DataStore dataStore, ExcelService excelService,
                             SummaryParserService summaryParserService,
                             EmployeeParserService employeeParserService) {
        this.dataStore = dataStore;
        this.excelService = excelService;
        this.summaryParserService = summaryParserService;
        this.employeeParserService = employeeParserService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        String filename = file.getOriginalFilename();
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }

        ExcelService.ParsedTable table;
        try {
            table = excelService.parse(fileBytes, filename.toLowerCase());
        } catch (Exception e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to parse file: " + e.getMessage()));
        }

        if (table.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "File contains no data"));
        }

        List<String> headers = table.headers;
        List<Map<String, String>> rows = table.rows;

        // Preview: first 3 rows, first 8 columns, values truncated to 30 chars
        List<String> previewCols = headers.subList(0, Math.min(8, headers.size()));
        List<Map<String, String>> preview = new ArrayList<>();
        for (int i = 0; i < Math.min(3, rows.size()); i++) {
            Map<String, String> row = rows.get(i);
            Map<String, String> previewRow = new LinkedHashMap<>();
            for (String c : previewCols) {
                previewRow.put(c, Normalizers.safeStr(row.getOrDefault(c, ""), 30));
            }
            preview.add(previewRow);
        }

        String fileId = UUID.randomUUID().toString();
        dataStore.getTempFiles().put(fileId, new TempFile(filename, headers, rows, fileBytes));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("file_id", fileId);
        result.put("file_name", filename);
        result.put("headers", headers);
        result.put("row_count", rows.size());
        result.put("preview", preview);
        result.put("preview_cols", previewCols);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processFile(@RequestBody Map<String, Object> body) {
        String fileId = (String) body.get("file_id");
        @SuppressWarnings("unchecked")
        Map<String, String> mapping = body.get("mapping") != null
                ? (Map<String, String>) body.get("mapping") : Map.of();

        if (fileId == null || !dataStore.getTempFiles().containsKey(fileId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown file_id"));
        }

        TempFile fileItem = dataStore.getTempFiles().get(fileId);
        String fileNameLower = fileItem.getName().toLowerCase();

        if (fileNameLower.contains("grade_dept_summary") || fileNameLower.contains("employee_strength")) {
            try {
                RequiredSummary summary = summaryParserService.parseFromBytes(fileItem.getBytes());
                dataStore.setRequiredSummary(summary);
                dataStore.getTempFiles().remove(fileId);

                if (summary.isEnabled()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("status", "summary_loaded");
                    result.put("message", "Summary file loaded: " + summary.getTotalCurrent()
                            + "/" + summary.getTotalRequired() + " employees");
                    result.put("total", dataStore.getMasterData().size());
                    result.put("summary_enabled", true);
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to parse summary file"));
                }
            } catch (Exception e) {
                dataStore.getTempFiles().remove(fileId);
                return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to process summary file: " + e.getMessage()));
            }
        }

        String deptCol = mapping.get("department");
        if (deptCol == null || deptCol.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department column mapping is required"));
        }

        List<String> headers = fileItem.getHeaders();
        List<String> missingCols = new ArrayList<>();
        for (String v : new LinkedHashSet<>(mapping.values())) {
            if (v != null && !v.isBlank() && !headers.contains(v)) missingCols.add(v);
        }
        if (!missingCols.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mapping refers to unknown columns: " + String.join(", ", missingCols)));
        }

        List<Employee> records = employeeParserService.buildWithUserMapping(fileItem.getRows(), mapping, fileNameLower);
        dataStore.addAll(records);
        dataStore.getTempFiles().remove(fileId);

        Set<String> uniqueDepts = new HashSet<>();
        for (Employee e : dataStore.getMasterData()) uniqueDepts.add(e.getDepartment());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("added", records.size());
        result.put("total", dataStore.getMasterData().size());
        result.put("unique_depts", uniqueDepts.size());
        result.put("summary_enabled", dataStore.isSummaryEnabled());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/autoload")
    public ResponseEntity<Map<String, Object>> autoload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        String filename = file.getOriginalFilename();
        String filenameLower = filename.toLowerCase();
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }

        if (filenameLower.contains("grade_dept_summary") || filenameLower.contains("employee_strength")) {
            try {
                RequiredSummary summary = summaryParserService.parseFromBytes(fileBytes);
                dataStore.setRequiredSummary(summary);
                if (summary.isEnabled()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("status", "summary_loaded");
                    result.put("message", "Summary file loaded: " + summary.getTotalCurrent()
                            + "/" + summary.getTotalRequired() + " employees");
                    result.put("loaded", 0);
                    result.put("total", dataStore.getMasterData().size());
                    result.put("summary_enabled", true);
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to parse summary file"));
                }
            } catch (Exception e) {
                return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to process summary file: " + e.getMessage()));
            }
        }

        ExcelService.ParsedTable table;
        try {
            table = excelService.parse(fileBytes, filenameLower);
        } catch (Exception e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "Failed to parse file: " + e.getMessage()));
        }

        if (table.isEmpty()) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "File contains no data"));
        }

        Set<String> available = new HashSet<>(table.headers);
        EmployeeParserService.AutoLoadResult autoResult =
                employeeParserService.buildWithAutoColumnMap(table.rows, available, filename);

        if (autoResult.deptCol == null) {
            return ResponseEntity.unprocessableEntity().body(Map.of(
                    "error", "Department column not found in file. Columns: " + table.headers));
        }

        dataStore.addAll(autoResult.records);

        Set<String> uniqueDepts = new HashSet<>();
        for (Employee e : dataStore.getMasterData()) uniqueDepts.add(e.getDepartment());

        Map<String, Object> columnsUsed = new LinkedHashMap<>();
        columnsUsed.put("department", autoResult.deptCol);
        columnsUsed.put("name", autoResult.nameCol);
        columnsUsed.put("designation", autoResult.desigCol);
        columnsUsed.put("grade", autoResult.gradeCol);
        columnsUsed.put("deposit", autoResult.depCol);
        columnsUsed.put("skills", autoResult.skillCol);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loaded", autoResult.records.size());
        result.put("skipped", autoResult.skipped);
        result.put("total", dataStore.getMasterData().size());
        result.put("unique_depts", uniqueDepts.size());
        result.put("summary_enabled", dataStore.isSummaryEnabled());
        result.put("columns_used", columnsUsed);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clear() {
        dataStore.clear();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
