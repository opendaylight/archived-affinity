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
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;

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
    public void testSubnetMatching() {

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
            HostNodeConnector hnc1 = new HostNodeConnector(InetAddress.getByName("128.0.0.1"), nc1);
            HostNodeConnector hnc2 = new HostNodeConnector(InetAddress.getByName("128.0.0.2"), nc2);
            HostNodeConnector hnc3 = new HostNodeConnector(InetAddress.getByName("129.0.0.1"), nc3);
            HostNodeConnector hnc4 = new HostNodeConnector(InetAddress.getByName("129.0.0.2"), nc4);
            Set<HostNodeConnector> allHosts = new HashSet<HostNodeConnector>(Arrays.asList(hnc1, hnc2, hnc3, hnc4));

            String subnet1 = "128.0.0.0/8"; // matches 128.*
            Set<Host> matchedHosts = am.getHostsInSubnet(subnet1, allHosts);
            Set<Host> unmatchedHosts = am.getHostsNotInSubnet(subnet1, allHosts);
            Assert.assertTrue(matchedHosts.size() == 2);
            Assert.assertTrue(matchedHosts.contains(hnc1));
            Assert.assertTrue(matchedHosts.contains(hnc2));
            Assert.assertTrue(unmatchedHosts.size() == 2);
            Assert.assertTrue(unmatchedHosts.contains(hnc3));
            Assert.assertTrue(unmatchedHosts.contains(hnc4));

            String subnet2 = "128.0.0.0/7"; // matches 128.* and 129.*
            matchedHosts = am.getHostsInSubnet(subnet2, allHosts);
            unmatchedHosts = am.getHostsNotInSubnet(subnet2, allHosts);
            Assert.assertTrue(matchedHosts.size() == 4);
            Assert.assertTrue(matchedHosts.contains(hnc1));
            Assert.assertTrue(matchedHosts.contains(hnc2));
            Assert.assertTrue(matchedHosts.contains(hnc3));
            Assert.assertTrue(matchedHosts.contains(hnc4));
            Assert.assertTrue(unmatchedHosts.size() == 0);
            
            String subnet3 = "128.0.0.2/32"; // matches 128.0.0.2
            matchedHosts = am.getHostsInSubnet(subnet3, allHosts);
            unmatchedHosts = am.getHostsNotInSubnet(subnet3, allHosts);
            Assert.assertTrue(matchedHosts.size() == 1);
            Assert.assertTrue(matchedHosts.contains(hnc2));
            Assert.assertTrue(unmatchedHosts.size() == 3);
            Assert.assertTrue(unmatchedHosts.contains(hnc1));
            Assert.assertTrue(unmatchedHosts.contains(hnc3));
            Assert.assertTrue(unmatchedHosts.contains(hnc4)); 
           
            String subnet4 = "128.0.0.1/31"; // matches 128.0.0.1
            matchedHosts = am.getHostsInSubnet(subnet4, allHosts);
            unmatchedHosts = am.getHostsNotInSubnet(subnet4, allHosts);
            Assert.assertTrue(matchedHosts.size() == 1);
            Assert.assertTrue(matchedHosts.contains(hnc1));
            Assert.assertTrue(unmatchedHosts.size() == 3);
            Assert.assertTrue(unmatchedHosts.contains(hnc2));
            Assert.assertTrue(unmatchedHosts.contains(hnc3));
            Assert.assertTrue(unmatchedHosts.contains(hnc4));

            String subnet5 = "10.0.0.0/8"; // matches none
            matchedHosts = am.getHostsInSubnet(subnet5, allHosts);
            unmatchedHosts = am.getHostsNotInSubnet(subnet5, allHosts);
            Assert.assertTrue(matchedHosts.size() == 0);
            Assert.assertTrue(unmatchedHosts.size() == 4);
            Assert.assertTrue(unmatchedHosts.contains(hnc1));
            Assert.assertTrue(unmatchedHosts.contains(hnc2));
            Assert.assertTrue(unmatchedHosts.contains(hnc3));
            Assert.assertTrue(unmatchedHosts.contains(hnc4));
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        } catch (UnknownHostException e ) {
            Assert.assertTrue(false);
        } finally {
            am.destroy();
        }
    }

    @Test
    public void testGetStatsBetweenHosts() {
        AnalyticsManager am = new AnalyticsManager();
        am.init();
        try {
            // Set up the network
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(100L));
            Node n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(101L));
            NodeConnector nc1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFC), n1);
            NodeConnector nc2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFD), n2);
            HostNodeConnector hnc1 = new HostNodeConnector(new byte[]{0,0,0,0,0,1}, InetAddress.getByName("10.0.0.1"), nc1, (short) 1);
            HostNodeConnector hnc2 = new HostNodeConnector(new byte[]{0,0,0,0,0,2}, InetAddress.getByName("10.0.0.2"), nc2, (short) 1);
            Set<HostNodeConnector> allHosts = new HashSet<HostNodeConnector>(Arrays.asList(hnc1, hnc2));

            // Two flows between the hosts; different protocols
            Match match1 = new Match();
            match1.setField(new MatchField(MatchType.IN_PORT, nc1));
            match1.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,2}));
            match1.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match1.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.ICMP.byteValue()));
            Flow f1 = new Flow();
            f1.setMatch(match1);
            FlowOnNode fon1 = new FlowOnNode(f1);
            fon1.setByteCount(200);
            fon1.setDurationSeconds(1);
            fon1.setDurationNanoseconds(100000000); // 1.1s
            List<FlowOnNode> flowStatsList = new ArrayList<FlowOnNode>();
            flowStatsList.add(fon1);

            Match match2 = new Match();
            match2.setField(new MatchField(MatchType.IN_PORT, nc1));
            match2.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,2}));
            match2.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match2.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.UDP.byteValue()));
            Flow f2 = new Flow();
            f2.setMatch(match2);
            FlowOnNode fon2 = new FlowOnNode(f2);
            fon2.setByteCount(76);
            fon2.setDurationSeconds(2);
            fon2.setDurationNanoseconds(0);
            flowStatsList.add(fon2);

            // Basic stats
            am.nodeFlowStatisticsUpdated(n1, flowStatsList, allHosts);
            Assert.assertTrue(am.getByteCount(hnc1, hnc2) == 276);
            Assert.assertTrue(am.getBitRate(hnc1, hnc2) == (276 * 8.0) / 2.0);

            // Per-protocol stats
            Assert.assertTrue(am.getByteCount(hnc1, hnc2, IPProtocols.ICMP.byteValue()) == 200);
            Assert.assertTrue(am.getBitRate(hnc1, hnc2, IPProtocols.ICMP.byteValue()) == (200 * 8.0) / 1.1);
            Assert.assertTrue(am.getByteCount(hnc1, hnc2, IPProtocols.UDP.byteValue()) == 76);
            Assert.assertTrue(am.getBitRate(hnc1, hnc2, IPProtocols.UDP.byteValue()) == (76 * 8.0) / 2.0);
            Assert.assertTrue(am.getByteCount(hnc1, hnc2, IPProtocols.TCP.byteValue()) == 0);
            Assert.assertTrue(am.getBitRate(hnc1, hnc2, IPProtocols.TCP.byteValue()) == 0.0);

            // All stats
            Map<Byte, Long> byteCounts = am.getAllByteCounts(hnc1, hnc2);
            Map<Byte, Double> bitRates = am.getAllBitRates(hnc1, hnc2);
            Assert.assertTrue(byteCounts.get(IPProtocols.ICMP.byteValue()) == am.getByteCount(hnc1, hnc2, IPProtocols.ICMP.byteValue()));
            Assert.assertTrue(bitRates.get(IPProtocols.ICMP.byteValue()) == am.getBitRate(hnc1, hnc2, IPProtocols.ICMP.byteValue()));
            Assert.assertTrue(byteCounts.get(IPProtocols.UDP.byteValue()) == am.getByteCount(hnc1, hnc2, IPProtocols.UDP.byteValue()));
            Assert.assertTrue(bitRates.get(IPProtocols.UDP.byteValue()) == am.getBitRate(hnc1, hnc2, IPProtocols.UDP.byteValue()));
            Assert.assertTrue(byteCounts.get(IPProtocols.TCP.byteValue()) == null);
            Assert.assertTrue(bitRates.get(IPProtocols.TCP.byteValue()) == null);

            // Correct flow over-writing
            FlowOnNode fon3 = new FlowOnNode(f1);
            fon3.setByteCount(300);
            fon3.setDurationSeconds(3);
            fon3.setDurationNanoseconds(100000000); // 3.1s
            flowStatsList.add(fon3);
            am.nodeFlowStatisticsUpdated(n2, flowStatsList, allHosts);
            Assert.assertTrue(am.getByteCount(hnc1, hnc2) == 376);
            Assert.assertTrue(am.getBitRate(hnc1, hnc2) == (376 * 8.0) / 3.1);
            Assert.assertTrue(am.getByteCount(hnc1, hnc2, IPProtocols.ICMP.byteValue()) == 300);
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        } catch (UnknownHostException e) {
            Assert.assertTrue(false);
        } finally {
            am.destroy();
        }
    }
}
