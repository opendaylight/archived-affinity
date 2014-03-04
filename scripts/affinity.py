#!/usr/local/bin/python

import httplib2
import json
import requests
import sys
import urllib2

#
# Configure an affinity link and set its waypoint address. 
#

def rest_method(url, verb): 
    global h

    print "REST call " + url
    resp, content = h.request(url, verb)

#    print content
    print "return code %d" % (resp.status)
    return content

# Generic REST query
def rest_method_2(url, rest_type, payload=None):
    if (rest_type == "GET"):
        resp = requests.get(url, auth=('admin', 'admin'))
        return resp.json()
    elif (rest_type == "PUT"):
        headers = {'content-type': 'application/json'}
        print json.dumps(payload)
        resp = requests.put(url, auth=('admin', 'admin'), data=json.dumps(payload), headers=headers)
        print resp.text
        return resp.status_code
    elif (rest_type == "DELETE"):
        resp = requests.delete(url, auth=('admin', 'admin'))

def list_all_hosts(): 
    print "list active hosts"
    get_url = 'http://localhost:8080/controller/nb/v2/hosttracker/default/hosts/active'
    content = rest_method(get_url, "GET")
    print content
    hostCfg = json.loads(content)
    for host in hostCfg['hostConfig']:
        print host

    print "list inactive hosts"
    get_url = 'http://localhost:8080/controller/nb/v2/hosttracker/default/hosts/inactive'
    rest_method(get_url, "GET")
    content = rest_method(get_url, "GET")
    hostCfg = json.loads(content)
    for host in hostCfg['hostConfig']:
        print host

def get_all_affinity_groups(): 
    print "get all affinity groups"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/affinity-groups'
    content = rest_method(get_url, "GET")
    print content

# Tbd
def get_all_affinity_links(): 
    print "get all affinity links"
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/affinity-links'
    content = rest_method(get_url, "GET")
    affylinks = json.loads(content)
    print affylinks
    for link in affylinks['affinityLinks']: 
        print "Affinity link: %s" % link
        get_affinity_link(link['name'])

def get_affinity_group(groupname): 
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/' + groupname
    rest_method(get_url, "GET")

def get_affinity_hosts(groupname): 
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/hosts/' + groupname
    content = rest_method(get_url, "GET")
    print content

def get_affinity_link(linkname):
    get_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + linkname
    content = rest_method(get_url, "GET")
    affyLinkCfg = json.loads(content)
    for key in sorted(affyLinkCfg):
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

#    print "add subnet to webservers"
#    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/webservers/addsubnet/ipprefix/10.0.0.1/mask/31'
#    rest_method(put_url, "PUT")

    print "add endpoint to webservers"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/webservers/add/ip/10.0.0.1'
    rest_method(put_url, "PUT")

    print "add ip to clients"    
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/clients/add/ip/10.0.0.3'
    rest_method(put_url, "PUT")


def drop_ws_objects(): 

    print "remove inflows link"
    put_url = "http://localhost:8080/affinity/nb/v2/affinity/default/delete/link/inflows"
    rest_method(put_url, "PUT")
    
    print "remove web servers group"
    put_url = "http://localhost:8080/affinity/nb/v2/affinity/default/delete/group/webservers"
    rest_method(put_url, "PUT")

    print "remove clients group"
    put_url = "http://localhost:8080/affinity/nb/v2/affinity/default/delete/group/clients"
    rest_method(put_url, "PUT")

def tap_example():
    # Create two affinity groups
    print "create group A" 
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/group/a'
    rest_method(put_url, "PUT")

    print "create group B"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/group/b'
    rest_method(put_url, "PUT")

    print "create link A -> B"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/link/a_to_b/from/a/to/b'
    rest_method(put_url, "PUT")

    print "create link B -> A"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/link/b_to_a/from/b/to/a'
    rest_method(put_url, "PUT")

    print "add ip addresses to A"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/a/add/ip/10.0.0.1'
    rest_method(put_url, "PUT")
#    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/a/add/ip/10.0.0.2'
#    rest_method(put_url, "PUT")

    print "add ip addresses to B"    
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/group/b/add/ip/10.0.0.3'
    rest_method(put_url, "PUT")


def repeat_add_link(): 
    print "create link inflows"
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/create/link/inflows/from/clients/to/webservers'
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

# Remove all tap servers and the tap affinity attribute. 
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

# Set affinity attributes and make sure they are associated with the affinity link. 
def add_deny():
    # Set deny. 
    set_deny('deny')
    get_affinity_link('inflows')
    set_deny('permit')
    get_affinity_link('inflows')
    
def add_waypoint(): 
    set_waypoint_address('inflows', '10.0.0.2')

# Add tap servers. 
def test_tap_1(): 
    tap_example()
    set_tap('a_to_b', '10.0.0.4')
#    add_static_host_tap('a_to_b', '10.0.0.20')
    get_affinity_link('a_to_b')
    enable_affinity() # Tap to '10.0.0.4'.

# Add tap servers. 
def test_tap_2(): 
    tap_example()
    set_tap('a_to_b', '10.0.0.2')
    set_tap('b_to_a', '10.0.0.2')
    get_affinity_link('a_to_b')
    get_affinity_link('b_to_a')
    enable_affinity() # Tap to '10.0.0.2'.

def add_isolate(): 
    set_path_isolate()    
    get_affinity_link('inflows')
    enable_affinity() # Large flow forwarding

def main():
    global h

    # Create two affinity groups and a link between them. 
    # Assign attributes. 
    client_ws_example()
    repeat_add_link()

    get_affinity_group('webservers')
    get_affinity_group('clients')
    get_affinity_link('inflows')
    add_waypoint()

    print "get_all_affinity_groups..."
    get_all_affinity_groups()
    print "get_all_affinity_links..."
    get_all_affinity_links()

    list_all_hosts()
    return

def add_static_host_tap(al, ipaddr):
    payload = {'dataLayerAddress':'00:00:00:00:01:01', 
                 'nodeType':'OF', 
                 "nodeId":"00:00:00:00:00:00:00:01", 
                 "nodeConnectorType":"OF", 
                 "nodeConnectorId":"9", 
                 "vlan":"1", 
                 "staticHost":'true', 
                 "networkAddress":ipaddr}
    url = "http://localhost:8080/controller/nb/v2/hosttracker/default/address/%s" % (ipaddr)
    print payload
    status = rest_method_2(url, "PUT", payload)
    print "add_static_host: ", status
    
    # Add tap to static host.
    print "add tap to new static host" + ipaddr
    put_url = 'http://localhost:8080/affinity/nb/v2/affinity/default/link/' + al + '/settap/' + ipaddr
    rest_method(put_url, "PUT")
    

#opener = {}
#init_urllib()
h = httplib2.Http(".cache")
h.add_credentials('admin', 'admin')
if __name__ == "__main__":
#    main()
    test_tap_1()


    

