package Model.IssueTrackers;


public class CommentBuilder {
    private String authorName;
    private String authorDisplayName;
    private String authorEmail;
    private String body;

    private CommentBuilder() {
    }

    public static CommentBuilder aComment() {
        return new CommentBuilder();
    }

    public CommentBuilder withAuthorName(String authorName) {
        this.authorName = authorName;
        return this;
    }

    public CommentBuilder withAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
        return this;
    }

    public CommentBuilder withAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
        return this;
    }

    public CommentBuilder withBody(String body) {
        this.body = body;
        return this;
    }

    public CommentBuilder but() {
        return aComment().withAuthorName(authorName).withAuthorDisplayName(authorDisplayName).withAuthorEmail(authorEmail).withBody(body);
    }

    public Comment build() {
        return new Comment(authorName, authorDisplayName, authorEmail, body);
    }
}
