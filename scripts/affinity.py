import httplib2
import json

h = httplib2.Http(".cache")
h.add_credentials('admin', 'admin')

resp, content = h.request('http://localhost:8080/controller/nb/v2/affinity/default/affinities', "GET")
content

put_url = 'http://localhost:8080/controller/nb/v2/affinity/default/test1/192.168.1.1/192.168.1.2/isolate'
resp, content = h.request(put_url, "PUT")
resp
content

resp, content = h.request('http://localhost:8080/controller/nb/v2/affinity/default/affinities', "GET")
content

#####

resp, content = h.request('http://localhost:8080/controller/nb/v2/statistics/default/flowstats', "GET")
resp
content

post_url = 'http://localhost:8080/controller/nb/v2/affinity/default/affinity-config'
resp, content = h.request(post_url, "POST")
resp
content
