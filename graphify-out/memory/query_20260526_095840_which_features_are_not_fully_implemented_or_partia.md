---
type: "query"
date: "2026-05-26T09:58:40.053496+00:00"
question: "Which features are not fully implemented or partial and need polishing? Where do dark/light themes cause text visibility issues?"
contributor: "graphify"
source_nodes: ["PortalService", "DataCacheService", "AppContext", "DatabaseManager", "PreferencesService", "DashboardTabView", "DateSheetTabView", "TimetableTabView", "SettingsTabView", "app.css"]
---

# Q: Which features are not fully implemented or partial and need polishing? Where do dark/light themes cause text visibility issues?

## Answer

Partial features: portal snapshot/notifications pipeline exists (PortalService.captureSnapshot + DataCacheService.saveSnapshot) but no UI/call site; dark_overlay + zoom_level preferences are stored but unused; theme hook only toggles the dark-theme class.
Theme contrast issues: Dashboard info grid text uses hard-coded dark colors on dark cards; DateSheet cards use #1a1a1a/#666/#004643 on dark cards; Timetable uses light row backgrounds with light theme-driven text in dark mode; checkbox labels use #666 in dark mode; light theme has very low-contrast placeholders (#ccc, #aaaaaa).

## Source Nodes

- PortalService
- DataCacheService
- AppContext
- DatabaseManager
- PreferencesService
- DashboardTabView
- DateSheetTabView
- TimetableTabView
- SettingsTabView
- app.css