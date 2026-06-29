# 🏭 NMDC HR Analytics Intelligence

<p align="center">
  <img src="NMDC LOGO.jpg" alt="NMDC Logo" width="160"/>
</p>

<p align="center">
  <b>Enterprise workforce analytics platform — re-engineered from Python Flask to Java Spring Boot</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img src="https://img.shields.io/badge/Deployed%20on-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white" />
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-lightgrey?style=for-the-badge" />
</p>

<p align="center">
  A full-stack HR analytics web application for <b>National Mineral Development Corporation (NMDC)</b> — delivering real-time insights into workforce distribution, grade analysis, department headcount, skill mapping, and retirement projections through an interactive dashboard.
</p>

---

## 📸 Dashboard Preview

| Login | Dashboard | Analytics |
|-------|-----------|-----------|
| ![Login](.github/screenshots/login.png) | ![Dashboard](.github/screenshots/dashboard.png) | ![Analytics](.github/screenshots/analytics.png) |

| Grade Analysis | Department View | Reports |
|----------------|-----------------|---------|
| ![Grade](.github/screenshots/grade.png) | ![Department](.github/screenshots/department.png) | ![Reports](.github/screenshots/reports.png) |

> 📷 _Screenshots coming soon — run locally or visit the live demo to explore._

---

## 🌐 Live Demo

<p align="center">
  <a href="https://your-app.onrender.com">
    <img src="https://img.shields.io/badge/🚀%20Open%20Live%20Demo-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white" />
  </a>
</p>

> 🔗 **URL:** `https://<your-render-service>.onrender.com` ← _Replace with actual URL_
>
> The demo loads with preconfigured sample data — **no setup or file upload needed.**

| Credential | Value |
|------------|-------|
| **Username** | _(configured in `Users.xlsx`)_ |
| **Password** | _(configured in `Users.xlsx`)_ |

---

## 💡 Why This Project?

Large organizations like NMDC manage thousands of employees across mines, departments, and grades spread across multiple locations. HR teams traditionally rely on manual Excel analysis — a slow, error-prone process with no real-time visibility.

**NMDC HR Analytics Intelligence** transforms raw Excel data into an interactive analytics dashboard, enabling HR managers to instantly understand workforce composition, identify headcount gaps, track skills, and plan for retirements — all from a browser.

---

## ⭐ Project Highlights

- 🔄 **Python Flask → Java Spring Boot** — Full backend re-architecture, not just a port
- 🏛️ **Enterprise Layered Architecture** — Controller → Service → Model → Utility
- 🔌 **15+ REST APIs** — Consistent JSON contracts, session-authenticated
- 🐳 **Dockerized Deployment** — Multi-stage build, production-ready image
- 📂 **Auto Demo Dataset Loading** — Explore every feature instantly, zero setup
- 📊 **Interactive HR Analytics Dashboard** — KPIs, charts, drill-downs, filters
- 📁 **Excel & CSV Processing Engine** — Apache POI, column mapping, normalization
- 📱 **Responsive UI** — Works across desktop and tablet viewports
- 🔐 **Session-Based Authentication** — Secure login, protected routes
- ☁️ **Cloud Deployment on Render** — With optional Cloudflare Tunnel

---

## 🔄 Migration Journey: Flask → Spring Boot

The application began as a **Python Flask** prototype — validating the analytics concept rapidly in a single-file backend. As the project matured, architectural limitations required a more robust solution:

| Concern | Flask Approach | Spring Boot Solution |
|---------|---------------|----------------------|
| Concurrency | Single-threaded dev server | Embedded Tomcat + thread-safe collections |
| Architecture | Monolithic `app.py` | Layered: Controller / Service / Model / Util |
| State Management | Global Python dicts | `ConcurrentHashMap` + `CopyOnWriteArrayList` |
| Deployment | `python app.py` | Executable JAR + multi-stage Docker image |
| Build & Deps | `pip install` | Maven with dependency caching |

The re-engineering was deliberate — not a mechanical translation. Every API contract, JSON shape, session cookie behavior, normalization rule, and the proportional headcount allocation algorithm were preserved exactly. The existing **frontend required zero modifications**.

---

## 🎯 Demo Information

> ✅ **No real NMDC data is included anywhere in this repository.**

