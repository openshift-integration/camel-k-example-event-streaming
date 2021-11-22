// camel-k: language=java property=file:application.properties
// camel-k: dependency=mvn:org.amqphub.quarkus:quarkus-qpid-jms dependency=github:openshift-integration:camel-k-example-event-streaming:main-SNAPSHOT

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.redhat.integration.common.Alert;
import com.redhat.integration.common.Data;

public class CrimeBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(CrimeBridge.class);

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String locationHeader = "location";

        from("kafka:crime-data?brokers={{kafka.bootstrap.address}}&groupId=crimebrige&autoOffsetReset=earliest")
                .unmarshal().json(JsonLibrary.Jackson, Data.class)
                .process(exchange -> {
                    Data eventData = exchange.getMessage().getBody(Data.class);
                    Alert alert = new Alert();

                    if (eventData.getReport().isAlert()) {
                        exchange.getMessage().setHeader(unsafeHeader, true);

                        alert.setSeverity("red");
                    }
                    else {
                        alert.setSeverity("yellow");
                    }

                    String text = String.format("There is a %s incident on %s", eventData.getReport().getMeasurement(),
                            eventData.getReport().getLocation());

                    alert.setText(text);

                    exchange.getMessage().setBody(alert);
                    exchange.getMessage().setHeader(locationHeader, eventData.getReport().getLocation());
                })
                .marshal().json()
                .convertBodyTo(String.class)
                .wireTap("direct:timeline")
                .choice()
                    .when(header(unsafeHeader).isEqualTo(true))
                        .to("jms://queue:alarms?timeToLive={{messaging.ttl.alarms}}")
                    .otherwise()
                        .to("jms://queue:notifications?timeToLive={{messaging.ttl.notifications}}");

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");
    }
}
