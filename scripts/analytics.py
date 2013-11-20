#!/usr/local/bin/python

import httplib2
import json
import sys
import time

from stats import Stats
from subnet import SubnetControl
from affinity_control import AffinityControl

# Generic REST query
def rest_method(url, rest_type):
    h = httplib2.Http(".cache")
    h.add_credentials('admin', 'admin')
    resp, content = h.request(url, rest_type)
    return json.loads(content)

### Host Statistics

def bytes_between_hosts(src, dst):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s" % (src, dst)
    data = rest_method(url, "GET")
    print("%d bytes between %s and %s" % (int(data["byteCount"]), src, dst))

def bytes_between_hosts_protocol(src, dst, protocol):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s/%d" % (src, dst, protocol)
    data = rest_method(url, "GET")
    print("%d bytes between %s and %s for protocol %d" % (int(data["byteCount"]), src, dst, protocol))

def rate_between_hosts(src, dst):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s" % (src, dst)
    data = rest_method(url, "GET")
    print("%s bit/s between %s and %s" % (data["bitRate"], src, dst))

def rate_between_hosts_protocol(src, dst, protocol):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s/%d" % (src, dst, protocol)
    data = rest_method(url, "GET")
    print("%s bit/s between %s and %s on protocol %d" % (data["bitRate"], src, dst, protocol))

def all_byte_counts_hosts(src, dst):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s/all" % (src, dst)
    data = rest_method(url, "GET")['data']['entry']
    for entry in data:
        protocol = entry['key']
        byte_count = entry['value']['byteCount']
        print("%s bytes from protocol %s" % (byte_count, protocol))

def all_bit_rates_hosts(src, dst):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s/all" % (src, dst)
    data = rest_method(url, "GET")['data']['entry']
    for entry in data:
        protocol = entry['key']
        bit_rate = entry['value']['bitRate']
        print("%s bit/s from protocol %s" % (bit_rate, protocol))

### Affinity link statistics

def run_interactive_mode():

    print "Usage: [host | link] [src dst | link-name] {protocol}"

    # Demo mode
    while True:
        request = raw_input("> ")
        try:
            request = request.split()
            request_type = request[0]

            if (request_type == "quit" or request_type == "exit"):
                sys.exit()

            if (request_type == "host"):
                if (len(request) == 3):
                    src, dst = request[1:3]
                    bytes_between_hosts(src, dst)
                    rate_between_hosts(src, dst)
                    all_byte_counts_hosts(src, dst)
                    all_bit_rates_hosts(src, dst)
                elif (len(request) == 4):
                    src, dst, protocol = request[1:4]
                    bytes_between_hosts_protocol(src, dst, int(protocol))
                    rate_between_hosts_protocol(src, dst, int(protocol))

            elif (request_type == "link"):
                link = request[1]

            elif (request_type == "subnet"):
                subnet = request[1]

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    # Set up an affinity link
#    affinity_control = AffinityControl()
#    affinity_control.add_affinity_group("testAG1", ips=["10.0.0.1", "10.0.0.2"])
#    affinity_control.add_affinity_group("testAG2", ips=["10.0.0.3", "10.0.0.4"])
#    affinity_control.add_affinity_link("testAL", "testAG1", "testAG2")
#    raw_input("[Press enter to continue]" )

    run_interactive_mode()

if __name__ == "__main__":
    main()