| Detail | Info |
|--------|------|
| **Auto-loaded on startup** | `employee_demo.xlsx` + `grade_demo.xlsx` |
| **Data accuracy** | All names, IDs, and figures are entirely fictional |
| **Upload your own data** | Supported — replaces demo data for the active session only |
| **Persistence** | Uploaded data is never written to disk |

---

## ✨ Key Features

**🔐 Authentication**
- Session-based login validated against `Users.xlsx`
- Protected routes redirect unauthenticated users to login
- Server-side session management via Spring Session

**📊 Analytics Dashboard**
- KPI cards: total employees, department counts, grade distribution
- Required vs. actual headcount with proportional allocation algorithm
- Section × Deposit cross-tabulation matrix

**📁 Excel Processing**
- Upload `.xlsx`, `.xls`, or `.csv` employee files
- Interactive column-mapping step before data is committed
- Automatic field normalization: departments, grades, deposit names

**📈 Reporting & Insights**
- Retirement projection — identify upcoming workforce attrition
- Skill mapping — query competencies across sections
- Employee drill-down — click any metric to view the underlying roster
- Attribute-based filtering across any mapped field

**☁️ Deployment**
- Multi-stage Docker build (Maven → JRE Alpine runtime)
- Docker Compose with Cloudflare Tunnel sidecar
- Deployed on Render as a containerized web service

---

## 🛠️ Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Java | 17 LTS |
| **Backend Framework** | Spring Boot | 3.3.4 |
| **Embedded Server** | Apache Tomcat | via Spring Boot |
| **Session Management** | Spring Session (HttpSession) | Cookie-based |
| **Excel Processing** | Apache POI | 5.2.5 |
| **CSV Processing** | Apache Commons CSV | 1.11.0 |
| **Code Generation** | Lombok | Latest stable |
| **Build Tool** | Apache Maven | 3.8+ |
| **Frontend** | HTML5, CSS3, Vanilla JS | Unchanged from Flask version |
| **Containerization** | Docker (multi-stage) | Maven 3.9.6 + JRE 17 Alpine |
| **Orchestration** | Docker Compose | 3.8 |
| **Cloud Platform** | Render | Container deployment |
| **Tunnel** | Cloudflare Tunnel (`cloudflared`) | Optional |

---

## 🏛️ System Architecture

```
Browser  (HTML5 · CSS3 · Vanilla JS)
        │
        ▼
Spring Boot REST Controllers
(/api/login · /api/analytics · /api/upload · /api/sections …)
        │
        ▼
Service Layer
(Business logic · Normalization · Headcount allocation)
        │
        ▼
Analytics Engine
(Aggregation · KPI computation · Matrix generation)
        │
        ▼
Apache POI / Commons CSV
(Excel & CSV parsing)
        │
        ▼
In-Memory Store
(ConcurrentHashMap · CopyOnWriteArrayList)
        │
        ▼
Excel Files
(Users.xlsx · employee_demo.xlsx · grade_demo.xlsx)
```

---

## 🗂️ Project Structure

```
NMDC-PROJECT-JAVA/
├── pom.xml                          # Maven build & dependencies
├── Dockerfile                       # Multi-stage Docker build
├── docker-compose.yml               # App + Cloudflare Tunnel
├── Users.xlsx                       # Login credentials (not committed)
├── employee_demo.xlsx               # Demo employee dataset
├── grade_demo.xlsx                  # Demo grade/summary dataset
├── nmdc_login.html                  # Login page (served by Spring Boot)
├── NMDC LOGO.jpg                    # Logo asset
├── start-all.sh / start-all.bat     # One-command launchers
├── frontend/
│   └── index.html                   # Analytics dashboard
└── src/main/java/com/nmdc/hranalytics/
    ├── HrAnalyticsApplication.java  # Entry point
    ├── config/                      # CORS, exception handling
    ├── controller/                  # Auth, Upload, Analytics controllers
    ├── model/                       # Employee, User, RequiredSummary, TempFile
    ├── service/                     # Business logic layer
    └── util/                        # Constants, normalizers, helpers
```

---

## 🚀 Getting Started

### Prerequisites
JDK 17+ &nbsp;·&nbsp; Maven 3.8+ &nbsp;·&nbsp; A modern browser

### Run Locally

```bash
git clone https://github.com/<your-username>/NMDC-PROJECT-JAVA.git
cd NMDC-PROJECT-JAVA
cp Users.sample.xlsx Users.xlsx      # Configure your credentials
mvn spring-boot:run
```

