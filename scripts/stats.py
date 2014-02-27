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
        self.prev_t = None
        self.prev_bytes = None

        self.large_flow_threshold = 5 * 10**6 # in bytes
        self.rate_threshold = 1 * 10**5 # bits/sec

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
            self.stats = analytics.stats_link(self.al, True)
            self.protocol_stats = analytics.all_stats_link(self.al, True)
        elif (self.stat_type == Stats.TYPE_SUBNET):
            self.stats = analytics.stats_subnet("null/null", self.subnet, True)
            self.protocol_stats = analytics.all_stats_subnet("null/null", self.subnet, True)
        try:
            # Flag this as high if above high threshold, and low if below low threshold
            is_high, is_low = self.calc_ewma_rate()
#            is_fast = self.handle_rate_ewma()
#            is_big = self.check_large_flow()
            return [is_high, is_low]
        except:
            return [False, False]

    def set_high_threshold(self, s):
        self.rate_high_threshold = s;

    def set_low_threshold(self, s):
        self.rate_low_threshold = s;

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
        alpha = .75
        anomaly_threshold = 2.0
        new_bitrate = self.get_bit_rate()
        print "bitrate = %f" % (new_bitrate)

        return_val = False

        if self.rate_ewma == None:
            self.rate_ewma = new_bitrate
        else:
            self.rate_ewma = alpha * new_bitrate + (1 - alpha) * self.rate_ewma

        if (new_rate_ewma > self.rate_threshold):
            return_val = True
        return return_val

    def get_ewma_rate(self):
        alpha = .75
        return self.rate_ewma

    # EWMA calculation for bit rate.  Returns true if there is an anomaly.
    def calc_ewma_rate(self):
        alpha = .75

        is_high = False
        is_low = False

        if (self.prev_t == None or self.prev_bytes == None): 
            new_bitrate = 0
        else:
            new_bitrate = 8.0 * (self.get_bytes() - self.prev_bytes)/(self.get_duration() - self.prev_t)
#            print "Rate is now %f" % (new_bitrate)

        # Calculate ewma rate from instantaneous rate and check if it crosses threshold.
        if (self.rate_ewma == None): 
            self.rate_ewma = 0
        else: 
            self.rate_ewma = alpha * new_bitrate + (1 - alpha) * self.rate_ewma

        if (self.rate_ewma > self.rate_high_threshold): 
            print "High rate: %3.3f Mbps" % (self.rate_ewma/1000000.0)
            is_high = True
        
        if (self.rate_ewma < self.rate_low_threshold): 
            print "Low rate: %3.3f Mbps" % (self.rate_ewma/1000000.0)
            is_low = True
        
        # Update the time and bytes snapshots
        self.prev_t = self.get_duration()
        self.prev_bytes = self.get_bytes()
        return [is_high, is_low]


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

    # Duration of longest flow in group
    def get_duration(self, protocol=None):
        try:
            if (protocol == None):
                seconds = long(self.stats["duration"])
            else:
                seconds = long(self.protocol_stats[protocol]["duration"])
        except Exception as e:
            seconds = 0
        return seconds

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
