package Model.IssueTrackers;

public class IssueLink {
    private final String originalIssueId;
    private final String linkedIssueId;
    private final String linkedIssueType;

    public IssueLink(String originalIssueId, String linkedIssueId, String linkedIssueType) {
        this.originalIssueId = originalIssueId;
        this.linkedIssueId = linkedIssueId;
        this.linkedIssueType = linkedIssueType;
    }

    public String getOriginalIssueId() {
        return originalIssueId;
    }

    public String getLinkedIssueId() {
        return linkedIssueId;
    }

    public String getLinkedIssueType() {
        return linkedIssueType;
    }
}
