package IssueTrackers;

import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;


public class IssueEntryBuilder {
    private String issueId;
    private String trackerType;
    private String issueAddress;
    private String assignee;
    private String reporter;
    private int priority;
    private String resolution;
    private DateTime reportedDate;
    private DateTime dueDate;
    private DateTime currentEstimate;
    private DateTime remainingEstimate;
    private DateTime originalEstimate;
    private String state;
    private String description;
    private String product;
    private List<String> components;
    private String release;
    private LinkedList<IssueLink> issueLinks;
    private LinkedList<Comment> comments;
    private List<CustomField> customFields;
    private String summary;

    private IssueEntryBuilder() {
    }

    public static IssueEntryBuilder anIssueEntry() {
        return new IssueEntryBuilder();
    }

    public IssueEntryBuilder withIssueId(String issueId) {
        this.issueId = issueId;
        return this;
    }

    public IssueEntryBuilder withTrackerType(String trackerType) {
        this.trackerType = trackerType;
        return this;
    }

    public IssueEntryBuilder withIssueAddress(String issueAddress) {
        this.issueAddress = issueAddress;
        return this;
    }

    public IssueEntryBuilder withAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    public IssueEntryBuilder withReporter(String reporter) {
        this.reporter = reporter;
        return this;
    }

    public IssueEntryBuilder withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public IssueEntryBuilder withResolution(String resolution) {
        this.resolution = resolution;
        return this;
    }

    public IssueEntryBuilder withReportedDate(DateTime reportedDate) {
        this.reportedDate = reportedDate;
        return this;
    }

    public IssueEntryBuilder withDueDate(DateTime dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public IssueEntryBuilder withCurrentEstimate(DateTime currentEstimate) {
        this.currentEstimate = currentEstimate;
        return this;
    }

    public IssueEntryBuilder withRemainingEstimate(DateTime remainingEstimate) {
        this.remainingEstimate = remainingEstimate;
        return this;
    }

    public IssueEntryBuilder withOriginalEstimate(DateTime originalEstimate) {
        this.originalEstimate = originalEstimate;
        return this;
    }

    public IssueEntryBuilder withState(String state) {
        this.state = state;
        return this;
    }

    public IssueEntryBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public IssueEntryBuilder withProduct(String product) {
        this.product = product;
        return this;
    }

    public IssueEntryBuilder withComponent(List<String> components) {
        this.components = components;
        return this;
    }

    public IssueEntryBuilder withRelease(String release) {
        this.release = release;
        return this;
    }

    public IssueEntryBuilder withIssueLinks(LinkedList<IssueLink> issueLinks) {
        this.issueLinks = issueLinks;
        return this;
    }

    public IssueEntryBuilder withComments(LinkedList<Comment> comments) {
        this.comments = comments;
        return this;
    }

    public IssueEntryBuilder withCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
        return this;
    }

    public IssueEntryBuilder withSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public IssueEntryBuilder but() {
        return anIssueEntry().withIssueId(issueId).withTrackerType(trackerType).withIssueAddress(issueAddress).withAssignee(assignee).withReporter(reporter).withPriority(priority)
                .withResolution(resolution).withReportedDate(reportedDate).withDueDate(dueDate).withCurrentEstimate(currentEstimate).withRemainingEstimate(remainingEstimate)
                .withOriginalEstimate(originalEstimate).withState(state).withDescription(description).withProduct(product).withComponent(components).withRelease(release)
                .withIssueLinks(issueLinks).withComments(comments).withCustomFields(customFields).withSummary(summary);
    }

    public IssueEntry build() {
        IssueEntry issueEntry = new IssueEntry(issueId, trackerType, issueAddress, assignee, reporter, priority, resolution, reportedDate, dueDate, currentEstimate, remainingEstimate,
                originalEstimate, state, description, product, components, release, issueLinks, comments, customFields, summary);
        return issueEntry;
    }
}
