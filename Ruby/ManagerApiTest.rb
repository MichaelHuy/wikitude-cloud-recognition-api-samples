# A simple example how to connect with the Wikitude Cloud Manager API using Ruby.
# This example is published under Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0.html
# @author Wikitude
require_relative "./ManagerAPI"

# The token to use when connecting to the endpoint
API_TOKEN = "INSERT_YOUR_TOKEN_HERE"
# The version of the API we will use
API_VERSION = 1

# create the object
api = ManagerAPI.new(API_TOKEN, API_VERSION)

# create a target colection with the name myTestTC 
# and store the id which is of the target collection which is returned
tcName = "myTestTC"
puts "\n\nCREATE TARGETCOLLECTION:\n"
result = api.createTargetCollection(tcName)
puts result
tcId = result['id']

# add a target to the target collection which was created in the previous step
puts "\n\nADD TARGET:\n"
target = {
    "name" => "TC1",
    "imageUrl" => "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg"
}
puts api.addTarget(tcId, target)

# publish the target collection. After the target collection has been published it can be used for recognition
puts "\n\nPUBLISH TARGETCOLLECTION:\n"
puts api.generateTargetCollection(tcId)