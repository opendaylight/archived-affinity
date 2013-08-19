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
import org.opendaylight.controller.affinity.AffinityConfig;

public class AffinityManagerImplTest {

    @Test
    public void testAffinityManagerAddRemoveConfig() {
        AffinityManagerImpl affinitymgr = new AffinityManagerImpl();
        affinitymgr.startUp();

        AffinityConfig ac = new AffinityConfig("test_affinity1", "10.0.0.1", "10.0.0.2", "Isolate");

        // Add status to update/add
        Status addResult = affinitymgr.updateAffinityConfig(ac);
        Assert.assertTrue(addResult.isSuccess());

        Status removeResult = (affinitymgr.removeAffinityConfig(ac.getName()));
        Assert.assertTrue(removeResult.isSuccess());

        AffinityConfig affinityConfigResult = affinitymgr.getAffinityConfig(ac.getName());
        Assert.assertTrue(affinityConfigResult == null);
        // System.out.println("*" + switchmgr.addSubnet(subnet) + "*");
    }
}
