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
