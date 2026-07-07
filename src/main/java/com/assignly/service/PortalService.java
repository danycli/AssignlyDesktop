package com.assignly.service;

import com.assignly.model.PortalSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;

public class PortalService {
    public static final String SIS_LOGIN_URL = "https://sis.cuiatd.edu.pk/login.aspx";
    private static final Gson GSON = new Gson();

    public void enableAutoLogin(WebEngine webEngine, String registrationNo, String password) {
        if (registrationNo == null || registrationNo.isBlank() || password == null || password.isBlank()) {
            return;
        }
        // Single-element array to allow mutation from within the lambda closure.
        // The script fires at most once per WebEngine session — if the user needs to
        // re-attempt (e.g., wrong credentials), showCaptchaDialog() creates a new
        // WebEngine which gets a fresh enableAutoLogin() call with a fresh guard.
        final boolean[] injected = {false};
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (!injected[0] && newState == Worker.State.SUCCEEDED) {
                String loc = webEngine.getLocation() != null ? webEngine.getLocation().toLowerCase() : "";
                String title = webEngine.getTitle() != null ? webEngine.getTitle().toLowerCase() : "";
                
                if (!loc.contains("cdn-cgi") && !title.contains("just a moment") && loc.contains("login.aspx")) {
                    injected[0] = true;
                    String script = buildAutoLoginScript(registrationNo, password);
                    webEngine.executeScript(script);
                }
            }
        });
    }

    public PortalSnapshot captureSnapshot(WebEngine webEngine) {
        Object raw = webEngine.executeScript(buildSnapshotScript());
        if (raw == null) {
            throw new IllegalStateException("Portal snapshot script returned no data.");
        }
        String json = String.valueOf(raw);
        if (json.isBlank() || "null".equalsIgnoreCase(json)) {
            throw new IllegalStateException("Portal snapshot contains no serializable data.");
        }
        try {
            PortalSnapshot snapshot = GSON.fromJson(json, PortalSnapshot.class);
            return snapshot == null ? new PortalSnapshot() : snapshot;
        } catch (JsonSyntaxException ex) {
            throw new IllegalStateException("Portal snapshot format was invalid.", ex);
        }
    }

    public void applyDarkOverlay(WebEngine webEngine, boolean enabled) {
        // Fast-path URL check in Java before even evaluating JavaScript
        String loc = webEngine.getLocation();
        if (loc != null && (loc.contains("/cdn-cgi/") || loc.contains("challenge-platform"))) {
            return;
        }

        String script = """
            (function() {
              // Strict safety guard to prevent style injection from polluting DOM during a challenge
              const isCaptchaActive = document.querySelector('[id*="cf-challenge"]') != null || 
                                      document.querySelector('.cf-turnstile') != null || 
                                      document.querySelector('iframe[src*="cloudflare"]') != null ||
                                      document.title.toLowerCase().includes('just a moment') ||
                                      (document.body && document.body.innerHTML.toLowerCase().includes('cloudflare'));
                                      
              if (isCaptchaActive || window.location.href.includes('cdn-cgi')) {
                  return; // Abort injection
              }

              const styleId = 'assignly-dark-overlay';
              let style = document.getElementById(styleId);
              if (%s) {
                if (!style) {
                  style = document.createElement('style');
                  style.id = styleId;
                  style.textContent = 'html { filter: invert(0.92) hue-rotate(180deg) contrast(0.95); } img, video, iframe { filter: invert(1) hue-rotate(180deg); }';
                  document.head.appendChild(style);
                }
              } else if (style) {
                style.remove();
              }
            })();
            """.formatted(enabled ? "true" : "false");
        webEngine.executeScript(script);
    }

    private String buildAutoLoginScript(String registrationNo, String password) {
        String escapedRegistration = escapeForJs(registrationNo);
        String escapedPassword = escapeForJs(password);
        return """
            (function() {
              const regCandidates = [
                '#txtusername', '#txtregNo', '#txtLoginID', 'input[name*=reg]', 'input[name*=login]', 'input[type=text]'
              ];
              const passCandidates = [
                '#txtpassword', '#txtPassword', 'input[name*=password]', 'input[type=password]'
              ];
              const submitCandidates = [
                '#btnLogin', '#btnsubmit', 'input[type=submit]', 'button[type=submit]', 'button'
              ];

              function first(candidates) {
                for (const selector of candidates) {
                  const node = document.querySelector(selector);
                  if (node) return node;
                }
                return null;
              }

              // Strict safety guard to prevent style injection from polluting DOM during a challenge
              const isCaptchaActive = document.querySelector('[id*="cf-challenge"]') != null || 
                                      document.querySelector('.cf-turnstile') != null || 
                                      document.querySelector('iframe[src*="cloudflare"]') != null ||
                                      document.title.toLowerCase().includes('just a moment') ||
                                      (document.body && document.body.innerHTML.toLowerCase().includes('cloudflare'));
                                      
              if (isCaptchaActive) {
                  console.warn("Cloudflare Challenge detected. Aborting auto-fill.");
                  return;
              }

              const reg = first(regCandidates);
              const pass = first(passCandidates);
              
              if (!reg || !pass) return;

              // Only inject if the registration input field '.value' properties are completely empty
              if (reg.value === '') {
                  reg.value = '%s';
                  reg.dispatchEvent(new Event('change', { bubbles: true }));
              }
              
              if (pass.value === '') {
                  pass.value = '%s';
                  pass.dispatchEvent(new Event('change', { bubbles: true }));
              }
            })();
            """.formatted(escapedRegistration, escapedPassword);
    }

    private String buildSnapshotScript() {
        return """
            (function() {
              function text(node) {
                return (node && node.innerText ? node.innerText : '').replace(/\\s+/g, ' ').trim();
              }

              const rows = Array.from(document.querySelectorAll('table tr'));
              const assignments = rows
                .map(row => Array.from(row.querySelectorAll('th, td')).map(cell => text(cell)))
                .filter(cells => cells.length >= 2)
                .filter(cells => /assign|quiz|lab|project|task|homework/i.test(cells.join(' ')))
                .slice(0, 20)
                .map(cells => ({
                  title: cells[0] || 'Assignment',
                  description: cells.slice(1, cells.length - 2).join(' | ') || '',
                  dueDate: cells[cells.length - 1] || '',
                  status: cells.length > 2 ? cells[cells.length - 2] : 'Pending'
                }));

              const announcementNodes = Array.from(document.querySelectorAll('h1,h2,h3,h4,p,span,li,a'));
              const announcementTexts = announcementNodes
                .map(node => text(node))
                .filter(line => /announcement|notice|important|circular/i.test(line))
                .slice(0, 20);
              const announcements = announcementTexts.map(line => ({
                title: line.length > 90 ? line.substring(0, 90) + '...' : line,
                content: line,
                publishDate: new Date().toISOString()
              }));

              const bodyText = text(document.body);
              const attendance = (bodyText.match(/attendance[^\\n\\r]{0,40}/i) || [''])[0];
              const results = (bodyText.match(/(cgpa|gpa|result)[^\\n\\r]{0,40}/i) || [''])[0];
              const timetable = (bodyText.match(/timetable[^\\n\\r]{0,60}/i) || [''])[0];
              const notifications = bodyText
                .split(/[\\.\\n]/)
                .map(x => x.trim())
                .filter(line => /deadline|due|submit|quiz|assignment|notice/i.test(line))
                .slice(0, 20);

              return JSON.stringify({
                assignments: assignments,
                announcements: announcements,
                notifications: notifications,
                attendanceSummary: attendance,
                resultSummary: results,
                timetableSummary: timetable
              });
            })();
            """;
    }

    private String escapeForJs(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
