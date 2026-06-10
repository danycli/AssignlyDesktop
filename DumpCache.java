import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DumpCache {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:assignly.db";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT html FROM web_cache WHERE urlKey='Timetable.aspx'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    System.out.println(rs.getString("html"));
                } else {
                    System.out.println("Timetable.aspx not found in cache.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
