/*
 * Copyright (c) 2013 Plexxi, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.analytics.internal;


import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class AnalyticsManagerTest extends TestCase {

        @Test
        public void testAnalyticsManagerCreation() {
                AnalyticsManager am = new AnalyticsManager();
                Assert.assertTrue(am != null);
        }

}
