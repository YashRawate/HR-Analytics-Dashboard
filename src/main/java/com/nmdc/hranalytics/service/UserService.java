package com.nmdc.hranalytics.service;

import com.nmdc.hranalytics.model.User;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads Users.xlsx fresh on every login attempt (mirrors load_users() in app.py) so
 * edits to the file are picked up without restarting the server. Validates credentials
 * against (usertype, username-or-empid, password).
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Location of Users.xlsx, resolved relative to the project root (same as BASE_DIR.parent in app.py). */
    @Value("${nmdc.users-file:Users.xlsx}")
    private String usersFilePath;

    private List<User> loadUsers() {
        Path path = Path.of(usersFilePath);
        if (!Files.exists(path)) {
            log.warn("Users.xlsx not found at {}", path.toAbsolutePath());
            return List.of();
        }

        List<User> users = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(path.toFile());
             Workbook wb = WorkbookFactory.create(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            var rowIter = sheet.iterator();
            if (!rowIter.hasNext()) return users;

            Row headerRow = rowIter.next();
            List<String> headers = new ArrayList<>();
            for (Cell c : headerRow) {
                headers.add(formatter.formatCellValue(c).trim().toLowerCase());
            }

            int empIdx = headers.indexOf("empid");
            int typeIdx = headers.indexOf("usertype");
            int userIdx = headers.indexOf("user");
            int passIdx = headers.indexOf("password");

            while (rowIter.hasNext()) {
                Row r = rowIter.next();
                String empid = empIdx >= 0 ? cell(r, empIdx, formatter) : "";
                String usertype = typeIdx >= 0 ? cell(r, typeIdx, formatter) : "";
                String user = userIdx >= 0 ? cell(r, userIdx, formatter) : "";
                String password = passIdx >= 0 ? cell(r, passIdx, formatter) : "";
                users.add(new User(empid.trim(), usertype.trim().toLowerCase(), user.trim(), password.trim()));
            }
        } catch (IOException e) {
            log.error("Failed to read Users.xlsx: {}", e.getMessage());
            return List.of();
        }
        return users;
    }

    private String cell(Row row, int idx, DataFormatter formatter) {
        Cell c = row.getCell(idx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return formatter.formatCellValue(c);
    }

    /**
     * Validates login credentials. username is matched against either the USER (display name)
     * or EMPID column. Returns the matching User on success, null on failure.
     */
    public User validateCredentials(String usertype, String username, String password) {
        List<User> users = loadUsers();
        String ut = usertype.trim().toLowerCase();
        String query = username.trim().toLowerCase();

        for (User u : users) {
            if (!u.getUserType().equals(ut)) continue;
            if (!u.getPassword().equals(password)) continue;
            boolean nameMatch = u.getUser().equalsIgnoreCase(query);
            boolean empIdMatch = u.getEmpId().equalsIgnoreCase(query);
            if (nameMatch || empIdMatch) return u;
        }
        return null;
    }
}
