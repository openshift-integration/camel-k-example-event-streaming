// camel-k: language=java property=file:application.properties
// camel-k: dependency=github:openshift-integration:camel-k-example-event-streaming:1.6.x-SNAPSHOT
// camel-k: dependency=camel-http
// camel-k: dependency=camel-quarkus-http

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.redhat.integration.pollution.OpenAQData;
import com.redhat.integration.pollution.PollutionData;

public class OpenAQConsumer extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(OpenAQConsumer.class);

    public static class MySplitter {
        public List<PollutionData> splitBody(OpenAQData data) {
            return data.getResults().stream()
            .filter(d -> d.getParameter().equals("pm25") || d.getParameter().equals("pm10"))
            .collect(toList());
        }
    }

    public void configure() throws Exception {
         /*
         Read the data at a fixed interval of 1 second between each request, logging the execution of the
         route, setting up the HTTP method to GET and hitting the OpenAQ measurement API.
         */
        from("timer:refresh?period={{consumers.fetch.period}}&fixedRate=true")
                .log("OpenAQ route running")
                .setHeader(Exchange.HTTP_METHOD).constant("GET")
                .to("{{consumers.fetch.url}}?limit={{consumers.fetch.limit}}")
                .unmarshal().json(JsonLibrary.Jackson, OpenAQData.class)

                /*
                In this example we are only interested on the measurement data ... and we want to sent each
                measurement separately. To do, we use a splitter to split the results array and send to Kafka
                only the results data and nothing more.
                */
                .split().method(MySplitter.class, "splitBody")
                /*
                 Then setup a wireTap route to log the data before sending it to our Kafka instance.
                 */
                .marshal().json()
                .convertBodyTo(String.class)
                .wireTap("direct:tap")
                .to("kafka:pm-data?brokers={{kafka.bootstrap.address}}");

        from("direct:tap")
                .setBody(simple("Received message from OpenAQ: ${body}"))
                .to("log:info");

    }
}
