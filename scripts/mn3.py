#!/usr/bin/python

from mininet.cli import CLI
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.link import TCLink, Intf
from mininet.log import setLogLevel
from mininet.node import CPULimitedHost, Controller, RemoteController

ctrl='172.15.1.151'
# To use this topology, run:
#   > sudo ./mn1.py
# Using spaces rather than '=' is very important, as is having the --link flag
#c1 = RemoteController( 'c1', ip='192.168.56.1' )
c1 = RemoteController( 'c1', ip=ctrl )


class TestTopo(Topo):

    def __init__(self, **opts):
        Topo.__init__(self, **opts)

        # Three switches
        s1 = self.addSwitch('s1')
        s2 = self.addSwitch('s2')
        s3 = self.addSwitch('s3')

        # Connect switches into tree
        self.addLink(s2, s1, bw=20)
        self.addLink(s3, s1, bw=20)
        self.addLink(s2, s3, bw=10)

        # Four hosts
        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        h3 = self.addHost('h3')
        h4 = self.addHost('h4')

        # Connect hosts to switches
        self.addLink(h1, s2, bw=10) # These two links get limited
        self.addLink(h2, s2, bw=10)
        self.addLink(h3, s3)
        self.addLink(h4, s3)

topos = { 'net1' : (lambda: TestTopo()) }

def demo():
    "Create network"
    topo = TestTopo()
    net = Mininet(topo=topo, host=CPULimitedHost, link=TCLink, controller=lambda name:RemoteController(name, ip=ctrl, port=6633))
    net.start()
    print "*** testing basic connectivity (in-rack and off-rack)"
#    h1, h2 = net.getNodeByName('h1', 'h2')    
#    net.ping( [ h1, h2 ] )
#    net.pingAll()

    CLI( net )
#    net.stop()
    
if __name__ == '__main__':
    setLogLevel('info')
    demo()
