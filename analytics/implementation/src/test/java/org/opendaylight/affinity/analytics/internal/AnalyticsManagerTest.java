/*
 * Copyright (c) 2013 Plexxi, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.affinity.analytics.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;

public class AnalyticsManagerTest extends TestCase {

    @Test
    public void testAnalyticsManagerCreation() {
        AnalyticsManager am = new AnalyticsManager();
        Assert.assertTrue(am != null);
    }

    @Test
    public void testGetHostsFromFlows() {

        AnalyticsManager am = new AnalyticsManager();
        am.init();

        try {
            // Set up nodes
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(100L));
            Node n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(101L));
            Node n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            Node n4 = new Node(Node.NodeIDType.OPENFLOW, new Long(111L));

            // Set up node connectors
            NodeConnector nc1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFC), n1);
            NodeConnector nc2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFD), n2);
            NodeConnector nc3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFE), n3);
            NodeConnector nc4 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFF), n4);

            // Set up host node connectors
            HostNodeConnector hnc1 = new HostNodeConnector(new byte[]{0,0,0,0,0,1}, InetAddress.getByName("10.0.0.1"), nc1, (short) 1);
            HostNodeConnector hnc2 = new HostNodeConnector(new byte[]{0,0,0,0,0,2}, InetAddress.getByName("10.0.0.2"), nc2, (short) 1);
            HostNodeConnector hnc3 = new HostNodeConnector(new byte[]{0,0,0,0,0,3}, InetAddress.getByName("10.0.0.3"), nc3, (short) 1);
            Set<HostNodeConnector> hosts = new HashSet<HostNodeConnector>(Arrays.asList(hnc1, hnc2, hnc3));

            // Set up a flow from nc1 to nc2
            Match match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,2}));
            Flow f = new Flow();
            f.setMatch(match);

            Host dstHost = am.getDestinationHostFromFlow(f, hosts);
            Host srcHost = am.getSourceHostFromFlow(f, hosts);

            Assert.assertTrue(dstHost.equals(hnc2));
            Assert.assertTrue(srcHost.equals(hnc1));

            // Set up a flow from nc3 to nc1
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc3));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,1}));
            f = new Flow();
            f.setMatch(match);

            dstHost = am.getDestinationHostFromFlow(f, hosts);
            srcHost = am.getSourceHostFromFlow(f, hosts);

            Assert.assertTrue(dstHost.equals(hnc1));
            Assert.assertTrue(srcHost.equals(hnc3));

            // Set up a flow from a switch to a non-host..
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc4));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,2}));
            f = new Flow();
            f.setMatch(match);

            dstHost = am.getDestinationHostFromFlow(f, hosts);
            srcHost = am.getSourceHostFromFlow(f, hosts);

            Assert.assertTrue(dstHost.equals(hnc2));
            Assert.assertTrue(srcHost == null);
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        } catch (UnknownHostException e ) {
            Assert.assertTrue(false);
        } finally {
            am.destroy();
        }
    }

    @Test
    public void testGetByteCountBetweenHosts() {
        // TODO: This test should exist, but it involves a lot of
        // integration with the statisticsManager, switchManager, and
        // hostTracker, and I'm not entirely sure how to go about that.
        Assert.assertTrue(true);
    }
}
