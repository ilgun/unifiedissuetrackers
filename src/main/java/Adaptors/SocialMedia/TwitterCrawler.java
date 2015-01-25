package Adaptors.SocialMedia;

import twitter4j.*;

import java.util.List;

public class TwitterCrawler {
    private final Twitter twitterClient;
    private final String queryToSearch;

    public TwitterCrawler(Twitter twitterClient, String queryToSearch) {
        this.twitterClient = twitterClient;
        this.queryToSearch = queryToSearch;
    }


    public void runCrawler() {
        Query query = new Query(queryToSearch);
        query.count(100);
        QueryResult result = null;
        try {
            result = twitterClient.search(query);
        } catch (TwitterException e) {
            e.printStackTrace();
            throw new RuntimeException("Something went wrong", e);
        }
        List<Status> tweets = result.getTweets();
        System.out.println(tweets);
    }
}
