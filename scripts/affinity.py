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

    print content
    print "return code %d" % (resp.status)
    print "done"
    return content


def list_all_hosts(): 

    print "list active hosts"
    put_url = 'http://localhost:8080/controller/nb/v2/hosttracker/default/hosts/active'
    content = rest_method(put_url, "GET")
    hostCfg = json.loads(content)
    for host in hostCfg['hostConfig']:
        print host

    print "list inactive hosts"
    put_url = 'http://localhost:8080/controller/nb/v2/hosttracker/default/hosts/inactive'
    rest_method(put_url, "GET")
    content = rest_method(put_url, "GET")
    hostCfg = json.loads(content)
    for host in hostCfg['hostConfig']:
        print host
    

def waypoint_init():
    # Create two affinity groups

    print "create web servers group"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/group/webservers'
    rest_method(put_url, "PUT")

    print "create external addresses"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/group/clients'
    rest_method(put_url, "PUT")

    print "create link inflows"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/link/inflows/from/clients/to/webservers'
    rest_method(put_url, "PUT")

#    print "add ip to webservers"
#    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/webservers/add/ip/10.0.0.1'
#    rest_method(put_url, "PUT")

    print "add subnet to webservers"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/webservers/addsubnet/ipprefix/10.0.0.1/mask/31'
    rest_method(put_url, "PUT")

#    print "add ip to webservers"
#    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/webservers/add/ip/10.0.0.2'
#    rest_method(put_url, "PUT")

    print "add ip to external"    
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/clients/add/ip/10.0.0.3'
    rest_method(put_url, "PUT")


def get_all_affinity_groups(): 
    print "get all affinity groups"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/affinity-groups'
    rest_method(get_url, "GET")

# Tbd
def get_all_affinity_links(): 
    print "get all affinity groups"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/affinity-links'
    rest_method(get_url, "GET")

def get_affinity_group(groupname): 
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/' + groupname
    rest_method(get_url, "GET")

def get_affinity_link(linkname):
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + linkname
    rest_method(get_url, "GET")

def set_waypoint_address():
    wp = "10.0.0.2"
    al = 'inflows'
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/setwaypoint/' + wp
    rest_method(put_url, "PUT")

def unset_waypoint_address():
    al = 'inflows'
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/unsetwaypoint'
    rest_method(put_url, "PUT")

def set_deny(setflag='deny'):
    al = 'inflows'
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/' + setflag + '/'
    rest_method(put_url, "PUT")

#def enable_waypoint():
#    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/inflows/enable/' 
#    rest_method(put_url, "PUT")

#def disable_waypoint():
#    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/inflows/disable/'
#    rest_method(put_url, "PUT")

def enable_affinity():
    put_url = 'http://localhost:8080/affinity/nb/v2/flatl2/default/enableaffinity/'
    rest_method(put_url, "PUT")

def disable_affinity():
    put_url = 'http://localhost:8080/affinity/nb/v2/flatl2/default/disableaffinity/'
    rest_method(put_url, "PUT")

# Add waypoint IP to an affinity link.
def main():
    global h

    waypoint_init()

    get_affinity_group('webservers')
    get_affinity_group('clients')
    get_affinity_link('inflows')

    get_all_affinity_groups()
    list_all_hosts()

    # Set affinity attributes and make sure they are associated with the affinity link. 
    set_waypoint_address()
    set_deny('deny')
    set_deny('permit')
    get_affinity_link('inflows')

    enable_affinity()
    disable_affinity()

#    enable_waypoint()
#    disable_waypoint()

#if __name__ == "__main__":
#    main()

h = httplib2.Http(".cache")
h.add_credentials('admin', 'admin')


    

