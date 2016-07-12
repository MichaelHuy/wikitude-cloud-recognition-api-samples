require 'uri'
require 'net/http'
require 'json'

# TargetsAPI shows a simple example how to interact with the Wikitude Cloud Targets API.
# This example is published under Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0.html
# @author Wikitude
class ManagerAPI

  # The endpoint where the Wikitude Cloud Targets API resides.
  @@API_ENDPOINT = 'https://api.wikitude.com'

  @@PLACEHOLDER_TC_ID       = '${TC_ID}'
  @@PLACEHOLDER_TARGET_ID   = '${TARGET_ID}'

  @@PATH_ADD_TC      = '/cloudrecognition/targetCollection'
  @@PATH_GET_TC      = '/cloudrecognition/targetCollection/${TC_ID}'
  @@PATH_GENERATE_TC = '/cloudrecognition/targetCollection/${TC_ID}/generation/cloudarchive'

  @@PATH_ADD_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target'
  @@PATH_GET_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target/${TARGET_ID}'

  # Creates a new TargetsAPI object that offers the service to interact with the Wikitude Cloud Targets API.
  # @param token: The token to use when connecting to the endpoint
  # @param version: The version of the API we will use
  def initialize(token, version)
    # The token to use when connecting to the endpoint
    @token = token
    # The version of the API we will use
    @version = version
  end
  
  private
  def sendHttpRequest(payload, method, path)
    url = @@API_ENDPOINT + path;
    uri = URI(url)
    
    http = Net::HTTP.new(uri.host, uri.port)
    http.use_ssl = true

    if method.upcase == "GET"
      req = Net::HTTP::Get.new(uri.path)
    elsif method.upcase == "POST"
      req = Net::HTTP::Post.new(uri.path)
    elsif method.upcase == "DELETE"
      req = Net::HTTP::Delete.new(uri.path)
    else
      req = Net::HTTP::Post.new(uri.path)
    end
    
    req["Content-Type"] = 'application/json'
    req["X-Token"] = @token
    req["X-Version"] = @version

    # prepare the body payload
    if payload != nil
      req.body = payload.to_json
    end

    #send the request
    response = http.start { |http| http.request(req) }

    jsonResponse = nil
    if response['Content-Type'] == "application/json" && response['Content-Length'] != "0"
      jsonResponse = JSON.parse(response.body);
    end

    if response.code == "200" || response.code == "202"
      if method.upcase == "DELETE"
        return nil
      else
        #parse the JSON object and return the WTC URL
        return jsonResponse
      end
    else
      #do some error handling! For now, we just print the response code and body
      if jsonResponse != nil
        res = jsonResponse;
        puts res["reason"] + " (" + res["code"].to_s + "): " + res["message"]
      else
        puts "Error in request (" + response.code + ")"
      end
      return nil
    end
  end

  public
  # Create target Collection with given name.
  # @param tcName target collection's name. Note that response contains an "id" 
  # tribute, which acts as unique identifier
  # @return array of the JSON representation of the created empty target collection
  def createTargetCollection(tcName)
    payload = { 'name' => tcName }
    return sendHttpRequest(payload, "POST", @@PATH_ADD_TC)
  end

  # Retrieve all created and active target collections
  # @return Array containing JSONObjects of all taregtCollection that were created
  def getAllTargetCollections()
    return sendHttpRequest(nil, "GET", @@PATH_ADD_TC)
  end

  # Rename existing target collection
  # @param tcId id of target collection
  # @param tcName new name to use for this target collection
  # @return the updated JSON representation as an array of the modified target collection
  def renameTargetCollection(tcId, tcName)
    payload = { 'name' => tcName }
    path = @@PATH_GET_TC.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    return sendHttpRequest(payload, "POST", path)
  end

   # Receive JSON representation of existing target collection (without making any modifications)
  # @param tcId id of the target collection
  # @return array of the JSON representation of target collection
  def getTargetCollection(tcId)
    path = @@PATH_GET_TC.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    return sendHttpRequest(nil, "POST", path)
  end

  # deletes existing target collection by id (NOT name)
  # @param tcId id of target collection
  # @return true on successful deletion, false otherwise
  def deleteTargetCollection(tcId)
    path = @@PATH_GET_TC.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    result = sendHttpRequest(nil, "DELETE", path)
    if result == nil
      ret = true
    else
      ret = false
    end
    return ret
  end

  # retrieve all targets from a target collection by id (NOT name)
  # @param tcId id of target collection
  # @return array of all targets of the requested target collection
  def getAllTargets(tcId)
    path = @@PATH_ADD_TARGET.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    return sendHttpRequest(nil, "GET", path)
  end

  # adds a target to an existing target collection
  # @param tcId
  # @param target array representation of target, e.g. array("name" => "TC1","imageUrl" => "http://myurl.com/image.jpeg");
  # @return array representation of created target (includes unique "id"-attribute)
  def addTarget(tcId, target)
    path = @@PATH_ADD_TARGET.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    return sendHttpRequest(target, "POST", path)
  end

  # Get target JSON of existing targetId and targetCollectionId
  # @param tcId id of target collection
  # @param targetId id of target
  # @return JSON representation of target as an array
  def getTarget(tcId, targetId)
    path = @@PATH_GET_TARGET.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    path[@@PLACEHOLDER_TARGET_ID] = targetId
    return sendHttpRequest(nil, "GET", path)
  end

  # Update target JSON properties of existing targetId and targetCollectionId
  # @param tcId id of target collection
  # @param targetId id of target
  # @param target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
  # @return JSON representation of target as an array
  def updateTarget(tcId, targetId, target)
    path = @@PATH_GET_TARGET.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    path[@@PLACEHOLDER_TARGET_ID] = targetId
    return sendHttpRequest(target, "POST", path)
  end

  # Delete existing target from a collection
  # @param tcId id of target collection
  # @param targetId id of target
  # @return true after successful deletion
  def deleteTarget(tcId, targetId)
    path = @@PATH_GET_TARGET.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    path[@@PLACEHOLDER_TARGET_ID] = targetId
    result = sendHttpRequest(nil, "DELETE", path)
    if result == nil
      ret = true
    else
      ret = false
    end
    return ret
  end

  # Gives command to start generation of given target collection. Note: Added targets will only be analized after generation.
  # @param tcId id of target collection
  # @return true on successful generation start. It will not wait until the generation is finished. The generation will take some time, depending on the amount of targets that have to be generated
  def generateTargetCollection(tcId)
    path = @@PATH_GENERATE_TC.dup
    path[@@PLACEHOLDER_TC_ID] = tcId
    return sendHttpRequest(nil, "POST", path)
  end
end