#!/usr/local/bin/python

import httplib2
import json

class AffinityControl:

    def __init__(self):
        self.http = httplib2.Http(".cache")
        self.http.add_credentials("admin", "admin")
        self.url_prefix = "http://localhost:8080/affinity/nb/v2/affinity/default/"

    # Add affinity group
    def add_affinity_group(self, group_name, **kwargs):
        # Create the group
        resp, content = self.http.request(self.url_prefix + "create/group/%s" % group_name, "PUT")
        if (resp.status != 201):
            print "AffinityGroup %s could not be created" % group_name
            return
        # If a list of IPs is passed, add each one
        if "ips" in kwargs:
            ips = kwargs['ips']
            for ip in ips:
                resp, content = self.http.request(self.url_prefix + "group/%s/add/ip/%s" % (group_name, ip), "PUT")
                if (resp.status != 201):
                    print "IP %s could not be added to AffinityGroup %s" % (ip, group_name)
                    return
            print "AffinityGroup %s added successfully. IPs are %s" % (group_name, ips)
        # If a subnet is passed, add that
        elif "subnet" in kwargs:
            ip, mask = kwargs['subnet'].split("/")
            resp, content = self.http.request(self.url_prefix + "group/%s/addsubnet/ipprefix/%s/mask/%s" % (group_name, ip, mask), "PUT")
            if (resp.status != 201):
                print "AffinityGroup could not be created for subnet %s/%s" % (ip, mask)
                return
            print "AffinityGroup %s added successfully. Subnet is %s/%s" % (group_name, ip, mask)

    # Add affinity link
    def add_affinity_link(self, link_name, src_group, dst_group):
        resp, content = self.http.request(self.url_prefix + "create/link/%s/from/%s/to/%s" % (link_name, src_group, dst_group), "PUT")
        if (resp.status != 201):
            print "AffinityLink %s could not be added between %s and %s" % (link_name, src_group, dst_group)
            return
        print "AffinityLink %s added between %s and %s" % (link_name, src_group, dst_group)

    # Add waypoint
    def add_waypoint(self, link_name, ip):
        resp, content = self.http.request(self.url_prefix + "link/%s/setwaypoint/%s" % (link_name, ip), "PUT")
        if (resp.status != 201):
            print "Waypoint %s could not be set for link %s" % (ip, link_name)
            return
        print "Waypoint %s successfully set for link %s" % (ip, link_name)

    # Enable waypoint
    def enable_waypoint(self, link_name):
        resp, content = self.http.request(self.url_prefix + "link/%s/enable" % link_name, "PUT")
        if (resp.status != 201):
            print "Waypoint could not be enabled for link %s" % link_name
            return
        print "Waypoint enabled for link %s" % link_name

    # Disable waypoint
    def disable_waypoint(self, link_name):
        resp, content = self.http.request(self.url_prefix + "link/%s/disable" % link_name, "PUT")
        if (resp.status != 201):
            print "Waypoint could not be disabled for link %s" % link_name
            return
        print "Waypoint disabled for link %s" % link_name
