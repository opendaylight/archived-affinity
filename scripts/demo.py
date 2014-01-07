#!/usr/local/bin/python

'''
Copyright (c) 2013 Plexxi, Inc.  All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html
'''

import httplib2
import json
import signal
import time

from threading import Thread

from affinity_control import AffinityControl
from subnet import SubnetControl
from stats import Stats

import analytics

'''
The instructions for running this demo are located at:
https://wiki.opendaylight.org/view/Project_Proposals:Affinity_Metadata_Service#Current_Status

Briefly:
1.  In config.ini, make sure affinity jars are loaded and that of.flowStatsPollInterval = 1
2.  Start the controller
3.  Start mininet with a 2-level tree topology
4.  In mininet:
     > pingall
     > h3 ping h1
You will see an anomaly detected in demo.py's output, and the pings between h3 and h1 will halt.
'''

# If True, SIG_INT has been captured
global sigint
sigint = False

# Handle SIG_INT
def signal_handler(signal, frame):
    global sigint
    sigint = True

# Monitors statistics
class WaypointMonitor(Thread):

    def __init__(self, monitor_type, **kwargs):
        Thread.__init__(self)
        self.stat = Stats(monitor_type, **kwargs)
        self.stat_type = monitor_type
        self.waypoint_address = None
        print "Created waypoint monitor for %s" % self.stat

    def set_waypoint(self, waypoint_ip):
        self.waypoint_address = waypoint_ip
        print "Registered waypoint for %s.  Any large flows will be redirected to %s." % (self.stat, waypoint_ip)

    def set_large_flow_threshold(self, s):
        self.stat.set_large_flow_threshold(s)
        print "Set threshold for large flows to %d bytes" % s
        print("-------------------------")

    def run(self):
        global sigint
        did_waypoint = False
        while not sigint:
            _, is_big = self.stat.refresh()
            if is_big and not did_waypoint:
                print "Large flow detected (%d bytes, %d packets, %3.3f bit/s)" % (self.stat.get_bytes(), self.stat.get_packets(), self.stat.get_bit_rate())
                print "   ICMP: %d bytes, %d packets" % (self.stat.get_bytes(1), self.stat.get_packets(1))
                print "   UDP: %d bytes, %d packets" % (self.stat.get_bytes(17), self.stat.get_packets(17))
                print "   TCP: %d bytes, %d packets" % (self.stat.get_bytes(6), self.stat.get_packets(6))
                print "   other: %d bytes, %d packets" % (self.stat.get_bytes(-1), self.stat.get_packets(-1))
                print("-------------------------")
                ac = AffinityControl()
                # First AG: Sources sending data into this subnet
                src_ag_name = "sources"
                src_ips = self.stat.get_large_incoming_hosts()
                if (self.waypoint_address in src_ips):
                    src_ips.remove(self.waypoint_address)
                ac.add_affinity_group(src_ag_name, ips=src_ips)
                # Second AG: This entity
                dst_ag_name = "client"
                if (self.stat_type == Stats.TYPE_SUBNET):
                    ac.add_affinity_group(dst_ag_name, subnet=self.stat.subnet)
                elif (self.stat_type == Stats.TYPE_HOST):
                    pass
                else:
                    print "type", self.stat_type, "not supported for redirection"
                # AL: Between them
                link_name = "inflows"
                ac.add_affinity_link(link_name, src_ag_name, dst_ag_name)
                ac.add_waypoint(link_name, self.waypoint_address)
#                ac.enable_waypoint(link_name)
                ac.enable_affinity()
                did_waypoint = True
                raw_input("[Press Enter to disable affinity rules] ")
                ac.disable_affinity()
#                ac.disable_waypoint(link_name)
            time.sleep(1)

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    raw_input("[Press enter when mininet is ready] ")
    print("-------------------------")

    # Add per-protocol flows so we can monitor stats that way
    x = analytics.add_protocol_flows()
    if (not x):
        print "Unable to add per-protocol flows"

    m = WaypointMonitor(Stats.TYPE_SUBNET, subnet="10.0.0.0/31")
    m.set_waypoint("10.0.0.2")
    m.set_large_flow_threshold(500) # 2000 bytes
    m.start()

    # Register signal-handler to catch SIG_INT
    signal.signal(signal.SIGINT, signal_handler)
    signal.pause()

    # join() won't return until SIG_INT has been captured
    m.join()

if __name__ == "__main__":
    main()
