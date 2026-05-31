package com.assignly.service;

import com.assignly.view.FeeTabView.FeeHistoryTable;
import com.assignly.view.ResultTabView.SemesterResultTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testExportResultCard() {
        PdfExportService service = new PdfExportService();
        File outputFile = tempDir.resolve("result_card.pdf").toFile();

        List<SemesterResultTable> tables = List.of(
            new SemesterResultTable(
                "Spring 2023",
                List.of("Course Code", "Course Title", "Credit Hours", "Marks", "Grade", "GP"),
                List.of(
                    List.of("CSC101", "Introduction to Computing", "4", "85", "A", "4.0"),
                    List.of("MTH101", "Calculus I", "3", "78", "B", "3.0")
                )
            )
        );

        assertDoesNotThrow(() -> {
            service.exportResultCard("Test Student", "SP23-BCS-001", tables, outputFile);
        });

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    public void testExportFeeHistory() {
        PdfExportService service = new PdfExportService();
        File outputFile = tempDir.resolve("fee_history.pdf").toFile();

        List<FeeHistoryTable> tables = List.of(
            new FeeHistoryTable(
                "Fee Details",
                List.of("Semester", "Challan No", "Amount Paid", "Status"),
                List.of(
                    List.of("Spring 2023", "12345", "50000", "Paid"),
                    List.of("Fall 2023", "67890", "55000", "Paid")
                )
            )
        );

        assertDoesNotThrow(() -> {
            service.exportFeeHistory("Test Student", "SP23-BCS-001", tables, outputFile);
        });

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }
}
