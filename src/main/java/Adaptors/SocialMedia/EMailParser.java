package Adaptors.SocialMedia;

import Adaptors.HelperMethods.DatabaseHelperMethods;
import Model.SocialMedia.SocialMediaChannel;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.field.LenientFieldParser;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
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
    private final String repositoryUrl;
    private final SocialMediaChannel channelType;

    public EmailParser(DatabaseHelperMethods helperMethods, String projectName, String projectUrl, String repositoryUrl, SocialMediaChannel channelType) {
        this.helperMethods = helperMethods;
        this.projectName = projectName;
        this.projectUrl = projectUrl;
        this.repositoryUrl = repositoryUrl;
        this.channelType = channelType;
    }

    public void parseAndSaveEmails(String emailsInMbox) throws IOException {
        int projectId = helperMethods.getOrCreateProject(projectName, projectUrl);
        int socialMediaRepositoryId = helperMethods.getOrCreateSocialMediaRepository(projectId,repositoryUrl, channelType);

        DefaultMessageBuilder messageBuilder = getMessageBuilder();
        List<String> emails = newArrayList(on("From ").split(emailsInMbox));

        for (String anEmail : emails) {
            Message message = messageBuilder.parseMessage(toInputStream(anEmail, "UTF-8"));
            if (!isEmpty(anEmail)) {
                if (message.getFrom() != null) {
                    List<String> to = getTo(message.getTo());
                    List<String> fromName = getFrom(message.getFrom());
                    List<String> fromEmail = fromEmail(message.getFrom());

                    String messageId = message.getMessageId();
                    String sentDate = message.getDate().toString();
                    String replyTo = getReplyTo(message.getHeader());
                    String subject = message.getSubject();
                    String context = getBody(message);

                    int userId = helperMethods.getOrCreateSocialMediaUser(projectId, join(fromName, ","), join(fromEmail, ","));
                    helperMethods.saveSocialMediaEntry(socialMediaRepositoryId, userId, messageId, context, join(replyTo, ","), join(to, ","), subject, sentDate, null, null, null, null);
                    logCount();
                } else if (message.getHeader().getField("from") != null) {
                    Header header = message.getHeader();
                    String from = header.getField("from").getBody();

                    String fromName = extractNameFrom(from);
                    String fromEmail = extractEmailFrom(from);
                    int userId = helperMethods.getOrCreateSocialMediaUser(projectId, fromName, fromEmail);
                    String messageId = header.getField("message-id").getBody();
                    String context = getBody(message);
                    String replyTo = getReplyTo(header);
                    String to = getReferences(header);
                    String subject = header.getField("subject").getBody();
                    String sentDate = header.getField("date").getBody();

                    helperMethods.saveSocialMediaEntry(socialMediaRepositoryId, userId, messageId, context, replyTo, to, subject, sentDate, null, null, null, null);
                    logCount();
                }
            }
        }
    }

    private String getReferences(Header header) {
        if (header.getField("references") != null) {
            return header.getField("references").getBody();
        }
        return "";
    }

    private String getReplyTo(Header header) {
        if (header.getField("in-reply-to") != null) {
            return header.getField("in-reply-to").getBody();
        }
        return null;
    }

    private String getBody(Message message) throws IOException {
        String context = null;
        Body rawBody = message.getBody();
        try {
            TextBody reader = (TextBody) rawBody;
            context = IOUtils.toString(reader.getReader());
        } catch (Exception e) {
            try {
                BinaryBody body = (BinaryBody) rawBody;
                context = IOUtils.toString(body.getInputStream());
            } catch (Exception ex) {
            }
        }
        return context;
    }

    private String extractEmailFrom(String input) {
        return input.substring(0, input.indexOf("("));
    }

    private String extractNameFrom(String input) {
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher match = pattern.matcher(input);
        match.find();
        return match.group(1);
    }

    private void logCount() {
        int i = count.incrementAndGet();
        if ((i % 2000) == 0) {
            LOGGER.info("Parsed Email Count: " + i);
        }
    }

    private List<String> fromEmail(MailboxList mailboxList) {
        List<String> emails = newArrayList();
        emails.addAll(mailboxList.stream().map(Mailbox::getAddress).collect(toList()));
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
        if (addressList == null) return to;
        to.addAll(addressList.stream().map(Address::toString).collect(toList()));
        return to;
    }

    private DefaultMessageBuilder getMessageBuilder() {
        BodyFactory bodyFactory = new BasicBodyFactory();
        BodyDescriptorBuilder bodyBuilder = new DefaultBodyDescriptorBuilder();

        DefaultMessageBuilder messageBuilder = new DefaultMessageBuilder();
        messageBuilder.setBodyFactory(bodyFactory);
        messageBuilder.setBodyDescriptorBuilder(bodyBuilder);
        messageBuilder.setFieldParser(new LenientFieldParser());

        MimeConfig mimeConfig = new MimeConfig();
        mimeConfig.setMaxLineLen(-1);
        mimeConfig.setMaxHeaderLen(-1);
        mimeConfig.setMaxHeaderCount(-1);
        messageBuilder.setMimeEntityConfig(mimeConfig);

        return messageBuilder;
    }
}
