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
                host_stat = Stats(Stats.TYPE_HOST, src=src, dst=dst)
                if (action == "bytes"):
                    print("%d bytes between %s and %s" % (host_stat.get_bytes(), src, dst))
                elif (action == "rate"):
                    print("%f bit/s between %s and %s" % (host_stat.get_bit_rate(), src, dst))
                else:
                    raise Exception

            elif (request_type == "link"):
                action = request[1]
                link = request[2]
                link_stat = Stats(Stats.TYPE_AL, al=link)
                if (action == "bytes"):
                    print("%d bytes on %s" % (link_stat.get_bytes(), link))
                elif (action == "rate"):
                    print("%f bit/s on %s" % (link_stat.get_bit_rate(), link))
                else:
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
                raise Exception
        except Exception as e:
            print "Error"
            print e

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    # Set up an affinity link
    affinity_control = AffinityControl()
    affinity_control.add_affinity_group("testAG1", ips=["10.0.0.1", "10.0.0.2"])
    affinity_control.add_affinity_group("testAG2", ips=["10.0.0.3", "10.0.0.4"])
    affinity_control.add_affinity_link("testAL", "testAG1", "testAG2")
    raw_input("[Press enter to continue]" )

    run_interactive_mode()

if __name__ == "__main__":
    main()
