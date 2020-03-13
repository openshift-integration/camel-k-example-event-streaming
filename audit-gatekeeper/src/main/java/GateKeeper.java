import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GateKeeper extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(GateKeeper.class);

    public void configure() throws Exception {
        /**
         * This leverages the Knative channels and decouples the route from the
         * transport mechanism used to send the events. By using this, it is possible
         * to use features like scale-to-zero from Knative.
         */
        from("knative:channel/audit")
                .streamCaching()
                .convertBodyTo(String.class)
                .wireTap("direct:log");

        from("direct:log")
                .to("log:info");
    }
}
