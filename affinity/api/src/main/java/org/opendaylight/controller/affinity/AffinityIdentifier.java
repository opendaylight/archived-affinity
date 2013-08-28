package org.opendaylight.controller.affinity;

import java.util.ArrayList;
import java.util.List;

/* Affinity identifier */
public class AffinityIdentifier<T> {
    private T value;
    private String name;

    public T get() {
        return value;
    }
    public void set(T t) {
	value = t;
    }
    public void setName(String name) {
	this.name = name;
    }
    public void print() {
	System.out.println(value);
    }
}

