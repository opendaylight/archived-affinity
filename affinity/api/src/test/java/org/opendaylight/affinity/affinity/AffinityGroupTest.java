package org.opendaylight.affinity.affinity;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

//import org.opendaylight.controller.sal.core.MacAddress;

public class AffinityGroupTest extends TestCase {
    @Test
    public void testAffinityGroup() throws Exception {
	InetAddress ipAddress1 = InetAddress.getByName("10.0.0.10");	
	InetAddress ipAddress2 = InetAddress.getByName("10.0.0.20");	
	Integer int1 = new Integer(10);
	
	AffinityGroup ag1 = new AffinityGroup("group1");

	// Add a valid IP and confirm. 
	Status ret1 = ag1.add("10.0.0.10");
	Assert.assertTrue(ret1.isSuccess());

	Status ret2 = ag1.add("10.0.0.20");
	Assert.assertTrue(ret2.isSuccess());

	// Add an invalid element. 
	Status ret3 = ag1.add("10");
	System.out.println(ret3);
	Assert.assertTrue(!ret3.isSuccess());

	// Second affinity group.
	AffinityGroup ag2 = new AffinityGroup("group2");
	ag2.add("20.0.0.10");
	ag2.add("20.0.0.20");

	// Add an affinity link from ag1 to ag2. 
	AffinityLink al1 = new AffinityLink();
	al1.setFromGroup(ag1);
	al1.setToGroup(ag2);
	al1.setName("link1");
	al1.setAttribute("redirect");
        al1.setWaypoint("20.0.0.11");

	// Add a self loop for ag2.
	AffinityLink al2 = new AffinityLink("link2", ag2, ag2);
	al2.setFromGroup(ag2);
	al2.setToGroup(ag2);
	al2.setName("link2");
	al2.setAttribute("hopcount");

    	System.out.println("Affinity group size is " + ag1.size());
        Assert.assertTrue(ag1.size() == 2);
	ag1.print();
    }
}

