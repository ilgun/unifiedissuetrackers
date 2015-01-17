package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
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

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.IOUtils.toInputStream;

public class EMailParser {
    private final DatabaseHelperMethods helperMethods;
    private final String projectName;
    private final String projectUrl;

    public EMailParser(DatabaseHelperMethods helperMethods, String projectName, String projectUrl) {
        this.helperMethods = helperMethods;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
    }

    public void parseAndSaveEmails(String emailsInMbox) throws IOException {
        DefaultMessageBuilder messageBuilder = getMessageBuilder();
        List<String> emails = newArrayList(on("From ").split(emailsInMbox));

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
