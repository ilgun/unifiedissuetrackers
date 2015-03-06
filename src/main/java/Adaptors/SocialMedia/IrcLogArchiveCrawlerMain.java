package Adaptors.SocialMedia;

import com.sun.jersey.api.client.Client;

import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;
import static Model.SocialMedia.SocialMediaChannel.IRC;

public class IrcLogArchiveCrawlerMain {
    public static void main(String[] args) {
        IrcLogArchiveCrawler logArchiveCrawler = new IrcLogArchiveCrawler(
                new Client(),
                getDatabaseConnection(),
                "HIBERNATE",
                "http://hibernate.org",
                "http://transcripts.jboss.org/channel/irc.freenode.org/%23hibernate-dev/",
                IRC);

        logArchiveCrawler.run();
    }
}
