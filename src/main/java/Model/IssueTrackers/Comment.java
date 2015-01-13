package Model.IssueTrackers;

public class Comment {
    private final String authorName;
    private final String authorDisplayName;
    private final String authorEmail;
    private final String body;

    protected Comment(String authorName, String authorDisplayName, String authorEmail, String body) {
        this.authorName = authorName;
        this.authorDisplayName = authorDisplayName;
        this.authorEmail = authorEmail;
        this.body = body;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getBody() {
        return body;
    }
}
