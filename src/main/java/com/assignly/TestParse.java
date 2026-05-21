package com.assignly;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestParse {
    private static final List<String> SKIP = List.of("details", "update", "delete", "edit", "action", "status", "view");

    public static void main(String[] args) throws Exception {
        String html = Files.readString(Paths.get("marks_raw.html"));
        Document doc = Jsoup.parse(html);
        
        System.out.println("Found " + doc.select("table").size() + " tables.");
        int validTables = 0;
        
        for (Element table : doc.select("table")) {
            String tt = table.text().toLowerCase();
            if (tt.contains("father name") || tt.contains("cnic")) {
                System.out.println("Skipped table containing father name");
                continue;
            }
            Elements rows = table.select("tr");
            if (rows.size() < 2) {
                System.out.println("Skipped table with < 2 rows");
                continue;
            }
            
            System.out.println("Processing table with " + rows.size() + " rows");
            Object card = buildNativeTable(rows);
            if (card != null) {
                validTables++;
                System.out.println(" -> Successfully built card!");
            } else {
                System.out.println(" -> buildNativeTable returned null");
            }
        }
        System.out.println("Total valid tables: " + validTables);
    }
    
    private static Object buildNativeTable(Elements rows) {
        Element hdrRow = null;
        int hdrIndex = 0;
        for (int i = 0; i < rows.size(); i++) {
            Element r = rows.get(i);
            Elements c = r.select("th, td");
            if (c.size() > 1) {
                hdrRow = r;
                hdrIndex = i;
                break;
            }
        }
        if (hdrRow == null) {
            System.out.println("    null: no header row found");
            return null;
        }

        Elements hdrCells = hdrRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element c : hdrCells) { String t = c.text().trim(); if (!t.isEmpty()) headers.add(t); }
        for (String h : headers) {
            if (SKIP.contains(h.toLowerCase())) {
                System.out.println("    null: SKIP contains " + h);
                return null;
            }
        }
        if (headers.isEmpty()) {
            System.out.println("    null: headers empty");
            return null;
        }
        return new Object();
    }
}
