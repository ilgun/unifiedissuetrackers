package Adaptors.JIRA;

import DatabaseConnectors.IssueTrackerConnector;
import b4j.core.*;
import b4j.core.session.BugzillaHttpSession;
import com.j2bugzilla.base.ConnectionException;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.join;

public class BugzillaAdapterMain {
    private static final AtomicInteger totalCount = new AtomicInteger();
    private static final Client client = new Client();
    private static final BugzillaHttpSession session = new BugzillaHttpSession();

    public BugzillaAdapterMain() {
    }

    public void run() throws IOException, ConnectionException {
        List<Integer> ids = getAllBugIds();
        System.out.println(ids.size());
        session.setBaseUrl(new URL("https://bugzilla.mozilla.org"));
        session.setBugzillaBugClass(DefaultIssue.class);
        session.open();
        for (Integer id : ids) {
            doForAnIssue(id);
        }
    }

    private void doForAnIssue(Integer id) {
        int count = totalCount.incrementAndGet();

        if ((count % 10) == 0) {
            System.out.println(count);
        }

        Issue issue = session.getIssue(String.valueOf(id));
        String uri = issue.getUri();
        String description = issue.getDescription();
        String assignee = issue.getAssignee().getName();
        Collection<Component> components = issue.getComponents();
        Collection<Comment> comments = issue.getComments();
        Collection<IssueLink> links = issue.getLinks();
        String priority = issue.getPriority().getName();
        String project = issue.getProject().getName();
        String summary = issue.getSummary();
        String type = issue.getType().getName();
        String status = issue.getStatus().getName();
        String resolution = issue.getResolution().getName();
        String reporter = issue.getReporter().getName();
        Collection<Version> release = issue.getFixVersions();
        String creationDate = issue.getCreationTimestamp().toString();

        Connection connection = new IssueTrackerConnector().getConnection();
        ResultSet rs = null;
        try {
            String sql = "INSERT INTO issues (`issueId`,\n" +
                    "`issueRepositoryId`,\n" +
                    "`issueAddress`,\n" +
                    "`assignee`,\n" +
                    "`reporter`,\n" +
                    "`priorityId`,\n" +
                    "`resolution`,\n" +
                    "`reportedDate`,\n" +
                    "`dueDate`,\n" +
                    "`currentEstimate`,\n" +
                    "`remainingEstimate`,\n" +
                    "`originalEstimate`,\n" +
                    "`state`,\n" +
                    "`description`,\n" +
                    "`product`,\n" +
                    "`components`,\n" +
                    "`release`,\n" +
                    "`summary`,\n" +
                    "`originalJson`)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, String.valueOf(id));
            //preparedStatement.setString(2, issueRepositoryId);
            preparedStatement.setString(3, uri);
            preparedStatement.setString(4, assignee);
            preparedStatement.setString(5, reporter);
            //preparedStatement.setString(6, priorityId);
            preparedStatement.setString(7, resolution);
            preparedStatement.setString(8, creationDate);
            preparedStatement.setString(9, null);
            preparedStatement.setString(10, null);
            preparedStatement.setString(11, null);
            preparedStatement.setString(12, null);
            preparedStatement.setString(13, status);
            preparedStatement.setString(14, description);
            preparedStatement.setString(15, project);
            preparedStatement.setString(16, join(components, ","));
            preparedStatement.setString(17, join(release, ","));
            preparedStatement.setString(18, summary);
            preparedStatement.setString(19, null);

            preparedStatement.executeUpdate();
        } catch (MySQLIntegrityConstraintViolationException ignored) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getAllBugIds() throws IOException {
        int startPoint = 0;
        List<Integer> allBugIds = newArrayList();
        while (true) {
            WebResource resource = client.resource("https://bugzilla.mozilla.org/rest/bug?include_fields=id&limit=0&product=Bugzilla&o1=greaterthan&f1=bug_id&v1=" + startPoint);
            ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
            String output = response.getEntity(String.class);

            JsonNode root = new ObjectMapper().readTree(output);
            JsonNode bugNodes = root.get("bugs");
            List<JsonNode> bugIds = bugNodes.findValues("id");
            if (bugIds.size() == 0) {
                break;
            }
            for (JsonNode ids : bugIds) {
                int id = ids.asInt();
                allBugIds.add(id);
                if (startPoint < id) {
                    startPoint = id;
                }
            }
        }
        return allBugIds;

    }

    public static void main(String[] args) throws IOException, ConnectionException {
        BugzillaAdapterMain main = new BugzillaAdapterMain();
        main.run();
    }
}
