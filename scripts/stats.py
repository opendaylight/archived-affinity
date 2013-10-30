#!/usr/bin/python

import httplib2
import json

'''
Class for keeping track of host stats or affinity link stats, depending.
'''
class Stats:

    def __init__(self, stat_type, **kwargs):
        self.stat_type = stat_type
        if stat_type == "host":
            self.src = kwargs['src']
            self.dst = kwargs['dst']
            self.url_prefix = "http://localhost:8080/affinity/nb/v2/analytics/default/hoststats/" 
        elif stat_type == "affinityLink":
            self.al = kwargs['al']
            self.url_prefix = "http://localhost:8080/affinity/nb/v2/analytics/default/affinitylinkstats/"
        elif stat_type == "prefix":
            self.subnet = kwargs['subnet']
            self.url_prefix = "http://localhost:8080/affinity/nb/v2/analytics/default/prefixstats/"
        else:
            print "incorrect stat type", stat_type

        self.stats = {}
        self.rate_ewma = None
        self.http = httplib2.Http(".cache")
        self.http.add_credentials('admin', 'admin')

    def __str__(self):
        if (self.stat_type == "host"):
            return "host pair %s -> %s" % (self.src, self.dst)
        elif (self.stat_type == "affinityLink"):
            return "AffinityLink %s" % self.al
        elif (self.stat_type == "prefix"):
            return "Prefix %s" % self.subnet
        else:
            return "Unknown Stats type"

    # Refresh statistics
    def refresh(self):
        if (self.stat_type == "host"):
            resp, content = self.http.request(self.url_prefix + self.src + "/" + self.dst, "GET")
        elif (self.stat_type == "affinityLink"):
            resp, content = self.http.request(self.url_prefix + self.al, "GET")
        elif (self.stat_type == "prefix"):
            resp, content = self.http.request(self.url_prefix + self.subnet, "GET")
        try:
            self.stats = json.loads(content)
            is_fast = self.handle_rate_ewma()
            is_big = self.check_large_flow()
            return [is_fast, is_big]
        except Exception as e:
            print "error: ", e
            return [False, False]

    # Return hosts that transferred data into this entity.  Right now only supports prefixes.
    def get_incoming_hosts(self):
        if (self.stat_type == "prefix"):
            resp, content = self.http.request(self.url_prefix + "incoming/" + self.subnet, "GET")
            data = json.loads(content)
            if (data != {}):
                # IPs sometimes (always?) get returned as strings like /1.2.3.4; strip off the leading /
                ips = [h.replace("/", "") for h in data['hosts']]
                return ips
        return []

    # EWMA calculation for bit rate.  Returns true if there is an anomaly.
    def handle_rate_ewma(self):
        alpha = .25
        anomaly_threshold = 2.0
        new_bitrate = self.get_bit_rate()

        return_val = False

        if self.rate_ewma == None:
            self.rate_ewma = new_bitrate
        else:
            new_rate_ewma = alpha * new_bitrate + (1 - alpha) * self.rate_ewma
            if (self.rate_ewma > 0 and new_rate_ewma > anomaly_threshold * self.rate_ewma):
                return_val = True
            self.rate_ewma = new_rate_ewma
        return return_val

    # Returns true if this is a large flow
    def check_large_flow(self):
        if (self.get_bytes() > 5 * (10**6)):
            return True
        return False

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
