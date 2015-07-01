package org.apache.camel.component.jms.batch;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jkorab
 */
public class ListAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String body = newExchange.getIn().getBody(String.class);
        if (oldExchange == null) {
            List<String> list = new ArrayList<String>();
            list.add(body);
            newExchange.getIn().setBody(list);
            return newExchange;
        }  else {
            List<String> list = oldExchange.getIn().getBody(List.class);
            list.add(body);
            return oldExchange;
        }
    }
}
