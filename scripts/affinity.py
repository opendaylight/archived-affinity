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

def get_all_affinity_groups(): 
    print "get all affinity groups"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/affinity-groups'
    rest_method(get_url, "GET")

# Tbd
def get_all_affinity_links(): 
    print "get all affinity links"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/affinity-links'
    rest_method(get_url, "GET")

def get_affinity_group(groupname): 
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/' + groupname
    rest_method(get_url, "GET")

def get_affinity_link(linkname):
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + linkname
    content = rest_method(get_url, "GET")
    affyLinkCfg = json.loads(content)
    for key in affyLinkCfg:
        print "%10s : %s" % (key, affyLinkCfg[key])

def client_ws_example():
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

    print "add subnet to webservers"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/webservers/addsubnet/ipprefix/10.0.0.1/mask/31'
    rest_method(put_url, "PUT")

    print "add ip to external"    
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/clients/add/ip/10.0.0.3'
    rest_method(put_url, "PUT")

# Only one waypoint supported. 
def set_waypoint_address(al, wp):
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/setwaypoint/' + wp
    rest_method(put_url, "PUT")

def unset_waypoint_address(al):
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/unsetwaypoint'
    rest_method(put_url, "PUT")

def set_deny(setflag='deny'):
    al = 'inflows'
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/' + setflag + '/'
    rest_method(put_url, "PUT")

# Add a tap to ipaddress.
def set_tap(al, ipaddr):
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/settap/' + ipaddr
    rest_method(put_url, "PUT")

# Add a tap to ipaddress.
def unset_tap(al, ipaddr):
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/unsettap/' + ipaddr
    rest_method(put_url, "PUT")

# Set path isolate. 
def set_path_isolate():
    al = 'inflows'
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/setisolate/'
    rest_method(put_url, "PUT")

# Re-program the network using OF southbound. 
def enable_affinity():
    put_url = 'http://localhost:8080/affinity/nb/v2/flatl2/default/enableaffinity/'
    rest_method(put_url, "PUT")

def disable_affinity():
    put_url = 'http://localhost:8080/affinity/nb/v2/flatl2/default/disableaffinity/'
    rest_method(put_url, "PUT")

def main():
    global h

    # Create two affinity groups and a link between them. 
    # Assign attributes. 
    client_ws_example()

    get_affinity_group('webservers')
    get_affinity_group('clients')
    get_affinity_link('inflows')

    print "get_all_affinity_groups..."
    get_all_affinity_groups()
    print "get_all_affinity_links..."
    get_all_affinity_links()

    set_attr()
    list_all_hosts()
    return

# Set affinity attributes and make sure they are associated with the affinity link. 
def set_attr(): 
    # Set deny. 
    set_deny('deny')
    get_affinity_link('inflows')

    # Set waypoint and tap. 
    set_deny('permit')
    set_waypoint_address('inflows', '10.0.0.2')
    set_tap('inflows', '10.0.0.6')
    set_tap('inflows', '10.0.0.4')
    get_affinity_link('inflows')
    
    # Change a few affinity attributes and get the new link configuration. 
    unset_tap('inflows', '10.0.0.6')    
    print "get_affinity_link..."
    get_affinity_link('inflows')

    enable_affinity() # Tap to '10.0.0.4'.
    unset_tap('inflows', '10.0.0.4')
    set_path_isolate()    
    get_affinity_link('inflows')
    enable_affinity() # No action for isolate. Restore normal forwarding. 
    
#if __name__ == "__main__":
#    main()

h = httplib2.Http(".cache")
h.add_credentials('admin', 'admin')


    

