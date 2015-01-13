package Model.IssueTrackers;


public class CustomField {
    private final String customFieldId;
    private final String issueId;
    private final String value;
    private final String description;

    protected CustomField(String customFieldId, String issueId, String value, String description) {
        this.customFieldId = customFieldId;
        this.issueId = issueId;
        this.value = value;
        this.description = description;
    }

    public String getCustomFieldId() {
        return customFieldId;
    }

    public String getIssueId() {
        return issueId;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
