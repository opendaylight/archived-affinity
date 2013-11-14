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

    // Host pair statistics
    long getByteCountBetweenHosts(Host src, Host dst);
    long getByteCountBetweenHosts(Host src, Host dst, Byte protocol);
    double getBitRateBetweenHosts(Host src, Host dst);
    double getBitRateBetweenHosts(Host src, Host dst, Byte protocol);

    // AffinityLink statistics
    long getByteCountOnAffinityLink(AffinityLink al);
    long getByteCountOnAffinityLink(AffinityLink al, Byte protocol);
    double getBitRateOnAffinityLink(AffinityLink al);
    double getBitRateOnAffinityLink(AffinityLink al, Byte protocol);

    // Prefix statistics
    long getByteCountIntoPrefix(String prefixAndMask);
    long getByteCountIntoPrefix(String prefixAndMask, Byte protocol);

    // Miscellaneous
    Map<Host, Long> getIncomingHosts(String prefixAndMask);
    Map<Host, Long> getIncomingHosts(String prefixAndMask, Byte protocol);
}
