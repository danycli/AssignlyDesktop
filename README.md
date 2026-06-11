# Assignly Desktop

Assignly Desktop is a premium, client-side academic portal companion app built **exclusively for the students of COMSATS University Islamabad, Abbottabad Campus**. It connects to the university portal to scrape, sync, and present academic data (assignments, schedules, grades, attendance, and financials) in a beautiful, modern desktop interface with powerful offline-first capabilities.

---

## Key Features

### 1. Branded Split-Screen Login Experience
*   **Modern 60/40 Layout**: Replaces basic single-column forms with an immersive split-screen layout.
*   **Branded Hero Section**: Prominent visual identity featuring custom ambient glows, dynamic gradients, and academic titles.
*   **Security First**: Input fields are secured with AES-256 encryption (`EncryptionUtil.java`) for local credentials preservation.
*   **Seamless Offline Access**: Allows instant offline bypass if local data cache is available.

### 2. High-Performance Dashboard
*   **Personalized Hero Banner**: Greets the user dynamically and renders the student profile avatar.
*   **Unified Stats Grid**: Multi-card metrics including cumulative GPA (CGPA), registered course count, attendance rate, scholarship status, outstanding fee balance, and total credit hours.
*   **Interactive GPA Chart**: Custom line chart rendering historical SGPA and CGPA trends with interactive node scale-ups, custom cursors, and detailed hover tooltips.
*   **Attendance Overview**: Course-by-course progress indicators with visual warning colors for attendance falling below the critical 75% threshold.
*   **Financial Card**: Quick summary of current scholarship awards, unpaid fee challan balances, and the last verified transaction.

### 3. Smart Timetable & Schedule Engine
*   **Multi-View Interface**: Includes a "Today" view showing current live classes (with "Ends In" countdowns), upcoming classes (with "Starts In" countdowns), and a comprehensive "Week Grid" and "All List" view.
*   **Smart Filtering**: Features an in-bar search and a context-aware "Show Empty Days" toggle (dynamically hidden when irrelevant) to filter out days with no scheduled academic activities.
*   **Details Drawer**: Slide-out panel presenting comprehensive lecture specs (room/location, faculty instructor, duration, slot type).

### 4. Offline-First Architecture & Local Caching
*   **Robust Cache Service**: Transparently caches academic portal snapshots inside a local SQLite database using `DataCacheService.java` and `DatabaseManager.java`.
*   **Seamless Fallback**: Full readability of portal statistics, grades, announcements, and schedules even when completely offline or during portal server downtimes.
*   **Offline UX Indicators**: Subtle warning banners and responsive action disabling for features requiring a live internet connection.

### 5. Academic Portal & Resource Explorer
*   **Course Portal View**: Tracks individual assignments, submissions, gradings, notice boards, and announcements. Features dynamic assignment uploads with instant UI auto-refresh, and customizable grid/list views with powerful sorting.
*   **Performance Tracking**: Granular breakdown of Quizzes, Assignments, Sessionals, Mid Term Marks, and Final Exam Marks.
*   **Attendance Timeline**: Advanced timeline logs of all class proceedings automatically sorted from newest to oldest, with mutually exclusive visual filters for quickly isolating Present or Absent lectures.
*   **Lecture Resources**: View and extract files/resources uploaded by professors.
*   **Exam Coupon & Roll Numbers**: Verification of enrollment details and retrieval of printable exam slips.
*   **GPA/Results View**: Track detailed semester-by-semester grades, credit hours, and progress transcripts.

### 6. Financial Ledger & Fee Management
*   **Challan Center**: View current dues, unpaid fee invoices, and download generated payment slips.
*   **Fee Ledger History**: Review complete payment records, dates, and amounts for past academic semesters.

### 7. Utility Services & Mobile Sync
*   **PDF Report Exporter**: In-app PDF generation to export transcripts, fees, or class schedules using `PdfExportService.java`.
*   **Mobile App Activation**: Provides secure and direct generation of app passwords alongside quick Play Store download links to easily pair local data with the mobile companion application.
*   **Web Portal Tab**: An integrated browser frame to interact with the raw university portal directly when manual submission is required.

---

## Technical Stack & Architecture

*   **Core Language**: Java 21+ / Kotlin
*   **UI Framework**: JavaFX (with rich styling utilizing modern CSS stylesheets)
*   **Database**: SQLite (via JDBC driver)
*   **HTTP Client & HTML Parser**: OkHttp & JSoup (for web scraping portal data securely)
*   **PDF Generator**: LibrePDF OpenPDF
*   **Build Tool**: Apache Maven
*   **Packaging Pipeline**: Custom `jlink` runtime image generation and `jpackage` for native Windows executable (.exe) packaging with the WiX Toolset.

---

## Project Structure

```
AssignlyDesktop/
│
├── src/main/java/com/assignly/
│   ├── Launcher.java          # Custom bootstrap entry point
│   ├── Main.java              # JavaFX Application initializer
│   │
│   ├── controller/            # MVC Controllers
│   │
│   ├── database/              # SQLite Database connection manager
│   │   └── DatabaseManager.java
│   │
│   ├── model/                 # Data entities (User, Assignment, Announcement, etc.)
│   │
│   ├── security/              # Security utilities (AES credentials encryption)
│   │   └── EncryptionUtil.java
│   │
│   ├── service/               # Data Cache, PDF Export, and Portal Services
│   │   ├── DataCacheService.java
│   │   ├── PdfExportService.java
│   │   └── PortalRepository.java
│   │
│   ├── util/                  # Navigation context and UI helper files
│   │   └── AppContext.java
│   │
│   └── view/                  # Modern custom components for all tabs
│       ├── LoginView.java
│       ├── DashboardTabView.java
│       ├── TimetableTabView.java
│       └── ...
│
├── build_installer.ps1        # Powershell script to package the app into a Windows EXE
├── pom.xml                    # Maven dependency configurations
└── README.md                  # Features and developer guide
```

---

## Getting Started

### Prerequisites
*   Java Development Kit (JDK) 21 or higher
*   Maven 3.8+
*   WiX Toolset v3.11+ (Optional, only required if compiling the `.exe` installer)

### Building and Running Locally
To compile the source code and run the JavaFX desktop app locally:
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.assignly.Launcher"
```

### Packaging a Production Installer
To produce a lightweight, standalone Windows installer (`.exe`) containing its own tailored JRE runtime:
1. Open PowerShell in the project root.
2. Execute the build installer script:
   ```powershell
   powershell -File .\build_installer.ps1
   ```
3. Find the output installer in the `target/installer/` directory.
