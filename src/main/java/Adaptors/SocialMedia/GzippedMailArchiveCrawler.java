package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Model.SocialMedia.SocialMediaChannel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;
import static Model.SocialMedia.SocialMediaChannel.EMAIL;
import static java.util.Locale.US;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.zip.GZIPInputStream.GZIP_MAGIC;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

public class GzippedMailArchiveCrawler {
    private static final Logger LOGGER = getLogger(GzippedMailArchiveCrawler.class);

    private final AtomicInteger emailFilesCount = new AtomicInteger(1);
    private final Connection connection;
    private final Client client;
    private final String projectName;
    private final String projectUrl;
    private final String repositoryUrl;
    private final SocialMediaChannel channelType;

    public GzippedMailArchiveCrawler(Client client, Connection connection, String projectName, String projectUrl, String repositoryUrl, SocialMediaChannel channelType) {
        this.connection = connection;
        this.client = client;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
        this.repositoryUrl = repositoryUrl;
        this.channelType = channelType;
    }

    public static void main(String[] args) throws Exception {
        GzippedMailArchiveCrawler hibernateEmailCrawler = new GzippedMailArchiveCrawler(
                new Client(),
                getDatabaseConnection(),
                "HIBERNATE",
                "http://hibernate.org",
                "http://lists.jboss.org/pipermail/hibernate-dev/",
                EMAIL);

        hibernateEmailCrawler.run();

        GzippedMailArchiveCrawler pulpEmailCrawler = new GzippedMailArchiveCrawler(
                new Client(),
                getDatabaseConnection(),
                "PULP",
                "http://www.pulpproject.org",
                "https://www.redhat.com/archives/pulp-list/",
                EMAIL);

        pulpEmailCrawler.run();
    }

    public void run() throws InterruptedException {
        SortedSet<String> urls = getArchiveUrls();
        DatabaseHelperMethods helperMethods = new DatabaseHelperMethods(connection);
        EmailParser parser = new EmailParser(helperMethods, projectName, projectUrl, repositoryUrl, channelType);

        LOGGER.info("Total Email Files: " + urls.size());

        for (String url : urls) {
            String emails = doForEachFile(0, 20, url);
            if (emails == null) continue;
            try {
                parser.parseAndSaveEmails(emails);
            } catch (IOException e) {
                e.printStackTrace();
            }
            LOGGER.info("Finished parsing file: " + url);
            logCount();
        }
        LOGGER.info("Process Finished");
    }

    private Elements getElements() {
        WebResource resource = client.resource(repositoryUrl);
        ClientResponse response = resource.accept(TEXT_HTML).get(ClientResponse.class);
        String output = response.getEntity(String.class);

        Document doc = parse(output);
        return doc.getElementsByTag("a");
    }

    private SortedSet<String> getArchiveUrls() {
        SortedSet<String> urls = new TreeSet<>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                try {
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MMMM", US);
                    return fmt.parse(o1).compareTo(fmt.parse(o2));
                } catch (ParseException ex) {
                    return o1.compareTo(o2);
                }
            }
        });
        return getArchiveUrlsFor(0, 100, urls);
    }

    private SortedSet<String> getArchiveUrlsFor(int i, int limit, SortedSet<String> urls) {
        Elements elements = getElements();

        for (Element anElement : elements) {
            String url = anElement.attributes().get("href");
            if (url.contains(".txt")) {
                urls.add(url);
            }
        }
        if (i < limit) {
            return getArchiveUrlsFor(++i, 100, urls);
        }
        return urls;
    }

    private String doForEachFile(int i, int limit, String url) throws InterruptedException {
        String fileUrl = repositoryUrl + url;
        String emails = null;
        InputStream is = null;
        BufferedReader bufferedReader = null;
        try {
            is = getInputStreamForUrl(fileUrl);

            if (fileUrl.contains("gz") && isGZipped(is)) {
                is = new GZIPInputStream(is);
            }

            bufferedReader = new BufferedReader(new InputStreamReader(is));
            emails = readFileToString(bufferedReader);
        } catch (IOException e) {
            if (i >= limit) {
                LOGGER.error("Error occurred while parsing file: " + fileUrl);
                e.printStackTrace();
            } else {
                LOGGER.error("Retrying parsing file: " + fileUrl);
                LOGGER.error("Cause of the error: ", e);
                MINUTES.sleep(1);
                doForEachFile(++i, limit, url);
            }
        } finally {
            closeQuietly(bufferedReader);
            closeQuietly(is);
        }
        return emails;
    }

    private String readFileToString(BufferedReader bufferedReader) throws IOException {
        StringBuffer sb = new StringBuffer();

        int BUFFER_SIZE = 1;
        char[] buffer = new char[BUFFER_SIZE];
        int charsRead = 0;
        while ((charsRead = bufferedReader.read(buffer, 0, BUFFER_SIZE)) != -1) {
            sb.append(buffer, 0, charsRead);
        }
        return sb.toString();
    }

    private InputStream getInputStreamForUrl(String fileUrl) throws IOException {
        return new URL(fileUrl).openStream();
    }

    private boolean isGZipped(InputStream in) {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in);
        }
        in.mark(2);
        int magic = 0;
        try {
            magic = in.read() & 0xff | ((in.read() << 8) & 0xff00);
            in.reset();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return false;
        }
        return magic == GZIP_MAGIC;
    }

    private void logCount() {
        int i = emailFilesCount.getAndIncrement();
        LOGGER.info("Parsed Email Files: " + i);
    }
}
