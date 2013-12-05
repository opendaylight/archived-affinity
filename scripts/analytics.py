#!/usr/local/bin/python

'''
Copyright (c) 2013 Plexxi, Inc.  All rights reserved.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License v1.0 which accompanies this distribution,
and is available at http://www.eclipse.org/legal/epl-v10.html
'''

import json
import requests
import sys
import time

from stats import Stats
from subnet import SubnetControl
from affinity_control import AffinityControl

# This class has nothing to do with the analytics API, but it makes
# adding flows *so* much easier.
class Flow:

    def __init__(self, node, data):
        self.node_id = node['id']
        self.priority = data['priority']
        self.ether_type = '0x800'
        for match_field in data['match']['matchField']:
            if match_field['type'] == 'DL_DST':
                self.dl_dst = match_field['value']
            elif match_field['type'] == 'IN_PORT':
                self.in_port = match_field['value'].split('@')[0].split('|')[-1]
        self.output_port = data['actions'][0]['port']['id']

    def set_priority(self, priority):
        self.priority = priority

    def set_protocol(self, protocol):
        self.protocol = protocol

    def get_json(self, name):
        json_data = {'installInHw' : 'true',
                     'name' : name,
                     'node' : {'id' : self.node_id, 'type' : 'OF'},
                     'priority' : self.priority,
                     'etherType' : self.ether_type,
                     'dlDst' : self.dl_dst,
                     'ingressPort' : self.in_port,
                     'protocol' : self.protocol,
                     'actions' : ['OUTPUT=%s' % self.output_port]}
        return json_data

# Generic REST query
def rest_method(url, rest_type, payload=None):
    if (rest_type == "GET"):
        resp = requests.get(url, auth=('admin', 'admin'))
        print "status:", resp.status_code
        return resp.json()
    elif (rest_type == "PUT"):
        headers = {'content-type': 'application/json'}
        resp = requests.put(url, auth=('admin', 'admin'), data=json.dumps(payload), headers=headers)
        print "status:", resp.status_code, resp.text
    elif (rest_type == "DELETE"):
        resp = requests.delete(url, auth=('admin', 'admin'))
        print "status:", resp.status_code


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
    data = rest_method(url, "GET")['stats']
    for entry in data:
        print("%s bytes from protocol %s" % (entry['stat']['byteCount'], entry['protocol']))
        print("%s bit/s from protocol %s" % (entry['stat']['bitRate'], entry['protocol']))

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
    data = rest_method(url, "GET")['stats']
    for entry in data:
        print("%s bytes from protocol %s" % (entry['stat']['byteCount'], entry['protocol']))
        print("%s bit/s from protocol %s" % (entry['stat']['bitRate'], entry['protocol']))

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
    data = rest_method(url, "GET")['stats']
    for entry in data:
        print("%s bytes from protocol %s" % (entry['stat']['byteCount'], entry['protocol']))
        print("%s bit/s from protocol %s" % (entry['stat']['bitRate'], entry['protocol']))

def incoming_hosts(subnet):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/subnetstats/incoming/%s" % subnet
    data = rest_method(url, "GET")
    data = rest_method(url, "GET")['stats']
    if (type(data) == type({})):
        data = [data]
    for entry in data:
        print("%s bytes from host %s" % (entry['byteCount'], entry['hostIP']))

def incoming_hosts_protocol(subnet, protocol):
    url = "http://localhost:8080/affinity/nb/v2/analytics/default/subnetstats/incoming/%s/%s" % (subnet, protocol)
    data = rest_method(url, "GET")['data']['entry']
    if (type(data) == type({})):
        data = [data]
    for entry in data:
        print("%s bytes from host %s" % (entry['byteCount'], entry['hostIP']))

# This is not part of the analytics NB API, but it is a necessary step
# if you want to monitor protocols
def add_protocol_flows():
    protocols = [1, 6, 17] # ICMP, TCP, UDP
    flows = get_flows()
    i = 0
    for flow in flows:
        i += 1
        name = "flow" + str(i)
        flow.set_priority(2)
        flow.set_protocol(1)
        add_flow(flow, name)

#### Flow control methods

def get_flows():
    url = "http://localhost:8080/controller/nb/v2/statistics/default/flow"
    data = rest_method(url, "GET")
    flows = []
    for item in data['flowStatistics']:
        n = item['node']
        for item in item['flowStatistic']:
            f = Flow(n, item['flow'])
            flows.append(f)
    return flows

def add_flow(flow, flow_name):
    print "adding flow %s" % flow_name
    url = "http://localhost:8080/controller/nb/v2/flowprogrammer/default/node/OF/%s/staticFlow/%s" % (flow.node_id, flow_name)
    rest_method(url, "PUT", flow.get_json(flow_name))

#### End flow control methods

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

    raw_input("press enter ")
    add_protocol_flows()

    run_interactive_mode()

if __name__ == "__main__":
    main()

