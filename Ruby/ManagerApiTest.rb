# A simple example how to connect with the Wikitude Cloud Manager API using Ruby.
# This example is published under Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0.html
# @author Wikitude
require_relative './ManagerAPI'

# The token to use when connecting to the endpoint
API_TOKEN = '<enter-your-manager-token-here>'
USER_EMAIL = '<enter-your-email-here>'
EXAMPLE_OBJECT_VIDEO = '<enter-path-to-object-video-here>'

API_VERSION = 3

# create the object
api = ManagerAPI.new(API_TOKEN, API_VERSION)

begin
  # create a Image Target Collection with the name myTestTC
  # and store the id which is of the target collection which is returned
  tcName = 'myTestTC'
  puts "\n\nCreate Image Target Collection:\n"
  result = api.createTargetCollection(tcName)
  puts result
  tcId = result['id']

  # add multiple targets to the Image Target Collection
  puts "\n\nAdd Targets:\n"
  targets = [
      {
          :name => 'TC2',
          :imageUrl => 'http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg'
      }
  ]
  puts api.addTargets(tcId, targets)

  # publish the Image Target Collection. After the target collection has been published it can be used for recognition
  puts "\n\nPublish Image Target Collection:\n"
  puts api.generateTargetCollection(tcId)

  # clean up
  puts "\n\nDelete Image Target Collection:\n"
  api.deleteTargetCollection(tcId)
  puts "deleted: #{tcId}"
rescue APIError => error
  puts error
end

begin
  # create an Object Target Collection with the name myTestTC
  # and store the id which is of the target collection which is returned
  tcName = 'myTestTC'
  puts "\n\nCreate Object Target Collection:\n"
  result = api.createObjectTargetCollection(tcName)
  puts result
  tcId = result['id']

  # add multiple targets to the target collection
  puts "\n\nAdd Targets:\n"
  targets = [
      {
          :name => 'New Object Target',
          :resource => {
            :uri => EXAMPLE_OBJECT_VIDEO,
            :fov => 60
          },
          :metadata => {
            :my => 'meta data'
          }
      }
  ]
  puts api.createObjectTargets(tcId, targets)

  # create a WTO file
  puts "\n\nCreate WTO for Object Target Collection:\n"
  puts api.generateWto(tcId, '7.0', USER_EMAIL)

  # clean up
  puts "\n\Delete Object Target Collection:\n"
  api.deleteObjectTargetCollection(tcId)
  puts "deleted: #{tcId}"
rescue APIError => error
  puts error
end