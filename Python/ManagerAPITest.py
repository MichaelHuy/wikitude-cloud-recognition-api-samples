# A simple example how to connect with the Wikitude Cloud Manager API using Python.
# This example is published under Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0.html
# @author Wikitude

from ManagerAPI import ManagerAPI
from ManagerAPI import APIException

# The token to use when connecting to the endpoint
API_TOKEN = "<enter-your-manager-token-here>"
USER_EMAIL = "<enter-your-email-here>"
EXAMPLE_OBJECT_VIDEO = "<enter-path-to-object-video-here>"

API_VERSION = "3"

api = ManagerAPI(API_TOKEN, API_VERSION)

EXAMPLE_IMAGE_URLS = [
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg",
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg"
]

try:
    # create a target colection with the name testCollection
    # and store the id which is of the target collection which is returned
    print '\n\nCreate Image Target Collection:'
    result = api.createTargetCollection('testCollection')
    print result
    tcId = result['id']

    # add multiple targets to the target collection
    print '\n\nAdd Targets:'
    targets = [{
        "name": "TC2",
        "imageUrl": EXAMPLE_IMAGE_URLS[1]
    }]
    print api.addTargets(tcId, targets)

    # publish the target collection. After the target collection has been published it can be used for recognition
    print '\n\nPublish Image Target Collection:'
    print api.generateTargetCollection(tcId)

    # clean up
    print '\n\nDelete Image Target Collection:'
    api.deleteTargetCollection(tcId)
    print 'deleted target collection: {0}'.format(tcId)
except APIException as e:
    print e

try:
    # create a target colection with the name testCollection
    # and store the id which is of the target collection which is returned
    print '\n\nCreate Object Target Collection:'
    result = api.createObjectTargetCollection('testCollection')
    print result
    tcId = result['id']

    # add multiple targets to the target collection
    print '\n\nAdd Targets:'
    targets = [{
        "name": "New Object Target",
        "resource": {
            "uri": EXAMPLE_OBJECT_VIDEO,
            "fov": 60
        },
        "metadata": {"my": "meta data"}
    }]
    print api.createObjectTargets(tcId, targets)

    # publish the target collection. After the target collection has been published it can be used for recognition
    print '\n\nCreate WTO for Object Target Collection:'
    print api.generateWto(tcId, '7.0', USER_EMAIL)

    # clean up
    print '\n\nDelete Object Target Collection:'
    api.deleteObjectTargetCollection(tcId)
    print 'deleted target collection: {0}'.format(tcId)
except APIException as e:
    print e