package SocialMedia;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.List;

public class SocialMediaEntry {
    private final String id;
    private final String content;
    private final String sender;
    private final List<Receiver> receiver;
    private final DateTime sentDate;
    private final DateTime receivedDate;
    private final SocialMediaAttachment attachment;
    private final Optional<String> subject;
    private final Optional<String> location;
    private final Optional<String> isReplied;
    private final Optional<Email> email;
    private final Optional<String> sentFrom;

    public static class Builder {
        private String id;
        private String content;
        private String sender;
        private List<Receiver> receiver;
        private DateTime sentDate;
        private DateTime receivedDate;
        private SocialMediaAttachment attachment;
        private Optional<String> subject;
        private Optional<String> location;
        private Optional<String> isReplied;
        private Optional<Email> email;
        private Optional<String> sentFrom;

        private Builder() {
        }

        public static Builder aSocialMediaEntry() {
            return new Builder();
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withContent(String content) {
            this.content = content;
            return this;
        }

        public Builder withSender(String sender) {
            this.sender = sender;
            return this;
        }

        public Builder withReceiver(List<Receiver> receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder withSentDate(DateTime sentDate) {
            this.sentDate = sentDate;
            return this;
        }

        public Builder withReceivedDate(DateTime receivedDate) {
            this.receivedDate = receivedDate;
            return this;
        }

        public Builder withAttachment(SocialMediaAttachment attachment) {
            this.attachment = attachment;
            return this;
        }

        public Builder withSubject(Optional<String> subject) {
            this.subject = subject;
            return this;
        }

        public Builder withLocation(Optional<String> location) {
            this.location = location;
            return this;
        }

        public Builder withIsReplied(Optional<String> isReplied) {
            this.isReplied = isReplied;
            return this;
        }

        public Builder withEmail(Optional<Email> email) {
            this.email = email;
            return this;
        }

        public Builder withSentFrom(Optional<String> sentFrom) {
            this.sentFrom = sentFrom;
            return this;
        }

        public SocialMediaEntry build() {
            return new SocialMediaEntry(id, content, sender, receiver, sentDate, receivedDate, attachment, subject, location, isReplied, email, sentFrom);
        }
    }

    private SocialMediaEntry(String id, String content, String sender, List<Receiver> receiver,
                             DateTime sentDate, DateTime receivedDate, SocialMediaAttachment attachment,
                             Optional<String> subject, Optional<String> location, Optional<String> isReplied,
                             Optional<Email> email, Optional<String> sentFrom) {
        this.id = id;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.sentDate = sentDate;
        this.receivedDate = receivedDate;
        this.attachment = attachment;
        this.subject = subject;
        this.location = location;
        this.isReplied = isReplied;
        this.email = email;
        this.sentFrom = sentFrom;
    }
}
