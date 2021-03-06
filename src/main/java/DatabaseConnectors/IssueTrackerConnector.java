package DatabaseConnectors;

import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.Class.forName;
import static java.sql.DriverManager.getConnection;

public class IssueTrackerConnector {
    public static Connection getDatabaseConnection() {
        Connection conn = null;
        try {
            forName("com.mysql.jdbc.Driver");
            conn = getConnection("jdbc:mysql://localhost/issueTrackers?" +
                    "user=root&password=156609768&autoReconnect=true&tcpKeepAlive=true&failOverReadOnly=false&maxReconnects=10&useUnicode=true&amp;characterEncoding=UTF8MB4");
            conn.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn = getConnection("jdbc:mysql://localhost/issueTrackers?" +
                        "user=root&password=156609768&autoReconnect=true&tcpKeepAlive=true&failOverReadOnly=false&maxReconnects=10&useUnicode=true&amp;characterEncoding=UTF8MB4");
                conn.setAutoCommit(false);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        return conn;
    }
}
