/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.affinity.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Latency;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.affinity.AffinityGroup;
import org.opendaylight.controller.affinity.AffinityLink;

public class AffinityManagerImplTest {

    @Test
    public void testAffinityManagerAddRemoveConfig() {
        AffinityManagerImpl affinitymgr = new AffinityManagerImpl();
        affinitymgr.startUp();

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
	al1.setAttribute("isolate");

	// Add a self loop for ag2.
	AffinityLink al2 = new AffinityLink("link2", ag2, ag2);
	al2.setFromGroup(ag2);
	al2.setToGroup(ag2);
	al2.setName("link2");
	al2.setAttribute("hopcount");

	System.out.println("Affinity group size is " + ag1.size());
        Assert.assertTrue(ag1.size() == 2);
	ag1.print();

        Status result;
	result = affinitymgr.addAffinityGroup(ag1);
        Assert.assertTrue(result.isSuccess());

        result = affinitymgr.addAffinityGroup(ag2);
        Assert.assertTrue(result.isSuccess());
	
        result = affinitymgr.addAffinityLink(al1);
        Assert.assertTrue(result.isSuccess());

        result = affinitymgr.addAffinityLink(al2);
        Assert.assertTrue(result.isSuccess());
	
	/* Constraint checking? */
        result = (affinitymgr.removeAffinityGroup(ag1.getName()));
        Assert.assertTrue(result.isSuccess());

	affinitymgr.saveConfiguration();
    }
}
