#!/usr/bin/python

import analytics

'''
Class for keeping track of host statistics
'''
class Stats:

    TYPE_HOST = 0
    TYPE_AL = 1 # AffinityLink
    TYPE_SUBNET = 2

    def __init__(self, stat_type, **kwargs):
        self.stat_type = stat_type
        if stat_type == Stats.TYPE_HOST:
            self.src = kwargs['src']
            self.dst = kwargs['dst']
        elif stat_type == Stats.TYPE_AL:
            self.al = kwargs['al']
        elif stat_type == Stats.TYPE_SUBNET:
            self.subnet = kwargs['subnet']
        else:
            print "incorrect stat type", stat_type

        self.stats = {}
        self.protocol_stats = {}
        self.rate_ewma = None
        self.large_flow_threshold = 5 * 10**6 # in bytes

    def __str__(self):
        if (self.stat_type == Stats.TYPE_HOST):
            return "host pair %s -> %s" % (self.src, self.dst)
        elif (self.stat_type == Stats.TYPE_AL):
            return "affinity link %s" % self.al
        elif (self.stat_type == Stats.TYPE_SUBNET):
            return "subnet %s" % self.subnet
        else:
            return "unknown Stats type"

    # Refresh statistics
    def refresh(self):
        if (self.stat_type == Stats.TYPE_HOST):
            self.stats = analytics.stats_hosts(self.src, self.dst, False)
            self.protocol_stats = analytics.all_stats_hosts(self.src, self.dst, False)
        elif (self.stat_type == Stats.TYPE_AL):
            self.stats = analytics.stats_link(self.al, False)
            self.protocol_stats = analytics.all_stats_link(self.al, False)
        elif (self.stat_type == Stats.TYPE_SUBNET):
            self.stats = analytics.stats_subnet("null/null", self.subnet, False)
            self.protocol_stats = analytics.all_stats_subnet("null/null", self.subnet, False)
        try:
            is_fast = self.handle_rate_ewma()
            is_big = self.check_large_flow()
            return [is_fast, is_big]
        except:
            return [False, False]

    def set_large_flow_threshold(self, s):
        self.large_flow_threshold = s;

    # Return all hosts that transferred a particular percentage of data into this entity.  Right now only supports subnets.
    def get_large_incoming_hosts(self):
        if (self.stat_type == Stats.TYPE_SUBNET):
            data = analytics.incoming_hosts(self.subnet, False)
            if (data == {}): return []
            ips = []
            total_bytes_in = self.get_bytes()
            n = len(data)
            for d in data:
                bytes_from_ip = int(d['byteCount'])
                ip = d['hostIP'].replace("/", "") # IPs sometimes (always?) get returned as strings like /1.2.3.4
                if (bytes_from_ip >= total_bytes_in / float(n)):
                    ips.append(ip)
            return ips
        else:
            print "Stat type not supported for incoming hosts"
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
        if (self.get_bytes() > self.large_flow_threshold):
            return True
        return False

    # Bytes
    def get_bytes(self, protocol=None):
        try:
            if (protocol == None):
                bytes = long(self.stats["byteCount"])
            else:
                bytes = long(self.protocol_stats[protocol]["byteCount"])
        except Exception as e:
            bytes = 0
        return bytes

    # Packets
    def get_packets(self, protocol=None):
        try:
            if (protocol == None):
                packets = long(self.stats["packetCount"])
            else:
                packets = long(self.protocol_stats[protocol]["byteCount"])
        except Exception as e:
            packets = 0
        return packets

    # Bit Rate
    def get_bit_rate(self):
        try:
            bitrate = float(self.stats["bitRate"])
        except Exception as e:
            bitrate = 0.0
        return bitrate
