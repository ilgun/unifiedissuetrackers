package Adaptors.SocialMedia;

import com.sun.jersey.api.client.Client;
import org.apache.commons.configuration.ConfigurationException;

import java.io.IOException;
import java.sql.Connection;

import static Adaptors.HelperMethods.JerseyClientHandler.getJerseyClient;
import static Adaptors.SocialMedia.TwitterCrawlerMain.runTwitterCrawlerForHiveAndHibernate;
import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;
import static Model.SocialMedia.SocialMediaChannel.EMAIL;
import static Model.SocialMedia.SocialMediaChannel.IRC;

public class SocialMediaUnifiedMain {
    public static void main(String[] args) throws IOException, InterruptedException, ConfigurationException {
        Client jerseyClient = getJerseyClient();
        Connection databaseConnection = getDatabaseConnection();

        MboxMailArchiveCrawler hiveMailCrawler = new MboxMailArchiveCrawler(
                jerseyClient,
                databaseConnection,
                "HIVE",
                "https://hive.apache.org",
                "http://mail-archives.apache.org/mod_mbox/hive-dev/",
                EMAIL);
        hiveMailCrawler.run();

        GzippedMailArchiveCrawler pulpEmailCrawler = new GzippedMailArchiveCrawler(
                jerseyClient,
                databaseConnection,
                "PULP",
                "http://www.pulpproject.org",
                "http://www.redhat.com/archives/pulp-list/",
                EMAIL);
        pulpEmailCrawler.run();

        GzippedMailArchiveCrawler hibernateEmailCrawler = new GzippedMailArchiveCrawler(
                jerseyClient,
                databaseConnection,
                "HIBERNATE",
                "http://hibernate.org",
                "http://lists.jboss.org/pipermail/hibernate-dev/",
                EMAIL);
        hibernateEmailCrawler.run();

        IrcLogArchiveCrawler hibernateIrcCrawler = new IrcLogArchiveCrawler(
                jerseyClient,
                databaseConnection,
                "HIBERNATE",
                "http://hibernate.org",
                "http://transcripts.jboss.org/channel/irc.freenode.org/%23hibernate-dev/",
                IRC);
        hibernateIrcCrawler.run();

        runTwitterCrawlerForHiveAndHibernate();
    }
}
