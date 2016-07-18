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
// interval used for polling asynchronous requests
var apiPollInterval = null;

// root url of the API
var API_ENDPOINT_ROOT       = "api.wikitude.com";

var DEFAULT_POLL_INTERVAL   = 1000;

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
var HTTP_OK         = 200;
var HTTP_ACCEPTED   = 202;
var HTTP_NO_CONTENT = 204;

/**
 * Creates a new TargetsAPI object that offers the service to interact with the Wikitude Cloud Targets API.
 *
 * @param token
 *            The token to use when connecting to the endpoint
 * @param version
 *            The version of the API we will use
 * @param [pollInterval=1000]
 *            The interval used for polling asynchronous requests
 */
module.exports = function (token, version, pollInterval) {

    // save the configured values
    apiToken = token;
    apiVersion = version;
    apiPollInterval = pollInterval || DEFAULT_POLL_INTERVAL;

    /**
     * Creates target collection with given name. Note: response contains unique 'id' attribute, which is required for any further modifications
     * @param name of target collection
     * @returns {Promise}
     *      resolved once target collection was added, value is JSON Object of created target collection
     */
    this.createTargetCollection = function (name) {
        var path = PATH_ADD_TC;
        var payload = {name};

        return sendRequest('POST', path, payload)
    };

    /**
    * Renames given target collection
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param name new name
    * @returns {Promise}
     *      resolved once target collection was updated, result is JSON Object of updated target collection
    */
    this.renameTargetCollection = function (tcId, name) {
        var path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId);
        var payload = {name};

        return sendRequest('POST', path, payload);
    };

    /**
    * Returns all target collection of current user (uses header's token informatino, no additional information required)
    * @returns {Promise}
     *      resolved once target collections' information is available, result is JSONArray of target collection JSONObjects
    */
    this.getAllTargetCollections = function() {
        var path = PATH_ADD_TC;

        return sendRequest('GET', path);
    };

    /**
     * Deletes given target collection including all of its target images. Note: this cannot be undone.
     * @param tcId target collection's unique identifier ('id'-attribute)
     * @returns {Promise}
     *      resolved once target collection was deleted
     */
    this.deleteTargetCollection = function (tcId) {
        var path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('DELETE', path);
    };

    /**
    * Receive target collection as JSON Object
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @returns {Promise}
     *      resolved once target collection is available, result is JSONObject of target collection
    */
    this.getTargetCollection = function (tcId) {
        var path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('GET', path);
    };

    /**
     * Adds target to existing target collectin. Note: You have to call generateTargetCollection to take changes into account
     * @param tcId target collection's unique identifier ('id'-attribute)
     * @param target JSONObject of taregtImages. Must contain 'name' and 'imageUrl' attribute
     * @returns {Promise}
     *      resolved once target image was added, result is JSONObject of target ('id' is unique targetId)
     */
    this.addTarget = function (tcId, target) {
        var path = PATH_ADD_TARGET.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('POST', path, target);
    };

    /**
    * Receive existing target image's information
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param targetId target's unique identifier ('id'-attribute)
    * @returns {Promise}
     *      resolved once target image was added, result is JSONObject of target collection
    */
    this.getTarget = function (tcId, targetId) {
        var path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('GET', path);
    };

    /**
     * Update target JSON properties of existing targetId and targetCollectionId
     * @param tcId id of target collection
     * @param targetId id of target
     * @param target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
     * @returns {Promise}
     *      resolved once the target was updated, result is a JSON representation of the target as an array
     */
    this.updateTarget = function (tcId, targetId, target) {
        var path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('POST', path, target);
    };

    /**
    * Deletes existing target from target collection
    * @param tcId target collection's unique identifier ('id'-attribute)
    * @param targetId target's unique identifier ('id'-attribute)
    * @returns {Promise}
     *      resolved once target image was deleted, result is JSONObject of target collection
    */
    this.deleteTarget = function (tcId, targetId) {
        var path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('DELETE', path);
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

        return sendRequest('POST', path)
    };
};

/**
 * HELPER method to send request to the Wikitude API.
 *
 * @param method
 *            request method (POST, GET, DELETE)
 * @param path
 *            path of api end-point (appended to API_ENDPOINT_ROOT-url)
 * @param [payload]
 *            the JSON object that will be posted into the body
 * @returns {Promise}
 *            resolved once operation finished
 */
function sendRequest(method, path, payload) {
    return fetch(path, method, payload).then(readResponse);
}

function fetch(path, method, payload) {
    return new Promise((fulfil, reject) => {
        var headers = {
            'X-Version': apiVersion,
            'X-Token': apiToken
        };
        var options = {
            // We mainly use HTTPS connections to encrypt the data that is sent across the net. The rejectUnauthorized
            // property set to false avoids that the HTTPS sendRequest fails when the certificate authority is not in the
            // certificate store. If you do not want to ignore unauthorized HTTPS connections, you need to add the HTTPS
            // certificate of the api.wikitude.com server to the certificate store and make it accessible in Node.js.
            // Otherwise, you need to use a http connection instead.
            rejectUnauthorized: false,
            hostname: API_ENDPOINT_ROOT,
            path,
            method,
            headers
        };
        var body;

        // set body content type to json, if set
        if (payload) {
            headers['Content-Type'] = 'application/json';
            body = JSON.stringify(payload);
        }

        // Create the request
        var request = https.request(options, fulfil);

        // On error, we reject
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

    if ( statusCode === HTTP_NO_CONTENT ) {
        return true;
    } else if ( statusCode === HTTP_ACCEPTED ) {
        return readProgress( response );
    } else if ( statusCode === HTTP_OK ) {
        return readJsonBody( response )
    } else {
        return readError( response)
    }
}

function readProgress( response ) {
    var location = response.headers['location'];

    return poll(location);
}

function poll( location ) {
    return delay(apiPollInterval)
        .then(sendRequest.bind(this, "GET", location))
        .then(progress => {
            if ( progress.status === "RUNNING") {
                return poll(location);
            } else {
                return progress;
            }
        });
}

function delay(time) {
    return new Promise( fulfil => {
        setTimeout(fulfil, time);
    })
}

function readJsonBody( response ) {
    return readBody(response).then(JSON.parse);
}

function readBody( response ) {
    return new Promise( (fulfil, reject) => {
        var body = "";

        response
            .on('data', data => {
                body += data;
            })
            .on('end', () => fulfil(body))
            .on('error', reject)
        ;
    });
}

function readError( response ) {
    return readJsonBody(response)
        .then(error => {
            throw error;
        });
}