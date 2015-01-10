package SocialMedia;


import com.google.common.base.Optional;

public class Email {
    private final Optional<Boolean> isImportant;
    private final Optional<Boolean> isSpam;

    public static class Builder {
        private Optional<Boolean> isImportant;
        private Optional<Boolean> isSpam;

        private Builder() {
        }

        public static Builder anEmail() {
            return new Builder();
        }

        public Builder isImportant(Optional<Boolean> value) {
            isImportant = value;
            return this;
        }

        public Builder isSpam(Optional<Boolean> value) {
            isSpam = value;
            return this;
        }

        public Email build() {
            return new Email(isImportant, isSpam);
        }
    }

    private Email(Optional<Boolean> isImportant, Optional<Boolean> isSpam) {
        this.isImportant = isImportant;
        this.isSpam = isSpam;
    }
}