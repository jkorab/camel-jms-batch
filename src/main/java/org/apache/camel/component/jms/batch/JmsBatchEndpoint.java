package org.apache.camel.component.jms.batch;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * @author jkorab
 */
public class JmsBatchEndpoint extends DefaultEndpoint {

    public static final int DEFAULT_COMPLETION_SIZE = 100; // less than the default pageSize in ActiveMQ
    public static final int DEFAULT_COMPLETION_TIMEOUT = 500;
    public static final String PROPERTY_BATCH_SIZE = "BatchJms.batchSize";

    private String queueName;
    private int concurrentConsumers = 1;
    private int jmsConsumers = 1;
    private Integer completionSize = DEFAULT_COMPLETION_SIZE;
    private Integer completionTimeout = DEFAULT_COMPLETION_TIMEOUT;
    private String aggregationStrategyRef;

    public JmsBatchEndpoint() {}

    public JmsBatchEndpoint(String endpointUri, Component component, String remaining) {
        super(endpointUri, component);
        this.queueName = remaining;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("Cannot produce though a " + JmsBatchEndpoint.class.getName());
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new JmsBatchConsumer(this, processor);
    }

    public String getAggregationStrategyRef() {
        return aggregationStrategyRef;
    }

    public void setAggregationStrategyRef(String aggregationStrategyRef) {
        this.aggregationStrategyRef = aggregationStrategyRef;
    }

    public Integer getCompletionSize() {
        return completionSize;
    }

    public void setCompletionSize(Integer completionSize) {
        this.completionSize = completionSize;
    }

    public Integer getCompletionTimeout() {
        return completionTimeout;
    }

    public void setCompletionTimeout(Integer completionTimeout) {
        this.completionTimeout = completionTimeout;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public int getJmsConsumers() {
        return jmsConsumers;
    }

    public void setJmsConsumers(int jmsConsumers) {
        this.jmsConsumers = jmsConsumers;
    }

}
