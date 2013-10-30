#!/usr/local/bin/python

import httplib2
import json
import signal
import sys
import time

from threading import Thread

from affinity_control import AffinityControl
from subnet import SubnetControl
from stats import Stats

# If True, SIG_INT has been captured
global sigint
sigint = False

# Monitors statistics
class Monitor(Thread):

    def __init__(self, monitor_type, **kwargs):
        Thread.__init__(self)
        self.stat = Stats(monitor_type, **kwargs)
        self.stat_type = monitor_type
        self.did_waypoint = False

    def run(self):
        global sigint
        while not sigint:
            is_fast, is_big = self.stat.refresh()
            self.stat.get_incoming_hosts()
            if is_big and not self.did_waypoint:
                print "Large flow; redirect here"
                ac = AffinityControl()
                # First AG: Sources sending data into this subnet
                src_ips = self.stat.get_incoming_hosts()
                ac.add_affinity_group("webservers", ips=src_ips)
                # Second AG: This entity
                if (self.stat_type == "prefix"):
                    ac.add_affinity_group("clients", subnet=self.stat.subnet)
                else:
                    print "type", self.stat_type, "not supported for redirection"
                # AL: Between them
                ac.add_affinity_link("inflows", "webservers", "client")
                # TODO: This IP should be an option
                ac.add_waypoint("inflows", "10.0.0.2")
                self.did_waypoint = True
            time.sleep(1)

# Handle SIG_INT
def signal_handler(signal, frame):
    global sigint
    sigint = True

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    m = Monitor("prefix", subnet="10.0.0.0/31")
    m.start()

    # Register signal-handler to catch SIG_INT
    signal.signal(signal.SIGINT, signal_handler)
    signal.pause()

    # join() won't return until SIG_INT has been captured
    m.join()

if __name__ == "__main__":
    main()
