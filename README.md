# CivicShield – Civic Intelligence & Crisis Vigilance Shield
> **Tagline:** *"Predict the chain. Break the chain."*

CivicShield is an enterprise-grade urban crisis prediction and response vigilance system built in **Java 17** and **Spring Boot 3**. 

Unlike conventional disaster systems that only register crisis events in isolation, CivicShield models the full **cascading chain reaction** across connected municipal infrastructure:
```
Rain (92%) → Flood (81%) → Road Blockage (74%) → Traffic Gridlock (69%) → Ambulance Delay (58%) → Hospital Strain (44%)
```
and guides municipal commanders directly to the highest-leverage **chain-breaking intervention** before physical gridlock paralyzes the city.

---

## 🏛️ Technology Stack

| Layer | Technologies |
|---|---|
| **Core Backend** | **Java 17**, **Spring Boot 3.3.4**, Maven |
| **View Layer** | **Spring MVC + Thymeleaf SSR** (zero SPA bundle lag), Reusable Fragments |
| **Security** | **Spring Security 6**, BCrypt Hashing, CSRF Enabled, 4-Tier RBAC |
| **Persistence** | **Spring Data JPA & Hibernate**, PostgreSQL (Production), H2 In-Memory (`demo` profile) |
| **Algorithms** | Built-in Java **`CascadeRiskEngine`**, Optional Reactive WebClient **Python AI** bridge |
| **Styling & UI** | **Tailwind CSS (CDN)**, Custom Glassmorphism, Space Grotesk / Inter / JetBrains Mono |
| **Client Scripting** | **Leaflet.js** (Interactive Crisis Maps), **Chart.js** (Trends & What-If Curves), **Lucide Icons** |

---

## 🔑 Demo Access Credentials

The demo profile automatically seeds a simulated Chennai monsoon scenario (Velachery, Adyar, T. Nagar, Guindy) on initial startup with **4 pre-configured authority accounts**:

| Role | Email | Password | Access Clearance |
|---|---|---|---|
| **ADMIN** | `admin@civicshield.in` | `Admin@123` | Full system access, data feeds, and user account admin |
| **AUTHORITY** | `authority@civicshield.in` | `Authority@123` | Control room dashboard, incidents, risk models, What-If simulator, and reports |
| **EMERGENCY_SERVICE** | `emergency@civicshield.in` | `Emergency@123` | Leaflet crisis map, incidents (read-only), alert dispatch, recommendations |
| **HOSPITAL** | `hospital@civicshield.in` | `Hospital@123` | Read-only vigilance dashboard, triage crisis map, emergency alerts |

*Tip: The `/login` page includes a 1-click **Quick Fill** matrix to test all 4 roles instantly.*

---

## 🚀 How to Run Locally

### Prerequisites
- **JDK 17** (or newer)
- **Apache Maven 3.8+**

### 1. Launch in Demo Mode (H2 In-Memory, No Setup Required)
```bash
# Set Java 17 path if needed
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export PATH="$JAVA_HOME/bin:$PATH"

# Run Spring Boot with the demo profile
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```
The application will boot on **`http://localhost:8080`**.

