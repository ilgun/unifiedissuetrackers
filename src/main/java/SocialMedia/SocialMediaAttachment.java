package SocialMedia;

public class SocialMediaAttachment {
    private final String id;
    private final byte[] content;

    public static class Builder {
        private String id;
        private byte[] content;

        private Builder() {
        }

        public static Builder anAttachment() {
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

        public SocialMediaAttachment build() {
            return new SocialMediaAttachment(id, content);
        }

    }

    private SocialMediaAttachment(String id, byte[] content) {
        this.id = id;
        this.content = content;
    }
}
