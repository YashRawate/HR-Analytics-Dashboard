package com.nmdc.hranalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NMDC HR Analytics Intelligence — Spring Boot Backend
 * ======================================================
 * Java/Spring Boot port of the original Flask backend (app.py).
 *
 * REST API that handles Excel/CSV uploads, server-side data parsing,
 * normalization, and analytics computation.
 *
 * Endpoints (see controller classes for full detail):
 *   GET    /                          -> redirects to /login
 *   GET    /login                     -> serves nmdc_login.html
 *   GET    /dashboard                 -> serves index.html (requires session)
 *   POST   /api/login                 -> authenticate against Users.xlsx
 *   POST   /api/logout                -> clear session
 *   POST   /api/upload                -> parse uploaded Excel/CSV; return headers + preview
 *   POST   /api/process               -> apply column mapping; store normalized records
 *   POST   /api/autoload              -> auto-detect + load file (summary or employee data)
 *   GET    /api/analytics             -> aggregated analytics JSON
 *   GET    /api/sections              -> section x deposit matrix
 *   GET    /api/section-details       -> employee rows for a section
 *   GET    /api/departments           -> department list with counts
 *   GET    /api/summary               -> sidebar quick counts
 *   GET    /api/employees-by-attribute-> employee rows for a given attribute value
 *   GET    /api/skill-sections        -> unique sections for a skill
 *   DELETE /api/clear                 -> clear all server-side data
 */
@SpringBootApplication
public class HrAnalyticsApplication {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("=".repeat(58));
        System.out.println("  NMDC HR Analytics Intelligence - Spring Boot Backend");
        System.out.println("=".repeat(58));
        System.out.println("  Frontend : http://localhost:5000");
        System.out.println("  API Docs :");
        System.out.println("    POST   /api/upload    -> Upload Excel/CSV file");
        System.out.println("    POST   /api/process   -> Apply column mapping");
        System.out.println("    GET    /api/analytics -> Get aggregated analytics");
        System.out.println("    DELETE /api/clear     -> Clear all data");
        System.out.println("=".repeat(58));
        System.out.println();

        SpringApplication.run(HrAnalyticsApplication.class, args);
    }
}
