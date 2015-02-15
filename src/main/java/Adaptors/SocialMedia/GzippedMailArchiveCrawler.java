package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Model.SocialMedia.SocialMediaChannel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.sql.Connection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;
import static Model.SocialMedia.SocialMediaChannel.EMAIL;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Thread.sleep;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.http.impl.client.HttpClientBuilder.create;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

public class GzippedMailArchiveCrawler {
    private static final Logger LOGGER = getLogger(GzippedMailArchiveCrawler.class);
    private static final String CHARSET = "UTF-8";

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
        GzippedMailArchiveCrawler crawler = new GzippedMailArchiveCrawler(
                new Client(),
                getDatabaseConnection(),
                "HIBERNATE",
                "http://hibernate.org",
                "http://lists.jboss.org/pipermail/hibernate-dev/",
                EMAIL);

        crawler.run();
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
        return magic == GZIPInputStream.GZIP_MAGIC;
    }

    public void run() throws InterruptedException {
        WebResource resource = client.resource(repositoryUrl);
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

    private String doForEachFile(int i, int limit, String url) throws InterruptedException {
        String fileUrl = repositoryUrl + url;
        String emails = null;
        try {
            InputStream is = getInputStreamForUrl(fileUrl);

            if (fileUrl.contains("gz")) {
                is = new GZIPInputStream(is);
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

            emails = readFileToString(bufferedReader);

            closeQuietly(bufferedReader);
            closeQuietly(is);
        } catch (IOException e) {
            if (i >= limit) {
                e.printStackTrace();
                return emails;
            } else {
                sleep(1000);
                doForEachFile(++i, limit, url);
            }
        }
        return emails;
    }

    private String readFileToString(BufferedReader bufferedReader) throws IOException {
        StringWriter writer = new StringWriter();

        char[] buffer = new char[10240];
        for (int length; (length = bufferedReader.read(buffer)) > 0; ) {
            writer.write(buffer, 0, length);
        }
        return writer.toString();
    }

    private InputStream getInputStreamForUrl(String fileUrl) throws IOException {
        HttpClient client = create().build();
        HttpGet request = new HttpGet(fileUrl);
        request.addHeader("User-Agent", "USER_AGENT");
        HttpResponse response = client.execute(request);
        return response.getEntity().getContent();
    }

    private void logCount() {
        int i = emailFilesCount.getAndIncrement();
        LOGGER.info("Parsed Email Files: " + i);
    }
}
