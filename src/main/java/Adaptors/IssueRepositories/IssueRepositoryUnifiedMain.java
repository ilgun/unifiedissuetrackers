package Adaptors.IssueRepositories;

import b4j.core.session.BugzillaHttpSession;
import com.sun.jersey.api.client.Client;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static Adaptors.HelperMethods.JerseyClientHandler.getJerseyClient;
import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;

public class IssueRepositoryUnifiedMain {
    public static void main(String[] args) throws IOException, SQLException {
        Client jerseyClient = getJerseyClient();
        Connection databaseConnection = getDatabaseConnection();

        BugzillaIngestion bugzillaIngestor = new BugzillaIngestion(
                jerseyClient,
                databaseConnection,
                new BugzillaHttpSession(),
                "PULP",
                "http://www.pulpproject.org",
                "https://bugzilla.redhat.com",
                "BUGZILLA");
        bugzillaIngestor.run();

        JiraIngestion hiveIngestion = new JiraIngestion(
                jerseyClient,
                databaseConnection,
                "HIVE",
                "https://hive.apache.org",
                "https://issues.apache.org/jira",
                "JIRA");
        hiveIngestion.run();

        JiraIngestion hibernateIngestion = new JiraIngestion(
                jerseyClient,
                databaseConnection,
                "HIBERNATE",
                "http://hibernate.org",
                "https://hibernate.atlassian.net",
                "JIRA");
        hibernateIngestion.run();

    }
}
