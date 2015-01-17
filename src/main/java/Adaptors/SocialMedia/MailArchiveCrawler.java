package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import DatabaseConnectors.IssueTrackerConnector;
import com.sun.jersey.api.client.Client;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.List;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.IOUtils.toInputStream;

public class MailArchiveCrawler {
    private DatabaseHelperMethods helperMethods;
    private Connection connection;
    private Client client;
    private String projectName;
    private String projectUrl;

    public MailArchiveCrawler(Client client, Connection connection, String projectName, String projectUrl) {
        this.connection = connection;
        this.client = client;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
    }

    public static void main(String[] args) throws Exception {
        MailArchiveCrawler crawler = new MailArchiveCrawler(
                new Client(),
                new IssueTrackerConnector().getConnection(),
                "HIVE",
                "https://hive.apache.org");

        crawler.run();
    }

    public void run() throws Exception {

        //TODO automate this
        helperMethods = new DatabaseHelperMethods(connection, client);
        String mboxUrl = "http://mail-archives.apache.org/mod_mbox/hive-dev/201501.mbox";
        InputStream is = new URL(mboxUrl).openStream();

        String output = IOUtils.toString(is, "UTF-8");
        List<String> emails = newArrayList(on("From ").split(output));


        DefaultMessageBuilder messageBuilder = getMessageBuilder();
        for (String anEmail : emails) {
            Message message = messageBuilder.parseMessage(toInputStream(anEmail, "UTF-8"));
            if (message.getFrom() != null) {
                String to = message.getTo().get(0).toString();
                String fromName = message.getFrom().get(0).getName();
                String fromEmail = message.getFrom().get(0).getAddress();
                String messageId = message.getMessageId();

                String sentDate = message.getDate().toString();
                String replyTo = message.getReplyTo().get(0).toString();

                String subject = message.getSubject();
                Mailbox sender = message.getSender();
                String context;
                try {
                    TextBody reader = (TextBody) message.getBody();
                    context = IOUtils.toString(reader.getReader());
                } catch (ClassCastException e) {
                    BinaryBody body = (BinaryBody) message.getBody();
                    context = IOUtils.toString(body.getInputStream());
                }
                int projectId = helperMethods.getOrCreateProject(projectName, projectUrl);
                int userId = helperMethods.getOrCreateSocialMediaUser(fromName, fromEmail);
                //TODO check the order
                //helperMethods.saveSocialMediaEntry(projectId,userId, messageId, context, SocialMediaChannel.EMAIL, to, null, , subject, sentDate, null, null, null, null);
            }
        }
        is.close();
    }

    private DefaultMessageBuilder getMessageBuilder() {
        DefaultMessageBuilder messageBuilder = new DefaultMessageBuilder();
        BodyFactory bodyFactory = new BasicBodyFactory();
        BodyDescriptorBuilder bodyBuilder = new DefaultBodyDescriptorBuilder();
        messageBuilder.setBodyFactory(bodyFactory);
        messageBuilder.setBodyDescriptorBuilder(bodyBuilder);
        messageBuilder.setContentDecoding(true);
        messageBuilder.setFlatMode(true);

        MimeConfig mimeConfig = new MimeConfig();
        mimeConfig.setMaxLineLen(-1);
        mimeConfig.setMaxHeaderLen(-1);
        mimeConfig.setMaxHeaderCount(-1);
        messageBuilder.setMimeEntityConfig(mimeConfig);
        return messageBuilder;
    }
}