### 2. Launch in Production Mode (PostgreSQL)
```bash
export DB_URL="jdbc:postgresql://localhost:5432/civicshield"
export DB_USERNAME="civicshield"
export DB_PASSWORD="your_secure_password"

mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 3. Optional Python AI Microservice Interop
If you have a Python model service running:
```bash
# Enabled via application.properties or environment variable
export AI_ENABLED=true
export AI_BASE_URL="http://localhost:8000"
```
> **Resiliency Guarantee:** If the Python microservice is disabled, offline, or returns a timeout, CivicShield's built-in Java `CascadeRiskEngine` executes automatically with zero downtime.

---

## 🌐 Application Sitemap & Page Routes

### 1. Public Website (No Login Required)
- **Home** (`/`) – Hero with live animated cascade graphic, problem cards, 6-feature grid, 4-step timeline, live What-If teaser widget, tech stack strip, stakeholders, and simulated impact stats.
- **Features** (`/features`) – Deep-dive cards into each of CivicShield's architectural capabilities.
- **How It Works** (`/how-it-works`) – 3-tier architecture overview (Frontend → Spring Boot → PostgreSQL + Python AI) and step-by-step Velachery cloudburst scenario.
- **Live Demo** (`/demo`) – Public interactive What-If Simulator with sliders for rainfall, traffic, and drainage, updating animated cascade nodes, factor bars, and recommended actions.
- **Technology** (`/technology`) – Architectural stack breakdown and rationale.
- **About** (`/about`) – Problem statement, vision, mission, system objectives, limitations, and future roadmap.
- **Team** (`/team`) – Multidisciplinary contributors and research team profiles.
- **Contact** (`/contact`) – Validated contact form persisted to the database and emergency command hotline info.
- **Login** (`/login`) – Split layout: animated cascade graphic + login form with 1-click demo role quick fill.

### 2. Authority Portal (Login Required, `/portal/**`)
- **Dashboard** (`/portal/dashboard`) – Active incidents KPI, high-risk zones, critical alerts, "Top Cascading Threat" hero visualizer, Chart.js incident trend line, risk distribution donut, and mini crisis map.
- **Crisis Map** (`/portal/map`) – Leaflet geo-spatial map with pulsing markers, risk zone overlays, filtering (Critical, High, Medium, Low), and slide-out incident summary drawer.
- **Incidents** (`/portal/incidents`) – Searchable, filterable, paginated registry with live cascade risk scores.
- **Log Incident** (`/portal/incidents/new`) – Incident reporting form with Bean Validation and Leaflet click-to-pick coordinate selector.
- **Incident Detail** (`/portal/incidents/{id}`) – 4-stage status timeline (Reported → Verified → In Progress → Resolved), status update dropdown, and tabs for Cascade Risk, Triggered Alerts, and Interventions.
- **Cascade Risk Deep Dive** (`/portal/risk/{incidentId}`) – Animated radial risk gauge, 6-node propagation visualizer, explainable AI factor weights, and break point guidance.
- **Alerts Center** (`/portal/alerts`) – Active / Acknowledged / Resolved workflow with priority badges and transition buttons.
- **Recommendations** (`/portal/recommendations`) – AI mitigation cards with Approve / Dismiss actions and a strict note on human authority governance.
- **What-If Simulator** (`/portal/simulator`) – Full simulation suite with BEFORE vs AFTER comparisons, risk-vs-rainfall response curve (Chart.js), and saved scenarios archive.
- **Audit Reports & Export** (`/portal/reports`) – Multi-criteria filters, risk breakdown statistics, and RFC-4180 CSV export.
- **Data Feeds** (`/portal/data-sources`) – Health monitoring (Online, Delayed, Offline) for weather radars, drainage sensors, and traffic feeds.
- **User Admin** (`/portal/admin/users`) – Admin user management, role assignments, and account enable/disable controls.

### 3. REST API v1 (`/api/v1/**`, Bearer Token Auth)
Full headless REST API compliant with [`docs/API.md`](docs/API.md):
- **Auth**: `POST /api/v1/auth/login` (rate-limited, returns 256-bit opaque Bearer token), `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`
- **System**: `GET /api/v1/system/status` (public), `GET /api/v1/data-sources`
- **Dashboard**: `GET /api/v1/dashboard/summary`, `GET /api/v1/dashboard/trend`, `GET /api/v1/dashboard/activity`
- **Incidents**: `GET /api/v1/incidents` (filter/sort/paginate), `POST /api/v1/incidents`, `GET /api/v1/incidents/{id}`, `PUT /api/v1/incidents/{id}`, `PATCH /api/v1/incidents/{id}/status`, `GET /api/v1/incidents/{id}/alerts`, `GET /api/v1/incidents/{id}/recommendations`
- **Cascade Risk**: `GET /api/v1/incidents/{id}/cascade`, `POST /api/v1/incidents/{id}/cascade`, `GET /api/v1/risk/model`
- **Map & Zones**: `GET /api/v1/map/incidents`, `GET /api/v1/zones`
- **Alerts**: `GET /api/v1/alerts`, `GET /api/v1/alerts/counts`, `POST /api/v1/alerts/{id}/acknowledge`, `POST /api/v1/alerts/{id}/resolve`
- **Recommendations**: `GET /api/v1/recommendations`, `GET /api/v1/recommendations/counts`, `POST /api/v1/recommendations/{id}/approve`, `POST /api/v1/recommendations/{id}/dismiss`
- **What-If Simulations**: `GET /api/v1/simulations/parameters`, `POST /api/v1/simulations/run`, `GET /api/v1/simulations`, `DELETE /api/v1/simulations/{id}`
- **Audit Reports**: `GET /api/v1/reports/summary`, `GET /api/v1/reports/incidents.csv` (RFC-4180 CSV export)
- **User Management**: `GET /api/v1/users`, `POST /api/v1/users`, `PUT /api/v1/users/{id}`, `POST /api/v1/users/{id}/toggle-enabled`

---

## 🧪 Automated Testing

CivicShield features a comprehensive Spring Boot test suite covering the cascade math engine, domain services, security filter chains, role-based access control, and REST APIs:

```bash
# Run all unit and integration tests
npm test
# Or directly via Maven
mvn test
```

---

## 🧮 Mathematical Cascade Engine

CivicShield computes multi-stage cascading risk via:
1. **Node Propagation**:
   $$\text{Probability}(n) = \text{Probability}(n-1) \times W_{n-1 \to n} + \text{LocalFactor} \times K$$
2. **Node Risk**:
   $$\text{Risk}_i = \text{Probability}_i \times \text{Severity}_i \times \text{Exposure}$$
3. **Composite Cascade Score (Normalized 0–100)**:
   $$\text{Score} = 100 \times \left(1 - \prod_{i=1}^{N} (1 - d \cdot \text{Risk}_i)\right)$$
4. **Optimal Break Point Search**:
   The engine simulates mobile pumps, traffic reroutes, green corridors, and hospital surge protocols, selecting the intervention that produces the largest drop ($\Delta \text{Score}$) in overall city risk.

---

## 🛡️ License
Built for municipal crisis resilience and open research. Developed with Java 17, Spring Boot 3, and Tailwind CSS.
