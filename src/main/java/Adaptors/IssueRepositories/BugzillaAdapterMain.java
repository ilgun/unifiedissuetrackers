package Adaptors.IssueRepositories;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import DatabaseConnectors.IssueTrackerConnector;
import b4j.core.*;
import b4j.core.session.BugzillaHttpSession;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.Integer.parseInt;
import static java.sql.Types.INTEGER;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

public class BugzillaAdapterMain {
    private static final Logger LOGGER = getLogger(BugzillaAdapterMain.class);
    private final AtomicInteger totalCount = new AtomicInteger();
    private final Client client;
    private final Connection connection;
    private final BugzillaHttpSession session;
    private final String repositoryUrl;
    private final String repositoryType;
    private final String projectName;
    private final String projectUrl;
    private int projectId;
    private int issueRepositoryId;
    private DatabaseHelperMethods helperMethods;

    public BugzillaAdapterMain(Client client, Connection connection, BugzillaHttpSession session, String projectName, String projectUrl, String repositoryUrl, String repositoryType) {
        this.client = client;
        this.connection = connection;
        this.session = session;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
        this.repositoryUrl = repositoryUrl;
        this.repositoryType = repositoryType;
    }

    public static void main(String[] args) throws IOException {
        BugzillaAdapterMain main = new BugzillaAdapterMain(
                new Client(),
                new IssueTrackerConnector().getConnection(),
                new BugzillaHttpSession(),
                "PULP",
                "http://www.pulpproject.org",
                "https://bugzilla.redhat.com",
                "BUGZILLA");

        main.run();
    }

    public void run() throws IOException {
        List<Integer> ids = getAllBugsIdsFromFile();
        LOGGER.info(ids.size());
        session.setBaseUrl(new URL(repositoryUrl));
        session.setBugzillaBugClass(DefaultIssue.class);
        session.open();

        client.setReadTimeout(60000);
        client.setConnectTimeout(20000);

        LOGGER.info("Session LoggedIn: " + session.isLoggedIn());
        helperMethods = new DatabaseHelperMethods(connection);
        projectId = helperMethods.getOrCreateProject(projectName, projectUrl);
        issueRepositoryId = helperMethods.getOrCreateIssueRepository(repositoryUrl, repositoryType, projectId);

        for (Integer id : ids) {
            doForAnIssue(id);
            logCount();
        }
        LOGGER.info("Finished");
    }

    private void logCount() {
        totalCount.incrementAndGet();
        if ((totalCount.get() % 100) == 0) {
            LOGGER.info(totalCount.get());
        }
    }

    private void doForAnIssue(Integer id) throws IOException {
        Issue issue = session.getIssue(String.valueOf(id));
        String issueId = issue.getId();
        String issueAddress = issue.getUri();
        String description = issue.getDescription();
        Collection<Component> components = issue.getComponents();
        Collection<Comment> comments = issue.getComments();
        String projectName = issue.getProject().getName();
        String summary = issue.getSummary();
        String status = issue.getStatus().getName();
        String resolutionStatus = issue.getResolution().getName();
        Collection<Version> release = issue.getFixVersions();
        String createdDate = issue.getCreationTimestamp().toString();
        Collection<IssueLink> links = issue.getLinks();
        String issueType = issue.getType().getName();

        int assigneeUserId = getAssigneeId(issue.getAssignee());
        int reporterUserId = getReporterId(issue.getReporter());
        int priorityId = getPriorityId(issue.getPriority());

        DateTime dueDate = null;
        Hours currentEstimate = null;
        Hours originalEstimate = null;
        int databaseIssueId = saveIssue(issueId, issueType, summary, reporterUserId, createdDate, description, priorityId, status, projectName, components, dueDate, assigneeUserId, currentEstimate,
                issueAddress, release, resolutionStatus, originalEstimate);

        if (databaseIssueId == 0) return;
        saveIssueLinks(links, databaseIssueId);
        saveHistory(parseInt(issueId), databaseIssueId);
        saveComments(comments, databaseIssueId);
    }

