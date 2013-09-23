package org.opendaylight.affinity.affinity;

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
    public String getName(String name) {
	return (this.name);
    }
    public void print() {
	System.out.println(name);
    }
    public String toString() {
	return "AffinityIdentifier [name= " + this.name + " value= " + value.toString() + "]";
    }
}

