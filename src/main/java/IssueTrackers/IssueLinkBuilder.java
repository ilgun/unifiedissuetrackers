package IssueTrackers;


public class IssueLinkBuilder {
    private String originalIssueId;
    private String linkedIssueId;
    private String linkedIssueType;

    private IssueLinkBuilder() {
    }

    public static IssueLinkBuilder anIssueLink() {
        return new IssueLinkBuilder();
    }

    public IssueLinkBuilder withOriginalIssueId(String originalIssueId) {
        this.originalIssueId = originalIssueId;
        return this;
    }

    public IssueLinkBuilder withLinkedIssueId(String linkedIssueId) {
        this.linkedIssueId = linkedIssueId;
        return this;
    }

    public IssueLinkBuilder withLinkedIssueType(String linkedIssueType) {
        this.linkedIssueType = linkedIssueType;
        return this;
    }

    public IssueLinkBuilder but() {
        return anIssueLink().withOriginalIssueId(originalIssueId).withLinkedIssueId(linkedIssueId).withLinkedIssueType(linkedIssueType);
    }

    public IssueLink build() {
        return new IssueLink(originalIssueId, linkedIssueId, linkedIssueType);
    }
}
