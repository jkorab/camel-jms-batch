package org.apache.camel.component.sjms.batch;

import java.io.Serializable;

/**
 * Test class used to validate JMS Object type binding.
 * @author jkorab
 */
public class Pojo implements Serializable {
    private String name;

    public Pojo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Pojo{" +
                "name='" + name + '\'' +
                '}';
    }
}
