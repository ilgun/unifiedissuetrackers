package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Model.SocialMedia.SocialMediaChannel;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.log4j.Logger.getLogger;

public class EmailParser {
    private static final Logger LOGGER = getLogger(EmailParser.class);
    private final AtomicInteger count = new AtomicInteger();
    private final DatabaseHelperMethods helperMethods;
    private final String projectName;
    private final String projectUrl;

    public EmailParser(DatabaseHelperMethods helperMethods, String projectName, String projectUrl) {
        this.helperMethods = helperMethods;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
    }

    public void parseAndSaveEmails(String emailsInMbox) throws IOException {
        int projectId = helperMethods.getOrCreateProject(projectName, projectUrl);

        DefaultMessageBuilder messageBuilder = getMessageBuilder();
        List<String> emails = newArrayList(on("From ").split(emailsInMbox));

        for (String anEmail : emails) {
            Message message = messageBuilder.parseMessage(toInputStream(anEmail, "UTF-8"));
            if (message.getFrom() != null) {
                List<String> to = getTo(message.getTo());
                List<String> fromName = getFrom(message.getFrom());
                List<String> fromEmail = fromEmail(message.getFrom());

                String messageId = message.getMessageId();
                String sentDate = message.getDate().toString();
                List<String> replyTo = getReplyTo(message.getReplyTo());
                String subject = message.getSubject();

                String context;
                try {
                    TextBody reader = (TextBody) message.getBody();
                    context = IOUtils.toString(reader.getReader());
                } catch (ClassCastException e) {
                    BinaryBody body = (BinaryBody) message.getBody();
                    context = IOUtils.toString(body.getInputStream());
                }
                int userId = helperMethods.getOrCreateSocialMediaUser(projectId, join(fromName, ","), join(fromEmail, ","));
                helperMethods.saveSocialMediaEntry(projectId, userId, messageId, context, SocialMediaChannel.EMAIL, join(replyTo, ","), join(to, ","), subject, sentDate, null, null, null, null);
                logCount();
            }
        }
    }

    private void logCount() {
        int i = count.incrementAndGet();
        if ((i % 2000) == 0) {
            LOGGER.info("Parsed Email Count: " + i);
        }
    }

    private List<String> getReplyTo(AddressList addresses) {
        List<String> replies = newArrayList();
        for (Address address : addresses) {
            replies.add(address.toString());
        }
        return replies;
    }

    private List<String> fromEmail(MailboxList mailboxList) {
        List<String> emails = newArrayList();
        for (Mailbox mailbox : mailboxList) {
            emails.add(mailbox.getAddress());
        }
        return emails;
    }

    private List<String> getFrom(MailboxList mailboxList) {
        List<String> names = newArrayList();
        for (Mailbox mailbox : mailboxList) {
            String name = mailbox.getName();
            if (name == null) {
                return names;
            }
            String splittedUserName = name.split("\\(")[0];
            if (splittedUserName == null || !isEmpty(splittedUserName)) {
                names.add(splittedUserName);
            } else {
                names.add(name);
            }
        }
        return names;
    }

    private List<String> getTo(AddressList addressList) {
        List<String> to = newArrayList();
        if(addressList == null) return to;
        for (Address address : addressList) {
            to.add(address.toString());
        }
        return to;
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
