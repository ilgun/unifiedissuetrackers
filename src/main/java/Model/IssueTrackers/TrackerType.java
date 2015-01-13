package Model.IssueTrackers;

import org.apache.commons.lang3.builder.ToStringBuilder;

public enum TrackerType {
    JIRA,
    TFS,
    BUGZILLA;

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .toString();
    }

}
