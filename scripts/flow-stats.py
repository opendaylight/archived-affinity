import httplib2
import json
import pprint
 
h = httplib2.Http(".cache")
h.add_credentials('admin', 'admin')

# Flow statistics. 
def get_flow_stats(): 
    global h
    resp, content = h.request('http://localhost:8080/controller/nb/v2/statistics/default/flow', "GET")
    allFlowStats = json.loads(content)

    # raw dump
    print content

    # raw dumps
    json.dumps(allFlowStats, indent=2, default=str)
    s = pprint.pformat(allFlowStats, indent=4)
    print s


    for fs in allFlowStats["flowStatistics"]: 
        node = fs["node"]
        flows = fs["flowStatistic"]

        print "#### Switch = " + node["id"] + ", type = " + node["type"]
        print "# flows =  %d" % len(flows)
        for f in flows: 
            print f["flow"]["match"], "priority = ", f["flow"]["priority"]
            print "seconds = ", f["durationSeconds"], "packets = ", f["packetCount"], "bytes = ", f["byteCount"] 
            print "\t Actions:"
            for a in f["flow"]["actions"]:
                print "\t \t", a 


def get_all_nodes():
    global h
    resp, content = h.request('http://localhost:8080/controller/nb/v2/switchmanager/default/nodes', 'GET')
    nodes = json.loads(content)
    return nodes

get_flow_stats()
get_all_nodes()
