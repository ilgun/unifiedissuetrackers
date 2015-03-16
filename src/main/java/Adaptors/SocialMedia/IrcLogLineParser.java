package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Adaptors.HelperMethods.TableName;
import Adaptors.HelperMethods.UserRelationshipManager;
import Model.SocialMedia.SocialMediaEvent;
import org.jsoup.nodes.Element;

import java.util.regex.Pattern;

import static Model.SocialMedia.SocialMediaEvent.NICKCHANGE;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.joda.time.LocalDateTime.parse;

public class IrcLogLineParser {
    private static final String NAME_CHANGE_DEFAULT_STRING = "is now known as";
    private static final String TOPIC_CHANGE = "changes topic to";
    private static final String SETS_MODE = "sets mode";
    private static final Pattern NEW_USER_SYSTEM_CHARACTERS = compile("^.*[<>@!~].*$");

    private final DatabaseHelperMethods helpers;
    private final int socialMediaRepositoryId;
    private final UserRelationshipManager userRelationshipManager;

    public IrcLogLineParser(DatabaseHelperMethods helpers, int socialMediaRepositoryId, UserRelationshipManager userRelationshipManager) {
        this.helpers = helpers;
        this.socialMediaRepositoryId = socialMediaRepositoryId;
        this.userRelationshipManager = userRelationshipManager;
    }

    public void parseAndSave(Element tableItem) {
        String action = tableItem.attr("class");
        String date = tableItem.attr("id");
        switch (action) {
            case "part":
            case "join": {
                String content = tableItem.text();
                String username = getUsername(tableItem.text());

                int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
                helpers.createSocialMediaEvent(socialMediaRepositoryId, userId, parseIrcDate(date), SocialMediaEvent.valueOf(action.toUpperCase()), content);

                break;
            }
            case "nickchange": {
                String content = tableItem.text();
                String oldUsername = content.substring(content.indexOf("*** ") + 4, content.indexOf(NAME_CHANGE_DEFAULT_STRING) - 1);
                String newUsername = content.substring(content.indexOf(NAME_CHANGE_DEFAULT_STRING) + 16, content.length() - 6);

                helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, newUsername, newUsername);
                int socialMediaUserId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, oldUsername, oldUsername);
                helpers.createSocialMediaEvent(socialMediaRepositoryId, socialMediaUserId, parseIrcDate(date), NICKCHANGE, oldUsername + " to " + newUsername);
                userRelationshipManager.createRelationshipsForNicknameChange(oldUsername, newUsername);

                break;
            }
            case "": {
                if (!isBlank(date)) {
                    String username = tableItem.getElementsByTag("th").text();
                    String content = tableItem.getElementsByTag("td").get(0).text();

                    int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
                    String inResponseTo = String.valueOf(helpers.getLastInsertedIdFor(TableName.socialmediaentries));
                    String originalEntryId = userId + "_" + date;

                    helpers.saveSocialMediaEntry(socialMediaRepositoryId, userId, originalEntryId, content, inResponseTo, null, "irc message", parseIrcDate(date), null, null, null, null);
                }
                break;
            }
            case "servermsg": {
                String content = tableItem.text();
                if (content.contains(TOPIC_CHANGE)) {
                    String username = content.substring(content.indexOf("*** ") + 4, content.indexOf(TOPIC_CHANGE) - 1);
                    int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
                    helpers.createSocialMediaEvent(socialMediaRepositoryId, userId, parseIrcDate(date), SocialMediaEvent.TOPIC_CHANGE, content);
                } else if (content.contains(SETS_MODE)) {
                    String username = content.substring(content.indexOf("*** ") + 4, content.indexOf(SETS_MODE) - 1);
                    int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
                    helpers.createSocialMediaEvent(socialMediaRepositoryId, userId, parseIrcDate(date), SocialMediaEvent.SETS_MODE, content);
                }
                break;
            }
            case "action": {
                String content = tableItem.text();
                String username;
                if (content.contains("*")) {
                    username = content.substring(2).split(" ")[0];
                } else {
                    username = content.split(" ")[0];
                }

                int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
                helpers.createSocialMediaEvent(socialMediaRepositoryId, userId, parseIrcDate(date), SocialMediaEvent.valueOf(action.toUpperCase()), content);

                break;
            }
            case "other": {
                String content = tableItem.text();
                String username = content.substring(1, content.indexOf("freenode.net-") + 12);

                int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
                helpers.createSocialMediaEvent(socialMediaRepositoryId, userId, parseIrcDate(date), SocialMediaEvent.valueOf(action.toUpperCase()), content);
                break;
            }
        }
    }

    private String parseIrcDate(String date) {
        if (!isBlank(date)) {
            return parse(date.substring(1, date.length())).toString();
        } else {
            return null;
        }
    }

    private String getUsername(String content) {
        String username = null;
        try {
            if (NEW_USER_SYSTEM_CHARACTERS.matcher(content).matches()) {
                username = content.substring(4, content.indexOf("<") - 1);
            } else if (content.contains("has joined")) {
                username = content.substring(4, content.indexOf("has joined") - 1);
            } else if (content.contains("has quit")) {
                username = content.substring(4, content.indexOf("has quit") - 1);
            } else if (content.contains("has left")) {
                username = content.substring(4, content.indexOf("has left") - 1);
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return username;
    }
}
