package com.nmdc.hranalytics.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Generic spreadsheet/CSV reader. Mirrors pandas' read_csv / read_excel(dtype=str, na_filter=False)
 * behavior used throughout app.py: every cell becomes a trimmed-ish string, blanks stay as "".
 */
@Service
public class ExcelService {

    public static class ParsedTable {
        public List<String> headers = new ArrayList<>();
        public List<Map<String, String>> rows = new ArrayList<>();
        public boolean isEmpty() { return rows.isEmpty(); }
    }

    /** Parses CSV or XLSX/XLS bytes into a header list + list of row maps (all string values). */
    public ParsedTable parse(byte[] fileBytes, String filenameLower) throws IOException {
        if (filenameLower.endsWith(".csv")) {
            return parseCsv(fileBytes);
        }
        return parseExcel(fileBytes);
    }

    private ParsedTable parseCsv(byte[] fileBytes) throws IOException {
        ParsedTable table = new ParsedTable();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(false)
                .build();

        try (InputStreamReader reader = new InputStreamReader(
                new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {

            table.headers = new ArrayList<>(parser.getHeaderNames());
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String h : table.headers) {
                    String v = record.isMapped(h) ? record.get(h) : "";
                    row.put(h, cleanNa(v));
                }
                table.rows.add(row);
            }
        }
        return table;
    }

    private ParsedTable parseExcel(byte[] fileBytes) throws IOException {
        ParsedTable table = new ParsedTable();
        DataFormatter formatter = new DataFormatter();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return table;

            Iterator<Row> rowIter = sheet.iterator();
            if (!rowIter.hasNext()) return table;

            Row headerRow = rowIter.next();
            int lastCol = headerRow.getLastCellNum();
            for (int c = 0; c < lastCol; c++) {
                Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String h = formatter.formatCellValue(cell);
                if (h.trim().isEmpty()) h = "Column" + (c + 1);
                table.headers.add(h);
            }

            while (rowIter.hasNext()) {
                Row r = rowIter.next();
                // Skip fully blank rows
                boolean allBlank = true;
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < table.headers.size(); c++) {
                    Cell cell = r.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String v = cleanNa(formatter.formatCellValue(cell));
                    if (!v.isEmpty()) allBlank = false;
                    rowMap.put(table.headers.get(c), v);
                }
                if (!allBlank) table.rows.add(rowMap);
            }
        }
        return table;
    }

    private String cleanNa(String v) {
        if (v == null) return "";
        String t = v.trim();
        if (t.equals("nan") || t.equals("NaN") || t.equals("<NA>") || t.equals("None")) return "";
        return t;
    }
}