Open **http://localhost:5000** — demo data loads automatically.

```bash
./start-all.sh      # macOS / Linux
start-all.bat        # Windows
```

### Run with Docker

```bash
docker compose up --build            # Build and start
docker compose up -d --build         # Detached mode
```

### Build Standalone JAR

```bash
mvn clean package
java -jar target/nmdc-hr-analytics.jar
```

> ⚠️ Run from the project root so relative paths (`Users.xlsx`, `frontend/`, etc.) resolve correctly.

---

## 🐳 Docker & Deployment

The project uses a **multi-stage Docker build** for a lean, secure production image:

- **Stage 1 — Build:** `maven:3.9.6-eclipse-temurin-17-alpine` compiles the source and packages the JAR
- **Stage 2 — Runtime:** `eclipse-temurin:17-jre-alpine` runs only the JAR — no build tools, no source code

Docker Compose orchestrates two services: the Spring Boot application on port `5000` and an optional `cloudflared` sidecar for Cloudflare Tunnel-based public URL exposure. The application is deployed on **Render** as a container web service.

---

## 🔌 REST API Overview

All `/api/*` endpoints require an active session. Responses are JSON.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/login` | Authenticate; returns session cookie |
| `POST` | `/api/logout` | Invalidate session |
| `POST` | `/api/upload` | Parse Excel/CSV; return headers + preview |
| `POST` | `/api/process` | Apply column mapping; store normalized records |
| `POST` | `/api/autoload` | Auto-detect and load demo or uploaded file |
| `GET` | `/api/analytics` | Full analytics payload (KPIs, charts, insights) |
| `GET` | `/api/sections` | Section × deposit headcount matrix |
| `GET` | `/api/section-details` | Employee roster for a section |
| `GET` | `/api/departments` | Department list with counts |
| `GET` | `/api/summary` | Sidebar quick counts |
| `GET` | `/api/employees-by-attribute` | Employees filtered by attribute value |
| `GET` | `/api/skill-sections` | Sections with a given skill |
| `DELETE` | `/api/clear` | Clear all in-memory data |

<details>
<summary><b>🔐 Authentication Flow</b></summary>

1. `POST /api/login` — credentials validated against `Users.xlsx`
2. Server sets `JSESSIONID` cookie on success
3. All protected endpoints return `401` if session is absent or expired
4. `POST /api/logout` — session invalidated server-side

</details>

---

## ⚙️ Engineering Highlights

| Practice | Implementation |
|----------|---------------|
| **Layered Architecture** | Controllers handle HTTP only; Services own all domain logic |
| **Thread Safety** | `ConcurrentHashMap` + `CopyOnWriteArrayList` for concurrent Tomcat threads |
| **Modular Design** | Stateless utility classes reusable across the service layer |
| **API Parity** | 100% identical JSON contracts to the original Flask backend |
| **Minimal Docker Image** | Build tools excluded from runtime stage — lean, secure image |
| **Clean Build** | Single executable JAR; no runtime dependencies beyond JRE |
| **Session Management** | Spring HttpSession mirrors Flask's cookie-based behavior exactly |

---

## 🔮 Future Enhancements

- 🗄️ **PostgreSQL** — Replace in-memory store with a persistent database
- 🔑 **JWT Authentication** — Stateless, microservice-ready auth
- 👥 **Role-Based Access Control** — Admin / Manager / Viewer permissions
- ⚡ **Redis Caching** — Cache computed analytics for large datasets
- 🤖 **AI Workforce Analytics** — Attrition prediction and hiring forecasts
- 📤 **Report Export** — PDF and Excel exports directly from the dashboard
- 🔔 **Notifications** — Alerts for upcoming retirements and headcount gaps
- 🔒 **Audit Logging** — Tamper-evident trail for all data operations

---

## 👤 Author

**Yash Rawate**  
[GitHub](https://github.com/YashRawate) · [LinkedIn](https://linkedin.com/in/your-profile) · [Email](yashrawate24@email.com)

---

## 📄 License

Licensed under the [MIT License](LICENSE).

---

<p align="center">
  <sub>NMDC HR Analytics Intelligence — Java Edition &nbsp;·&nbsp; Engineered for scale, built for clarity</sub>
</p>
