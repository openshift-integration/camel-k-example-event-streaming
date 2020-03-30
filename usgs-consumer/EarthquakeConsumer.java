// camel-k: language=java

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

public class EarthquakeConsumer extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(EarthquakeConsumer.class);

    public static class MySplitter {
        public List<String> splitBody(Data data) {
            List<String> ret = new ArrayList<>(data.getFeatures().size());

            ObjectMapper mapper = new ObjectMapper();
            for (Feature feature: data.getFeatures()) {

                try {
                    ret.add(mapper.writeValueAsString(feature));
                } catch (JsonProcessingException e) {
                    LOG.error("Unable to serialize record: {}", feature);
                }
            }

            return ret;
        }
    }

    public void configure() throws Exception {
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat();

        jacksonDataFormat.setUnmarshalType(Data.class);
/*
         Read the data at a fixed interval of 1 second between each request, logging the execution of the
         route, setting up the HTTP method to GET and hitting the OpenAQ measurement API.
         */
        from("timer:refresh?period={{consumers.fetch.period}}&fixedRate=true")
                .log("USGS Earthquake route running")
                .setHeader(Exchange.HTTP_METHOD).constant("GET")
                .to("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson")
                .unmarshal(jacksonDataFormat)
                /*
                In this example we are only interested on the measurement data ... and we want to sent each
                measurement separately. To do, we use a splitter to split the results array and send to Kafka
                only the results data and nothing more.
                */
                .split().method(MySplitter.class, "splitBody")

                /*
                 Then setup a wireTap route to log the data before sending it to our Kafka instance.
                 */

                .wireTap("direct:tap")
                .to("kafka:earthquake-data?brokers={{kafka.bootstrap.address}}");

        from("direct:tap")
                .setBody(simple("Received message from USGS Earthquake Alert System: ${body}"))
                .to("log:info");
    }
}
