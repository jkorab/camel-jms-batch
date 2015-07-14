package org.apache.camel.component.sjms.batch;

import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * @author jkorab
 */
class SessionCompletion implements Synchronization {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Session session;

    public SessionCompletion(Session session) {
        assert (session != null);
        this.session = session;
    }

    @Override
    public void onComplete(Exchange exchange) {
        try {
            log.debug("Committing");
            session.commit();
        } catch (JMSException ex) {
            log.error("Exception caught while committing: {}", ex.getMessage());
            exchange.setException(ex);
        }
    }

    @Override
    public void onFailure(Exchange exchange) {
        try {
            log.debug("Rolling back");
            session.rollback();
        } catch (JMSException ex) {
            log.error("Exception caught while rolling back: {}", ex.getMessage());
            exchange.setException(ex);
        }
    }
}
