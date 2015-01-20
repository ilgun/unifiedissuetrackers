package Adaptors.IssueRepositories;


import Adaptors.HelperMethods.DatabaseHelperMethods;
import DatabaseConnectors.IssueTrackerConnector;
import Model.IssueTrackers.Comment;
import Model.IssueTrackers.CustomField;
import Model.IssueTrackers.IssueLink;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Seconds;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static Model.IssueTrackers.CommentBuilder.aComment;
import static Model.IssueTrackers.CustomFieldBuilder.aCustomField;
import static Model.IssueTrackers.IssueLinkBuilder.anIssueLink;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static java.sql.Types.INTEGER;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.log4j.Logger.getLogger;
import static org.joda.time.DateTime.parse;

public class JiraAdapterMain implements IssueRepositoryConsumer<JsonNode, JsonNode> {

    private static final Logger LOGGER = getLogger(JiraAdapterMain.class);
    private final Client client;
    private final Connection connection;
    private final String repositoryUrl;
    private final String repositoryType;
    private final String projectName;
    private final String projectUrl;
    private int projectId;
    private int issueRepositoryId;
    private DatabaseHelperMethods helperMethods;

    public JiraAdapterMain(Client client, Connection connection, String projectName, String projectUrl, String repositoryUrl, String repositoryType) {
        this.client = client;
        this.connection = connection;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
        this.repositoryUrl = repositoryUrl;
        this.repositoryType = repositoryType;
    }

    public static void main(String[] args) throws IOException {
        JiraAdapterMain main = new JiraAdapterMain(
                new Client(),
                new IssueTrackerConnector().getConnection(),
                "JENKINS",
                "http://jenkins-ci.org/",
                "https://issues.jenkins-ci.org/browse",
                "JIRA");

        main.run();
    }

    public void run() throws IOException {
        int startPoint = 0;
        int maxNumber = 1;

        while (startPoint < maxNumber) {
            WebResource resource = client.resource("https://issues.jenkins-ci.org/rest/api/latest/search?jql=&startAt=" + startPoint +
                    "&maxResults=100&expand=names,changelog");
            ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
            String output = response.getEntity(String.class);

            helperMethods = new DatabaseHelperMethods(connection);
            projectId = helperMethods.getOrCreateProject(projectName, projectUrl);
            issueRepositoryId = helperMethods.getOrCreateIssueRepository(repositoryUrl, repositoryType, projectId);

            JsonNode root = new ObjectMapper().readTree(output);
            maxNumber = root.findValue("total").asInt();
            JsonNode names = root.get("names");
            JsonNode issues = root.get("issues");
            for (int i = 0; i < issues.size(); i++) {
                saveIssue(issues.get(i), names);
            }
            startPoint = startPoint + 100;
            LOGGER.info(startPoint);
        }
        LOGGER.info("Finished");
    }

    private void saveIssue(JsonNode root, JsonNode names) throws IOException {
        String originalIssueId = root.get("id").asText();
        JsonNode fields = root.get("fields");
        String issueType = fields.get("issuetype").get("name").asText();
        String summary = fields.get("summary").asText();
        DateTime dueDate = getDueDate(fields);
        DateTime createdDate = parse(fields.get("created").asText());
        String description = fields.get("description").asText();
        String status = fields.get("status").get("name").asText();
        String projectName = fields.get("project").get("name").asText();
        List<String> componentNames = fields.get("components").findValuesAsText("name");
        String issueAddress = root.get("self").asText();
        String resolutionStatus = getResolutionStatus(fields);
        Hours currentEstimate = getCurrentEstimate(fields);
        String release = getRelease(fields);
        Hours originalEstimate = getOriginalEstimate(fields);

        int reporterUserId = getReporterId(fields);
        int assigneeUserId = getAssigneeId(fields);
        int priorityId = getPriorityId(fields, issueRepositoryId);

        int databaseIssueId = saveIssue(root, originalIssueId, summary, issueType, reporterUserId, createdDate, description, priorityId, status,
                projectName, componentNames, dueDate, assigneeUserId, currentEstimate, issueAddress, release, resolutionStatus, originalEstimate);
        if (databaseIssueId == 0) return;

        saveHistory(originalIssueId, databaseIssueId);

        List<Comment> comments = getComments(originalIssueId);
        saveComments(comments, databaseIssueId);

        List<CustomField> customFields = getCustomFields(originalIssueId, fields, names);
        saveCustomFields(customFields, databaseIssueId);

        List<IssueLink> issueLinks = getIssueLinks(fields);
        saveIssueLinks(databaseIssueId, issueLinks);
    }

    @Override
    public void saveIssueLinks(int databaseIssueId, List<IssueLink> issueLinks) {
        for (IssueLink anIssueLink : issueLinks) {
            String issueType = anIssueLink.getLinkedIssueType();
            String linkedIssueId = anIssueLink.getLinkedIssueId();
            helperMethods.saveIssueLink(issueRepositoryId, databaseIssueId, issueType, linkedIssueId);
        }
    }

