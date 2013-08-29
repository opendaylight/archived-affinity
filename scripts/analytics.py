#!/usr/local/bin/python

import httplib2
import json
import sys

# 1. Start the controller
# 2. On the mininet VM, run:
#    > sudo mn --controller=remote,ip=192.168.56.1 --topo tree,2
#    > h1 ping h2
# 3. On the local machine (e.g., your laptop), run this script.
#    > python analytics.py
# Should see output like: "xxx bytes between 10.0.0.1 and 10.0.0.2",
# where xxx is a positive integer.

class HostStats:

    def __init__(self, src, dst):
        self.src = src
        self.dst = dst
        self.http = httplib2.Http(".cache")
        self.http.add_credentials('admin', 'admin')
        self.refresh()

    def refresh(self):
        resp, content = self.http.request("http://localhost:8080/controller/nb/v2/analytics/default/hoststats/" + self.src + "/" + self.dst, "GET")
        if (resp.status == 404):
            print "404 Error; exiting"
            sys.exit()
        if (resp.status == 503):
            print "503 Error; exiting"
            sys.exit()
        self.host_stats = json.loads(content)

    def get_bytes(self):
        try:
            bytes = long(self.host_stats["byteCount"])
        except Exception as e:
            print "exception: ", e
            bytes = None
        return bytes

    def get_bit_rate(self):

        try:
            bitrate = float(self.host_stats["bitRate"])
        except Exception as e:
            print "exception: ", e
            bitrate = None
        return bitrate

def main():
    src = "10.0.0.1"
    dst = "10.0.0.2"

    h = HostStats(src, dst)
    print("%d bytes between %s and %s" % (h.get_bytes(), src, dst))
    print("%f mbit/s between %s and %s" % (h.get_bit_rate(), src, dst))

if __name__ == "__main__":
    main()
