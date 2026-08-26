import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.raexjkuowzwgxtstwvsg";
        String password = "Buildhub@2026";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version, description, success FROM lms.flyway_schema_history")) {
            while (rs.next()) {
                System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getBoolean(3));
            }
        }
    }
}