    private void saveIssueLinks(Collection<IssueLink> links, int databaseIssueId) {
        for (IssueLink issueLink : links) {
            String linkType = issueLink.getLinkTypeName();
            String linkedIssueId = issueLink.getIssueId();
            helperMethods.saveIssueLink(issueRepositoryId, databaseIssueId, linkType, linkedIssueId);
        }
    }

    public void saveComments(Collection<Comment> comments, int databaseIssueId) {
        for (Comment aComment : comments) {
            String content = aComment.getTheText();
            String authorEmail = aComment.getAuthor().getName();
            String authorName = aComment.getAuthor().getRealName();
            helperMethods.saveComment(issueRepositoryId, databaseIssueId, authorName, authorEmail, content);
        }
    }

    public void saveHistory(int issueId, int databaseIssueId) throws IOException {
        String historyUrl = repositoryUrl + "/show_activity.cgi?id=" + issueId;
        WebResource resource = client.resource(historyUrl);
        ClientResponse response = resource.accept(TEXT_HTML).get(ClientResponse.class);
        String output = response.getEntity(String.class);

        Document doc = parse(output);
        Elements elements = doc.getElementsByTag("tr");

        String username;
        String date = null;
        int userId = 0;
        int count = 0;
        for (Element anElement : elements) {
            count++;
            if (count < 4) continue;
            Elements tds = anElement.getElementsByTag("td");

            // New User Found
            if (tds.size() > 4) {
                username = tds.get(0).text();
                date = tds.get(1).text();
                String field = tds.get(2).text();
                String from = tds.get(3).text();
                String to = tds.get(4).text();
                userId = helperMethods.createOrGetRepositoryUser(username, username, issueRepositoryId);
                helperMethods.saveHistoryIfNotExists(databaseIssueId, from, to, field, userId, date);
            } // User is same however, there is new activity.
            else {
                String what = tds.get(0).text();
                String removed = tds.get(1).text();
                String added = tds.get(2).text();
                helperMethods.saveHistoryIfNotExists(databaseIssueId, removed, added, what, userId, date);
            }
        }
    }

    public int getPriorityId(Priority priority) {
        String priorityName = priority.getName();
        return helperMethods.createOrGetPriorityId(priorityName, priorityName, issueRepositoryId);
    }

    public int getReporterId(User reporter) {
        String assigneeName = reporter.getRealName();
        String assigneeEmail = reporter.getName();
        if (isEmpty(assigneeName) && isEmpty(assigneeEmail)) {
            return 0;
        }
        if (isEmpty(assigneeName)) {
            assigneeName = assigneeEmail;
        }
        if (isEmpty(assigneeEmail)) {
            assigneeEmail = assigneeName;
        }
        return helperMethods.createOrGetRepositoryUser(assigneeName, assigneeEmail, issueRepositoryId);
    }

    public int getAssigneeId(User assignee) {
        String assigneeName = assignee.getRealName();
        String assigneeEmail = assignee.getName();
        if (isEmpty(assigneeName) && isEmpty(assigneeEmail)) {
            return 0;
        }
        if (isEmpty(assigneeName)) {
            assigneeName = assigneeEmail;
        }
        if (isEmpty(assigneeEmail)) {
            assigneeEmail = assigneeName;
        }
        return helperMethods.createOrGetRepositoryUser(assigneeName, assigneeEmail, issueRepositoryId);
    }

