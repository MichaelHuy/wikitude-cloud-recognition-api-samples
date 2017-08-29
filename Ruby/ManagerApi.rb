# TargetsAPI shows a simple example how to interact with the Wikitude Cloud Targets API.
#
# This example is published under Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0.html
#
# @author Wikitude

require 'uri'
require 'net/http'
require 'json'

class APIError < StandardError
  def initialize(message, code)
    @message = message
    @code = code
  end
  
  def to_s
    return "(#{@code}): #{@message}"
  end
end

class ServiceError < APIError
  def initialize(message, code, reason)
    super(message, code)
    @reason = reason
  end
  
  def to_s
    return "#{@reason} (#{@code}): #{@message}"
  end
end

class ManagerAPI
  
  # The endpoint where the Wikitude Cloud Targets API resides.
  API_ENDPOINT = 'https://api.wikitude.com'
  
  # placeholders used for url-generation
  PLACEHOLDER_TC_ID       = '${TC_ID}'
  PLACEHOLDER_TARGET_ID   = '${TARGET_ID}'
  
  # paths used for manipulation of target collection and target images
  PATH_ADD_TC      = '/cloudrecognition/targetCollection'
  PATH_GET_TC      = '/cloudrecognition/targetCollection/${TC_ID}'
  PATH_GENERATE_TC = '/cloudrecognition/targetCollection/${TC_ID}/generation/cloudarchive'
  
  PATH_ADD_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target'
  PATH_ADD_TARGETS = '/cloudrecognition/targetCollection/${TC_ID}/targets'
  PATH_GET_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target/${TARGET_ID}'
  
  PATH_CREATE_OBJECT_TARGETS = '/cloudrecognition/objectTargetCollection/${TC_ID}/targets'
  PATH_GET_OBJECT_TARGET  = '/cloudrecognition/objectTargetCollection/${TC_ID}/target/${TARGET_ID}'
  PATH_GET_ALL_OBJECT_TARGETS = '/cloudrecognition/objectTargetCollection/${TC_ID}/target'
  PATH_GET_OBJECT_TARGET_GENERATION_INFORMATION = '/cloudrecognition/objectTargetCollection/${TC_ID}/generation/target/${GENERATION_ID}'
  
  PATH_CREATE_OBJECT_TC = '/cloudrecognition/objectTargetCollection/'
  PATH_GET_OBJECT_TC = '/cloudrecognition/objectTargetCollection/${TC_ID}'
  PATH_GENERATE_WTO = '/cloudrecognition/objectTargetCollection/${TC_ID}/generation/wto'
  PATH_WTO_GENERATION_STATUS = '/cloudrecognition/objectTargetCollection/${TC_ID}/generation/wto/${GENERATION_ID}'
  PATH_GET_OBJECT_TC_JOBS = '/cloudrecognition/objectTargetCollection/${TC_ID}/jobs'
  
  PATH_GET_ALL_PROJECTS = '/cloudrecognition/projects'
  
  PATH_GENERATE_HEATMAP = '/cloudrecognition/heatmap'
  
  CONTENT_TYPE_JSON = 'application/json'
  
  # status codes as returned by the api
  HTTP_OK         = '200'
  HTTP_ACCEPTED   = '202'
  HTTP_NO_CONTENT = '204'
  
  # Creates a new TargetsAPI object that offers the service to interact with the Wikitude Cloud Targets API.
  # @param token: The token to use when connecting to the endpoint
  # @param version: The version of the API we will use
  # @param pollInterval: in milliseconds used to poll status of asynchronous operations
  def initialize(token, version, pollInterval = 10000)
    # save the configured values
    @token = token
    @version = version
    @pollInterval = pollInterval
  end
  
  public
  # Create target collection with given name. Note: response contains unique "id" attribute, which is required for any further modifications
  # @param name of the target collection
  # @return array of the JSON representation of the created empty target collection
  def createTargetCollection(name)
    path = PATH_ADD_TC
    payload = { :name => name }
    return sendRequest('POST', path, payload)
  end
  
  # Retrieve all created and active target collections
  # @return Array containing JSONObjects of all targetCollection that were created
  def getAllTargetCollections
    return sendRequest('GET', PATH_ADD_TC)
  end
  
  # Rename existing target collection
  # @param tcId id of target collection
  # @param name new name to use for this target collection
  # @return the updated JSON representation as an array of the modified target collection
  def renameTargetCollection(tcId, name)
    payload = { :name => name }
    path = PATH_GET_TC.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendRequest('POST', path, payload)
  end
  
  # Receive JSON representation of existing target collection (without making any modifications)
  # @param tcId id of the target collection
  # @return array of the JSON representation of target collection
  def getTargetCollection(tcId)
    path = PATH_GET_TC.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendRequest('POST', path)
  end
  
  # Deletes given target collection including all of its target images. Note: this cannot be undone.
  # @param tcId id of target collection
  # @return true on successful deletion, raises an APIError otherwise
  def deleteTargetCollection(tcId)
    path = PATH_GET_TC.dup
    path[PLACEHOLDER_TC_ID] = tcId
    sendRequest('DELETE', path)
    return true
  end
  
  # retrieve all targets from a target collection by id (NOT name)
  # @param tcId id of target collection
  # @return array of all targets of the requested target collection
  def getAllTargets(tcId)
    path = PATH_ADD_TARGET.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendRequest('GET', path)
  end
  
  # adds multiple targets to an existing target collection
  # @param tcId
  # @param targets array representation of targets, e.g. array(array("name" => "TC1","imageUrl" => "http://myurl.com/image.jpeg"))
  # @return array representation of the status of the operation
  #      Note: this method will wait until the operation is finished, depending on the amount of targets this
  #      operation may take seconds to minutes
  def addTargets(tcId, targets)
    path = PATH_ADD_TARGETS.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendAsyncRequest('POST', path, targets)
  end
  
  # Get target JSON of existing targetId and targetCollectionId
  # @param tcId id of target collection
  # @param targetId id of target
  # @return JSON representation of target as an array
  def getTarget(tcId, targetId)
    path = PATH_GET_TARGET.dup
    path[PLACEHOLDER_TC_ID] = tcId
    path[PLACEHOLDER_TARGET_ID] = targetId
    return sendRequest('GET', path)
  end
  
  # Update target JSON properties of existing targetId and targetCollectionId
  # @param tcId id of target collection
  # @param targetId id of target
  # @param target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
  # @return JSON representation of target as an array
  def updateTarget(tcId, targetId, target)
    path = PATH_GET_TARGET.dup
    path[PLACEHOLDER_TC_ID] = tcId
    path[PLACEHOLDER_TARGET_ID] = targetId
    return sendRequest('POST', path, target)
  end
  
  # Delete existing target from a collection
  # @param tcId id of target collection
  # @param targetId id of target
  # @return true on successful deletion, raises an APIError otherwise
  def deleteTarget(tcId, targetId)
    path = PATH_GET_TARGET.dup
    path[PLACEHOLDER_TC_ID] = tcId
    path[PLACEHOLDER_TARGET_ID] = targetId
    sendRequest('DELETE', path)
    return true
  end
  
  # Gives command to start generation of given target collection. Note: Added targets will only be analyzed after generation.
  # @param tcId id of target collection
  # @return array representation of the status of the operation
  #      Note: this method will wait until the operation is finished, depending on the amount of targets this
  #      operation may take seconds to minutes
  def generateTargetCollection(tcId)
    path = PATH_GENERATE_TC.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendAsyncRequest('POST', path)
  end
  
  # Creates a set of up to 10 new Object Targets in an Object Target Collection in your account.
  # @param {string} tcId The id of the Object Target Collection.
  # @param {JSONObject[]} targets An array of Object Targets to create.
  # @returns {Promise} JSON representation of the status of the operation
  #      resolved once the operation finished, for the result the service will be polled
  #      Note: Depending on the amount of targets this operation may take from seconds to minutes
  def createObjectTargets(tcId, targets) 
    path = PATH_CREATE_OBJECT_TARGETS.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendAsyncRequest('POST', path, targets)
  end
  
  # Delete a particular Object Target from your Object Target Collection.
  # @param {string} tcId The id of the Object Target Collection.
  # @param {string} targetId The id of the Object Target.
  # @returns {Promise}
  #      Resolves with an empty response body.
  def deleteObjectTarget(tcId, targetId) 
    path = PATH_GET_OBJECT_TARGET.dup
    path[PLACEHOLDER_TC_ID] =  tcId
    path[PLACEHOLDER_TARGET_ID] = targetId
    return sendRequest('DELETE', path)
  end
  
  # Request a particular Object Target of an Object Target Collection.
  # @param {string} tcId The id of Object Target Collection.
  # @param {string} targetId The id of the Object Target.
  # @returns {Promise}
  #      Resolves with the particular requested Object Target.
  def getObjectTarget(tcId, targetId)
    path = PATH_GET_OBJECT_TARGET.dup
    path[PLACEHOLDER_TC_ID] = tcId
    path[PLACEHOLDER_TARGET_ID] = targetId
    return sendRequest('GET', path)
  end
  
  # Request all Object Targets of your account.
  # @param {string} tcId The id of target collection.
  # @returns {Promise}
  #      Resolves with an array of Object Targets of your Object Target Collection.
  def getAllObjectTargets(tcId)
    path = PATH_GET_ALL_OBJECT_TARGETS.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendRequest('GET', path)
  end
  
  # Retrieves information status about a particular scheduled Object Target creation.
  # @param {string} tcId The id of target collection.
  # @param {string} generationId The id that identifies the Object Target creation.
  # @returns {Promise}
  #      Resolves with the job status.
  def getObjectTargetGenerationInformation(tcId, generationId)
    path = PATH_GET_OBJECT_TARGET_GENERATION_INFORMATION.dup
    path[PLACEHOLDER_TC_ID] = tcId
    path[PLACEHOLDER_GENERATION_ID] = generationId
    return sendRequest('GET', path)
  end
  
  # Create a new Object Target Collection in your account.
  # @param {string} name The name of the target collection.
  # @returns {Promise}
  #      resolved once target collection was added, value is JSON Object of the created empty target collection
  def createObjectTargetCollection(name) 
    path = PATH_CREATE_OBJECT_TC.dup
    payload = { :name => name }
    return sendRequest('POST', path, payload)
  end
  
  # Delete a Object Target Collection and all its Object Targets
  # @param {string} tcId The id of the Object Target Collection.
  # @returns {Promise}
  #      resolved once the Object Target Collection was deleted,
  #      value is an empty response body
  def deleteObjectTargetCollection(tcId)
    path = PATH_GET_OBJECT_TC.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendRequest('DELETE', path)
  end
  
  # Request a particular Object Target Collection in your account.
  # @param {string} tcId The id of the Object Target Collection.
  # @returns {Promise}
  #      Resolves with the particular requested Object Target Collection.
  def getObjectTargetCollection(tcId)
    path = PATH_GET_OBJECT_TC.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendRequest('GET', path)
  end
  
  # Request all Object Target Collections in your account.
  # @returns {Promise}
  #      Resolves with an array of all Object Target Collections in your account.
  def getAllObjectTargetCollections()
    path = PATH_CREATE_OBJECT_TC.dup
    return sendRequest('GET', path)
  end
  
  # Generate a Object Target Collection and all its Object Targets as WTO.
  # @param {string} tcId The id of the Object Target Collection.
  # @param {string} sdkVersion Version of the Wikitude SDK to generated the file for. Valid values "7.0".
  # @param {string} [email] Address to send email notification to after generation finished.
  def generateWto(tcId, sdkVersion, email)
    path = PATH_GENERATE_WTO.dup
    path[PLACEHOLDER_TC_ID] = tcId
    payload = { :sdkVersion => sdkVersion, :email => email }
    return sendAsyncRequest('POST', path, payload)
  end
  
  # Retrieves information about a particular scheduled wto generation.
  # @param {string} tcId The id of the Object Target Collection.
  # @param {string} generationId The id that identifies the Object Targets creation.
  # @returns {Promise}
  #      Resolves with the list of jobs.
  def getWtoGenerationStatus(tcId, generationId)
    path = PATH_WTO_GENERATION_STATUS.dup
    path[PLACEHOLDER_TC_ID] = tcId
    path[PLACEHOLDER_GENERATION_ID] = generationId
    return sendRequest('GET', path)
  end
  
  # Retrieves a list of asynchronous jobs sorted by creation date.
  # @param {string} tcId The id of the Object Target Collection.
  # @returns {Promise}
  #      Resolves with a list of asynchronous jobs.
  def getObjectTargetCollectionJobs(tcId)
    path = PATH_GET_OBJECT_TC_JOBS.dup
    path[PLACEHOLDER_TC_ID] = tcId
    return sendRequest('GET', path)
  end
  
  # Updates an existing Object Target Collection in your account.
  # @param {string} tcId The id of target collection.
  # @param {string} name The name of the target collection.
  # @param {JSONObject} metadata Arbitrary JSON data that should be updated in the Object Target Collection.
  # @returns {Promise}
  #      resolved once the Object Target Collection was updated,
  #      value is the JSON Object of the updated Object Target Collection
  def updateObjectTargetCollection(tcId, name, metadata)
    path = PATH_GET_OBJECT_TC.dup
    path[PLACEHOLDER_TC_ID] = tcId
    payload = { :name => name, :metadata => metadata }
    return sendRequest('PUT', path, payload)
  end
  
  # Request all projects in your account.
  # @returns {Promise}
  #      Resolves with an array of all projects in your account.
  def getAllProjects()
    path = PATH_GET_ALL_PROJECTS.dup
    return sendRequest('GET', path)
  end
  
  # Generates a greyscale image out of the input image,
  # where areas with recognition and tracking relevance are highlighted in color.
  # @param {string} imageUrl The path to the image of which a heatmap should be created.
  # @returns {Promise}
  #      Resolves with the completed heatmap generation job object.
  def generateHeatmap(imageUrl)
    path = PATH_GENERATE_HEATMAP.dup
    payload = { :imageUrl => imageUrl }
    return sendAsyncRequest('POST', path, payload)
  end
  
  private
  # HELPER method to send request to the Wikitude API.
  # @param [String] method
  # @param [String] path
  # @param payload
  def sendRequest(method, path, payload = nil)
    response = sendAPIRequest(method, path, payload)
    
    jsonResponse = nil
    if hasJsonContent(response)
      jsonResponse = readJsonBody(response)
    end
    
    return jsonResponse
  end
  
  def sendAPIRequest(method, path, payload = nil)
    url = API_ENDPOINT + path
    uri = URI(url)
    
    http = Net::HTTP.new(uri.host, uri.port)
    http.use_ssl = true
    
    if method.upcase == 'GET'
      request = Net::HTTP::Get.new(uri.path)
    elsif method.upcase == 'POST'
      request = Net::HTTP::Post.new(uri.path)
    elsif method.upcase == 'DELETE'
      request = Net::HTTP::Delete.new(uri.path)
    else
      request = Net::HTTP::Post.new(uri.path)
    end
    
    request['Content-Type'] = CONTENT_TYPE_JSON
    request['X-Token'] = @token
    request['X-Version'] = @version
    
    # prepare the body payload
    if payload != nil
      request.body = payload.to_json
    end
    
    #send the request
    response = http.start { |client| client.request(request) }
    
    if isResponseSuccess(response)
      return response
    else
      raise readAPIError(response)
    end
  end
  
  def isResponseSuccess(response)
    code = response.code
    return code == HTTP_OK || code == HTTP_ACCEPTED || code == HTTP_NO_CONTENT
  end
  
  def readAPIError(response)
    if hasJsonContent(response)
      return readServiceError(response)
    else
      return readGeneralError(response)
    end
  end
  
  def hasJsonContent(response)
    contentType = response['Content-Type']
    contentLength = response['Content-Length']
    return contentType == CONTENT_TYPE_JSON && contentLength != '0'
  end
  
  def readServiceError(response)
    error = readJsonBody(response)
    message = error['message']
    code = error['code']
    reason = error['reason']
    return ServiceError.new(message, code, reason)
  end
  
  def readJsonBody(response)
    return JSON.parse(response.body)
  end
  
  def readGeneralError(response)
    message = response.body
    code = response.code
    return APIError.new(message, code)
  end
  
  # @param [String] method
  # @param [String] path
  # @param payload
  def sendAsyncRequest(method, path, payload = nil)
    response = sendAPIRequest(method, path, payload)
    location = getLocation(response)
    initialDelay = @pollInterval
    
    if hasJsonContent(response)
      status = readJsonBody(response)
      initialDelay = status['estimatedLatency']
    end
    
    wait(initialDelay)
    return pollStatus(location)
  end
  
  def getLocation(response)
    return response['Location']
  end
  
  def wait(milliseconds)
    seconds = milliseconds / 1000
    sleep(seconds)
  end
  
  def pollStatus(location)
    loop do
      status = readStatus(location)
      if isCompleted(status)
        return status
      end
      wait(@pollInterval)
    end
  end
  
  def readStatus(location)
    response = sendAPIRequest('GET', location)
    return readJsonBody(response)
  end
  
  def isCompleted(status)
    return status['status'] == 'COMPLETED'
  end
end