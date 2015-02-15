package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Model.SocialMedia.SocialMediaChannel;
import org.apache.log4j.Logger;
import twitter4j.*;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.System.exit;
import static java.lang.Thread.sleep;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.log4j.Logger.getLogger;

public class TwitterCrawler {
    private static final Logger LOGGER = getLogger(TwitterCrawler.class);

    private final AtomicInteger tweetCount = new AtomicInteger();
    private final DatabaseHelperMethods helperMethods;
    private final Twitter twitterClient;
    private final String queryToSearch;
    private final String projectName;
    private final String projectUrl;
    private final String repositoryUrl;
    private final SocialMediaChannel channelType;

    public TwitterCrawler(Twitter twitterClient, String queryToSearch, DatabaseHelperMethods helperMethods, String projectName, String projectUrl, String repositoryUrl, SocialMediaChannel channelType) {
        this.twitterClient = twitterClient;
        this.queryToSearch = queryToSearch;
        this.helperMethods = helperMethods;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
        this.repositoryUrl = repositoryUrl;
        this.channelType = channelType;
    }


    public void runCrawler() {
        int projectId = helperMethods.getOrCreateProject(projectName, projectUrl);
        int socialMediaRepositoryId = helperMethods.getOrCreateSocialMediaRepository(projectId, repositoryUrl, channelType);
        Query query = new Query(queryToSearch);
        query.count(100);
        try {
            QueryResult result;
            do {
                result = twitterClient.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                    User user = tweet.getUser();
                    int senderUserId = helperMethods.getOrCreateSocialMediaUser(socialMediaRepositoryId, user.getName(), user.getURL());

                    String originalEntryId = String.valueOf(tweet.getId());
                    String text = tweet.getText();
                    String inResponseTo = String.valueOf(tweet.getInReplyToStatusId());
                    String subject = join(getHashtags(tweet), ",");
                    String location = getLocation(tweet);
                    Date createdDate = tweet.getCreatedAt();

                    helperMethods.getOrSaveSocialMediaEntry(socialMediaRepositoryId, senderUserId, originalEntryId, text,
                            inResponseTo, null, subject, createdDate.toString(), null, null, null, location);
                    logCount();
                }
                if (result.getRateLimitStatus().getRemaining() < 2) {
                    sleep(result.getRateLimitStatus().getSecondsUntilReset() * 1000);
                }
            } while ((query = result.nextQuery()) != null);
            LOGGER.info("Finished Crawling Tweets");
            exit(0);
        } catch (TwitterException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getHashtags(Status tweet) {
        Set<String> hashtags = newHashSet();
        HashtagEntity[] hashtagEntities = tweet.getHashtagEntities();
        for (HashtagEntity entity : hashtagEntities) {
            hashtags.add(entity.getText());
        }
        return hashtags;
    }

    private String getLocation(Status tweet) {
        String location = null;
        GeoLocation geoLocation = tweet.getGeoLocation();
        if (geoLocation != null) {
            location = geoLocation.toString();
        }
        return location;
    }

    private void logCount() {
        int i = tweetCount.incrementAndGet();
        if ((i % 100) == 0) LOGGER.info("Tweet Count: " + i);
    }
}
