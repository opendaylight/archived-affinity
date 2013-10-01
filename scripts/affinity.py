#!/usr/local/bin/python

import httplib2
import json
import sys

#
# Configure an affinity link and set its waypoint address. 
#

def rest_method(url, verb): 
    global h

    print "REST call " + url
    resp, content = h.request(url, verb)

    if (resp.status != 200):
        print "Error code %d" % (resp.status)
        return
    
    if (verb == "GET"): 
        print content

    print "done"
    

def waypoint_demo():
    # Create two affinity groups

    print "create web servers group"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/group/webservers'
    rest_method(put_url, "POST")

    print "create external addresses"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/group/external'
    rest_method(put_url, "PUT")

    print "create link inflows"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/link/inflows'
    rest_method(put_url, "PUT")

    print "add ip to webservers"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/add/ip/webservers/192.168.1.1'
    rest_method(put_url, "PUT")

    print "add ip to webservers"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/add/ip/webservers/192.168.1.2'
    rest_method(put_url, "PUT")

    print "add ip to external"    
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/add/ip/external/10.10.0.0'
    rest_method(put_url, "PUT")


def get_all_affinity_groups(): 
    print "get all affinity groups"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/affinity-groups'
    rest_method(get_url, "GET")

def get_affinity_group(): 
    print "get affinity group"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/webserver'
    rest_method(get_url, "GET")

# Add waypoint IP to an affinity link.
def main():
    global h
    h = httplib2.Http(".cache")
    h.add_credentials('admin', 'admin')

    waypoint_demo()
#    get_all_affinity_groups()
#    get_affinity_group()
if __name__ == "__main__":
    main()

    

