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

var pollDelay = 1000;

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

// status codes as returned by the api
var CODE_OK         = 200;
var CODE_ACCEPTED   = 202;
var CODE_NO_CONTENT = 204;

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
     * @param name of target collection
     * @returns {Promise}
     *      resolved once target collection was added, value is JSON Object of created target collection
     */
    this.createTargetCollection = function (name) {
        var path = PATH_ADD_TC;
        var payload = { 'name' : name };

        return request("POST", path, payload)
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
        sendHttpRequest(null, 'DELETE', PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId), callback);
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
     * @returns {Promise}
     *      resolved once target image was added ( callback(error, result) ), result is JSONObject of target ('id' is unique targetId)
     */
    this.addTarget = function (tcId, target) {
        var path = PATH_ADD_TARGET.replace(PLACEHOLDER_TC_ID, tcId);

        return request('POST', path, target);
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
        sendHttpRequest(null, 'DELETE', PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId), callback);
    };

    /**
     * Generates target collection. Note: You must call this to put target image changes live. Before calling this target images are only marked ass added/removed internally
     * @param tcId target collection's unique identifier ('id'-attribute)
     * @returns {Promise}
     *      resolved once target collection was created for the result the service will be polled
     *      Note: Depending on the number of targetImages this operation may take from seconds to minutes
     */
    this.generateTargetCollection = function (tcId) {
        var path = PATH_GENERATE_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return request('POST', path)
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
 */
function sendHttpRequest(payload, method, path, callback) {

    // The configuration of the request
    var headers = {
        'X-Version' : apiVersion,
        'X-Token' : apiToken
    };
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
        headers : headers
    };
    var request_body;

    // set body content type to json, if set
    if (payload) {
        headers['Content-Type'] = 'application/json';
        request_body = JSON.stringify(payload);
    }

    // Create the request
    var request = https.request(request_options, function (response) {
        response.setEncoding('utf8');

        var statusCode = response.statusCode;
        if (isResponseStatusError(statusCode)) {
            // call was unsuccessful, callback with the error
            console.log("Unexpected StatusCode returned: " + statusCode);
            parseResponse(response, function(json) {
                callback(json.message, json);
            });
        } else {
            if (hasNoResponseBody(statusCode)) {
                callback();
            } else {
                // when we receive data, we call the callback function with err as null, and the received data
                parseResponse(response, function(json) {
                    callback(null, json);
                });
            }
        }
    });

    // On error, we call the callback with the error parameter and the error data
    request.on('error', function (e) {
        callback(e.message, e);
    });

    // write to body
    request.end(request_body);
}

function isResponseStatusError(statusCode) {
    return statusCode !== CODE_OK && statusCode !== CODE_ACCEPTED;
}

function hasNoResponseBody(statusCode) {
    return statusCode === CODE_ACCEPTED || statusCode === CODE_NO_CONTENT;
}

function parseResponse(response, callback) {
    var responseBody = "";

    response
        .on('data', function (data) {
            responseBody += data;
            if (isJsonString(responseBody)) {
                callback(JSON.parse(responseBody));
            }
        });
}

/**
 *
 * @param method
 * @param path
 * @param [payload]
 * @returns {Promise}
 */
function request( method, path, payload) {
    return new Promise(( fulfil, reject ) => {
        var headers = {
            'X-Version' : apiVersion,
            'X-Token' : apiToken
        };
        var options = {
            // We mainly use HTTPS connections to encrypt the data that is sent across the net. The rejectUnauthorized
            // property set to false avoids that the HTTPS request fails when the certificate authority is not in the
            // certificate store. If you do not want to ignore unauthorized HTTPS connections, you need to add the HTTPS
            // certificate of the api.wikitude.com server to the certificate store and make it accessible in Node.js.
            // Otherwise, you need to use a http connection instead.
            rejectUnauthorized : false,
            hostname : API_ENDPOINT_ROOT,
            path : path,
            method : method,
            headers : headers
        };
        var body;

        // set body content type to json, if set
        if (payload) {
            headers['Content-Type'] = 'application/json';
            body = JSON.stringify(payload);
        }

        // Create the request
        var request = https.request(options, fulfil);

        // On error, we call the callback with the error parameter and the error data
        request.on('error', reject);

        // write to body
        request.end(body);
    });
}

/**
 *
 * @param response
 */
function readResponse( response ) {
    response.setEncoding('utf8');

    var statusCode = response.statusCode;

    if ( statusCode === CODE_NO_CONTENT ) {
        return;
    } else if ( statusCode === CODE_ACCEPTED ) {
        return readProgress( response );
    } else if ( statusCode === CODE_OK ) {
        return readJsonBody( response )
    } else {
        return readError( response)
    }
}

function readProgress( response ) {
    var location = response.headers['Location'];

    return poll(location);
}

function poll( location ) {
    return Promise.resolve();
}

function readJsonBody( response ) {
    return readBody(response).then(JSON.parse);
}

function readBody( response ) {
    return new Promise( fulfil => {
        var body = "";

        response
            .on('data', data => {
                body += data;
            })
            .on('end', () => fulfil(body))
        ;
    });
}

function readError( response ) {
    var json = readJsonBody(response);

    throw new Error( json.message );
}