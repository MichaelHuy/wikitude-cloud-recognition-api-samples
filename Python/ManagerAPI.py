# TargetsAPI shows a simple example how to interact with the Wikitude Cloud Targets API.
#
# This example is published under Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0.html
#
# @author Wikitude

# Make sure that you have the requests-library installed
# You can get the library here: http://docs.python-requests.org/
# or install it via commandline: pip install requests

import requests
import json
import time


class APIException(Exception):
    def __init__(self, message, code=0):
        self.message = message
        self.code = code

    def __str__(self):
        return '({0}): {1}'.format(self.code, self.message)


class ServiceException(APIException):
    def __init__(self, message, code, reason):
        super(ServiceException, self).__init__(message, code)
        self.reason = reason

    def __str__(self):
        return '{0} ({1}): {2}'.format(self.reason, self.code, self.message)


class ManagerAPI:

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
    HTTP_OK         = 200
    HTTP_ACCEPTED   = 202
    HTTP_NO_CONTENT = 204

    # Creates a new TargetsAPI object that offers the service to interact with the Wikitude Cloud Targets API.
    # @param token: The token to use when connecting to the endpoint
    # @param version: The version of the API we will use
    # @param pollInterval: in milliseconds used to poll status of asynchronous operations
    def __init__(self, token, version, pollInterval=10000):
        self.token = token
        self.version = version
        self.pollInterval = pollInterval

    # Create target collection with given name.
    # @param tcName target collection's name. Note that response contains an "id" 
    #       attribute, which acts as unique identifier
    # @return array of the JSON representation of the created empty target collection
    def createTargetCollection(self, tcName):
        payload = {'name': tcName}
        return self.__sendHttpRequest('POST', ManagerAPI.PATH_ADD_TC, payload)

    # Retrieve all created and active target collections
    # @return Array containing JSONObjects of all targetCollection that were created
    def getAllTargetCollections(self):
        return self.__sendHttpRequest('GET', ManagerAPI.PATH_ADD_TC)

    # Rename existing target collection
    # @param tcId id of target collection
    # @param tcName new name to use for this target collection
    # @return the updated JSON representation as an array of the modified target collection
    def renameTargetCollection(self, tcId, tcName):
        payload = { 'name': tcName }
        path = ManagerAPI.PATH_GET_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('POST', path, payload)

    # Receive JSON representation of existing target collection (without making any modifications)
    # @param tcId id of the target collection
    # @return array of the JSON representation of target collection
    def getTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GET_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('POST', path)

    # deletes existing target collection by id (NOT name)
    # @param tcId id of target collection
    # @return True on successful deletion, raises an APIException otherwise
    def deleteTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GET_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        self.__sendHttpRequest('DELETE', path)
        return True

    # retrieve all targets from a target collection by id (NOT name)
    # @param tcId id of target collection
    # @return array of all targets of the requested target collection
    def getAllTargets(self, tcId):
        path = ManagerAPI.PATH_ADD_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest("GET", path)

    # adds a target to an existing target collection
    # @param tcId
    # @param target JSON representation of target, e.g. { "name": "TC1", "imageUrl": "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg" }
    # @return array representation of created target (includes unique "id"-attribute)
    def addTarget(self, tcId, target):
        path = ManagerAPI.PATH_ADD_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('POST', path, target)

    # adds multiple targets to an existing target collection
    # @param tcId
    # @param targets JSON representation of targets, e.g. [{ "name": "TC1", "imageUrl": "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg" }]
    # @return array representation of the status of the operation
    #      Note: this method will wait until the operation is finished, depending on the amount of targets this
    #      operation may take seconds to minutes
    def addTargets(self, tcId, targets):
        path = ManagerAPI.PATH_ADD_TARGETS.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendAsyncRequest('POST', path, targets)

    # Get target JSON of existing targetId and targetCollectionId
    # @param tcId id of target collection
    # @param targetId id of target
    # @return JSON representation of target as an array
    def getTarget(self, tcId, targetId):
        path = (ManagerAPI.PATH_GET_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_TARGET_ID, targetId)
        return self.__sendHttpRequest('GET', path)

    # Update target JSON properties of existing targetId and targetCollectionId
    # @param tcId id of target collection
    # @param targetId id of target
    # @param target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
    # @return JSON representation of target as an array
    def updateTarget(self, tcId, targetId, target):
        path = (ManagerAPI.PATH_GET_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_TARGET_ID, targetId)
        return self.__sendHttpRequest('POST', path, target)

    # Delete existing target from a collection
    # @param tcId id of target collection
    # @param targetId id of target
    # @return True on successful deletion, raises an APIException otherwise
    def deleteTarget(self, tcId, targetId):
        path = (ManagerAPI.PATH_GET_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_TARGET_ID, targetId)
        self.__sendHttpRequest('DELETE', path)
        return True

    # Gives command to start generation of given target collection. Note: Added targets will only be analyzed
    # after generation.
    # @param tcId id of target collection
    # @return array representation of the status of the operation
    #      Note: this method will wait until the operation is finished, depending on the amount of targets this
    #      operation may take seconds to minutes
    def generateTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GENERATE_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendAsyncRequest('POST', path)

    # Creates a set of up to 10 new Object Targets in an Object Target Collection in your account.
    # @param tcId The id of the Object Target Collection.
    # @param targets An array of Object Targets to create.
    # @return JSON representation of the status of the operation
    #      resolved once the operation finished, for the result the service will be polled
    #      Note: Depending on the amount of targets this operation may take from seconds to minutes
    def createObjectTargets(self, tcId, targets):
        path = ManagerAPI.PATH_CREATE_OBJECT_TARGETS.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendAsyncRequest('POST', path, targets)

    # Delete a particular Object Target from your Object Target Collection.
    # @param tcId The id of the Object Target Collection.
    # @param targetId The id of the Object Target.
    # @return Resolves with an empty response body.
    def deleteObjectTarget(self, tcId, targetId):
        path = (ManagerAPI.PATH_GET_OBJECT_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_TARGET_ID, targetId)
        return self.__sendHttpRequest('DELETE', path)

    # Request a particular Object Target of an Object Target Collection.
    # @param tcId The id of Object Target Collection.
    # @param targetId The id of the Object Target.
    # @return Resolves with the particular requested Object Target.
    def getObjectTarget(self, tcId, targetId):
        path = (ManagerAPI.PATH_GET_OBJECT_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_TARGET_ID, targetId)
        return self.__sendHttpRequest('GET', path)

    # Request all Object Targets of your account.
    # @param tcId The id of target collection.
    # @returns Resolves with an array of Object Targets of your Object Target Collection.
    def getAllObjectTargets(self, tcId):
        path = ManagerAPI.PATH_GET_ALL_OBJECT_TARGETS.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('GET', path)

    # Retrieves information status about a particular scheduled Object Target creation.
    # @param tcId The id of target collection.
    # @param generationId The id that identifies the Object Target creation.
    # @returns Resolves with the job status.
    def getObjectTargetGenerationInformation(self, tcId, generationId):
        path = (ManagerAPI.PATH_GET_OBJECT_TARGET_GENERATION_INFORMATION.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_GENERATION_ID, generationId)
        return self.__sendHttpRequest('GET', path)

    # Create a new Object Target Collection in your account.
    # @param name The name of the target collection.
    # @return resolved once target collection was added, 
    # value is JSON Object of the created empty target collection
    def createObjectTargetCollection(self, name):
        path = ManagerAPI.PATH_CREATE_OBJECT_TC
        payload = { 'name': name }
        return self.__sendHttpRequest('POST', path, payload)

    # Delete a Object Target Collection and all its Object Targets
    # @param tcId The id of the Object Target Collection.
    # @return resolved once the Object Target Collection was deleted,
    # value is an empty response body
    def deleteObjectTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GET_OBJECT_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('DELETE', path)

    # Request a particular Object Target Collection in your account.
    # @param tcId The id of the Object Target Collection.
    # @return Resolves with the particular requested Object Target Collection.
    def getObjectTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GET_OBJECT_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('GET', path)

    # Request all Object Target Collections in your account.
    # @return Resolves with an array of all Object Target Collections in your account.
    def getAllObjectTargetCollections(self):
        path = ManagerAPI.PATH_CREATE_OBJECT_TC
        return self.__sendHttpRequest('GET', path)

    # Generate a Object Target Collection and all its Object Targets as WTO.
    # @param tcId The id of the Object Target Collection.
    # @param sdkVersion Version of the Wikitude SDK to generated the file for. Valid values "7.0".
    # @param [email] Address to send email notification to after generation finished.
    def generateWto(self, tcId, sdkVersion, email):
        path = ManagerAPI.PATH_GENERATE_WTO.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        payload = { 'sdkVersion': sdkVersion, 'email': email }
        return self.__sendAsyncRequest('POST', path, payload)

    # Retrieves information about a particular scheduled wto generation.
    # @param tcId The id of the Object Target Collection.
    # @param generationId The id that identifies the Object Targets creation.
    # @return Resolves with the list of jobs.
    def getWtoGenerationStatus(self, tcId, generationId):
        path = (ManagerAPI.PATH_WTO_GENERATION_STATUS.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_GENERATION_ID, generationId)
        return self.__sendHttpRequest('GET', path)

    # Retrieves a list of asynchronous jobs sorted by creation date.
    # @param tcId The id of the Object Target Collection.
    # @returns Resolves with a list of asynchronous jobs.
    def getObjectTargetCollectionJobs(self, tcId):
        path = ManagerAPI.PATH_GET_OBJECT_TC_JOBS.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('GET', path)

    # Updates an existing Object Target Collection in your account.
    # @param tcId The id of target collection.
    # @param name The name of the target collection.
    # @param metadata Arbitrary JSON data that should be updated in the Object Target Collection.
    # @return resolved once the Object Target Collection was updated,
    # value is the JSON Object of the updated Object Target Collection
    def updateObjectTargetCollection(self, tcId, name, metadata):
        path = ManagerAPI.PATH_GET_OBJECT_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        payload = { 'name': name, 'metadata': metadata }
        return self.__sendHttpRequest('PUT', path, payload)

    # Request all projects in your account.
    # @return Resolves with an array of all projects in your account.
    def getAllProjects(self):
        path = ManagerAPI.PATH_GET_ALL_PROJECTS
        return self.__sendHttpRequest('GET', path)
    
    # Generates a greyscale image out of the input image,
    # where areas with recognition and tracking relevance are highlighted in color.
    # @param imageUrl The path to the image of which a heatmap should be created.
    # @returns Resolves with the completed heatmap generation job object.
    def generateHeatmap(imageUrl):
        path = ManagerAPI.PATH_GENERATE_HEATMAP
        payload = { 'imageUrl': imageUrl }
        return self.__sendAsyncRequest('POST', path, payload)

    # Send a request to the Wikitude Cloud Targets API.
    #
    # @param method
    #              the HTTP-method which will be used when sending the request
    # @param path
    #              the path to the service which is defined in the private variables
    # @param payload
    #              the array which will be converted to a JSON object which will be posted into the body
    def __sendHttpRequest(self, method, path, payload=None):
        response = self.__sendApiRequest(method, path, payload)
        jsonStr = None
        if self.__hasJsonContent(response):
            jsonStr = response.json()
        return jsonStr

    def __sendApiRequest(self, method, path, payload=None):
        url = ManagerAPI.API_ENDPOINT + path

        headers = {
            'Content-Type': ManagerAPI.CONTENT_TYPE_JSON,
            'X-Token': self.token,
            'X-Version': self.version
        }

        if payload is None:
            data = None
        else:
            data = json.dumps(payload)

        response = requests.request(method, url, headers=headers, data=data, verify=False)

        if self.__isResponseSuccess(response):
            return response
        else:
            raise self.__readApiError(response)

    def __isResponseSuccess(self, response):
        code = response.status_code
        return code == ManagerAPI.HTTP_OK or code == ManagerAPI.HTTP_ACCEPTED or code == ManagerAPI.HTTP_NO_CONTENT

    def __readApiError(self, response):
        if self.__hasJsonContent(response):
            return self.__readServiceException(response)
        else:
            return self.__readGeneralException(response)

    def __hasJsonContent(self, response):
        contentType = response.headers['content-type']
        contentLength = response.headers['content-length']
        return contentType == ManagerAPI.CONTENT_TYPE_JSON and contentLength != '0'

    def __readServiceException(self, response):
        error = self.__readJsonBody(response)
        code = error["code"]
        reason = error["reason"]
        message = error["message"]
        return ServiceException(message, code, reason)

    def __readJsonBody(self, response):
        return response.json()

    def __readGeneralException(self, response):
        message = response.text
        code = response.status_code
        return APIException(message, code)

    def __sendAsyncRequest(self, method, path, payload=None):
        response = self.__sendApiRequest(method, path, payload)
        location = self.__getLocation(response)
        initialDelay = self.pollInterval

        if self.__hasJsonContent(response):
            status = response.json()
            initialDelay = status['estimatedLatency']

        self.__wait(initialDelay)
        return self.__pollStatus(location)

    def __getLocation(self, response):
        return response.headers['location']

    def __wait(self, milliseconds):
        seconds = milliseconds / 1000
        time.sleep(seconds)

    def __pollStatus(self, location):
        while True:
            status = self.__readStatus(location)
            if self.__isCompleted(status):
                return status
            self.__wait(self.pollInterval)

    def __readStatus(self, location):
        response = self.__sendApiRequest('GET', location)
        return self.__readJsonBody(response)

    def __isCompleted(self, status):
        return status['status'] == 'COMPLETED'
