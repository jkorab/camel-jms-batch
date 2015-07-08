package org.apache.camel.component.jms.batch;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.commons.lang.time.StopWatch;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.JmsTransactionManager;

import javax.jms.ConnectionFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertTrue;

/**
 * @author jkorab
 */
public class JmsBatchConsumerTest extends BrokerTestSupport {
    public static final String PROPAGATION_REQUIRED = "PROPAGATION_REQUIRED";
    private final Logger LOG = LoggerFactory.getLogger(JmsBatchConsumerTest.class);

    @Rule
    public EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker("localhost");

    private SimpleRegistry registry;

    @Override
    public CamelContext createCamelContext() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("testStrategy", new ListAggregationStrategy());
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(broker.getTcpConnectorUri());

        // set up transactions to enable faster sends
        JmsTransactionManager transactionManager = new JmsTransactionManager(connectionFactory);

        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setConnectionFactory(connectionFactory);
        activeMQComponent.setTransactionManager(transactionManager);

        SpringTransactionPolicy policy = new SpringTransactionPolicy(transactionManager);
        policy.setPropagationBehaviorName(PROPAGATION_REQUIRED);
        registry.put(PROPAGATION_REQUIRED, policy);

        JmsBatchComponent jmsBatchComponent = new JmsBatchComponent();
        jmsBatchComponent.setConnectionFactory(connectionFactory);

        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("jms", activeMQComponent);
        context.addComponent("batchjms", jmsBatchComponent);
        return context;
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Test(expected = org.apache.camel.FailedToCreateProducerException.class)
    public void testProducerFailure() throws Exception {
        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:in").to("batchjms:testQueue");
            }
        });
        context.start();
    }

    @Test
    public void testConsumption() throws Exception {

        final int messageCount = 5000;
        final int jmsConsumerCount = 1;
        final int concurrentConsumers = 5;

        final String queueName = getQueueName();
        context.addRoutes(new TransactedSendHarness(queueName));
        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("batchjms:" + queueName +
                        "?completionTimeout=200" +
                        "&completionSize=100" +
                        "&jmsConsumers=" + jmsConsumerCount +
                        "&concurrentConsumers=" + concurrentConsumers +
                        "&aggregationStrategy=#testStrategy").routeId("batchConsumer").startupOrder(10)
                    .wireTap("mock:batches")
                    .split(body())
                        .to("mock:split")
                        .stop()
                    .end();
            }
        });
        context.start();


        MockEndpoint mockBefore = getMockEndpoint("mock:before");
        mockBefore.setExpectedMessageCount(messageCount);

        MockEndpoint mockSplit = getMockEndpoint("mock:split");
        mockSplit.setExpectedMessageCount(messageCount);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LOG.info("Sending messages");
        template.sendBody("direct:in", generatePojos(messageCount));
        LOG.info("Send complete");

        assertMockEndpointsSatisfied();
        stopWatch.stop();
        long time = stopWatch.getTime();
        LOG.info("Processed {} messages in {} ms", messageCount, time);
        LOG.info("Average throughput {} msg/s", messageCount / (time / 1000));
    }

    private Pojo[] generatePojos(int messageCount) {
        Pojo[] pojos = new Pojo[messageCount];
        for (int i = 0; i < messageCount; i++) {
            pojos[i] = new Pojo("pojo:" + i);
        }
        return pojos;
    }

    @Test
    public void testConsumption_completionSize() throws Exception {
        // FIXME hidden problem - using completionInterval
        final int batchSize = 5;
        final String queueName = getQueueName();
        context.addRoutes(new TransactedSendHarness(queueName));
        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("batchjms:" + queueName +
                        "?completionSize=" + batchSize + "&aggregationStrategy=#testStrategy").routeId("batchConsumer").startupOrder(10)
                        .log("${body.size}")
                        .to("mock:batches");
            }
        });
        context.start();

        int messageCount = 100;
        MockEndpoint mockBatches = getMockEndpoint("mock:batches");
        mockBatches.expectedMessageCount(messageCount / batchSize);

        template.sendBody("direct:in", generatePojos(messageCount));
        mockBatches.assertIsSatisfied();
    }

    @Test
    public void testConsumption_completionTimeout() throws Exception {
        final int completionTimeout = 2000;
        final String queueName = getQueueName();
        context.addRoutes(new TransactedSendHarness(queueName));
        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("batchjms:" + queueName +
                        "?completionTimeout=" + completionTimeout + "&aggregationStrategy=#testStrategy").routeId("batchConsumer").startupOrder(10)
                        .to("mock:batches");
            }
        });
        context.start();

        int messageCount = 50; // too small to match default completion size
        assertTrue(messageCount < JmsBatchEndpoint.DEFAULT_COMPLETION_SIZE);
        MockEndpoint mockBatches = getMockEndpoint("mock:batches");
        mockBatches.expectedMessageCount(1);  // everything batched together

        template.sendBody("direct:in", generatePojos(messageCount));
        mockBatches.assertIsSatisfied();
    }

    private String getQueueName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddhhmmss");
        return "batchJms-" + sdf.format(new Date());
    }

    private static class TransactedSendHarness extends RouteBuilder {
        private final String queueName;

        public TransactedSendHarness(String queueName) {
            this.queueName = queueName;
        }

        @Override
        public void configure() throws Exception {
            from("direct:in").routeId("harness").startupOrder(20)
                // FIXME this transacted part isn't working for some reason
                //.transacted(PROPAGATION_REQUIRED)
                .split(body())
                    .to("mock:before")
                    .to("jms:queue:" + queueName)
                .end();
        }
    }
}
