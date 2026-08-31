import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility script to check Supabase Database connection and inspect LMS schema user statuses.
 */
public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.raexjkuowzwgxtstwvsg";
        String password = "Buildhub@2026";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // Display user statuses
            String query = "SELECT u.id, u.email, u.is_active, u.is_locked, u.name, r.name as role_name " +
                           "FROM lms.users u " +
                           "LEFT JOIN lms.user_role ur ON u.id = ur.user_id " +
                           "LEFT JOIN lms.roles r ON ur.role_id = r.id";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                System.out.println("--- User Accounts Status ---");
                while (rs.next()) {
                    System.out.println(rs.getString("email") + " | Active: " + rs.getBoolean("is_active") + " | Locked: " + rs.getBoolean("is_locked") + " | Role: " + rs.getString("role_name"));
                }
            }
        }
    }
}
