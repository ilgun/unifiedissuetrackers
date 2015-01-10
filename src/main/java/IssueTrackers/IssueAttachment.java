package IssueTrackers;

public class IssueAttachment {
    private final String id;
    private final byte[] content;

    public static class Builder {
        private String id;
        private byte[] content;

        private Builder() {
        }

        public static Builder anIssueAttachment() {
            return new Builder();
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withContent(byte[] content) {
            this.content = content;
            return this;
        }

        public IssueAttachment build() {
            return new IssueAttachment(id, content);
        }
    }

    private IssueAttachment(String id, byte[] content) {
        this.id = id;
        this.content = content;
    }
}
