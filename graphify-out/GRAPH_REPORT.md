# Graph Report - C:\\VS Code\\AssignlyDesktop  (2026-05-29)

## Corpus Check
- 60 files · ~79,002 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 617 nodes · 1458 edges · 18 communities (6 shown, 12 thin omitted)
- Extraction: 79% EXTRACTED · 21% INFERRED · 0% AMBIGUOUS · INFERRED: 308 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]

## God Nodes (most connected - your core abstractions)
1. `PortalRepository` - 75 edges
2. `PortalRepository` - 75 edges
3. `CoursePortalTabView` - 56 edges
4. `AppContext` - 51 edges
5. `CoursesTabView` - 25 edges
6. `FeeTabView` - 22 edges
7. `SettingsTabView` - 18 edges
8. `DataCacheService` - 15 edges
9. `ExamCouponTabView` - 14 edges
10. `UserPreferences` - 12 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities (18 total, 12 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.05
Nodes (21): AttendanceColumnMapping, AttendanceInsight, CaptchaRequired, DownloadResult, Error, HtmlDownloadCandidate, InstructionFile, InstructionFilesResult (+13 more)

### Community 1 - "Community 1"
Cohesion: 0.05
Nodes (5): User, EncryptionUtil, CredentialManager, AppContext, LoginView

### Community 3 - "Community 3"
Cohesion: 0.05
Nodes (11): Application, Main, DatabaseManager, Announcement, Assignment, PortalAnnouncement, PortalAssignment, PortalSnapshot (+3 more)

### Community 5 - "Community 5"
Cohesion: 0.1
Nodes (4): UserPreferences, PortalService, PreferencesService, WebPortalTabView

### Community 6 - "Community 6"
Cohesion: 0.08
Nodes (8): IOException, PremiumPageEventHelper, DownloadResult, InstructionFilesResult, LoginResult, UploadResult, SessionExpiredException, ExamCouponTabView

### Community 8 - "Community 8"
Cohesion: 0.13
Nodes (3): PdfExportService, PdfExportServiceTest, ResultTabView

## Knowledge Gaps
- **15 isolated node(s):** `LoginResult`, `InvalidCredentials`, `CaptchaRequired`, `UploadResult`, `NetworkError` (+10 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PortalRepository` connect `Community 4` to `Community 1`, `Community 2`, `Community 5`, `Community 6`, `Community 7`, `Community 9`, `Community 11`?**
  _High betweenness centrality (0.395) - this node is a cross-community bridge._
- **Why does `AppContext` connect `Community 1` to `Community 2`, `Community 5`, `Community 7`, `Community 9`, `Community 10`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **What connects `LoginResult`, `InvalidCredentials`, `CaptchaRequired` to the rest of the system?**
  _15 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.1 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._