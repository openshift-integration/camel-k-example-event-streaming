// camel-k: language=java property=file:application.properties
// camel-k: dependency=github:openshift-integration:camel-k-example-event-streaming:1.8.x-SNAPSHOT

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.redhat.integration.earthquake.Data;
import com.redhat.integration.earthquake.Feature;

public class EarthquakeConsumer extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(EarthquakeConsumer.class);

    public static class MySplitter {
        public List<Feature> splitBody(Data data) {
            return data.getFeatures();
        }
    }

    public void configure() throws Exception {
        /*
         Read the data at a fixed interval of 1 second between each request, logging the execution of the
         route, setting up the HTTP method to GET and hitting the OpenAQ measurement API.
         */
        from("timer:refresh?period={{consumers.fetch.period}}&fixedRate=true")
                .log("USGS Earthquake route running")
                .setHeader(Exchange.HTTP_METHOD).constant("GET")
                .to("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson")
                .unmarshal().json(JsonLibrary.Jackson, Data.class)
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
                .to("kafka:earthquake-data?brokers={{kafka.bootstrap.address}}");

        from("direct:tap")
                .setBody(simple("Received message from USGS Earthquake Alert System: ${body}"))
                .to("log:info");
    }
}
