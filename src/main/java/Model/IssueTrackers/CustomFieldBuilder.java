package Model.IssueTrackers;

public class CustomFieldBuilder {
    private String customFieldId;
    private String issueId;
    private String value;
    private String description;

    private CustomFieldBuilder() {
    }

    public static CustomFieldBuilder aCustomField() {
        return new CustomFieldBuilder();
    }

    public CustomFieldBuilder withCustomFieldId(String customFieldId) {
        this.customFieldId = customFieldId;
        return this;
    }

    public CustomFieldBuilder withIssueId(String issueId) {
        this.issueId = issueId;
        return this;
    }

    public CustomFieldBuilder withValue(String value) {
        this.value = value;
        return this;
    }

    public CustomFieldBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CustomField build() {
        return new CustomField(customFieldId, issueId, value, description);
    }
}
