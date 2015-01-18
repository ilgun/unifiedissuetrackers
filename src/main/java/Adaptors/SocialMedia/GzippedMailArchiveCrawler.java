package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import DatabaseConnectors.IssueTrackerConnector;
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

import static com.google.common.collect.Sets.newHashSet;
import static java.nio.charset.Charset.forName;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

public class GzippedMailArchiveCrawler {
    private static final Logger LOGGER = getLogger(GzippedMailArchiveCrawler.class);
    private final AtomicInteger emailFilesCount = new AtomicInteger();
    private final String baseUrl;
    private final Connection connection;
    private final Client client;
    private final String projectName;
    private final String projectUrl;

    public GzippedMailArchiveCrawler(Client client, Connection connection, String projectName, String projectUrl, String baseUrl) {
        this.baseUrl = baseUrl;
        this.connection = connection;
        this.client = client;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
    }

    public static void main(String[] args) throws Exception {
        GzippedMailArchiveCrawler crawler = new GzippedMailArchiveCrawler(
                new Client(),
                new IssueTrackerConnector().getConnection(),
                "PULP",
                "http://www.pulpproject.org",
                "https://www.redhat.com/archives/pulp-list/");

        crawler.run();
    }

    public void run() throws IOException, InterruptedException {
        WebResource resource = client.resource(baseUrl);
        ClientResponse response = resource.accept(TEXT_HTML).get(ClientResponse.class);
        String output = response.getEntity(String.class);

        Document doc = parse(output);
        Elements elements = doc.getElementsByTag("a");

        Set<String> urls = newHashSet();
        for (Element anElement : elements) {
            String url = anElement.attributes().get("href");
            if (url.endsWith("txt.gz")) {
                urls.add(url);
            }
        }

        DatabaseHelperMethods helperMethods = new DatabaseHelperMethods(connection);
        EmailParser parser = new EmailParser(helperMethods, projectName, projectUrl);

        LOGGER.info("Total Email Files: " + urls.size());

        for (String url : urls) {
            String emails = doForEachFile(0, 5, url);
            if (emails == null) continue;
            parser.parseAndSaveEmails(emails);
            logCount();
        }
        LOGGER.info("Process Finished");
    }

    private String doForEachFile(int i, int limit, String url) throws IOException {
        String fileUrl = baseUrl + url;
        String emails = null;

        try (InputStream is = new URL(fileUrl).openStream()) {
            emails = IOUtils.toString(is, forName("UTF-8"));
        } catch (IOException e) {
            if (i >= limit) {
                e.printStackTrace();
                return emails;
            } else {
                doForEachFile(++i, limit, url);
            }
        }
        return emails;
    }

    private void logCount() {
        int i = emailFilesCount.getAndIncrement();
        LOGGER.info("Parsed Email Files: " + i);
    }
}
