package com.nmdc.hranalytics.service;

import com.nmdc.hranalytics.model.Employee;
import com.nmdc.hranalytics.model.RequiredSummary;
import com.nmdc.hranalytics.model.TempFile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton in-memory data store. Mirrors the module-level globals in app.py:
 *   MASTER_DATA    -> list of normalized employee dicts
 *   TEMP_FILES     -> file_id -> {name, headers, rows, bytes}
 *   REQUIRED_SUMMARY -> parsed Employee_Grade_Dept_Summary.xlsx data
 *
 * As in the original Flask app, this is a single shared in-memory store
 * (not per-user-session) — fine for the original app's scope, kept as-is
 * to preserve identical behavior. Thread-safe collections used since
 * Spring's embedded Tomcat serves requests concurrently (Flask's dev
 * server is single-threaded by default).
 */
@Service
public class DataStore {

    private final List<Employee> masterData = new CopyOnWriteArrayList<>();
    private final Map<String, TempFile> tempFiles = new ConcurrentHashMap<>();
    private volatile RequiredSummary requiredSummary = null;

    public List<Employee> getMasterData() {
        return masterData;
    }

    public void addAll(List<Employee> records) {
        masterData.addAll(records);
    }

    public Map<String, TempFile> getTempFiles() {
        return tempFiles;
    }

    public RequiredSummary getRequiredSummary() {
        return requiredSummary;
    }

    public void setRequiredSummary(RequiredSummary requiredSummary) {
        this.requiredSummary = requiredSummary;
    }

    public boolean isSummaryEnabled() {
        return requiredSummary != null && requiredSummary.isEnabled();
    }

    public synchronized void clear() {
        masterData.clear();
        tempFiles.clear();
        // Note: app.py's /api/clear does NOT reset REQUIRED_SUMMARY — preserved here too.
    }

    /** Snapshot copy, mirrors `df = list(MASTER_DATA)` pattern used throughout app.py. */
    public List<Employee> snapshot() {
        return new ArrayList<>(masterData);
    }
}
