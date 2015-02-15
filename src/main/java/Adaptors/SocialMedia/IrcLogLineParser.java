package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Adaptors.HelperMethods.TableName;
import Model.SocialMedia.SocialMediaEvent;
import org.jsoup.nodes.Element;

import java.util.regex.Pattern;

import static Model.SocialMedia.SocialMediaEvent.NICKCHANGE;
import static java.util.regex.Pattern.compile;
import static org.joda.time.DateTime.parse;

public class IrcLogLineParser {
    private static final String NAME_CHANGE_DEFAULT_STRING = "is now known as";
    private static final String TOPIC_CHANGE = "changes topic to";
    private static final Pattern NEW_USER_SYSTEM_CHARACTERS = compile("^.*[<>@!~].*$");

    private final DatabaseHelperMethods helpers;
    private final int socialMediaRepositoryId;

    public IrcLogLineParser(DatabaseHelperMethods helpers, int socialMediaRepositoryId) {
        this.helpers = helpers;
        this.socialMediaRepositoryId = socialMediaRepositoryId;
    }

    public void parseAndSave(Element tableItem) {
        String action = tableItem.attr("class");
        if (action.equals("part") || action.equals("join")) {
            String date = tableItem.attr("id");
            String username = getUsername(action, tableItem.text());

            int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
            helpers.createSocialMediaEvent(socialMediaRepositoryId, userId, parse(date).toString(), SocialMediaEvent.valueOf(action), null);

        } else if (action.equals("nickchange")) {
            String date = tableItem.attr("id");
            String content = tableItem.text();
            String oldUsername = content.substring(content.indexOf("*** ") + 4, content.indexOf(NAME_CHANGE_DEFAULT_STRING) - 1);
            String newUsername = content.substring(content.indexOf(NAME_CHANGE_DEFAULT_STRING) + 16, content.length() - 6);

            helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, newUsername, newUsername);
            int socialMediaUserId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, oldUsername, oldUsername);
            helpers.createSocialMediaEvent(socialMediaRepositoryId, socialMediaUserId, parse(date).toString(), NICKCHANGE, oldUsername + "to" + newUsername);
            helpers.createUserRelationship(newUsername, oldUsername);

        } else if (action.equals("")) {
            String date = tableItem.attr("id");
            String username = tableItem.getElementsByTag("th").text();
            String content = tableItem.getElementsByTag("td").get(0).text();

            int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
            String inResponseTo = String.valueOf(helpers.getLastInsertedIdFor(TableName.socialmediaentries));
            String originalEntryId = userId + "_" + date;

            helpers.getOrSaveSocialMediaEntry(socialMediaRepositoryId, userId, originalEntryId, content, inResponseTo, null, "irc message", parse(date).toString(), null, null, null, null);

        } else if (action.equals("other") || action.equals("servermsg") || action.equals("action")) {
            String content = tableItem.text();
            String date = tableItem.attr("id");
            String username = content.substring(content.indexOf("*** ") + 4, content.indexOf(TOPIC_CHANGE) - 1);

            int userId = helpers.getOrCreateSocialMediaUser(socialMediaRepositoryId, username, username);
            helpers.createSocialMediaEvent(socialMediaRepositoryId, userId, parse(date).toString(), SocialMediaEvent.valueOf(action), content);
        }
    }

    private String getUsername(String action, String content) {
        String username = null;
        try {
            if (NEW_USER_SYSTEM_CHARACTERS.matcher(content).matches()) {
                username = content.substring(4, content.indexOf("<") - 1);
            } else if (action.equals("join")) {
                username = content.substring(4, content.indexOf("has joined") - 1);
            } else if (action.equals("has quit")) {
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
