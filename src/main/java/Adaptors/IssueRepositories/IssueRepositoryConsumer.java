package Adaptors.IssueRepositories;

import Model.IssueTrackers.Comment;
import Model.IssueTrackers.CustomField;
import Model.IssueTrackers.IssueLink;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.io.IOException;
import java.util.List;

public interface IssueRepositoryConsumer<T, Y> {
    int getAssigneeId(Y t);

    int getReporterId(Y t);

    int getPriorityId(T t, int issueRepositoryId) throws IOException;

    void saveHistory(T history, int databaseIssueId);

    void saveIssueLinks(int databaseIssueId, List<IssueLink> issueLinks);

    void saveComments(List<Comment> comments, int databaseIssueId);

    void saveCustomFields(List<CustomField> customFields, int issueId);

    int saveIssue(T root, String issueId, String summary, String issueType, int reporterUserId, DateTime createdDate, String description, int priorityId, String status,
                  String projectName, List<String> componentNames, DateTime dueDate, int assigneeUserId, Hours currentEstimate, String issueAddress, String release,
                  String resolutionStatus, Hours originalEstimate);
}
