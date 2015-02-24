package Adaptors.IssueRepositories;

import b4j.core.session.BugzillaHttpSession;
import com.sun.jersey.api.client.Client;

import java.io.IOException;
import java.sql.SQLException;

import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;

public class IssueRepositoryUnifiedMain {
    public static void main(String[] args) throws IOException, SQLException {
        BugzillaIngestion bugzillaIngestor = new BugzillaIngestion(
                new Client(),
                getDatabaseConnection(),
                new BugzillaHttpSession(),
                "PULP",
                "http://www.pulpproject.org",
                "https://bugzilla.redhat.com",
                "BUGZILLA");

        JiraIngestion hiveIngestion = new JiraIngestion(
                new Client(),
                getDatabaseConnection(),
                "HIVE",
                "https://hive.apache.org",
                "https://issues.apache.org/jira",
                "JIRA");

        JiraIngestion hibernateIngestion = new JiraIngestion(
                new Client(),
                getDatabaseConnection(),
                "HIBERNATE",
                "http://hibernate.org",
                "https://hibernate.atlassian.net",
                "JIRA");

        hiveIngestion.run();
        hibernateIngestion.run();
        bugzillaIngestor.run();
    }
}
