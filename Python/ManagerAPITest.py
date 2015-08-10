# A simple example how to connect with the Wikitude Cloud Manager API using Python.
# This example is published under Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0.html
# @author Wikitude
import json

from ManagerAPI import ManagerAPI

# The token to use when connecting to the endpoint
API_TOKEN = "<enter-your-token-here>"
API_VERSION = 2

api = ManagerAPI(API_TOKEN, API_VERSION)

# create a target colection with the name testCollection 
# and store the id which is of the target collection which is returned
print '\n\nCREATE TARGETCOLLECTION:'
result = api.createTargetCollection('testCollection')
print result
tcId = result['id']

# add a target to the target collection which was created in the previous step
print '\n\nADD TARGET:'
target = {
    "name": "TC1",
    "imageUrl": "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg"
}
result = api.addTarget(tcId, target)
targetId = result['id']
print targetId

# publish the target collection. After the target collection has been published it can be used for recognition
print '\n\nPUBLISH TARGETCOLLECTION:'
result = api.getTargetCollection(tcId)
print result