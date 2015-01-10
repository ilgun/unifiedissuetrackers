package SocialMedia;

import com.google.common.base.Optional;

import java.util.List;

public class Receiver {
    private final String id;
    private final List<String> receivers;
    private final Optional<String> cc;
    private final Optional<String> bcc;



    public static class Builder {
        private String id;
        private List<String> receivers;
        private Optional<String> cc;
        private Optional<String> bcc;

        private Builder() {
        }
        public static Builder aReceiver() {
            return new Builder();
        }
        public Builder withId(String value) {
            id = value;
            return this;
        }

        public Builder withReceivers(List<String> value) {
            receivers = value;
            return this;
        }

        public Builder withCc(Optional<String> value) {
            cc = value;
            return this;
        }

        public Builder withBcc(Optional<String> value) {
            bcc = value;
            return this;
        }

        public Receiver build() {
            return new Receiver(id, receivers, cc, bcc);
        }
    }

    private Receiver(String id, List<String> receivers, Optional<String> cc, Optional<String> bcc) {
        this.id = id;
        this.receivers = receivers;
        this.cc = cc;
        this.bcc = bcc;
    }
}
