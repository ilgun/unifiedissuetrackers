package Adaptors.SocialMedia;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.DefaultBodyDescriptorBuilder;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.BodyDescriptorBuilder;
import org.apache.james.mime4j.stream.MimeConfig;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.io.IOUtils.toInputStream;

public class MailArchiveCrawler {

    public static void main(String[] args) throws Exception {
        MailArchiveCrawler crawler = new MailArchiveCrawler();
        crawler.run();
    }

    public void run() throws Exception {
        String mboxUrl = "http://mail-archives.apache.org/mod_mbox/hive-dev/201501.mbox";
        InputStream is = new URL(mboxUrl).openStream();

        String output = IOUtils.toString(is, "UTF-8");
        List<String> emails = newArrayList(on("From ").split(output));


        DefaultMessageBuilder messageBuilder = getMessageBuilder();
        for (String anEmail : emails) {
            Message message = messageBuilder.parseMessage(toInputStream(anEmail, "UTF-8"));
            if (message.getFrom() != null && message.getFrom().size() > 1) {
                System.out.println(message.getFrom().size());
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
