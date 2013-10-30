#!/usr/bin/python

import httplib2
import json
import sys

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
