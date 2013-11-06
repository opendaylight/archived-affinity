/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics;

import java.util.Map;

import org.opendaylight.affinity.affinity.AffinityLink;
import org.opendaylight.controller.sal.core.Host;

public interface IAnalyticsManager {

    long getByteCountBetweenHosts(Host src, Host dst);

    double getBitRateBetweenHosts(Host src, Host dst);

    long getByteCountOnAffinityLink(AffinityLink al);

    double getBitRateOnAffinityLink(AffinityLink al);

    long getByteCountIntoPrefix(String ipAndMask);

    Map<Host, Long> getIncomingHosts(String ipAndMask);
}
