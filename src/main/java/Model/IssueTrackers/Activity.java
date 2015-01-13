package Model.IssueTrackers;

public class Activity {
    private final String id;
    private final String comments;
    private final String worklog;
    private final String history;
    private final String activities;
    private final String transactions;
    private final String source;


    private Activity(String id, String comments, String workLog, String history, String activities, String transactions, String source) {
        this.id = id;
        this.comments = comments;
        this.worklog = workLog;
        this.history = history;
        this.activities = activities;
        this.transactions = transactions;
        this.source = source;
    }

    public static class Builder {
        private String id;
        private String comments;
        private String worklog;
        private String history;
        private String activities;
        private String transactions;
        private String source;

        private Builder() {
        }

        public static Builder anActivity() {
            return new Builder();
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withComments(String comments) {
            this.comments = comments;
            return this;
        }

        public Builder withWorklog(String worklog) {
            this.worklog = worklog;
            return this;
        }

        public Builder withHistory(String history) {
            this.history = history;
            return this;
        }

        public Builder withActivity(String activities) {
            this.activities = activities;
            return this;
        }

        public Builder withTransactions(String transactions) {
            this.transactions = transactions;
            return this;
        }

        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        public Activity build() {
            return new Activity(id, comments, worklog, history, activities, transactions, source);
        }
    }
}