    @Override
    public void saveComments(List<Comment> comments, int databaseIssueId) {
        for (Comment aComment : comments) {
            String content = aComment.getBody();
            String authorEmail = aComment.getAuthorEmail();
            String authorName = aComment.getAuthorName();
            helperMethods.saveComment(issueRepositoryId, databaseIssueId, authorName, authorEmail, content);
        }
    }

    @Override
    public void saveHistory(String originalIssueId, int databaseIssueId) throws IOException {
        WebResource resource = client.resource("https://issues.jenkins-ci.org/rest/api/latest/issue/" + originalIssueId + "?expand=changelog");
        ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
        String output = response.getEntity(String.class);
        JsonNode root = new ObjectMapper().readTree(output);

        JsonNode histories = root.get("changelog").get("histories");

        if (histories.isNull()) return;
        for (JsonNode history : histories) {
            if (history.isNull() || history.get("author") == null || history.get("author").isNull()) {
                continue;
            }
            JsonNode authorNode = history.get("author");
            String authorName = null;
            String authorEmail = null;
            if (authorNode != null &&
                    authorNode.get("name") != null &&
                    authorNode.get("emailAddress") != null) {
                authorName = authorNode.get("name").asText();
                authorEmail = authorNode.get("emailAddress").asText();
            } else if (authorNode.get("emailAddress") == null) {
                authorName = authorNode.get("name").asText();
                authorEmail = authorName;
            }
            String date = null;
            if (history.get("created") != null) {
                date = history.get("created").asText();
            }
            int userId = helperMethods.getOrCreateRepositoryUser(authorName, authorEmail, issueRepositoryId);
            JsonNode itemsNode = history.get("items");
            for (JsonNode anItem : itemsNode) {
                String field = anItem.get("field").asText();
                String from = anItem.get("fromString").asText();
                String to = anItem.get("toString").asText();
                helperMethods.saveHistory(databaseIssueId, from, to, field, userId, date);
            }
        }
    }

    @Override
    public void saveCustomFields(List<CustomField> customFields, int issueId) {
        for (CustomField aCustomField : customFields) {
            if (aCustomField.getDescription() == null) continue;
            int customFieldId = helperMethods.getOrCreateCustomField(issueRepositoryId, aCustomField.getDescription());
            helperMethods.saveCustomFieldValue(issueId, customFieldId, aCustomField.getValue());
        }
    }

    private String getCustomFieldDescription(String customFieldId, JsonNode names) {
        return names.get(customFieldId).asText();
    }

    @Override
    public int getPriorityId(JsonNode fields, int issueRepositoryId) throws IOException {
        JsonNode priority = fields.get("priority");
        String id = priority.get("id").asText();
        String priorityName = priority.get("name").asText();
        String description = getPriorityDescription(id);
        return helperMethods.createOrGetPriorityId(priorityName, description, issueRepositoryId);
    }

    public String getPriorityDescription(String priorityId) throws IOException {
        WebResource resource = client.resource("https://issues.jenkins-ci.org/rest/api/latest/priority/" + priorityId);
        ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
        String jsonString = response.getEntity(String.class);
        JsonNode root = new ObjectMapper().readTree(jsonString);
        response.close();
        return root.get("description").asText();
    }

    private String getRelease(JsonNode fields) {
        JsonNode fixVersionsNode = fields.get("fixVersions");
        String release;
        if (fixVersionsNode != null && !fixVersionsNode.isNull() && fixVersionsNode.get(0) != null) {
            release = fixVersionsNode.get(0).get("name").asText();
        } else {
            release = null;
        }
        return release;
    }

    @Override
    public int getReporterId(JsonNode fields) {
        JsonNode reporterNode = fields.get("reporter");
        String reporterEmail = reporterNode.get("emailAddress").asText();
        String reporterName = reporterNode.get("name").asText();
        return helperMethods.getOrCreateRepositoryUser(reporterName, reporterEmail, issueRepositoryId);
    }

