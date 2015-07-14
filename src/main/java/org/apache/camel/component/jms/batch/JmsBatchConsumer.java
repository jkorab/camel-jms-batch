package org.apache.camel.component.jms.batch;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jkorab
 */
public class JmsBatchConsumer extends DefaultConsumer {
    private static final boolean TRANSACTED = true;
    private final Logger LOG = LoggerFactory.getLogger(JmsBatchConsumer.class);

    private final JmsBatchEndpoint jmsBatchEndpoint;
    private final AggregationStrategy aggregationStrategy;
    private final int completionSize;
    private final int completionTimeout;
    private final int jmsConsumers;
    private final ConnectionFactory connectionFactory;
    private final String destinationName;
    private final Processor processor;
    private ExecutorService executorService;
    private final int concurrentConsumers;

    public JmsBatchConsumer(JmsBatchEndpoint jmsBatchEndpoint, Processor processor) {
        super(jmsBatchEndpoint, processor);

        this.jmsBatchEndpoint = ObjectHelper.notNull(jmsBatchEndpoint, "batchJmsEndpoint");
        this.processor = ObjectHelper.notNull(processor, "processor");

        destinationName = ObjectHelper.notEmpty(jmsBatchEndpoint.getDestinationName(), "jmsBatchEndpoint.destinationName");

        completionSize = jmsBatchEndpoint.getCompletionSize();
        completionTimeout = jmsBatchEndpoint.getCompletionTimeout();

        JmsBatchComponent jmsBatchComponent = (JmsBatchComponent) jmsBatchEndpoint.getComponent();
        connectionFactory = jmsBatchComponent.getConnectionFactory();

        AggregationStrategy aggregationStrategy = jmsBatchEndpoint.getAggregationStrategy();
        this.aggregationStrategy = ObjectHelper.notNull(aggregationStrategy, "aggregationStrategy");

        concurrentConsumers = jmsBatchEndpoint.getConcurrentConsumers();
        if (concurrentConsumers > 0) {
            throw new IllegalArgumentException("concurrentConsumers must be greater than 0");
        }

        jmsConsumers = jmsBatchEndpoint.getJmsConsumers();
        if (jmsConsumers > 0) {
            throw new IllegalArgumentException("jmsConsumers must be greater than 0");
        }
    }

    @Override
    public Endpoint getEndpoint() {
        return jmsBatchEndpoint;
    }

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicReference<CountDownLatch> consumersShutdownLatchRef = new AtomicReference<>();

    private Connection connection;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // TODO may not like multiple consumers being spun up; test
        executorService = getEndpoint().getCamelContext().getExecutorServiceManager()
                .newFixedThreadPool(this, "SjmsBatchConsumer", concurrentConsumers);

