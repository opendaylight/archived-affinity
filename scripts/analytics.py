#!/usr/local/bin/python

import httplib2
import json
import sys
import time

from stats import Stats
from subnet import SubnetControl
from affinity_control import AffinityControl

# 1. Start the controller
# 2. On the local machine (e.g., your laptop), start this script.
#    > python analytics.py
# 3. On the mininet VM, run:
#    > sudo mn --controller=remote,ip=192.168.56.1 --topo tree,2
#    > h1 ping h3
# 4. Give commands to analytics.py.  For instance:
#    > host bytes 10.0.0.1 10.0.0.3
#   (There is a usage prompt that prints at the beginning of analytics.py)
# 5. Type 'quit' to exit analytics.py

def run_interactive_mode():

    print "Usage: [host | link] [bytes | rate] [src dst | link-name]"

    # Demo mode
    while True:
        request = raw_input("> ")
        try:
            request = request.split()
            request_type = request[0]

            if (request_type == "quit"):
                sys.exit()

            if (request_type == "host"):
                action = request[1]
                src, dst = request[2:4]
                host_stat = Stats("host", src=src, dst=dst)
                if (action == "bytes"):
                    print("%d bytes between %s and %s" % (host_stat.get_bytes(), src, dst))
                elif (action == "rate"):
                    print("%f bit/s between %s and %s" % (host_stat.get_bit_rate(), src, dst))
                else:
                    print "wrong action"
                    raise Exception

            elif (request_type == "link"):
                action = request[1]
                link = request[2]
                link_stat = Stats("affinityLink", al=link)
                if (action == "bytes"):
                    print("%d bytes on %s" % (link_stat.get_bytes(), link))
                elif (action == "rate"):
                    print("%f bit/s on %s" % (link_stat.get_bit_rate(), link))
                else:
                    print "wrong action 2"
                    raise Exception

            elif (request_type == "prefix"):
                prefix = request[1]
                h = httplib2.Http(".cache")
                h.add_credentials("admin", "admin")
                url_prefix = "http://localhost:8080/affinity/nb/v2/analytics/default/prefixstats/"
                resp, content = h.request(url_prefix + prefix, "GET")
                if (resp.status == 200):
                    data = json.loads(content)
                    print data['byteCount'], "bytes"

            else:
                print "something else"
                raise Exception
        except Exception as e:
            print "Error"
            print e


def get_all_hosts():

    h = httplib2.Http(".cache")
    h.add_credentials("admin", "admin")

    resp, content = h.request("http://localhost:8080/controller/nb/v2/hosttracker/default/hosts/active", "GET")
    host_content = json.loads(content)

    # Even if there are no active hosts, host_content['hostConfig']
    # still exists (and is empty)
    active_hosts = []
    for host_data in host_content['hostConfig']:
        active_hosts.append(host_data['networkAddress'])
    return active_hosts


def run_passive_mode(affinity_links):
    # TODO: Get affinity_links automatically
    affinity_link_stats = {}

    # Go through all affinity link stats
    while True:
        for al in affinity_links:
            if al not in affinity_link_stats:
                affinity_link_stats[al] = Stats("affinityLink", al=al)
            stat = affinity_link_stats[al]
            stat.refresh()
            print "%d bytes (%1.1f Mbit/s) on %s" % (stat.get_bytes(), (stat.get_bit_rate() / (10**6)), al)
        time.sleep(2)

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    # Set up an affinity link
    affinity_control = AffinityControl()
    affinity_control.add_affinity_group("testAG1", ["10.0.0.1", "10.0.0.2"])
    affinity_control.add_affinity_group("testAG2", ["10.0.0.3", "10.0.0.4"])
    affinity_control.add_affinity_link("testAL", "testAG1", "testAG2")
    raw_input("[Press enter to continue]" )

    interactive_mode = True

    if interactive_mode:
        run_interactive_mode()
    else:
        run_passive_mode(["testAL"])

if __name__ == "__main__":
    main()
