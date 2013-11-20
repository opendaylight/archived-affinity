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

def stats_hosts(src, dst):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s" % (src, dst)
    data = rest_method(url, "GET")
    print("%s bytes between %s and %s" % (data["byteCount"], src, dst))
    print("%s bit/s between %s and %s" % (data["bitRate"], src, dst))

def stats_hosts_protocol(src, dst, protocol):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s/%d" % (src, dst, protocol)
    data = rest_method(url, "GET")
    print("%s bytes between %s and %s for protocol %d" % (data["byteCount"], src, dst, protocol))
    print("%s bit/s between %s and %s on protocol %d" % (data["bitRate"], src, dst, protocol))

def all_stats_hosts(src, dst):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/%s/%s/all" % (src, dst)
    data = rest_method(url, "GET")['data']['entry']
    if (type(data) == type({})):
        host = data['key']
        byte_count = data['value']
        print("%s bytes from host %s" % (byte_count, host))
    else:
        for entry in data:
            protocol = entry['key']
            byte_count = entry['value']['byteCount']
            bit_rate = entry['value']['bitRate']
            print("%s bytes from protocol %s" % (byte_count, protocol))
            print("%s bit/s from protocol %s" % (bit_rate, protocol))

### Affinity link statistics

def stats_link(al):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/affinitylinkstats/%s" % al
    data = rest_method(url, "GET")
    print("%s bytes on link %s" % (data['byteCount'], al))
    print("%s bit/s on link %s" % (data['bitRate'], al))

def stats_link_protocol(al, protocol):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/affinitylinkstats/%s/%s" % (al, protocol)
    data = rest_method(url, "GET")
    print("%s bytes on link %s for protocol %s" % (data['byteCount'], al, protocol))
    print("%s bit/s on link %s for protocol %s" % (data['bitRate'], al, protocol))

def all_stats_link(al):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/affinitylinkstats/%s/all" % al
    data = rest_method(url, "GET")['data']['entry']
    if (type(data) == type({})):
        host = data['key']
        byte_count = data['value']
        print("%s bytes from host %s" % (byte_count, host))
    else:
        for entry in data:
            protocol = entry['key']
            byte_count = entry['value']['byteCount']
            bit_rate = entry['value']['bitRate']
            print("%s bytes from protocol %s" % (byte_count, protocol))
            print("%s bit/s from protocol %s" % (bit_rate, protocol))

### Subnet statistics

def stats_subnet(src_sub, dst_sub):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/subnetstats/%s/%s" % (src_sub, dst_sub)
    data = rest_method(url, "GET")
    if (src_sub == "null/null"):
        print("%s bytes into %s" % (data['byteCount'], dst_sub))
        print("%s bit/s into %s" % (data['bitRate'], dst_sub))
    elif (dst_sub == "null/null"):
        print("%s bytes out of %s" % (data['byteCount'], src_sub))
        print("%s bit/s out of %s" % (data['bitRate'], src_sub))
    else:
        print("%s bytes between %s and %s" % (data['byteCount'], src_sub, dst_sub))
        print("%s bit/s between %s and %s" % (data['bitRate'], src_sub, dst_sub))

def stats_subnet_protocol(src_sub, dst_sub, protocol):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/subnetstats/%s/%s/%s" % (src_sub, dst_sub, protocol)
    data = rest_method(url, "GET")
    if (src_sub == "null/null"):
        print("%s bytes into %s from protocol %s" % (data['byteCount'], dst_sub, protocol))
        print("%s bit/s into %s from protocol %s" % (data['bitRate'], dst_sub, protocol))
    elif (dst_sub == "null/null"):
        print("%s bytes out of %s from protocol %s" % (data['byteCount'], src_sub, protocol))
        print("%s bit/s out of %s from protocol %s" % (data['bitRate'], src_sub, protocol))
    else:
        print("%s bytes between %s and %s from protocol %s" % (data['byteCount'], src_sub, dst_sub, protocol))
        print("%s bit/s between %s and %s from protocol %s" % (data['bitRate'], src_sub, dst_sub, protocol))

def all_stats_subnet(src_sub, dst_sub):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/subnetstats/%s/%s/all" % (src_sub, dst_sub)
    data = rest_method(url, "GET")['data']['entry']
    if (type(data) == type({})):
        host = data['key']
        byte_count = data['value']
        print("%s bytes from host %s" % (byte_count, host))
    else:
        for entry in data:
            protocol = entry['key']
            byte_count = entry['value']['byteCount']
            bit_rate = entry['value']['bitRate']
            print("%s bytes from protocol %s" % (byte_count, protocol))
            print("%s bit/s from protocol %s" % (bit_rate, protocol))

def incoming_hosts(subnet):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/subnetstats/incoming/%s" % subnet
    data = rest_method(url, "GET")['data']['entry']
    if (type(data) == type({})):
        host = data['key']
        byte_count = data['value']
        print("%s bytes from host %s" % (byte_count, host))
    else:
        for entry in data:
            host = entry['key']
            byte_count = entry['value']
            print("%s bytes from host %s" % (byte_count, host))

def incoming_hosts_protocol(subnet, protocol):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/subnetstats/incoming/%s/%s" % (subnet, protocol)
    data = rest_method(url, "GET")['data']['entry']
    if (type(data) == type({})):
        host = data['key']
        byte_count = data['value']
        print("%s bytes from host %s" % (byte_count, host))
    else:
        for entry in data:
            host = entry['key']
            byte_count = entry['value']
            print("%s bytes from host %s" % (byte_count, host))

def run_interactive_mode():

    print "Usage: [host | link | subnet] [src dst | link-name | src_sub dst_sub] {protocol}"

    # Demo mode
    while True:
        request = raw_input("> ")
        request = request.split()
        request_type = request[0]

        if (request_type == "quit" or request_type == "exit"):
            sys.exit()

        if (request_type == "host"):
            if (len(request) == 3):
                src, dst = request[1:3]
                stats_hosts(src, dst)
                all_stats_hosts(src, dst)
            elif (len(request) == 4):
                src, dst, protocol = request[1:4]
                stats_hosts_protocol(src, dst, int(protocol))

        elif (request_type == "link"):
            if (len(request) == 2):
                link = request[1]
                stats_link(link)
                all_stats_link(link)
            elif (len(request) == 3):
                link, protocol = request[1:3]
                stats_link_protocol(link, protocol)
                
        elif (request_type == "subnet"):
            if (len(request) == 3):
                src_sub, dst_sub = request[1:3]
                if src_sub == "null": src_sub = "null/null"
                if dst_sub == "null": dst_sub = "null/null"
                stats_subnet(src_sub, dst_sub)
                all_stats_subnet(src_sub, dst_sub)
            elif (len(request) == 4):
                src_sub, dst_sub, protocol = request[1:4]
                if src_sub == "null": src_sub = "null/null"
                if dst_sub == "null": dst_sub = "null/null"
                stats_subnet_protocol(src_sub, dst_sub, protocol)

def main():

    # Default subnet is required for the host tracker to work.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    # Set up an affinity link
    affinity_control = AffinityControl()
    affinity_control.add_affinity_group("testAG1", ips=["10.0.0.1", "10.0.0.2"])
    affinity_control.add_affinity_group("testAG2", ips=["10.0.0.3", "10.0.0.4"])
    affinity_control.add_affinity_link("testAL", "testAG1", "testAG2")

    run_interactive_mode()

if __name__ == "__main__":
    main()
