#!/usr/local/bin/python

import httplib2
import json
import signal
import time

from threading import Thread

from affinity_control import AffinityControl
from subnet import SubnetControl
from stats import Stats

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

    def run(self):
        global sigint
        did_waypoint = False
        while not sigint:
            _, is_big = self.stat.refresh()
            if is_big and not did_waypoint:
                print "Large flow detected (%d bytes)" % self.stat.get_bytes()
                ac = AffinityControl()
                # First AG: Sources sending data into this subnet
                src_ag_name = "sources"
                src_ips = self.stat.get_large_incoming_hosts()
                if (self.waypoint_address in src_ips):
                    src_ips.remove(self.waypoint_address)
                ac.add_affinity_group(src_ag_name, ips=src_ips)
                # Second AG: This entity
                dst_ag_name = "client"
                if (self.stat_type == Stats.TYPE_PREFIX):
                    ac.add_affinity_group(dst_ag_name, subnet=self.stat.subnet)
                elif (self.stat_type == Stats.TYPE_HOST):
                    pass
                else:
                    print "type", self.stat_type, "not supported for redirection"
                # AL: Between them
                link_name = "inflows"
                ac.add_affinity_link(link_name, src_ag_name, dst_ag_name)
                ac.add_waypoint(link_name, self.waypoint_address)
                ac.enable_waypoint(link_name)
                did_waypoint = True
                raw_input("[Press Enter to disable waypoint redirection] ")
                ac.disable_waypoint(link_name)
            time.sleep(1)

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    m = WaypointMonitor(Stats.TYPE_PREFIX, subnet="10.0.0.0/31")
    m.set_waypoint("10.0.0.2")
    m.set_large_flow_threshold(2000) # 2000 bytes
    m.start()

    # Register signal-handler to catch SIG_INT
    signal.signal(signal.SIGINT, signal_handler)
    signal.pause()

    # join() won't return until SIG_INT has been captured
    m.join()

if __name__ == "__main__":
    main()
