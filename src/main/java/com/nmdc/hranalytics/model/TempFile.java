package com.nmdc.hranalytics.model;

import java.util.List;
import java.util.Map;

/**
 * Mirrors a TEMP_FILES[file_id] entry in app.py:
 *   { 'name': str, 'headers': list, 'rows': list[dict], 'bytes': bytes }
 */
public class TempFile {
    private final String name;
    private final List<String> headers;
    private final List<Map<String, String>> rows;
    private final byte[] bytes;

    public TempFile(String name, List<String> headers, List<Map<String, String>> rows, byte[] bytes) {
        this.name = name;
        this.headers = headers;
        this.rows = rows;
        this.bytes = bytes;
    }

    public String getName() { return name; }
    public List<String> getHeaders() { return headers; }
    public List<Map<String, String>> getRows() { return rows; }
    public byte[] getBytes() { return bytes; }
}
