package Adaptors.HelperMethods;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import static java.util.concurrent.TimeUnit.SECONDS;

public class JerseyClientHandler {
    private static Client jerseyClient = null;

    public static Client getJerseyClient() {
        if (jerseyClient != null) {
            return jerseyClient;
        } else {
            jerseyClient = new Client();
            jerseyClient.addFilter(new ClientFilter() {
                private final int maxRetries = 10;

                @Override
                public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                    int i = 0;
                    while (i < maxRetries) {
                        i++;
                        try {
                            return getNext().handle(cr);
                        } catch (ClientHandlerException ignored) {
                            try {
                                SECONDS.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    throw new ClientHandlerException("Connection retries limit exceeded.");
                }
            });
            return jerseyClient;
        }
    }
}
