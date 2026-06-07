package com.assignly.service;

import com.assignly.view.FeeTabView.FeeHistoryTable;
import com.assignly.view.ResultTabView.SemesterResultTable;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfExportService {
    private static final Color CYPRUS_GREEN = new Color(0, 70, 67);
    private static final Color SOFT_ROW = new Color(236, 244, 243);
    private static final Color SAND_ROW = new Color(249, 248, 247);
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, CYPRUS_GREEN);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, CYPRUS_GREEN);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font BODY_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

    public void exportResultCard(String studentName, String regNo, List<SemesterResultTable> tables, File outputFile) {
        if (tables == null || tables.isEmpty()) {
            throw new IllegalArgumentException("No result data to export.");
        }
        exportResultDocument(studentName, regNo, "Academic Result Card", tables, outputFile, true);
    }

    public void exportFeeHistory(String studentName, String regNo, List<FeeHistoryTable> tables, File outputFile) {
        if (tables == null || tables.isEmpty()) {
            throw new IllegalArgumentException("No fee history data to export.");
        }
        exportFeeDocument(studentName, regNo, "Fee History Report", tables, outputFile);
    }

    private void exportResultDocument(String studentName, String regNo, String docTitle, List<SemesterResultTable> tables, File outputFile, boolean highlightGpaRows) {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new PremiumPageEventHelper("COMSATS UNIVERSITY ISLAMABAD"));
            document.open();

            addHeaderBlock(document, docTitle, studentName, regNo);

            for (SemesterResultTable table : tables) {
                addTableSection(document, table.title(), table.headers(), table.data(), highlightGpaRows);
            }

            document.close();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Failed to export PDF: " + ex.getMessage(), ex);
        }
    }

    private void exportFeeDocument(String studentName, String regNo, String docTitle, List<FeeHistoryTable> tables, File outputFile) {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new PremiumPageEventHelper("COMSATS UNIVERSITY ISLAMABAD"));
            document.open();

            addHeaderBlock(document, docTitle, studentName, regNo);

            for (FeeHistoryTable table : tables) {
                addTableSection(document, table.title(), table.headers(), table.data(), false);
            }

            document.close();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Failed to export PDF: " + ex.getMessage(), ex);
        }
    }

    private byte[] loadLogoBytes() {
        try {
            java.io.File file = new java.io.File(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cui_logo.png");
            if (file.exists()) {
                return java.nio.file.Files.readAllBytes(file.toPath());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private PdfPTable buildFallbackInitialsBadge() {
        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setTotalWidth(50f);
        badgeTable.setLockedWidth(true);
        badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        PdfPCell badgeCell = new PdfPCell(new Phrase("CUI", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE)));
        badgeCell.setBackgroundColor(CYPRUS_GREEN);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        badgeCell.setFixedHeight(50f);
        badgeCell.setBorder(Rectangle.BOX);
        badgeCell.setBorderColor(CYPRUS_GREEN);
        badgeCell.setBorderWidth(1f);
        
        badgeTable.addCell(badgeCell);
        return badgeTable;
    }

    private void addHeaderBlock(Document document, String subtitle, String name, String regNo) throws DocumentException {
        // 1. Create a 2-column header table (Logo + Titles)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{15, 85});
        headerTable.setSpacingAfter(10);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        byte[] logoBytes = loadLogoBytes();
        if (logoBytes != null && logoBytes.length > 0) {
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(logoBytes);
                logo.scaleToFit(50, 50);
                logoCell.addElement(logo);
            } catch (Exception e) {
                logoCell.addElement(buildFallbackInitialsBadge());
            }
        } else {
            logoCell.addElement(buildFallbackInitialsBadge());
        }
        headerTable.addCell(logoCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Paragraph title = new Paragraph("COMSATS University Islamabad", TITLE_FONT);
        Paragraph docSub = new Paragraph(subtitle, SUBTITLE_FONT);
        Paragraph genInfo = new Paragraph("Official Report \u2022 Generated on: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date()), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
        
        textCell.addElement(title);
        textCell.addElement(docSub);
        textCell.addElement(genInfo);
        headerTable.addCell(textCell);

        headerTable.completeRow();
        document.add(headerTable);

        // 2. Add Student Info Cards Row
        // 2. Add Student Info Cards Row (Styled as a card matching the Sand theme)
        if (name == null) name = "N/A";
        if (regNo == null) regNo = "N/A";

        PdfPTable cardTable = new PdfPTable(1);
        cardTable.setWidthPercentage(100);
        cardTable.setSpacingAfter(15);

        PdfPCell cardCell = new PdfPCell();
        cardCell.setBackgroundColor(SAND_ROW);
        cardCell.setPadding(10);
        cardCell.setBorder(Rectangle.LEFT | Rectangle.BOX);
        cardCell.setBorderColor(new Color(224, 219, 217)); // sand border
        cardCell.setBorderColorLeft(CYPRUS_GREEN); // accent border
        cardCell.setBorderWidthLeft(3.5f);
        cardCell.setBorderWidth(0.5f);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{55, 45});

        PdfPCell nameCell = new PdfPCell(new Phrase("STUDENT NAME:   " + name.toUpperCase(), BODY_BOLD_FONT));
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPadding(2);
        infoTable.addCell(nameCell);

        PdfPCell regCell = new PdfPCell(new Phrase("REGISTRATION NO:  " + regNo.toUpperCase(), BODY_BOLD_FONT));
        regCell.setBorder(Rectangle.NO_BORDER);
        regCell.setPadding(2);
        infoTable.addCell(regCell);

        infoTable.completeRow();
        cardCell.addElement(infoTable);
        cardTable.addCell(cardCell);
        document.add(cardTable);

        // 3. Add dividing line
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderWidthBottom(1.5f);
        lineCell.setBorderColorBottom(CYPRUS_GREEN);
        lineCell.setFixedHeight(1);
        lineTable.addCell(lineCell);
        lineTable.setSpacingAfter(15);
        document.add(lineTable);
    }

    private int getAlignmentForHeader(String header) {
        if (header == null) return Element.ALIGN_LEFT;
        String l = header.toLowerCase();
        if (l.contains("gpa") || l.contains("cgpa") || l.contains("sgpa") || l.contains("marks") || 
            l.contains("percentage") || l.contains("cr. hr") || l.contains("credit") || 
            l.contains("amount") || l.contains("ch") || l.contains("status")) {
            return Element.ALIGN_CENTER;
        }
        return Element.ALIGN_LEFT;
    }

    private void addTableSection(Document document, String title, List<String> headers, List<List<String>> data, boolean highlightGpaRows)
            throws DocumentException {
        if (headers == null || headers.isEmpty()) return;
        List<List<String>> rows = data == null ? List.of() : data;

        Paragraph sectionTitle = new Paragraph(title, SECTION_FONT);
        sectionTitle.setSpacingBefore(6);
        sectionTitle.setSpacingAfter(6);
        document.add(sectionTitle);

        PdfPTable pdfTable = new PdfPTable(headers.size());
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingAfter(12);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(CYPRUS_GREEN);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(Color.WHITE);
            cell.setBorderWidth(1.5f);
            cell.setPadding(8);
            cell.setHorizontalAlignment(getAlignmentForHeader(header));
            pdfTable.addCell(cell);
        }

        boolean shade = false;
        for (List<String> row : rows) {
            boolean isGpaRow = highlightGpaRows && row != null && row.stream().anyMatch(this::isGpaCell);
            if (isGpaRow) {
                // Find and combine the non-empty text in the GPA row
                StringBuilder sb = new StringBuilder();
                if (row != null) {
                    for (String val : row) {
                        if (val != null && !val.trim().isEmpty()) {
                            if (sb.length() > 0) sb.append("     |     ");
                            sb.append(val.trim());
                        }
                    }
                }
                PdfPCell cell = new PdfPCell(new Phrase(sb.toString(), BODY_BOLD_FONT));
                cell.setColspan(headers.size());
                cell.setPadding(8);
                cell.setBorder(Rectangle.BOTTOM | Rectangle.TOP);
                cell.setBorderColor(new Color(224, 219, 217)); // sand border
                cell.setBorderWidth(1f);
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cell.setBackgroundColor(SOFT_ROW);
                pdfTable.addCell(cell);
            } else {
                for (int i = 0; i < headers.size(); i++) {
                    String value = row != null && i < row.size() ? row.get(i) : "";
                    PdfPCell cell = new PdfPCell(new Phrase(value, BODY_FONT));
                    cell.setPadding(7);
                    cell.setBorder(Rectangle.BOTTOM); // Clean horizontal separator lines only
                    cell.setBorderColor(new Color(230, 225, 222)); // light sand border
                    cell.setBorderWidth(0.5f);
                    cell.setHorizontalAlignment(getAlignmentForHeader(headers.get(i)));
                    if (shade) {
                        cell.setBackgroundColor(SAND_ROW);
                    } else {
                        cell.setBackgroundColor(Color.WHITE);
                    }
                    pdfTable.addCell(cell);
                }
                shade = !shade;
            }
        }

        document.add(pdfTable);
    }

    private boolean isGpaCell(String value) {
        if (value == null) return false;
        String normalized = value.toUpperCase();
        return normalized.contains("SGPA") || normalized.contains("CGPA");
    }

    private static class PremiumPageEventHelper extends com.lowagie.text.pdf.PdfPageEventHelper {
        private final String watermarkText;
        public PremiumPageEventHelper(String watermarkText) {
            this.watermarkText = watermarkText;
        }
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            
            // 1. Draw Watermark under content with transparent alpha
            com.lowagie.text.pdf.PdfContentByte cbUnder = writer.getDirectContentUnder();
            cbUnder.saveState();
            try {
                com.lowagie.text.pdf.PdfGState gstate = new com.lowagie.text.pdf.PdfGState();
                gstate.setFillOpacity(0.04f); // 4% opacity (extremely faint and professional)
                cbUnder.setGState(gstate);
                cbUnder.setColorFill(CYPRUS_GREEN); // Cyprus Green watermark
                
                Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42);
                com.lowagie.text.pdf.BaseFont bf = font.getCalculatedBaseFont(false);
                cbUnder.beginText();
                cbUnder.setFontAndSize(bf, 42);
                float x = (document.right() - document.left()) / 2 + document.leftMargin();
                float y = (document.top() - document.bottom()) / 2 + document.bottomMargin();
                cbUnder.showTextAligned(Element.ALIGN_CENTER, watermarkText, x, y, 45);
                cbUnder.endText();
            } catch (Exception ignored) {}
            cbUnder.restoreState();
            
            // 2. Draw Page Footer
            cb.saveState();
            Phrase footer = new Phrase("Generated by Assignly Desktop \u2022 Page " + writer.getPageNumber() + " \u2022 " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
            com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 10, 0);
            cb.restoreState();
        }
    }
}
