/*
 * Copyright (c) 2013 Plexxi, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics;

import java.util.Map;
import java.util.Set;

import org.opendaylight.affinity.affinity.AffinityLink;
import org.opendaylight.controller.sal.core.Host;

public interface IAnalyticsManager {

    // Host pair statistics
    long getByteCount(Host src, Host dst);
    long getByteCount(Host src, Host dst, Byte protocol);
    double getDuration(Host src, Host dst);
    double getDuration(Host src, Host dst, Byte protocol);
    double getBitRate(Host src, Host dst);
    double getBitRate(Host src, Host dst, Byte protocol);
    Map<Byte, Long> getAllByteCounts(Host src, Host dst);
    Map<Byte, Double> getAllBitRates(Host src, Host dst);

    // AffinityLink statistics
    long getByteCount(AffinityLink al);
    long getByteCount(AffinityLink al, Byte protocol);
    double getDuration(AffinityLink al);
    double getDuration(AffinityLink al, Byte protocol);
    double getBitRate(AffinityLink al);
    double getBitRate(AffinityLink al, Byte protocol);
    Map<Byte, Long> getAllByteCounts(AffinityLink al);
    Map<Byte, Double> getAllBitRates(AffinityLink al);

    // Subnet statistics
    long getByteCount(String srcSubnet, String dstSubnet);
    long getByteCount(String srcSubnet, String dstSubnet, Byte protocol);
    double getDuration(String srcSubnet, String dstSubnet);
    double getDuration(String srcSubnet, String dstSubnet, Byte protocol);
    double getBitRate(String srcSubnet, String dstSubnet);
    double getBitRate(String srcSubnet, String dstSubnet, Byte protocol);
    Map<Byte, Long> getAllByteCounts(String srcSubnet, String dstSubnet);
    Map<Byte, Double> getAllBitRates(String srcSubnet, String dstSubnet);

    // Miscellaneous
    Map<Host, Long> getIncomingHostByteCounts(String subnet);
    Map<Host, Long> getIncomingHostByteCounts(String subnet, Byte protocol);
}
