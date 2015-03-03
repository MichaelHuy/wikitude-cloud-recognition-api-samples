# Make sure that you have the requests-library installed
# You can get the library here: http://docs.python-requests.org/
# or install it via commandline: pip install requests
import requests
import json

class ManagerAPI:

    API_ENDPOINT = 'https://api.wikitude.com/cloudrecognition'

    PLACEHOLDER_TC_ID       = '${TC_ID}'
    PLACEHOLDER_TARGET_ID   = '${TARGET_ID}'

    PATH_ADD_TC      = '/targetCollection'
    PATH_GET_TC      = '/targetCollection/${TC_ID}'
    PATH_GENERATE_TC = '/targetCollection/${TC_ID}/generation'

    PATH_ADD_TARGET  = '/targetCollection/${TC_ID}/target'
    PATH_GET_TARGET  = '/targetCollection/${TC_ID}/target/${TARGET_ID}'

    def __init__(self, token, version):
        self.token = token
        self.version = version

     # Send the POST request to the Wikitude Cloud Targets API.
     # 
     # @param payload
     #              the array which will be converted to a JSON object which will be posted into the body
     # @param method
     #              the HTTP-method which will be used when sending the request
     # @param path
     #              the path to the service which is defined in the private variables
    def __sendHttpRequest(self, payload, method, path):
        url = ManagerAPI.API_ENDPOINT + path;

        headers = {
            'Content-Type' : 'application/json',
            'X-Token' : self.token,
            'X-Version' : self.version
        }

        if method == 'GET' or method == 'DELETE':
            data = None
        else:
            data = json.dumps(payload)

        res = requests.request(method, url, headers=headers, data=data)
        if res.status_code == 200 and method != 'DELETE':
            return res.json()
        else:
            return None

    # Create target Collection with given name.
    # @param tcName target collection's name. Note that response contains an "id" 
    # tribute, which acts as unique identifier
    # @return array of the JSON representation of the created empty target collection
    def createTargetCollection(self, tcName):
        payload = { 'name': tcName }
        return self.__sendHttpRequest(payload, 'POST', ManagerAPI.PATH_ADD_TC)

    # Retrieve all created and active target collections
    # @return Array containing JSONObjects of all taregtCollection that were created
    def getAllTargetCollections(self):
        return self.__sendHttpRequest('', 'GET', ManagerAPI.PATH_ADD_TC)

    # Rename existing target collection
    # @param tcId id of target collection
    # @param tcName new name to use for this target collection
    # @return the updated JSON representation as an array of the modified target collection
    def renameTargetCollection(self, tcId, tcName):
        payload = { 'name': tcName }
        path = ManagerAPI.PATH_GET_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest(payload, 'POST', path)

    # Receive JSON representation of existing target collection (without making any modifications)
    # @param tcId id of the target collection
    # @return array of the JSON representation of target collection
    def getTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GET_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('', 'POST', path)

    # deletes existing target collection by id (NOT name)
    # @param tcId id of target collection
    # @return true on successful deletion, false otherwise
    def deleteTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GET_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        result = self.__sendHttpRequest('', 'DELETE', path)
        if result == None:
            ret = True
        else:
            ret = False
        return ret

    # retrieve all targets from a target collection by id (NOT name)
    # @param tcId id of target collection
    # @return array of all targets of the requested target collection
    def getAllTargets(self, tcId):
        path = ManagerAPI.PATH_ADD_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('', "GET", path)

    # adds a target to an existing target collection
    # @param tcId
    # @param target JSON representation of target, e.g. { "name": "TC1", "imageUrl": "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg" }
    # @return array representation of created target (includes unique "id"-attribute)
    def addTarget(self, tcId, target):
        path = ManagerAPI.PATH_ADD_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest(target, 'POST', path)

    # Get target JSON of existing targetId and targetCollectionId
    # @param tcId id of target collection
    # @param targetId id of target
    # @return JSON representation of target as an array
    def getTarget(self, tcId, targetId):
        path = (ManagerAPI.PATH_GET_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_TARGET_ID, targetId)
        return self.__sendHttpRequest('', "GET", path)

    # Delete existing target from a collection
    # @param tcId id of target collection
    # @param targetId id of target
    # @return true after successful deletion
    def deleteTarget(self, tcId, targetId):
        path = (ManagerAPI.PATH_GET_TARGET.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)).replace(ManagerAPI.PLACEHOLDER_TARGET_ID, targetId)
        result = self.__sendHttpRequest('', 'DELETE', path)
        if result == None:
            ret = True
        else:
            ret = False
        return ret

    # Gives command to start generation of given target collection. Note: Added targets will only be analized after generation.
    # @param tcId id of target collection
    # @return true on successful generation start. It will not wait until the generation is finished. The generation will take some time, depending on the amount of targets that have to be generated
    def generateTargetCollection(self, tcId):
        path = ManagerAPI.PATH_GENERATE_TC.replace(ManagerAPI.PLACEHOLDER_TC_ID, tcId)
        return self.__sendHttpRequest('', 'POST', path)
