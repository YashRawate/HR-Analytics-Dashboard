package com.nmdc.hranalytics.controller;

import com.nmdc.hranalytics.service.AnalyticsService;
import com.nmdc.hranalytics.service.EmployeeQueryService;
import com.nmdc.hranalytics.service.SectionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mirrors the read-only analytics GET routes in app.py.
 */
@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SectionsService sectionsService;
    private final EmployeeQueryService employeeQueryService;

    public AnalyticsController(AnalyticsService analyticsService,
                                SectionsService sectionsService,
                                EmployeeQueryService employeeQueryService) {
        this.analyticsService = analyticsService;
        this.sectionsService = sectionsService;
        this.employeeQueryService = employeeQueryService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics(
            @RequestParam(defaultValue = "All") String deposit,
            @RequestParam(defaultValue = "All") String cat,
            @RequestParam(defaultValue = "All") String dept,
            @RequestParam(defaultValue = "") String show) {
        return ResponseEntity.ok(analyticsService.buildAnalytics(deposit, cat, dept, show));
    }

    @GetMapping("/sections")
    public ResponseEntity<Map<String, Object>> sections(
            @RequestParam(defaultValue = "All") String dept,
            @RequestParam(defaultValue = "All") String cat,
            @RequestParam(defaultValue = "All") String deposit) {
        return ResponseEntity.ok(sectionsService.sections(dept, cat, deposit));
    }

    @GetMapping("/section-details")
    public ResponseEntity<Map<String, Object>> sectionDetails(
            @RequestParam(defaultValue = "All") String dept,
            @RequestParam(defaultValue = "All") String cat,
            @RequestParam(defaultValue = "All") String deposit,
            @RequestParam(defaultValue = "All") String section) {
        return ResponseEntity.ok(sectionsService.sectionDetails(dept, cat, deposit, section));
    }

    @GetMapping("/departments")
    public ResponseEntity<Map<String, Object>> departments(
            @RequestParam(defaultValue = "All") String category) {
        return ResponseEntity.ok(employeeQueryService.departments(category));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(employeeQueryService.summary());
    }

    @GetMapping("/employees-by-attribute")
    public ResponseEntity<Map<String, Object>> employeesByAttribute(
            @RequestParam(defaultValue = "deposit") String type,
            @RequestParam(defaultValue = "") String value,
            @RequestParam(defaultValue = "All") String cat,
            @RequestParam(defaultValue = "All") String dept,
            @RequestParam(defaultValue = "All") String section) {
        return ResponseEntity.ok(employeeQueryService.employeesByAttribute(type, value, cat, dept, section));
    }

    @GetMapping("/skill-sections")
    public ResponseEntity<Map<String, Object>> skillSections(
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "All") String cat,
            @RequestParam(defaultValue = "All") String dept) {
        if (skill.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Skill parameter required"));
        }
        return ResponseEntity.ok(employeeQueryService.skillSections(skill, cat, dept));
    }
}
