#!/usr/local/bin/python

import httplib2
import json
import sys
import time

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


'''
Class for keeping track of host stats or affinity link stats, depending.
'''
class Stats:

    # TODO: Each stat should probably be a thread, and handle its
    # own output and refreshing for the EWMA

    def __init__(self, stat_type, **kwargs):
        self.stat_type = stat_type
        if stat_type == "host":
            self.src = kwargs['src']
            self.dst = kwargs['dst']
            self.url_prefix = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/" 
        elif stat_type == "affinityLink":
            self.al = kwargs['al']
            self.url_prefix = "http://localhost:8080/affinity/nb/v2/analytics/default/affinitylinkstats/"
        else:
            print "incorrect stat type", stat_type

        self.stats = {}
        self.rate_ewma = None

        self.http = httplib2.Http(".cache")
        self.http.add_credentials('admin', 'admin')
        self.refresh()

    def __str__(self):
        if (self.stat_type == "host"):
            return "host pair %s -> %s" % (self.src, self.dst)
        elif (self.stat_type == "affinityLink"):
            return "AffinityLink %s" % self.al
        else:
            return "Unknown Stats type"

    # Refresh statistics
    def refresh(self):
        if (self.stat_type == "host"):
            resp, content = self.http.request(self.url_prefix + self.src + "/" + self.dst, "GET")
        elif (self.stat_type == "affinityLink"):
            resp, content = self.http.request(self.url_prefix + self.al, "GET")
        self.stats = json.loads(content)
        self.handle_rate_ewma()
        self.check_large_flow()

    # EWMA calculation for bit rate
    def handle_rate_ewma(self):
        alpha = .25
        anomaly_threshold = 2.0
        new_bitrate = self.get_bit_rate()

        if self.rate_ewma == None:
            self.rate_ewma = new_bitrate
        else:
            new_rate_ewma = alpha * new_bitrate + (1 - alpha) * self.rate_ewma
            if (self.rate_ewma > 0 and new_rate_ewma > anomaly_threshold * self.rate_ewma):
                print "!! Anomaly detected on %s" % self
                print "!! Rate rose from %1.1f Mbit/s to %1.1f Mbit/s" % ((self.rate_ewma/10**6), (new_rate_ewma/10**6))
            self.rate_ewma = new_rate_ewma

    def check_large_flow(self):
        if (self.get_bytes() > 5 * (10**6)):
            print "!! Large flow detected on %s" % self

    # Bytes
    def get_bytes(self):
        try:
            bytes = long(self.stats["byteCount"])
        except Exception as e:
            bytes = 0
        return bytes

    # Bit Rate
    def get_bit_rate(self):
        try:
            bitrate = float(self.stats["bitRate"])
        except Exception as e:
            bitrate = 0.0
        return bitrate


class AffinityControl:

    def __init__(self):
        self.http = httplib2.Http(".cache")
        self.http.add_credentials("admin", "admin")
        self.url_prefix = "http://localhost:8080/affinity/nb/v2/affinity/default/"
        self.groups = []
        self.links = []        

    def add_affinity_group(self, group_name, ips):
        resp, content = self.http.request(self.url_prefix + "create/group/%s" % group_name, "PUT")
        if (resp.status != 201):
            print "AffinityGroup %s could not be created" % group_name
            return
        for ip in ips:
            resp, content = self.http.request(self.url_prefix + "group/%s/add/ip/%s" % (group_name, ip), "PUT")
            if (resp.status != 201):
                print "IP %s could not be added to AffinityGroup %s" % (ip, group_name)
                return
        self.groups.append(group_name)
        print "AffinityGroup %s added successfully. IPs are %s" % (group_name, ips)


    def add_affinity_link(self, link_name, src_group, dst_group):
        resp, content = self.http.request(self.url_prefix + "create/link/%s/from/%s/to/%s" % (link_name, src_group, dst_group), "PUT")
        if (resp.status != 201):
            print "AffinityLink %s could not be added between %s and %s" % (link_name, src_group, dst_group)
            return
        self.links.append(link_name)
        print "AffinityLink %s added between %s and %s" % (link_name, src_group, dst_group)


'''
Class for controlling subnets.  Right now, just adds subnets and
checks whether they exist, because that's all we need.
'''
class SubnetControl:

    def __init__(self):
        self.http = httplib2.Http(".cache")
        self.http.add_credentials("admin", "admin")
        self.url_prefix = "http://localhost:8080/controller/nb/v2/subnetservice/default/"

    # Checks whether subnet exists.  Checks against the actual subnet
    # string (e.g., "10.0.0.255/1"), not the subnet name.  Will not
    # catch things like overlapping subnets.
    def exists(self, subnet):
        resp, content = self.http.request(self.url_prefix + "subnets", "GET")
        if (resp.status != 200):
            print "Fatal error - can't check for subnet existence"
            sys.exit(-1)
        data = json.loads(content)

        for key in data["subnetConfig"]:
            if (key["subnet"] == subnet):
                return True
        return False

    # Add a subnet if it doesn't already exist.
    def add_subnet(self, subnet_name, subnet):
        if (self.exists(subnet)):
            print "subnet", subnet, "already exists"
            return
        subnet_config = dict(name=subnet_name, subnet=subnet)
        json_data = json.dumps(subnet_config)
        resp, content = self.http.request(self.url_prefix + "subnet/" + subnet_name, "POST", json_data, {'Content-Type': 'application/json'})
        if (resp.status == 201):
            print "subnet", subnet, "added"
        else:
            print "subnet", subnet, "could not be added"


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
