#!/usr/local/bin/python

import httplib2
import json
import sys

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
Class to keep track of host statistics (byte count, bit rate)
'''
class HostStats:

    def __init__(self, src, dst):
        self.src = src
        self.dst = dst
        self.http = httplib2.Http(".cache")
        self.http.add_credentials('admin', 'admin')
        self.refresh()

    def refresh(self):
        resp, content = self.http.request("http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/" + self.src + "/" + self.dst, "GET")
        if (resp.status == 404):
            print "404 error"
            return
        if (resp.status == 503):
            print "503 error"
            return
        self.host_stats = json.loads(content)

    def get_bytes(self):
        try:
            bytes = long(self.host_stats["byteCount"])
        except Exception as e:
            print "exception:", e
            bytes = 0
        return bytes

    def get_bit_rate(self):

        try:
            bitrate = float(self.host_stats["bitRate"])
        except Exception as e:
            bitrate = 0.0
        return bitrate

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


def main():

    # Default subnet is required for the host tracker to work.  Run
    # this script once *before* you start mininet.
    subnet_control = SubnetControl()
    subnet_control.add_subnet("defaultSubnet", "10.0.0.254/8")

    demo_mode = True

    # Test mode
    if (not demo_mode):

        src = "10.0.0.1"
        dst = "10.0.0.2"
        host_stat = HostStats(src, dst)

        # These counts should be nonzero
        print("%d bytes between %s and %s" % (host_stat.get_bytes(), src, dst))
        print("%f mbit/s between %s and %s" % (host_stat.get_bit_rate(), src, dst))
        sys.exit()

    print "Usage: [host | link] [bytes | rate] [src dst | link-name]"

    # Demo mode
    while True:
        request = raw_input("> ")
        try:
            request = request.split()
            request_type = request[0]

            if (request_type == "quit"):
                sys.exit()

            action = request[1]

            if (request_type == "host"):
                src, dst = request[2:4]
                if (action == "bytes"):
                    host_stat = HostStats(src, dst)
                    print("%d bytes between %s and %s" % (host_stat.get_bytes(), src, dst))
                elif (action == "rate"):
                    host_stat = HostStats(src, dst)
                    print("%f bit/s between %s and %s" % (host_stat.get_bit_rate(), src, dst))
                else:
                    raise Exception

            elif (request_type == "link"):
                link = request[2]
                h = httplib2.Http(".cache")
                h.add_credentials("admin", "admin")
                resp, content = h.request("http://localhost:8080/controller/nb/v2/analytics/default/affinitylinkstats/" + link, "GET")
                al_stats = json.loads(content)

                if (action == "bytes"):
                    print("%d bytes on %s" % (long(al_stats["byteCount"]), link))
                elif (action == "rate"):
                    print("%f bit/s on %s" % (float(al_stats["bitRate"]), link))
                else:
                    raise Exception

            else:
                raise Exception
        except Exception as e:
            print "Error"


if __name__ == "__main__":
    main()
