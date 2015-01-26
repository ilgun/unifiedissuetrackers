package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import DatabaseConnectors.IssueTrackerConnector;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;

public class TwitterCrawlerMain {
    public static void main(String[] args) throws ConfigurationException, IOException {

        PropertiesConfiguration configuration = new PropertiesConfiguration("configuration/dev/twitter.properties");
        String consumerKey = configuration.getString("consumer.key");
        String consumerSecret = configuration.getString("consumer.secret");
        String accessToken = configuration.getString("access.token");
        String accessSecret = configuration.getString("access.secret");
        boolean isDebugEnabled = configuration.getBoolean("debug");

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(isDebugEnabled)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessSecret);

        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitterInstance = tf.getInstance();

        DatabaseHelperMethods helperMethods = new DatabaseHelperMethods(new IssueTrackerConnector().getConnection());
        TwitterCrawler crawler = new TwitterCrawler(twitterInstance, "#Hive", helperMethods, "HIVE", "https://hive.apache.org");

        crawler.runCrawler();
    }
}