    public int saveIssue(String issueId, String issueType, String summary, int reporterUserId, String createdDate, String description, int priorityId, String status, String projectName,
                         Collection<Component> componentNames, DateTime dueDate, int assigneeUserId, Hours currentEstimate, String issueAddress, Collection<Version> release, String resolutionStatus,
                         Hours originalEstimate) {
        Map<Integer, TableColumnName> intMap = newHashMap();
        intMap.put(Integer.valueOf(issueId), TableColumnName.issueId);
        Map<String, TableColumnName> stringMap = newHashMap();
        stringMap.put(issueAddress, TableColumnName.issueAddress);

        int foundIssueId = helperMethods.checkIfExits("issues", stringMap, intMap);
        if (foundIssueId != 0) return 0;

        int newIssueId = 0;
        try {
            String sql = "INSERT INTO issues (`issueId`,\n" +
                    "`issueRepositoryId`,\n" +
                    "`issueAddress`,\n" +
                    "`assigneeUserId`,\n" +
                    "`reporterUserId`,\n" +
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
                    "`issueType`,\n" +
                    "`summary`)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql, new String[]{"id"});
            preparedStatement.setString(1, issueId);
            preparedStatement.setInt(2, issueRepositoryId);
            preparedStatement.setString(3, issueAddress);
            if (assigneeUserId != 0) {
                preparedStatement.setInt(4, assigneeUserId);
            } else {
                preparedStatement.setNull(4, INTEGER);
            }
            preparedStatement.setInt(5, reporterUserId);
            preparedStatement.setInt(6, priorityId);
            preparedStatement.setString(7, resolutionStatus);
            if (createdDate != null) {
                preparedStatement.setString(8, createdDate.toString());
            } else {
                preparedStatement.setString(8, null);
            }
            if (dueDate != null) {
                preparedStatement.setString(9, dueDate.toString());
            } else {
                preparedStatement.setString(9, null);
            }
            if (currentEstimate != null) {
                preparedStatement.setInt(10, currentEstimate.getHours());
            } else {
                preparedStatement.setString(10, null);
            }

            preparedStatement.setString(11, null);

            if (originalEstimate != null) {
                preparedStatement.setInt(12, originalEstimate.getHours());
            } else {
                preparedStatement.setString(12, null);
            }

            preparedStatement.setString(13, status);
            preparedStatement.setString(14, description);
            preparedStatement.setString(15, projectName);
            preparedStatement.setString(16, join(componentNames, ","));
            preparedStatement.setString(17, join(release, ","));
            preparedStatement.setString(18, issueType);
            preparedStatement.setString(19, summary);

            if (preparedStatement.executeUpdate() > 0) {
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    newIssueId = generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return newIssueId;
    }

    private List<Integer> getAllBugsIdsFromFile() throws IOException {
        List<Integer> ids = newArrayList();
        String line = "";

        File csvFile = new File(getFilePath("bugs-2015-01-15.csv"));
        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        //For the first line
        br.readLine();
        while ((line = br.readLine()) != null) {
            ids.add(parseInt(line));
        }
        return ids;
    }

    //example query
    //https://bugzilla.redhat.com/buglist.cgi?columnlist=&limit=0&product=pulp&query_format=advanced&ctype=csv&human=1
    private String getFilePath(String fileName) {
        return getSystemClassLoader().getResource(fileName).getPath();
    }

    /**
     * This method is designed for Bugzilla repositories which support REST API.
     *
     * @return
     * @throws IOException
     */
    private List<Integer> getAllBugIdsForRestAPI() throws IOException {
        int startPoint = 0;
        List<Integer> allBugIds = newArrayList();
        while (true) {
            WebResource resource = client.resource(repositoryUrl + "/rest/bug?include_fields=id&limit=0&product=" + projectName + "&o1=greaterthan&f1=bug_id&v1=" + startPoint);
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

    /*
    * This method is designed for Bugzilla repositories which support REST API.
    * */
    public void saveHistoryForRest(String issueId, int databaseIssueId) throws IOException {
        WebResource resource = client.resource(repositoryUrl + "/rest/bug/" + issueId + "/history");
        ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
        String output = response.getEntity(String.class);
        JsonNode root = new ObjectMapper().readTree(output);
        JsonNode bugs = root.get("bugs");
        JsonNode histories = bugs.get(0).get("history");
        if (histories == null || histories.isNull()) return;
        for (JsonNode history : histories) {
            if (history.isNull() || history.get("who") == null || history.get("who").isNull()) {
                continue;
            }
            String author = null;
            if (!history.get("who").isNull()) {
                author = history.get("who").asText();
            }
            String date = null;
            if (!history.get("when").isNull()) {
                date = history.get("when").asText();
            }

            int userId = helperMethods.createOrGetRepositoryUser(author, author, issueRepositoryId);
            JsonNode changes = history.get("changes");
            for (JsonNode anItem : changes) {
                String field = anItem.get("field_name").asText();
                String from = anItem.get("removed").asText();
                String to = anItem.get("added").asText();
                helperMethods.saveHistoryIfNotExists(databaseIssueId, from, to, field, userId, date);
            }
        }
    }
}
