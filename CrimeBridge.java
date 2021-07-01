// camel-k: language=java property=file:application.properties
// camel-k: dependency=mvn:org.apache.activemq:artemis-jms-client:2.11.0.redhat-00005 dependency=github:openshift-integration:camel-k-example-event-streaming

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.redhat.integration.common.Alert;
import com.redhat.integration.common.Data;

public class CrimeBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(CrimeBridge.class);

    @PropertyInject("messaging.broker.url")
    private String messagingBrokerUrl;

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String locationHeader = "location";

        Sjms2Component sjms2Component = new Sjms2Component();
        sjms2Component.setConnectionFactory(new ActiveMQConnectionFactory(messagingBrokerUrl));
        getContext().addComponent("sjms2", sjms2Component);

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
                        .to("sjms2://queue:alarms?ttl={{messaging.ttl.alarms}}")
                    .otherwise()
                        .to("sjms2://queue:notifications?ttl={{messaging.ttl.notifications}}");

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");
    }
}