        // start up a shared connection
        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException ex) {
            LOG.error("Exception caught closing connection: {}", getStackTrace(ex));
            return;
        }

        LOG.info("Starting {} consumer(s) for {}:{}", jmsConsumers, destinationName, completionSize);
        consumersShutdownLatchRef.set(new CountDownLatch(jmsConsumers));
        ExecutorService jmsConsumerExecutors = Executors.newFixedThreadPool(jmsConsumers);
        for (int i = 0; i < jmsConsumers; i++) {
            jmsConsumerExecutors.execute(new BatchConsumptionLoop());
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        running.set(false);
        CountDownLatch consumersShutdownLatch = consumersShutdownLatchRef.get();
        if (consumersShutdownLatch != null) {
            LOG.info("Stop signalled, waiting on consumers to shut down");
            consumersShutdownLatch.await();
            LOG.info("All consumers have shut down");
        } else {
            LOG.info("Stop signalled while there are no consumers yet, so no need to wait for consumers");
        }

        try {
            LOG.debug("Shutting down connection");
            connection.close();
        } catch (JMSException jex) {
            LOG.error("Exception caught closing connection: {}", getStackTrace(jex));
        }

        if (executorService != null) {
            getEndpoint().getCamelContext().getExecutorServiceManager().shutdownGraceful(executorService);
        }

    }

    private String getStackTrace(Exception ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private class BatchConsumptionLoop implements Runnable {
        @Override
        public void run() {
            try {
                batchConsumption: while(running.get()) {
                    // a batch corresponds to a single session that will be committed or rolled back by a background thread
                    final Session session = connection.createSession(TRANSACTED, Session.CLIENT_ACKNOWLEDGE);
                    // TODO destinationName only creates queues
                    Queue queue = session.createQueue(destinationName);
                    MessageConsumer consumer = session.createConsumer(queue);

                    int messageCount = 0;
                    long timeElapsed = 0;

                    long startTime = 0;
                    Exchange aggregatedExchange = null;

                    batch: while (messageCount < completionSize) {
                        // check periodically to see whether we should be shutting down
                        long timeRemaining = completionTimeout - timeElapsed;
                        if (timeElapsed > 0) {
                            LOG.debug("Time remaining this batch: {}", timeRemaining);
                        }

                        if (timeRemaining <= 0) { // ensure that the thread doesn't wait indefinitely
                            timeRemaining = 1;
                        }
                        final long waitTime = (timeRemaining > 100) ? 100 : timeRemaining;
                        LOG.debug("waiting for {}", waitTime);
                        Message message = consumer.receive(waitTime);

                        if (running.get()) { // no interruptions received
                            if (message == null) {
                                // timed out, no message received
                                LOG.trace("No message received");
                            } else {
                                if (messageCount == 0) { // this is the first message
                                    startTime = new Date().getTime(); // start counting down the period for this batch
                                }
                                messageCount++;
                                LOG.debug("Message received: {}", messageCount);
                                if ((message instanceof ObjectMessage)
                                        || (message instanceof TextMessage)) {
                                    aggregatedExchange = aggregationStrategy.aggregate(aggregatedExchange, getExchange(message));
                                    aggregatedExchange.setProperty(JmsBatchEndpoint.PROPERTY_BATCH_SIZE, messageCount);
                                } else {
                                    throw new IllegalArgumentException("Unexpected message type: "
                                            + message.getClass().toString());
                                }
                            }
                            if (startTime > 0) {
                                // a batch has been started, check whether it should be timed out
                                long currentTime = new Date().getTime();
                                timeElapsed = currentTime - startTime;

                                if (timeElapsed > completionTimeout) {
                                    // batch finished by timeout
                                    break batch;
                                }
                            }
                        } else {
                            LOG.info("Shutdown signal received - rolling batch back");
                            session.rollback();
                            break batchConsumption;
                        }
                    } // batch
                    assert (aggregatedExchange != null);
                    consumer.close();
                    executorService.submit(new ProcessBatchTask(processor, aggregatedExchange, session));
                }
            } catch (Exception ex) {
                LOG.error("Exception caught consuming from {}: {}", destinationName, getStackTrace(ex));
            } finally {
                // indicate that we have shut down
                CountDownLatch consumersShutdownLatch = consumersShutdownLatchRef.get();
                consumersShutdownLatch.countDown();
            }
        }

        // TODO replace with final DefaultExchange exchange = (DefaultExchange) JmsMessageHelper.createExchange(message, getEndpoint());
        private Exchange getExchange(Message message) throws JMSException {
            Exchange exchange = jmsBatchEndpoint.createExchange();
            org.apache.camel.Message in = exchange.getIn();
            populateExchangeHeaders(exchange, message);
            if (message instanceof ObjectMessage) {
                Serializable object = ((ObjectMessage) message).getObject();
                LOG.debug("Processing " + object);
                in.setBody(object);
            } else if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                LOG.debug("Processing " + text);
                in.setBody(text);
            }
            return exchange;
        }

        private void populateExchangeHeaders(Exchange exchange, Message message) throws JMSException {
            // TODO copy any interesting headers from the JMS Message to the Exchange here
            // TODO header strategy
        }
    }
}
