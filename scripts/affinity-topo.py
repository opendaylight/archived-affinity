#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.link import TCLink

# To use this topology, run:
#   > sudo mn --custom affinity-topo.py --topo affinity --link tc
# Using spaces rather than '=' is very important, as is having the --link flag

class CustomTopo(Topo):

    def __init__(self, **opts):
        Topo.__init__(self, **opts)

        # Three switches
        s1 = self.addSwitch('s1')
        s2 = self.addSwitch('s2')
        s3 = self.addSwitch('s3')

        # Connect switches into tree
        self.addLink(s2, s1)
        self.addLink(s3, s1)

        # Four hosts
        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        h3 = self.addHost('h3')
        h4 = self.addHost('h4')
        h1.setIP('10.0.0.10')
        h1.setIP('10.0.0.20')
        h1.setIP('10.0.0.30')
        h1.setIP('10.0.0.40')


        # Connect hosts to switches
        self.addLink(h1, s2, bw=10) # These two links get limited
        self.addLink(h2, s2, bw=10)
        self.addLink(h3, s3)
        self.addLink(h4, s3)

topos = { 'affinity' : (lambda: CustomTopo()) }
