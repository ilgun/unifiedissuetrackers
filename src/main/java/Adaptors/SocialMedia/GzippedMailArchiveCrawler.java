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

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Thread.sleep;
import static java.util.zip.GZIPInputStream.GZIP_MAGIC;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

public class GzippedMailArchiveCrawler {
    private static final Logger LOGGER = getLogger(GzippedMailArchiveCrawler.class);
    private static final String CHARSET = "UTF-8";
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
                "HIBERNATE",
                "http://hibernate.org",
                "http://lists.jboss.org/pipermail/hibernate-dev/");

        crawler.run();
    }

    private static boolean isGzipStream(byte[] bytes) {
        int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
        return GZIP_MAGIC == head;
    }

    public void run() throws InterruptedException {
        WebResource resource = client.resource(baseUrl);
        ClientResponse response = resource.accept(TEXT_HTML).get(ClientResponse.class);
        String output = response.getEntity(String.class);

        Document doc = parse(output);
        Elements elements = doc.getElementsByTag("a");

        Set<String> urls = newHashSet();
        for (Element anElement : elements) {
            String url = anElement.attributes().get("href");
            if (url.contains(".txt")) {
                urls.add(url);
            }
        }

        DatabaseHelperMethods helperMethods = new DatabaseHelperMethods(connection);
        EmailParser parser = new EmailParser(helperMethods, projectName, projectUrl);

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

    private String doForEachFile(int i, int limit, String url) throws InterruptedException {
        String fileUrl = baseUrl + url;
        String emails = null;
        Reader reader = null;
        StringWriter writer = null;

        try (InputStream is = new URL(fileUrl).openStream()) {
            byte[] responseBytes = toByteArray(is);
            if (isGzipStream(responseBytes)) {
                ByteArrayInputStream bais = new ByteArrayInputStream(responseBytes);
                GZIPInputStream decompressedStream = new GZIPInputStream(bais);
                reader = new InputStreamReader(decompressedStream, CHARSET);
                writer = new StringWriter();

                char[] buffer = new char[10240];
                for (int length; (length = reader.read(buffer)) > 0; ) {
                    writer.write(buffer, 0, length);
                }
                emails = writer.toString();
                closeQuietly(bais);
                closeQuietly(decompressedStream);
            } else {
                emails = IOUtils.toString(is, CHARSET);
            }
        } catch (IOException e) {
            if (i >= limit) {
                e.printStackTrace();
                return emails;
            } else {
                sleep(1000);
                doForEachFile(++i, limit, url);
            }
        } finally {
            closeQuietly(reader);
            closeQuietly(writer);
        }
        return emails;
    }

    private void logCount() {
        int i = emailFilesCount.getAndIncrement();
        LOGGER.info("Parsed Email Files: " + i);
    }
}