    @Override
    public int saveIssue(JsonNode root, String issueId, String summary, String issueType, int reporterUserId, DateTime createdDate, String description, int priorityId, String status,
                         String projectName, List<String> componentNames, DateTime dueDate, int assigneeUserId, Hours currentEstimate, String issueAddress, String release,
                         String resolutionStatus, Hours originalEstimate) {

        Map<Integer, TableColumnName> intMap = newHashMap();
        intMap.put(Integer.valueOf(issueId), TableColumnName.issueId);
        Map<String, TableColumnName> stringMap = newHashMap();
        stringMap.put(issueAddress, TableColumnName.issueAddress);

        int foundIssueId = helperMethods.checkIfExits("issues", stringMap, intMap);
        if (foundIssueId != 0) return foundIssueId;

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
            preparedStatement.setString(17, release);
            preparedStatement.setString(18, issueType);
            preparedStatement.setString(19, summary);

            if (preparedStatement.executeUpdate() > 0) {
                ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    newIssueId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return newIssueId;
    }

    private DateTime getResolutionDate(JsonNode fields) {
        JsonNode resolutionDateField = fields.get("resolutiondate");
        DateTime resolutionDate = null;
        if (!resolutionDateField.isNull()) {
            resolutionDate = parse(resolutionDateField.getTextValue());
        }
        return resolutionDate;
    }

    private Hours getOriginalEstimate(JsonNode fields) {
        JsonNode originalEstimateString = fields.get("timeoriginalestimate");
        Seconds originalEstimate;
        if (originalEstimateString != null && !originalEstimateString.isNull()) {
            originalEstimate = Seconds.seconds(originalEstimateString.getIntValue());
            return originalEstimate.toStandardHours();
        }
        return null;
    }

    private Hours getCurrentEstimate(JsonNode fields) {
        JsonNode aggregateTimeEstimate = fields.get("aggregatetimeestimate");
        Seconds currentEstimate;
        if (aggregateTimeEstimate != null && !aggregateTimeEstimate.isNull()) {
            currentEstimate = Seconds.seconds(aggregateTimeEstimate.getIntValue());
            return currentEstimate.toStandardHours();
        }
        return null;
    }

    @Override
    public int getAssigneeId(JsonNode fields) {
        JsonNode assignee = fields.get("assignee");
        if (!assignee.isNull() && assignee.get("name") != null) {
            String assigneeName = assignee.get("name").asText();
            String assigneeEmail = assignee.get("emailAddress").asText();
            return helperMethods.getOrCreateRepositoryUser(assigneeName, assigneeEmail, issueRepositoryId);
        } else {
            return 0;
        }
    }

    private DateTime getDueDate(JsonNode fields) {
        JsonNode dueDateField = fields.get("duedate");
        DateTime dueDate = null;
        if (dueDateField != null && !dueDateField.isNull()) {
            dueDate = parse(dueDateField.getTextValue());
        }
        return dueDate;
    }

    private String getResolutionStatus(JsonNode fields) {
        JsonNode resolutionField = fields.get("resolution");
        String resolutionStatus;
        if (!resolutionField.isNull() && resolutionField.get("name") != null) {
            resolutionStatus = resolutionField.get("name").asText();
        } else {
            resolutionStatus = "Unresolved";
        }
        return resolutionStatus;
    }

    private List<CustomField> getCustomFields(String issueId, JsonNode fields, JsonNode names) throws IOException {
        List<CustomField> customFieldsList = newArrayList();
        Iterator<String> allFieldNames = fields.getFieldNames();
        while (allFieldNames.hasNext()) {
            String next = allFieldNames.next();
            JsonNode nextCustomValue = fields.findValue(next);
            if (next.startsWith("customfield") && !nextCustomValue.isNull() && !isEmpty(nextCustomValue.asText())) {
                CustomField customField = aCustomField()
                        .withCustomFieldId(next)
                        .withIssueId(issueId)
                        .withDescription(getCustomFieldDescription(next, names))
                        .withValue(nextCustomValue.asText())
                        .build();
                customFieldsList.add(customField);
            }
        }
        return customFieldsList;
    }

    private LinkedList<Comment> getComments(String issueId) throws IOException {
        WebResource resource = client.resource("https://issues.jenkins-ci.org/rest/api/latest/issue/" + issueId + "/comment");
        ClientResponse response = resource.accept("application/json").get(ClientResponse.class);
        String jsonString = response.getEntity(String.class);
        JsonNode commentsNode = new ObjectMapper().readTree(jsonString);
        LinkedList<Comment> commentsList = newLinkedList();
        if (!commentsNode.get("comments").isNull()) {
            JsonNode comments = commentsNode.get("comments");
            for (int i = 0; i < comments.size(); i++) {
                String authorName = comments.get(i).get("author").get("name").asText();
                String authorEmail = comments.get(i).get("author").get("emailAddress").asText();
                String body = comments.get(i).get("body").asText();
                Comment aComment = aComment()
                        .withAuthorEmail(authorEmail)
                        .withBody(body)
                        .withAuthorName(authorName)
                        .build();
                commentsList.add(aComment);
            }
        }
        response.close();
        return commentsList;
    }

    private List<IssueLink> getIssueLinks(JsonNode fields) {
        List<IssueLink> links = newArrayList();
        JsonNode issueLinks = fields.get("issuelinks");
        if (!issueLinks.isNull()) {
            for (int i = 0; i < issueLinks.size(); i++) {
                JsonNode outwardIssue = issueLinks.get(i).get("outwardIssue");
                JsonNode inwardIssue = issueLinks.get(i).get("inwardIssue");
                JsonNode type = issueLinks.get(i).get("type");

                String linkedIssueId;
                String linkedIssueType;
                if (outwardIssue != null && outwardIssue.isNull()) {
                    linkedIssueId = outwardIssue.get("id").asText();
                    linkedIssueType = type.get("outward").asText();
                } else if (inwardIssue != null && inwardIssue.isNull()) {
                    linkedIssueId = inwardIssue.get("id").asText();
                    linkedIssueType = type.get("inward").asText();
                } else {
                    continue;
                }

                IssueLink issueLink = anIssueLink()
                        .withLinkedIssueId(linkedIssueId)
                        .withLinkedIssueType(linkedIssueType)
                        .build();
                links.add(issueLink);
            }
        }
        return links;
    }
}
