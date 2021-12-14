// camel-k: language=java property=file:application.properties property=quarkus.http.cors=true
// camel-k: dependency=camel-jackson dependency=github:openshift-integration:camel-k-example-event-streaming:1.6.x-SNAPSHOT

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class TimelineBridge extends RouteBuilder {

    private static final int MAX_INCIDENTS = 1024;
    private static final List<String> incidents = new ArrayList<>(MAX_INCIDENTS);

    @Override
    public void configure() throws Exception {

        rest("/")
                .get("/timeline").to("direct:timeline");


        from("kafka:timeline-data?brokers={{kafka.bootstrap.address}}&groupId=timelinebrige&autoOffsetReset=earliest")
                .process(exchange -> {
                   while (incidents.size() >= MAX_INCIDENTS) {
                       incidents.remove(0);
                   }

                   incidents.add(exchange.getMessage().getBody(String.class));
                });

        from("direct:timeline")
                .process(exchange -> {
                    ObjectMapper mapper = new ObjectMapper();

                    String reply = mapper.writeValueAsString(incidents);
                    exchange.getMessage().setBody(reply);
                    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                });

    }
}
