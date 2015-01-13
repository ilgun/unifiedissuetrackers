package Model.IssueTrackers;

import org.joda.time.DateTime;

import java.util.LinkedList;
import java.util.List;
/*CREATE TABLE `issuetrackers`.`issues` (
  `issueId` INT NOT NULL,
  `trackerType` VARCHAR(20) NULL,
  `issueAddress` VARCHAR(100) NULL,
  `assignee` VARCHAR(45) NULL,
  `reporter` VARCHAR(45) NULL,
  `priority` INT NULL,
  `resolution` VARCHAR(45) NULL,
  `reportedDate` DATETIME NULL,
  `dueDate` DATETIME NULL,
  `currentEstimate` DATETIME NULL,
  `remainingEstimate` DATETIME NULL,
  `originalEstimate` DATETIME NULL,
  `state` VARCHAR(45) NULL,
  `description` VARCHAR(45) NULL,
  `product` VARCHAR(45) NULL,
  `components` VARCHAR(100) NULL,
  `release` VARCHAR(45) NULL,
  `issueLinks` INT NULL,
  `comments` INT NULL,
  `customFields` INT NULL,
  `summary` VARCHAR(150) NULL,
  PRIMARY KEY (`issueId`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;*/
public class IssueEntry {
    private final String issueId;
    private final String trackerType;
    private final String issueAddress;
    private final String assignee;
    private final String reporter;
    private final int priority;
    private final String resolution;
    private final DateTime reportedDate;
    private final DateTime dueDate;
    private final DateTime currentEstimate;
    private final DateTime remainingEstimate;
    private final DateTime originalEstimate;
    private final String state;
    private final String description;
    private final String product;
    private final List<String> components;
    private final String release;
    private final LinkedList<IssueLink> issueLinks;
    private final LinkedList<Comment> comments;
    private final List<CustomField> customFields;
    private final String summary;

    public IssueEntry(String issueId, String trackerType, String issueAddress, String assignee, String reporter, int priority, String resolution, DateTime reportedDate,
                      DateTime dueDate, DateTime currentEstimate, DateTime remainingEstimate, DateTime originalEstimate, String state, String description, String product,
                      List<String> components, String release, LinkedList<IssueLink> issueLinks, LinkedList<Comment> comments, List<CustomField> customFields, String summary) {
        this.issueId = issueId;
        this.trackerType = trackerType;
        this.issueAddress = issueAddress;
        this.assignee = assignee;
        this.reporter = reporter;
        this.priority = priority;
        this.resolution = resolution;
        this.reportedDate = reportedDate;
        this.dueDate = dueDate;
        this.currentEstimate = currentEstimate;
        this.remainingEstimate = remainingEstimate;
        this.originalEstimate = originalEstimate;
        this.state = state;
        this.description = description;
        this.product = product;
        this.components = components;
        this.release = release;
        this.issueLinks = issueLinks;
        this.comments = comments;
        this.customFields = customFields;
        this.summary = summary;
    }
}
