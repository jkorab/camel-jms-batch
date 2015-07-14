package org.apache.camel.component.sjms.batch;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.toolbox.AggregationStrategies;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author jkorab
 */
public class SjmsBatchEndpointTest extends CamelTestSupport {

    @Rule
    public EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker("localhost");

    @Override
    protected CamelContext createCamelContext() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("aggStrategy", AggregationStrategies.groupedExchange());

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(broker.getTcpConnectorUri());

        SjmsComponent sjmsComponent = new SjmsComponent();
        sjmsComponent.setConnectionFactory(connectionFactory);

        SjmsBatchComponent sjmsBatchComponent = new SjmsBatchComponent();
        sjmsBatchComponent.setConnectionFactory(connectionFactory);

        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("sjmsbatch", sjmsBatchComponent);
        context.addComponent("sjms", sjmsComponent);

        return context;
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test
    public void testMessageFlow() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("sjmsbatch:in?aggregationStrategy=#aggStrategy").routeId("in")
                    .to("mock:out");
            }
        });
        context.start();

        MockEndpoint mockOut = getMockEndpoint("mock:out");
        mockOut.setExpectedMessageCount(1);

        template.sendBody("sjms:in", "Ping");
        assertMockEndpointsSatisfied();
    }

    @Test(expected = org.apache.camel.FailedToCreateProducerException.class)
    public void testProducerFailure() throws Exception {
        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:in").to("sjmsbatch:testQueue");
            }
        });
        context.start();
    }


}
