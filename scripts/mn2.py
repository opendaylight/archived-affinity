#!/usr/bin/python

from mininet.cli import CLI
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.link import TCLink, Intf
from mininet.log import setLogLevel
from mininet.node import CPULimitedHost, Controller, RemoteController

# To use this topology, run:
#   > sudo ./mn2.py
# Using spaces rather than '=' is very important, as is having the --link flag

ctrl='172.15.1.151'
c1 = RemoteController( 'c1', ip=ctrl )


class TestTopo(Topo):

    def __init__(self, **opts):
        Topo.__init__(self, **opts)

        # Three switches
        s8 = self.addSwitch('s8')
        # Four hosts
        h8 = self.addHost('h8')
        # Connect hosts to switches
        self.addLink(h8, s8, bw=10) # These two links get limited
topos = { 'net1' : (lambda: TestTopo()) }

def demo():
    "Create network"
    topo = TestTopo()
    net = Mininet(topo=topo, host=CPULimitedHost, link=TCLink, controller=lambda name:RemoteController(name, ip=ctrl, port=6633))
    net.start()
    host8 = net.getNodeByName('h8')
    host8.setIP('10.0.0.8')
    
    CLI( net )
    
if __name__ == '__main__':
    setLogLevel('info')
    demo()
