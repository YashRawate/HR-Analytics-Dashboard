package com.nmdc.hranalytics.controller;

import com.nmdc.hranalytics.model.User;
import com.nmdc.hranalytics.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mirrors the page-serving and auth routes in app.py:
 *   GET  /            -> redirect to /login
 *   GET  /login        -> serve nmdc_login.html
 *   GET  /dashboard     -> serve index.html (requires session)
 *   GET  /NMDC LOGO.jpg -> serve the logo from project root
 *   POST /api/login     -> authenticate against Users.xlsx
 *   POST /api/logout    -> clear session
 */
@RestController
public class AuthController {

    private final UserService userService;

    @Value("${nmdc.login-page:nmdc_login.html}")
    private String loginPagePath;

    @Value("${nmdc.frontend-dir:frontend}")
    private String frontendDir;

    @Value("${nmdc.logo-path:NMDC LOGO.jpg}")
    private String logoPath;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
    }

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> loginPage() throws IOException {
        Path path = Path.of(loginPagePath);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(new FileSystemResource(path));
    }

    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> dashboard(HttpSession session) throws IOException {
        Object loggedIn = session.getAttribute("logged_in");
        if (loggedIn == null || !Boolean.TRUE.equals(loggedIn)) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        Path path = Path.of(frontendDir, "index.html");
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(new FileSystemResource(path));
    }

    @GetMapping(value = "/retirement-report/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> retirementReport(HttpSession session) throws IOException {
        Object loggedIn = session.getAttribute("logged_in");
        if (loggedIn == null || !Boolean.TRUE.equals(loggedIn)) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        Path path = Path.of(frontendDir, "retirement-report", "index.html");
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(new FileSystemResource(path));
    }

    @GetMapping("/NMDC LOGO.jpg")
    public ResponseEntity<Resource> nmdcLogo() {
        Path path = Path.of(logoPath);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new FileSystemResource(path));
    }

    @PostMapping("/api/login")
    public ResponseEntity<Map<String, Object>> apiLogin(@RequestBody Map<String, String> body, HttpSession session) {
        String usertype = body.getOrDefault("usertype", "").trim();
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "").trim();

        if (usertype.isEmpty() || username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required."));
        }

        User user = userService.validateCredentials(usertype, username, password);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials. Please check your User Type, Username and Password."));
        }

        session.setAttribute("logged_in", true);
        session.setAttribute("username", user.getUser());
        session.setAttribute("usertype", user.getUserType());
        session.setAttribute("empid", user.getEmpId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("user", user.getUser());
        result.put("usertype", user.getUserType());
        result.put("empid", user.getEmpId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Map<String, Object>> apiLogout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }
}
