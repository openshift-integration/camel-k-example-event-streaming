// camel-k: language=java property=file:application.properties
// camel-k: dependency=mvn:org.amqphub.quarkus:quarkus-qpid-jms
// camel-k: dependency=github:openshift-integration:camel-k-example-event-streaming

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.redhat.integration.common.Alert;
import com.redhat.integration.earthquake.Feature;

public class EarthquakeBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(EarthquakeBridge.class);

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String titleHeader = "title";

        from("kafka:earthquake-data?brokers={{kafka.bootstrap.address}}&groupId=earthquakebrige&autoOffsetReset=earliest")
                .unmarshal().json(JsonLibrary.Jackson, Feature.class)
                .process(exchange -> {
                    Feature feature = exchange.getMessage().getBody(Feature.class);

                    String alert = feature.getProperties().getAlert();
                    double magnitude = feature.getProperties().getMag();
                    int tsunami = feature.getProperties().getTsunami();

                    if (alert != null || magnitude > 4.0 || tsunami != 0) {
                        exchange.getMessage().setHeader(unsafeHeader, true);

                        Alert alertMessage = new Alert();
                        String text = "Critical geological event: " + feature.getProperties().getTitle();

                        if (tsunami != 0) {
                            text = text + " with possibility of tsunami";
                        }

                        alertMessage.setSeverity("red");
                        alertMessage.setText(text);

                        exchange.getMessage().setBody(alertMessage);
                    }
                    else {
                        LOG.debug("Non-critical geological event: {}", feature.getProperties().getTitle());
                    }
                    exchange.getMessage().setHeader(titleHeader ,feature.getProperties().getTitle());
                })
                .marshal().json()
                .convertBodyTo(String.class)
                .choice()
                    .when(header(unsafeHeader).isEqualTo(true))
                        .wireTap("direct:timeline")
                        .to("jms://queue:alarms?timeToLive={{messaging.ttl.alarms}}", "jms://queue:notifications?timeToLive={{messaging.ttl.notifications}}");

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");
    }
}
