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

global link_name
global rate_low
global rate_high
global prefix

link_name = "inflows"
rate_high = (100 * 10**6)  # 100 mbps
rate_low = (10 * 10**6) # 10 mbps
prefix = "10.0.0.254/8"

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

    def set_high_threshold(self, s):
        self.stat.set_high_threshold(s)
        print "Set high threshold for flow rate to %3.3f Mbps" % (int(s) * (1.0/1000000))
        print("-------------------------")

    def set_low_threshold(self, s):
        self.stat.set_low_threshold(s)
        print "Set low threshold for flow rate to %3.3f Mbps" % (int(s) * (1.0/1000000))
        print("-------------------------")

    def run(self):
        global sigint
        high_rate_set = False
        while not sigint:
            is_high, is_low = self.stat.refresh()
            if is_high and not high_rate_set:
                ac = AffinityControl()
                high_rate_set = True
#                ac.add_waypoint(link_name, self.waypoint_address)
                ac.add_isolate(link_name)
                ac.enable_affinity()
                print "Adding isolate affinity to link: %s" % (link_name)
#                time.sleep(3)
            elif is_low and high_rate_set: 
                high_rate_set = False
                ac.remove_isolate(link_name)
                ac.enable_affinity()
                print "Adding isolate affinity to link: %s" % (link_name)
            time.sleep(10)

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.list()
    subnet_control.add_subnet("defaultSubnet", prefix)

    raw_input("[Press enter when mininet is ready] ")
    print("-------------------------")

    m = WaypointMonitor(Stats.TYPE_AL, al="inflows")
#    m.set_waypoint("10.0.0.2")
    m.set_high_threshold(rate_high) 
    m.set_low_threshold(rate_low)
    m.start()

    # Register signal-handler to catch SIG_INT
    signal.signal(signal.SIGINT, signal_handler)
    signal.pause()

    # join() won't return until SIG_INT has been captured
    m.join()

if __name__ == "__main__":
    main()
