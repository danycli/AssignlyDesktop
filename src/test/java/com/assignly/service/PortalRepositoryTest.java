package com.assignly.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PortalRepositoryTest {

    private PortalRepository portalRepository;

    @BeforeEach
    public void setup() {
        portalRepository = new PortalRepository();
    }

    private String loadSnapshot(String filename) throws IOException {
        return Files.readString(Paths.get("src/test/resources/snapshots", filename));
    }

    @Test
    public void testParseGpaHistory_Standard() throws IOException {
        String html = loadSnapshot("result_card_standard.html");
        List<PortalRepository.GpaHistoryData> history = portalRepository.parseGpaHistory(html);
        
        assertEquals(2, history.size());
        assertEquals("Spring 2023 Semester", history.get(0).semesterTitle());
        assertEquals(3.85, history.get(0).sgpa(), 0.001);
        assertEquals(3.90, history.get(0).cgpa(), 0.001);

        assertEquals("Fall 2023 Semester", history.get(1).semesterTitle());
        assertEquals(3.50, history.get(1).sgpa(), 0.001);
        assertEquals(3.75, history.get(1).cgpa(), 0.001);
    }

    @Test
    public void testParseGpaHistory_BrokenLayout() throws IOException {
        String html = loadSnapshot("result_broken_layout.html");
        List<PortalRepository.GpaHistoryData> history = portalRepository.parseGpaHistory(html);
        
        assertEquals(1, history.size());
        assertEquals("Spring 2024", history.get(0).semesterTitle());
        assertEquals(3.95, history.get(0).sgpa(), 0.001);
        assertEquals(3.88, history.get(0).cgpa(), 0.001);
    }

    @Test
    public void testParseScholarships_Standard() throws IOException {
        String html = loadSnapshot("scholarship_standard.html");
        List<PortalRepository.ScholarshipTable> scholarships = portalRepository.parseScholarships(html);
        
        assertEquals(1, scholarships.size());
        PortalRepository.ScholarshipTable table = scholarships.get(0);
        assertEquals("Scholarship Status", table.title());
        assertEquals("Title", table.headers().get(0));
        assertEquals(2, table.data().size());
        assertEquals("Merit Scholarship", table.data().get(0).get(0));
        assertEquals("Approved", table.data().get(0).get(2));
    }

    @Test
    public void testParseScholarships_BrokenLayout() throws IOException {
        String html = loadSnapshot("scholarship_broken_layout.html");
        List<PortalRepository.ScholarshipTable> scholarships = portalRepository.parseScholarships(html);
        
        assertEquals(1, scholarships.size());
        PortalRepository.ScholarshipTable table = scholarships.get(0);
        assertEquals("Financial Aid Status", table.title());
        assertEquals("Title", table.headers().get(0));
        assertEquals(1, table.data().size());
        assertEquals("PEEF", table.data().get(0).get(0));
        assertEquals("80000", table.data().get(0).get(1));
    }

    @Test
    public void testParseScholarships_MultipleGrids() {
        String html = "<!DOCTYPE html><html><body>" +
            "<h2>Active Awards</h2>" +
            "<table id='gridActive'>" +
            "  <tr><th>Name</th><th>Status</th></tr>" +
            "  <tr><td>HEC Need Based</td><td>Active</td></tr>" +
            "</table>" +
            "<h2>Applied Status</h2>" +
            "<table id='gridApplied'>" +
            "  <tr><th>Name</th><th>Status</th></tr>" +
            "  <tr><td>PEEF Scholarship</td><td>Under Process</td></tr>" +
            "</table>" +
            "</body></html>";
        List<PortalRepository.ScholarshipTable> tables = portalRepository.parseScholarships(html);
        assertEquals(2, tables.size());
        
        assertEquals("Active Awards", tables.get(0).title());
        assertEquals("Name", tables.get(0).headers().get(0));
        assertEquals("HEC Need Based", tables.get(0).data().get(0).get(0));
        
        assertEquals("Applied Status", tables.get(1).title());
        assertEquals("Name", tables.get(1).headers().get(0));
        assertEquals("PEEF Scholarship", tables.get(1).data().get(0).get(0));
    }

    @Test
    public void testParseScholarships_ColspanTitleRow() {
        // Simulates the real portal structure where a single-cell "title" row spans all columns
        String html = "<!DOCTYPE html><html><body>" +
            "<h3>Student Console</h3>" +
            "<table id='gvScholarship' class='gridview'>" +
            "  <tr><th colspan='12'>Scholarship Awarded Information</th></tr>" +
            "  <tr><td>S.No</td><td>Scholarship</td><td>Date</td><td>Session</td><td>Fee</td><td>Meal</td></tr>" +
            "  <tr><td>1</td><td>Shining Star</td><td>22/09/2025</td><td>Fall 2025</td><td>30000</td><td>0</td></tr>" +
            "  <tr><td>2</td><td>Shining Star</td><td>15/04/2025</td><td>Spring 2025</td><td>20000</td><td>0</td></tr>" +
            "</table>" +
            "</body></html>";
        List<PortalRepository.ScholarshipTable> tables = portalRepository.parseScholarships(html);
        assertEquals(1, tables.size());
        PortalRepository.ScholarshipTable table = tables.get(0);
        // The actual headers should be S.No, Scholarship, Date, etc. — not "Scholarship Awarded Information"
        assertEquals(6, table.headers().size());
        assertEquals("S.No", table.headers().get(0));
        assertEquals("Scholarship", table.headers().get(1));
        assertEquals(2, table.data().size());
        assertEquals("Shining Star", table.data().get(0).get(1));
    }

    @Test
    public void testParseScholarships_ExcludesPersonalInfoAndWrappers() {
        // Simulates a portal page containing a personal info table,
        // and a nested gridview wrapper table to verify exclusion filters.
        String html = "<!DOCTYPE html><html><body>" +
            "<!-- Personal Info Table (Should be excluded) -->" +
            "<table class='gridview'>" +
            "  <tr><td>Registration No:</td><td>SP25-BCS-001</td></tr>" +
            "  <tr><td>Student Name:</td><td>Danial Ahmed</td></tr>" +
            "  <tr><td>Father Name:</td><td>Father's Name</td></tr>" +
            "</table>" +
            "" +
            "<!-- Outer wrapper table (Should be skipped) -->" +
            "<table id='outerWrapper' class='gridview'>" +
            "  <tr><td>" +
            "    <!-- Inner actual data table (Should be parsed) -->" +
            "    <table id='gvScholarship' class='gridview'>" +
            "      <tr><th>S.No</th><th>Scholarship</th><th>Status</th></tr>" +
            "      <tr><td>1</td><td>Need Based</td><td>Awarded</td></tr>" +
            "    </table>" +
            "  </td></tr>" +
            "</table>" +
            "</body></html>";

        List<PortalRepository.ScholarshipTable> tables = portalRepository.parseScholarships(html);
        // Only the inner HEC Need Based table should be parsed. The personal info and outer wrapper must be ignored.
        assertEquals(1, tables.size());
        PortalRepository.ScholarshipTable table = tables.get(0);
        assertEquals("S.No", table.headers().get(0));
        assertEquals("Need Based", table.data().get(0).get(1));
    }
}
