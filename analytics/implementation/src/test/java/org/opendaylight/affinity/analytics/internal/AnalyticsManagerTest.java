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

import org.opendaylight.affinity.affinity.AffinityGroup;
import org.opendaylight.affinity.affinity.AffinityLink;
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

    @Test public void testGetProtocols() {
        AnalyticsManager am = new AnalyticsManager();
        am.init();

        try {
            // Set up network
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(100L));
            Node n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(101L));
            NodeConnector nc1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFC), n1);
            NodeConnector nc2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFD), n2);
            HostNodeConnector hnc1 = new HostNodeConnector(new byte[]{0,0,0,0,0,1}, InetAddress.getByName("10.0.0.1"), nc1, (short) 1);
            HostNodeConnector hnc2 = new HostNodeConnector(new byte[]{0,0,0,0,0,2}, InetAddress.getByName("10.0.0.2"), nc2, (short) 1);
            Set<HostNodeConnector> allHosts = new HashSet<HostNodeConnector>(Arrays.asList(hnc1, hnc2));
            Set<Host> srcHosts = new HashSet<Host>(Arrays.asList(hnc1));
            Set<Host> dstHosts = new HashSet<Host>(Arrays.asList(hnc2));

            // Two flows between the nodes
            Match match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,2}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.UDP.byteValue()));
            Flow f = new Flow();
            f.setMatch(match);
            FlowOnNode fon = new FlowOnNode(f);
            fon.setByteCount(200);
            fon.setDurationSeconds(1);
            List<FlowOnNode> flowStatsList = new ArrayList<FlowOnNode>();
            flowStatsList.add(fon);

            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,2}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.TCP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(200);
            fon.setDurationSeconds(1);
            flowStatsList.add(fon);

            am.nodeFlowStatisticsUpdated(n1, flowStatsList, allHosts);
            Set<Byte> protocols = am.getProtocols(srcHosts, dstHosts);
            Assert.assertTrue(protocols.size() == 2);
            Assert.assertTrue(protocols.contains(IPProtocols.UDP.byteValue()));
            Assert.assertTrue(protocols.contains(IPProtocols.TCP.byteValue()));
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
    public void testStats() {
        AnalyticsManager am = new AnalyticsManager();
        am.init();
        try {
            // Set up the network
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(100L));
            Node n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(101L));
            Node n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            Node n4 = new Node(Node.NodeIDType.OPENFLOW, new Long(111L));
            Node n5 = new Node(Node.NodeIDType.OPENFLOW, new Long(011L));
            NodeConnector nc1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFC), n1);
            NodeConnector nc2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFD), n2);
            NodeConnector nc3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFE), n3);
            NodeConnector nc4 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFF), n4);
            NodeConnector nc5 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAEF), n5);
            HostNodeConnector hnc1 = new HostNodeConnector(new byte[]{0,0,0,0,0,1}, InetAddress.getByName("10.0.0.1"), nc1, (short) 1);
            HostNodeConnector hnc2 = new HostNodeConnector(new byte[]{0,0,0,0,0,2}, InetAddress.getByName("10.0.0.2"), nc2, (short) 1);
            HostNodeConnector hnc3 = new HostNodeConnector(new byte[]{0,0,0,0,0,3}, InetAddress.getByName("10.0.0.3"), nc3, (short) 1);
            HostNodeConnector hnc4 = new HostNodeConnector(new byte[]{0,0,0,0,0,4}, InetAddress.getByName("10.0.0.4"), nc4, (short) 1);
            HostNodeConnector hnc5 = new HostNodeConnector(new byte[]{0,0,0,0,0,5}, InetAddress.getByName("10.0.0.5"), nc5, (short) 1);

            Set<HostNodeConnector> allHosts = new HashSet<HostNodeConnector>(Arrays.asList(hnc1, hnc2, hnc3, hnc4, hnc5));
            List<FlowOnNode> flowStatsListn1 = new ArrayList<FlowOnNode>();
            List<FlowOnNode> flowStatsListn2 = new ArrayList<FlowOnNode>();

            // 10.0.0.1 -> 10.0.0.3: 200Bytes over 1.1sec, UDP
            Match match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,3}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.UDP.byteValue()));
            Flow f = new Flow();
            f.setMatch(match);
            FlowOnNode fon = new FlowOnNode(f);
            fon.setByteCount(200);
            fon.setPacketCount(6);
            fon.setDurationSeconds(1);
            fon.setDurationNanoseconds(100000000);
            flowStatsListn1.add(fon);

            // 10.0.0.1 -> 10.0.0.3: 64Bytes over 1sec, ICMP
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,3}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.ICMP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(64);
            fon.setPacketCount(5);
            fon.setDurationSeconds(1);
            fon.setDurationNanoseconds(0);
            flowStatsListn1.add(fon);

            // 10.0.0.1 -> 10.0.0.4: 76Bytes over 2sec, TCP
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,4}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.TCP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(76);
            fon.setPacketCount(4);
            fon.setDurationSeconds(2);
            fon.setDurationNanoseconds(0);
            flowStatsListn1.add(fon);

            // 10.0.0.2 -> 10.0.0.4: 300Bytes over 1.2sec, TCP
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc2));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,4}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.TCP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(300);
            fon.setPacketCount(3);
            fon.setDurationSeconds(1);
            fon.setDurationNanoseconds(200000000);
            flowStatsListn2.add(fon);

            // 10.0.0.1 -> 10.0.0.5: 27Bytes over 1sec, UDP
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,5}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.UDP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(27);
            fon.setPacketCount(4);
            fon.setDurationSeconds(1);
            fon.setDurationNanoseconds(0);
            flowStatsListn1.add(fon);

            // 10.0.0.2 -> 10.0.0.5: 234Bytes over 2sec, TCP
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc2));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,5}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.TCP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(234);
            fon.setPacketCount(2);
            fon.setDurationSeconds(2);
            fon.setDurationNanoseconds(0);
            flowStatsListn2.add(fon);

            // 10.0.0.2 -> 10.0.0.5: 54Bytes over 3.1sec, ICMP
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc2));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,5}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.ICMP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(54);
            fon.setPacketCount(1);
            fon.setDurationSeconds(3);
            fon.setDurationNanoseconds(100000000);
            flowStatsListn2.add(fon);

            am.nodeFlowStatisticsUpdated(n1, flowStatsListn1, allHosts);
            am.nodeFlowStatisticsUpdated(n2, flowStatsListn2, allHosts);

            // Host pairs - basic stats
            Assert.assertTrue(am.getByteCount(hnc1, hnc3) == 200 + 64);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc3) == 6 + 5);
            Assert.assertTrue(am.getDuration(hnc1, hnc3) == 1.1);
            Assert.assertTrue(am.getBitRate(hnc1, hnc3) == ((200 + 64) * 8.0) / 1.1);
            Assert.assertTrue(am.getByteCount(hnc1, hnc4) == 76);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc4) == 4);
            Assert.assertTrue(am.getDuration(hnc1, hnc4) == 2.0);
            Assert.assertTrue(am.getBitRate(hnc1, hnc4) == (76 * 8.0) / 2.0);
            Assert.assertTrue(am.getByteCount(hnc2, hnc4) == 300);
            Assert.assertTrue(am.getPacketCount(hnc2, hnc4) == 3);
            Assert.assertTrue(am.getDuration(hnc2, hnc4) == 1.2);
            Assert.assertTrue(am.getBitRate(hnc2, hnc4) == (300 * 8.0) / 1.2);
            Assert.assertTrue(am.getByteCount(hnc1, hnc5) == 27);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc5) == 4);
            Assert.assertTrue(am.getDuration(hnc1, hnc5) == 1.0);
            Assert.assertTrue(am.getBitRate(hnc1, hnc5) == (27 * 8.0) / 1.0);
            Assert.assertTrue(am.getByteCount(hnc2, hnc5) == 234 + 54);
            Assert.assertTrue(am.getPacketCount(hnc2, hnc5) == 2 + 1);
            Assert.assertTrue(am.getDuration(hnc2, hnc5) == 3.1);
            Assert.assertTrue(am.getBitRate(hnc2, hnc5) == ((234 + 54) * 8.0) / 3.1);

            // Host pairs - per-protocol stats
            Assert.assertTrue(am.getByteCount(hnc1, hnc3, IPProtocols.ICMP.byteValue()) == 64);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc3, IPProtocols.ICMP.byteValue()) == 5);
            Assert.assertTrue(am.getDuration(hnc1, hnc3, IPProtocols.ICMP.byteValue()) == 1.0);
            Assert.assertTrue(am.getBitRate(hnc1, hnc3, IPProtocols.ICMP.byteValue()) == (64 * 8.0) / 1.0);
            Assert.assertTrue(am.getByteCount(hnc1, hnc3, IPProtocols.UDP.byteValue()) == 200);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc3, IPProtocols.UDP.byteValue()) == 6);
            Assert.assertTrue(am.getDuration(hnc1, hnc3, IPProtocols.UDP.byteValue()) == 1.1);
            Assert.assertTrue(am.getBitRate(hnc1, hnc3, IPProtocols.UDP.byteValue()) == (200 * 8.0) / 1.1);
            Assert.assertTrue(am.getByteCount(hnc1, hnc3, IPProtocols.TCP.byteValue()) == 0);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc3, IPProtocols.TCP.byteValue()) == 0);
            Assert.assertTrue(am.getDuration(hnc1, hnc3, IPProtocols.TCP.byteValue()) == 0.0);
            Assert.assertTrue(am.getBitRate(hnc1, hnc3, IPProtocols.TCP.byteValue()) == 0.0);
            Assert.assertTrue(am.getByteCount(hnc1, hnc4, IPProtocols.TCP.byteValue()) == 76);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc4, IPProtocols.TCP.byteValue()) == 4);
            Assert.assertTrue(am.getDuration(hnc1, hnc4, IPProtocols.TCP.byteValue()) == 2.0);
            Assert.assertTrue(am.getBitRate(hnc1, hnc4, IPProtocols.TCP.byteValue()) == (76 * 8.0) / 2.0);
            Assert.assertTrue(am.getByteCount(hnc2, hnc4, IPProtocols.TCP.byteValue()) == 300);
            Assert.assertTrue(am.getPacketCount(hnc2, hnc4, IPProtocols.TCP.byteValue()) == 3);
            Assert.assertTrue(am.getDuration(hnc2, hnc4, IPProtocols.TCP.byteValue()) == 1.2);
            Assert.assertTrue(am.getBitRate(hnc2, hnc4, IPProtocols.TCP.byteValue()) == (300 * 8.0) / 1.2);
            Assert.assertTrue(am.getByteCount(hnc1, hnc5, IPProtocols.UDP.byteValue()) == 27);
            Assert.assertTrue(am.getPacketCount(hnc1, hnc5, IPProtocols.UDP.byteValue()) == 4);
            Assert.assertTrue(am.getDuration(hnc1, hnc5, IPProtocols.UDP.byteValue()) == 1.0);
            Assert.assertTrue(am.getBitRate(hnc1, hnc5, IPProtocols.UDP.byteValue()) == (27 * 8.0) / 1.0);
            Assert.assertTrue(am.getByteCount(hnc2, hnc5, IPProtocols.ICMP.byteValue()) == 54);
            Assert.assertTrue(am.getPacketCount(hnc2, hnc5, IPProtocols.ICMP.byteValue()) == 1);
            Assert.assertTrue(am.getDuration(hnc2, hnc5, IPProtocols.ICMP.byteValue()) == 3.1);
            Assert.assertTrue(am.getBitRate(hnc2, hnc5, IPProtocols.ICMP.byteValue()) == (54 * 8.0) / 3.1);
            Assert.assertTrue(am.getByteCount(hnc2, hnc5, IPProtocols.TCP.byteValue()) == 234);
            Assert.assertTrue(am.getPacketCount(hnc2, hnc5, IPProtocols.TCP.byteValue()) == 2);
            Assert.assertTrue(am.getDuration(hnc2, hnc5, IPProtocols.TCP.byteValue()) == 2.0);
            Assert.assertTrue(am.getBitRate(hnc2, hnc5, IPProtocols.TCP.byteValue()) == (234 * 8.0) / 2.0);

            // Host pairs - all stats
            Set<Byte> allProtocols = new HashSet<Byte>(Arrays.asList(IPProtocols.ICMP.byteValue(), IPProtocols.UDP.byteValue(), IPProtocols.TCP.byteValue()));
            for (Host h1 : allHosts) {
                for (Host h2 : allHosts) {
                    Map<Byte, Long> byteCounts = am.getAllByteCounts(h1, h2);
                    Map<Byte, Long> packetCounts = am.getAllPacketCounts(h1, h2);
                    Map<Byte, Double> durations = am.getAllDurations(h1, h2);
                    Map<Byte, Double> bitRates = am.getAllBitRates(h1, h2);
                    for (Byte protocol : allProtocols) {
                        if (byteCounts.get(protocol) == null) {
                            Assert.assertTrue(am.getByteCount(h1, h2, protocol) == 0);
                            Assert.assertTrue(am.getPacketCount(h1, h2, protocol) == 0);
                            Assert.assertTrue(am.getDuration(h1, h2, protocol) == 0.0);
                            Assert.assertTrue(am.getBitRate(h1, h2, protocol) == 0.0);
                        }
                        else {
                            Assert.assertTrue(byteCounts.get(protocol) == am.getByteCount(h1, h2, protocol));
                            Assert.assertTrue(packetCounts.get(protocol) == am.getPacketCount(h1, h2, protocol));
                            Assert.assertTrue(durations.get(protocol) == am.getDuration(h1, h2, protocol));
                            Assert.assertTrue(bitRates.get(protocol) == am.getBitRate(h1, h2, protocol));
                        }
                    }
                }
            }

            // Host groups - basic stats
            Set<Host> srcHosts = new HashSet<Host>(Arrays.asList(hnc1, hnc2));
            Set<Host> dstHosts = new HashSet<Host>(Arrays.asList(hnc3, hnc4));
            Assert.assertTrue(am.getByteCount(srcHosts, dstHosts, null) == 64 + 200 + 76 + 300);
            Assert.assertTrue(am.getPacketCount(srcHosts, dstHosts, null) == 6 + 5 + 4 + 3);
            Assert.assertTrue(am.getDuration(srcHosts, dstHosts, null) == 2.0);
            Assert.assertTrue(am.getBitRate(srcHosts, dstHosts, null) == ((64 + 200 + 76 + 300) * 8.0) / 2.0);

            // Host groups - per-protocol stats
            Assert.assertTrue(am.getByteCount(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()) == 64);
            Assert.assertTrue(am.getPacketCount(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()) == 5);
            Assert.assertTrue(am.getDuration(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()) == 1.0);
            Assert.assertTrue(am.getBitRate(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()) == (64 * 8.0) / 1.0);
            Assert.assertTrue(am.getByteCount(srcHosts, dstHosts, IPProtocols.TCP.byteValue()) == 76 + 300);
            Assert.assertTrue(am.getPacketCount(srcHosts, dstHosts, IPProtocols.TCP.byteValue()) == 4 + 3);
            Assert.assertTrue(am.getDuration(srcHosts, dstHosts, IPProtocols.TCP.byteValue()) == 2.0);
            Assert.assertTrue(am.getBitRate(srcHosts, dstHosts, IPProtocols.TCP.byteValue()) == ((76 + 300) * 8.0) / 2.0);
            Assert.assertTrue(am.getByteCount(srcHosts, dstHosts, IPProtocols.UDP.byteValue()) == 200);
            Assert.assertTrue(am.getPacketCount(srcHosts, dstHosts, IPProtocols.UDP.byteValue()) == 6);
            Assert.assertTrue(am.getDuration(srcHosts, dstHosts, IPProtocols.UDP.byteValue()) == 1.1);
            Assert.assertTrue(am.getBitRate(srcHosts, dstHosts, IPProtocols.UDP.byteValue()) == (200 * 8.0) / 1.1);

            // Host groups - all stats
            Map<Byte, Long> byteCounts = am.getAllByteCounts(srcHosts, dstHosts);
            Map<Byte, Long> packetCounts = am.getAllPacketCounts(srcHosts, dstHosts);
            Map<Byte, Double> durations = am.getAllDurations(srcHosts, dstHosts);
            Map<Byte, Double> bitRates = am.getAllBitRates(srcHosts, dstHosts);
            Assert.assertTrue(byteCounts.get(IPProtocols.ICMP.byteValue()) == am.getByteCount(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()));
            Assert.assertTrue(byteCounts.get(IPProtocols.UDP.byteValue()) == am.getByteCount(srcHosts, dstHosts, IPProtocols.UDP.byteValue()));
            Assert.assertTrue(byteCounts.get(IPProtocols.TCP.byteValue()) == am.getByteCount(srcHosts, dstHosts, IPProtocols.TCP.byteValue()));
            Assert.assertTrue(byteCounts.get(IPProtocols.ANY.byteValue()) == null);
            Assert.assertTrue(packetCounts.get(IPProtocols.ICMP.byteValue()) == am.getPacketCount(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()));
            Assert.assertTrue(packetCounts.get(IPProtocols.UDP.byteValue()) == am.getPacketCount(srcHosts, dstHosts, IPProtocols.UDP.byteValue()));
            Assert.assertTrue(packetCounts.get(IPProtocols.TCP.byteValue()) == am.getPacketCount(srcHosts, dstHosts, IPProtocols.TCP.byteValue()));
            Assert.assertTrue(packetCounts.get(IPProtocols.ANY.byteValue()) == null);
            Assert.assertTrue(durations.get(IPProtocols.ICMP.byteValue()) == am.getDuration(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()));
            Assert.assertTrue(durations.get(IPProtocols.UDP.byteValue()) == am.getDuration(srcHosts, dstHosts, IPProtocols.UDP.byteValue()));
            Assert.assertTrue(durations.get(IPProtocols.TCP.byteValue()) == am.getDuration(srcHosts, dstHosts, IPProtocols.TCP.byteValue()));
            Assert.assertTrue(durations.get(IPProtocols.ANY.byteValue()) == null);
            Assert.assertTrue(bitRates.get(IPProtocols.ICMP.byteValue()) == am.getBitRate(srcHosts, dstHosts, IPProtocols.ICMP.byteValue()));
            Assert.assertTrue(bitRates.get(IPProtocols.UDP.byteValue()) == am.getBitRate(srcHosts, dstHosts, IPProtocols.UDP.byteValue()));
            Assert.assertTrue(bitRates.get(IPProtocols.TCP.byteValue()) == am.getBitRate(srcHosts, dstHosts, IPProtocols.TCP.byteValue()));
            Assert.assertTrue(bitRates.get(IPProtocols.ANY.byteValue()) == null);

            // Correct flow over-writing
            fon = new FlowOnNode(f); // 10.0.0.2 -> 10.0.0.5, ICMP
            fon.setByteCount(300);
            fon.setPacketCount(7);
            fon.setDurationSeconds(4);
            fon.setDurationNanoseconds(100000000);
            flowStatsListn2.add(fon);
            am.nodeFlowStatisticsUpdated(n2, flowStatsListn2, allHosts);
            Assert.assertTrue(am.getByteCount(hnc2, hnc5) == 300 + 234);
            Assert.assertTrue(am.getPacketCount(hnc2, hnc5) == 2 + 7);
            Assert.assertTrue(am.getBitRate(hnc2, hnc5) == ((300 + 234) * 8.0) / 4.1);
            Assert.assertTrue(am.getByteCount(hnc2, hnc5, IPProtocols.ICMP.byteValue()) == 300);
            Assert.assertTrue(am.getPacketCount(hnc2, hnc5, IPProtocols.ICMP.byteValue()) == 7);
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        } catch (UnknownHostException e) {
            Assert.assertTrue(false);
        } finally {
            am.destroy();
        }
    }

    @Test
    public void testGetIncomingHostByteCounts() {
        AnalyticsManager am = new AnalyticsManager();
        am.init();
        try {
            // Set up the network
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(100L));
            Node n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(101L));
            Node n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            Node n4 = new Node(Node.NodeIDType.OPENFLOW, new Long(111L));
            Node n5 = new Node(Node.NodeIDType.OPENFLOW, new Long(011L));
            NodeConnector nc1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFC), n1);
            NodeConnector nc2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFD), n2);
            NodeConnector nc3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFE), n3);
            NodeConnector nc4 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAFF), n4);
            NodeConnector nc5 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, new Short((short) 0xCAEF), n5);
            HostNodeConnector hnc1 = new HostNodeConnector(new byte[]{0,0,0,0,0,1}, InetAddress.getByName("128.0.0.1"), nc1, (short) 1);
            HostNodeConnector hnc2 = new HostNodeConnector(new byte[]{0,0,0,0,0,2}, InetAddress.getByName("128.0.0.2"), nc2, (short) 1);
            HostNodeConnector hnc3 = new HostNodeConnector(new byte[]{0,0,0,0,0,3}, InetAddress.getByName("128.0.0.3"), nc3, (short) 1);
            HostNodeConnector hnc4 = new HostNodeConnector(new byte[]{0,0,0,0,0,4}, InetAddress.getByName("129.0.0.1"), nc4, (short) 1);
            HostNodeConnector hnc5 = new HostNodeConnector(new byte[]{0,0,0,0,0,5}, InetAddress.getByName("129.0.0.2"), nc5, (short) 1);
            List<FlowOnNode> flowStatsList = new ArrayList<FlowOnNode>();
            Set<HostNodeConnector> allHosts = new HashSet<HostNodeConnector>(Arrays.asList(hnc1, hnc2, hnc3, hnc4, hnc5));

            // 128.0.0.1 -> 129.0.0.1
            Match match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc1));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,4}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.TCP.byteValue()));
            Flow f = new Flow();
            f.setMatch(match);
            FlowOnNode fon = new FlowOnNode(f);
            fon.setByteCount(100);
            flowStatsList.add(fon);
            am.nodeFlowStatisticsUpdated(n1, flowStatsList, allHosts);

            // 128.0.0.2 -> 129.0.0.1
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc2));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,5}));
            match.setField(new MatchField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue()));
            match.setField(new MatchField(MatchType.NW_PROTO, IPProtocols.UDP.byteValue()));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(200);
            flowStatsList = new ArrayList<FlowOnNode>();
            flowStatsList.add(fon);
            am.nodeFlowStatisticsUpdated(n2, flowStatsList, allHosts);

            // 128.0.0.3 -> 128.0.0.1
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc3));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,1}));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(300);
            flowStatsList = new ArrayList<FlowOnNode>();
            flowStatsList.add(fon);
            am.nodeFlowStatisticsUpdated(n3, flowStatsList, allHosts);

            // 129.0.0.2 -> 129.0.0.1
            match = new Match();
            match.setField(new MatchField(MatchType.IN_PORT, nc5));
            match.setField(new MatchField(MatchType.DL_DST, new byte[]{0,0,0,0,0,4}));
            f = new Flow();
            f.setMatch(match);
            fon = new FlowOnNode(f);
            fon.setByteCount(400);
            flowStatsList = new ArrayList<FlowOnNode>();
            flowStatsList.add(fon);
            am.nodeFlowStatisticsUpdated(n5, flowStatsList, allHosts);

            Map<Host, Long> hostCounts = am.getIncomingHostByteCounts("129.0.0.0/8", null, allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 2);
            Assert.assertTrue(hostCounts.get(hnc1) == 100);
            Assert.assertTrue(hostCounts.get(hnc2) == 200);
            hostCounts = am.getIncomingHostByteCounts("129.0.0.0/8", IPProtocols.TCP.byteValue(), allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 1);
            Assert.assertTrue(hostCounts.get(hnc1) == 100);
            hostCounts = am.getIncomingHostByteCounts("129.0.0.0/8", IPProtocols.UDP.byteValue(), allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 1);
            Assert.assertTrue(hostCounts.get(hnc2) == 200);
            hostCounts = am.getIncomingHostByteCounts("129.0.0.0/8", IPProtocols.ICMP.byteValue(), allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 0);
            hostCounts = am.getIncomingHostByteCounts("129.0.0.1/32", null, allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 2);
            Assert.assertTrue(hostCounts.get(hnc1) == 100);
            Assert.assertTrue(hostCounts.get(hnc5) == 400);
            hostCounts = am.getIncomingHostByteCounts("128.0.0.0/8", null, allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 0);
            hostCounts = am.getIncomingHostByteCounts("128.0.0.1/31", null, allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 1);
            Assert.assertTrue(hostCounts.get(hnc3) == 300);
            hostCounts = am.getIncomingHostByteCounts("128.0.0.2/32", null, allHosts);
            Assert.assertTrue(hostCounts.keySet().size() == 0);
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        } catch (UnknownHostException e) {
            Assert.assertTrue(false);
        } finally {
            am.destroy();
        }
    }
}
