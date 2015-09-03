/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Targets API.
 * 
 * This example is published under Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * @author Wikitude
 * 
 */
var https = require('https');

// Your API key
var apiToken = null;
// The version of the API we will use
var apiVersion = null;

// root url of the API
var API_ENDPOINT_ROOT       = "api.wikitude.com";

// placeholders used for url-generation
var PLACEHOLDER_TC_ID       = "${TC_ID}";
var PLACEHOLDER_TARGET_ID   = "${TARGET_ID}";
    
// paths used for manipulation of target collection and target images
var PATH_ADD_TC      = "/cloudrecognition/targetCollection";
var PATH_GET_TC      = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID;
var PATH_GENERATE_TC = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/generation/cloudarchive";
var PATH_ADD_TARGET  = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target";
var PATH_GET_TARGET  = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target/" + PLACEHOLDER_TARGET_ID;

/**
 * Creates a new TargetsAPI object that offers the service to interact with the Wikitude Cloud Targets API.
 * 
 * @param token
 *            The token to use when connecting to the endpoint
 * @param version
 *            The version of the API we will use
 */
module.exports = function (token, version) {

    // save the configured values
    apiToken = token;
    apiVersion = version;

    /**
    * Creates target collection with given name. Note: response contains unique 'id' attribute, which is required for any further modifications
    * @param tcName name of target collection
    * @param callback called once target collection was added ( callback(error, result) ), result is JSON Object of created target collection
    */
    this.createTargetCollection = function (tcName, callback) {
        var payload = { 'name' : tcName };
        sendHttpRequest(payload, 'POST', PATH_ADD_TC, callback);
    };

    /**
    * Renames given target collection
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param tcName new name
    * @param callback called once target collection was updated ( callback(error, result) ), result is JSON Object of updated target collection
    */
    this.renameTargetCollection = function (tcId, tcName, callback) {
        var payload = { 'name' : tcName };
        sendHttpRequest(payload, 'POST', PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId), callback);
    };

    /**
    * Returns all target collection of current user (uses header's token informatino, no additional information required)
    * @param callback called once target collections' information is available ( callback(error, result) ), result is JSONArray of target collection JSONObjects
    */
    this.getAllTargetCollections = function(callback) {
      sendHttpRequest(null, 'GET', PATH_ADD_TC, callback);
    };

    /**
    * Deletes given target collection including all of its target images. Note: this cannot be undone.
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param callback called once target collection was deleted ( callback(error, result) ), error and result are undefined in positive case
    */
    this.deleteTargetCollection = function (tcId, callback) {
        sendHttpRequest(null, 'DELETE', PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId), callback, true);
    };

    /**
    * Receive target collection as JSON Object
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param callback called once target collection is available ( callback(error, result) ), result is JSONObject of target collection
    */
    this.getTargetCollection = function (tcId, callback) {
        sendHttpRequest(null, 'GET', PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId), callback);
    };

    /**
    * Adds target to existing target collectin. Note: You have to call generateTargetCollection to take changes into account
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param target JSONObject of taregtImages. Must contain 'name' and 'imageUrl' attribute
    * @param callback called once target image was added ( callback(error, result) ), result is JSONObject of target ('id' is unique targetId)
    */
    this.addTarget = function (tcId, target, callback) {
        sendHttpRequest(target, 'POST', PATH_ADD_TARGET.replace(PLACEHOLDER_TC_ID, tcId), callback);
    };

    /**
    * Receive existing target image's information
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param targetId target's unique identifier ('id'-attribute)
    * @param callback called once target image was added ( callback(error, result) ), result is JSONObject of target collection
    */
    this.getTarget = function (tcId, targetId, callback) {
        sendHttpRequest(null, 'GET', PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId), callback);
    };

    /**
     * Update target JSON properties of existing targetId and targetCollectionId
     * @param tcId id of target collection
     * @param targetId id of target
     * @param target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
     * @param callback called once the target was updated, result is a JSON representation of the target as an array
     */
    this.updateTarget = function (tcId, targetId, target, callback) {
        sendHttpRequest(target, 'POST', PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId), callback);
    };

    /**
    * Deletes existing target from target collection
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param targetId target's unique identifier ('id'-attribute)
    * @param callback called once target image was deleted ( callback(error, result) ), result is JSONObject of target collection
    */
    this.deleteTarget = function (tcId, targetId, callback) {
        sendHttpRequest(null, 'DELETE', PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId), callback, true);
    };

    /**
    * Generates target collection. Note: You must call this to put target image changes live. Before calling this target images are only marked ass added/removed internally
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param callback called once target collection was created ( callback(error, result) ), error and result are undefined in positive case. Note: Depending on the number of targetImages this operation may take from seconds to minutes
    */
    this.generateTargetCollection = function (tcId, callback) {
        sendHttpRequest(null, 'GET', PATH_GENERATE_TC.replace(PLACEHOLDER_TC_ID, tcId), callback, true);
    };
};

function isJsonString(str) {
    try {
        JSON.parse(str);
    } catch (e) {
        return false;
    }
    return true;
}

/**
 * HELPER method to send request to the Wikitude API.
 * 
 * @param payload
 *            the JSON object that will be posted into the body
 * @param method 
 *            request method (POST, GET, DELETE)
 * @param path
 *            path of api end-point (appended to API_ENDPOINT_ROOT-url)
 * @param callback
 *            function called once operation finished. first param: error, second: result
 * @param checkStatusCodeOnly
 *            helper flag to ignore repsone body and only check for status code 200
 * @param callback
 *            the callback triggered when the request completes or fails. Needs to comply with the common (err, data)
 *            signature.
 */
function sendHttpRequest (payload, method, path, callback, checkStatusCodeOnly) {

    // The configuration of the request
    var request_options = {
        // We mainly use HTTPS connections to encrypt the data that is sent across the net. The rejectUnauthorized
        // property set to false avoids that the HTTPS request fails when the certificate authority is not in the
        // certificate store. If you do not want to ignore unauthorized HTTPS connections, you need to add the HTTPS
        // certificate of the api.wikitude.com server to the certificate store and make it accessible in Node.js.
        // Otherwise, you need to use a http connection instead.
        rejectUnauthorized : false,
        hostname : API_ENDPOINT_ROOT,
        path : path,
        method : method,
        headers : {
            'X-Version' : apiVersion,
            'X-Token' : apiToken
        }
    };

    var CODE_POSITIVE_200 = 200;
    var CODE_POSITIVE_202 = 202;

    // set body content type to json, if set
    if (payload) {
        request_options.headers['Content-Type'] = 'application/json';
    }

    // Create the request
    var request = https.request(request_options, function (res) {
        res.setEncoding('utf8');

        // check for the status of the response
        if (res.statusCode !== CODE_POSITIVE_200 && res.statusCode !== CODE_POSITIVE_202) {
            // call was unsuccessful, callback with the error
            console.log("Unexpected StatusCode returned: " + res.statusCode);
            callback("Error: Status code " + res.statusCode);
        } else {
            // when we receive data, we call the callback function with err as null, and the wtc URL
            if (checkStatusCodeOnly) {
                callback();
                return;
            }

            var jsonString = "";
            res.on('data', function (responseBody) {
                jsonString += responseBody;
                if(isJsonString(jsonString)) {
                    callback(null, JSON.parse(jsonString));
                }
            });
        }
    });

    // On error, we call the callback with the error parameter and the error data
    request.on('error', function (e) {
        callback(e.message, e);
    });

    // write to body
    request.end(payload ? JSON.stringify(payload) : undefined);
}
