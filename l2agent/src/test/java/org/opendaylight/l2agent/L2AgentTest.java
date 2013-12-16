
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.l2agent;


import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class L2AgentTest extends TestCase {

        @Test
        public void testL2AgentCreation() {

                L2Agent ah = null;
                ah = new L2Agent();
                Assert.assertTrue(ah != null);

        }

}
