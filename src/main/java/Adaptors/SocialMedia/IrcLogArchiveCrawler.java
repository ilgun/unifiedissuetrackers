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

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;
import static com.google.common.collect.Sets.newTreeSet;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.log4j.Logger.getLogger;
import static org.jsoup.Jsoup.parse;

public class IrcLogArchiveCrawler {
    private static Logger LOGGER = getLogger(IrcLogArchiveCrawler.class);
    private final Client client;
    private final String projectName;
    private final String projectUrl;
    private final String repositoryUrl;
    private final SocialMediaChannel channelType;
    private AtomicInteger lineCount = new AtomicInteger(1);

    public IrcLogArchiveCrawler(Client client, String projectName, String projectUrl, String repositoryUrl, SocialMediaChannel channelType) {
        this.client = client;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
        this.repositoryUrl = repositoryUrl;
        this.channelType = channelType;
    }


    public void run() {
        DatabaseHelperMethods helperMethods = new DatabaseHelperMethods(getDatabaseConnection());
        int projectId = helperMethods.getOrCreateProject(projectName, projectUrl);
        int socialMediaRepositoryId = helperMethods.getOrCreateSocialMediaRepository(projectId, repositoryUrl, channelType);

        IrcLogLineParser parser = new IrcLogLineParser(helperMethods, socialMediaRepositoryId);

        TreeSet<String> logUrls = getLogUrls();
        Set<String> fileUrls = getAllFileUrls(logUrls);
        for (String fileUrl : fileUrls) {
            String httpResponse = getHttpResponse(fileUrl);
            Document doc = parse(httpResponse);
            Elements tableItems = doc.select("tr");
            for (Element tableItem : tableItems) {
                parser.parseAndSave(tableItem);
                helperMethods.commitTransaction();
                int i = lineCount.incrementAndGet();
                if ((i % 10000) == 0) LOGGER.info("Count is: " + i);
            }
        }
    }

    private Set<String> getAllFileUrls(Set<String> logUrls) {
        TreeSet<String> fileUrls = newTreeSet();
        for (String logUrl : logUrls) {
            fileUrls.addAll(getLogFileUrls(logUrl));
        }
        return fileUrls;
    }

    private TreeSet<String> getLogFileUrls(String logUrl) {
        TreeSet<String> fileUrls = newTreeSet();

        String output = getHttpResponse(repositoryUrl + logUrl);
        Document doc = parse(output);

        Elements directories = doc.getElementsByClass("name");
        if (!directories.isEmpty()) {
            for (Element directory : directories) {
                Elements dir = directory.getElementsByClass("file");
                if (dir.size() > 0 && dir.text().contains("html") && dir.text().contains("hibernate")) {
                    fileUrls.add(repositoryUrl + logUrl + "/" + dir.text().replace("#", "%23"));
                }
            }
        } else {
            Elements listItems = doc.select("a");
            for (Element listItem : listItems) {
                if (listItem.attr("href").contains("hibernate")) {
                    fileUrls.add(repositoryUrl + logUrl + "/" + listItem.attr("href"));
                }
            }
        }
        return fileUrls;
    }

    private TreeSet<String> getLogUrls() {
        String httpResponse = getHttpResponse(repositoryUrl);

        TreeSet<String> logUrls = newTreeSet();
        Document doc = parse(httpResponse);
        Elements directories = doc.getElementsByClass("name");
        for (Element anElement : directories) {
            Elements dir = anElement.getElementsByClass("dir");
            if (dir.size() > 0) {
                logUrls.add(dir.text());
            }
        }
        return logUrls;
    }

    private String getHttpResponse(String url) {
        WebResource resource = client.resource(url);
        ClientResponse response = resource.accept(TEXT_HTML).get(ClientResponse.class);
        return response.getEntity(String.class);
    }
}
