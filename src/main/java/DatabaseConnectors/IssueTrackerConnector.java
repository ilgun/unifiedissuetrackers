package DatabaseConnectors;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class IssueTrackerConnector {
    public Connection getConnection() {
        Connection conn = null;
        try {
            // this will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // setup the connection with the DB.
            conn = DriverManager.getConnection("jdbc:mysql://localhost/issueTrackers?" +
                    "user=root&password=156609768&autoReconnect=true&tcpKeepAlive=true&failOverReadOnly=false&maxReconnects=10");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn = DriverManager.getConnection("jdbc:mysql://localhost/issueTrackers?" +
                        "user=root&password=156609768&autoReconnect=true&tcpKeepAlive=true&failOverReadOnly=false&maxReconnects=10");
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        return conn;
    }
}
