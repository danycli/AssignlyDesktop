
package com.assignly.scratch;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.File;
import java.nio.file.Files;

public class ExtractHtml {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:assignly.db");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT category, value FROM academic_cache WHERE category LIKE 'html_%'");
        new File("scratch").mkdirs();
        while (rs.next()) {
            String url = rs.getString("category").substring(5); // remove 'html_'
            if (url.contains("aspx")) {
                String html = rs.getString("value");
                File f = new File("scratch/" + url.replace("/", "_"));
                Files.write(f.toPath(), html.getBytes("UTF-8"));
                System.out.println("Saved " + f.getAbsolutePath());
            }
        }
        rs.close();
        stmt.close();
        conn.close();
    }
}
