package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Model.SocialMedia.SocialMediaChannel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;
import static Model.SocialMedia.SocialMediaChannel.EMAIL;
import static com.google.common.collect.Sets.newHashSet;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

public class MboxMailArchiveCrawler {
    private static final Logger LOGGER = getLogger(MboxMailArchiveCrawler.class);
    private final AtomicInteger emailFilesCount = new AtomicInteger();
    private final Connection connection;
    private final Client client;
    private final String projectName;
    private final String projectUrl;
    private final String repositoryUrl;
    private final SocialMediaChannel channelType;

    public MboxMailArchiveCrawler(Client client, Connection connection, String projectName, String projectUrl, String repositoryUrl, SocialMediaChannel channelType) {
        this.connection = connection;
        this.client = client;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
        this.repositoryUrl = repositoryUrl;
        this.channelType = channelType;
    }

    public static void main(String[] args) throws Exception {
        MboxMailArchiveCrawler crawler = new MboxMailArchiveCrawler(
                new Client(),
                getDatabaseConnection(),
                "HIVE",
                "https://hive.apache.org",
                "http://mail-archives.apache.org/mod_mbox/hive-dev/",
                EMAIL);

        crawler.run();
    }

    public void run() throws IOException {
        WebResource resource = client.resource(repositoryUrl);
        ClientResponse response = resource.accept(TEXT_HTML).get(ClientResponse.class);
        String output = response.getEntity(String.class);

        Document doc = parse(output);
        Elements elements = doc.getElementsByTag("a");

        Set<String> mboxUrls = newHashSet();
        for (Element anElement : elements) {
            String mboxUrl = anElement.attributes().get("href");
            if (mboxUrl.startsWith("20")) {
                mboxUrls.add(mboxUrl.split("/")[0]);
            }
        }

        DatabaseHelperMethods helperMethods = new DatabaseHelperMethods(connection);
        EmailParser parser = new EmailParser(helperMethods, projectName, projectUrl, repositoryUrl, channelType);

        LOGGER.info("Total Email Files: " + mboxUrls.size());

        for (String mboxLink : mboxUrls) {
            String emailsInMbox = doForeachMboxFile(0, 5, mboxLink);
            if (emailsInMbox == null) continue;
            parser.parseAndSaveEmails(emailsInMbox);
            logCount();
        }
        LOGGER.info("Process Finished");
    }

    private String doForeachMboxFile(int i, int limit, String mboxLink) {
        String mboxUrl = repositoryUrl + mboxLink;
        String emailsInMbox = null;
        try (InputStream is = new URL(mboxUrl).openStream()) {
            emailsInMbox = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            if (i >= limit) {
                e.printStackTrace();
            }
            doForeachMboxFile(++i, limit, mboxLink);
        }
        return emailsInMbox;
    }

    private void logCount() {
        int i = emailFilesCount.getAndIncrement();
        LOGGER.info("Parsed Email Files: " + i);
    }
}
