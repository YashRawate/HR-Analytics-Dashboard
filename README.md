# 🏭 NMDC HR Analytics — Java Edition

<p align="center">
  <img src="NMDC LOGO.jpg" alt="NMDC Logo" width="180"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Status-Active-brightgreen" />
  <img src="https://img.shields.io/badge/Frontend-HTML%20%7C%20CSS%20%7C%20JS-blue" />
  <img src="https://img.shields.io/badge/Backend-Java%2017%20%7C%20Spring%20Boot-orange" />
  <img src="https://img.shields.io/badge/License-MIT-lightgrey" />
</p>

---

## 📌 Overview

This is the **Java/Spring Boot** port of the original Flask-based NMDC HR Analytics
application. It is a full-stack web app for **National Mineral Development Corporation
(NMDC)** providing employee records management, grade/department analytics, and
secure role-based login — with **identical REST API behavior** to the original Python
backend, so the existing frontend (`index.html`, `nmdc_login.html`) works unmodified.

This conversion preserves:
- The same JSON request/response shapes for every endpoint
- The same session-cookie-based login flow
- The same department/deposit/grade normalization rules
- The same proportional "required headcount" allocation algorithm
- The same single-port deployment model (one app serves both API and frontend)

---

## 🗂️ Project Structure

```
NMDC-PROJECT-JAVA/
├── pom.xml                          # Maven build file
├── src/main/java/com/nmdc/hranalytics/
│   ├── HrAnalyticsApplication.java  # Spring Boot entry point
│   ├── config/                      # CORS + global exception handling
│   ├── controller/                  # REST controllers (Auth, Upload, Analytics)
│   ├── model/                       # Employee, RequiredSummary, TempFile, User
│   ├── service/                     # Business logic (1:1 port of app.py functions)
│   └── util/                        # Constants, normalizers, analytics helpers
├── src/main/resources/
│   └── application.properties       # Port, session, multipart, path config
├── frontend/
│   └── index.html                   # Main dashboard (unchanged from original)
├── nmdc_login.html                  # Login page (unchanged from original)
├── NMDC LOGO.jpg                    # Official NMDC logo
├── Users.sample.xlsx                # Sample credentials file — copy to Users.xlsx
├── start-all.sh / start-all.bat     # One-command launchers
└── .github/                         # CI workflow + issue templates
```

> **Note:** `Employee_Grade_Dept_Summary.xlsx` and your real employee data file are
> **not** included — upload them through the app's UI after logging in, exactly as in
> the original. `Users.xlsx` (real credentials) is also not included for security; use
> `Users.sample.xlsx` as a template.

---

## 🚀 Getting Started

### Prerequisites

- **JDK 17** or newer ([Temurin](https://adoptium.net/) recommended)
- **Maven 3.8+** (or use your IDE's bundled Maven)
- A modern web browser (Chrome, Edge, Firefox)

### Installation & Run

```bash
# 1. Clone / unzip the project
cd NMDC-PROJECT-JAVA

# 2. Add your real credentials file (or rename the sample to try it out)
cp Users.sample.xlsx Users.xlsx

# 3. Build and run
mvn spring-boot:run
```

Or use the launcher scripts:

```bash
./start-all.sh      # macOS / Linux
start-all.bat        # Windows
```

The app starts on **http://localhost:5000** — same port as the original Flask app.

### Building a standalone JAR

```bash
mvn clean package
java -jar target/nmdc-hr-analytics.jar
```

Run the JAR from the project root (not from `target/`) so it can find
`Users.xlsx`, `nmdc_login.html`, `frontend/`, and `NMDC LOGO.jpg` via their
relative paths — same convention as the original `BASE_DIR.parent` resolution
in `app.py`. If you need to run it from elsewhere, override the paths via
`application.properties` or `-D` system properties:

```bash
java -jar target/nmdc-hr-analytics.jar \
  --nmdc.users-file=/path/to/Users.xlsx \
  --nmdc.login-page=/path/to/nmdc_login.html \
  --nmdc.frontend-dir=/path/to/frontend \
  --nmdc.logo-path="/path/to/NMDC LOGO.jpg"
```

---

## 🖥️ Usage

1. Run the app (`mvn spring-boot:run` or the launcher script).
2. Open `http://localhost:5000` — it redirects to `/login`.
3. Log in with credentials from `Users.xlsx`.
4. Upload your employee Excel/CSV file and (optionally)
   `Employee_Grade_Dept_Summary.xlsx` for "Required" headcount mode.
5. Explore department, grade, deposit, skills, and section analytics.

---

## 🔌 API Reference

All endpoints are unchanged from the Flask version — same paths, params, and
JSON shapes.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/` | Redirects to `/login` |
| GET    | `/login` | Serves the login page |
| GET    | `/dashboard` | Serves the dashboard (requires session) |
| POST   | `/api/login` | Authenticate against `Users.xlsx` |
| POST   | `/api/logout` | Clear session |
| POST   | `/api/upload` | Parse Excel/CSV, return headers + preview |
| POST   | `/api/process` | Apply column mapping, store normalized records |
| POST   | `/api/autoload` | Auto-detect + load a file (summary or employee data) |
| GET    | `/api/analytics` | Aggregated analytics (KPIs, charts, insights) |
| GET    | `/api/sections` | Section x deposit breakdown matrix |
| GET    | `/api/section-details` | Employee rows for a section |
| GET    | `/api/departments` | Department list with counts |
| GET    | `/api/summary` | Sidebar quick counts |
| GET    | `/api/employees-by-attribute` | Employee rows for a given attribute value |
| GET    | `/api/skill-sections` | Unique sections for a given skill |
| DELETE | `/api/clear` | Clear all server-side data |

---

## 🛠️ Tech Stack

| Layer       | Technology                          |
|-------------|--------------------------------------|
| Frontend    | HTML5, CSS3, JavaScript (unchanged)  |
| Backend     | Java 17, Spring Boot 3.3             |
| Spreadsheets| Apache POI (xlsx/xls), Apache Commons CSV |
| Build       | Maven                                |
| Sessions    | Spring Session (HttpSession, cookie-based) |

---

## Differences from the Python version

- **Concurrency**: the in-memory data store uses thread-safe collections
  (`CopyOnWriteArrayList`, `ConcurrentHashMap`) since Spring's embedded Tomcat
  handles requests concurrently by default (Flask's dev server is single-threaded).
  Behavior for a single user is identical; this just makes the Java version safer
  under concurrent access.
- **Packaging**: ships as a single executable JAR (`java -jar ...`) instead of
  `python app.py`.
- Everything else — endpoints, JSON shapes, normalization rules, the headcount
  allocation algorithm, session-based auth — is a deliberate 1:1 behavioral port.

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork this repository
2. Create a new branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">Made with care for NMDC — Java Edition</p>
