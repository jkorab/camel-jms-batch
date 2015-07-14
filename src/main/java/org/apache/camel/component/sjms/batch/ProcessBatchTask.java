package org.apache.camel.component.sjms.batch;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.spi.Synchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Session;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class ProcessBatchTask implements Runnable {
    private final Logger LOG = LoggerFactory.getLogger(ProcessBatchTask.class);

    private final Exchange aggregatedExchange;
    private final Session session;
    private final Processor processor;

    public ProcessBatchTask(Processor processor, Exchange aggregatedExchange, Session session) {
        this.processor = processor;
        this.aggregatedExchange = aggregatedExchange;
        this.session = session;
    }

    private static AtomicInteger batchCount = new AtomicInteger();
    private static AtomicLong messagesReceived = new AtomicLong();
    private static AtomicLong messagesProcessed = new AtomicLong();

    @Override
    public void run() {
        int id = batchCount.getAndIncrement();
        int batchSize = aggregatedExchange.getProperty(SjmsBatchEndpoint.PROPERTY_BATCH_SIZE, Integer.class);
        LOG.debug("Processing batch[{}]:size={}:total={}", id, batchSize, messagesReceived.addAndGet(batchSize));

        aggregatedExchange.addOnCompletion(new Synchronization() {
            @Override
            public void onComplete(Exchange exchange) {
                try {
                    LOG.debug("Committing");
                    session.commit();
                } catch (JMSException e) {
                    LOG.error("Exception caught while committing: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(Exchange exchange) {
                try {
                    LOG.debug("Rolling back");
                    session.rollback();
                } catch (JMSException e) {
                    LOG.error("Exception caught while rolling back: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        });
        try {
            processor.process(aggregatedExchange);
            LOG.debug("Completed processing[{}]:total={}", id, messagesProcessed.addAndGet(batchSize));
        } catch (Exception e) {
            LOG.error("Error processing exchange: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
