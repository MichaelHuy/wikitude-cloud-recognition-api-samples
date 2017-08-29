/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Cloud Targets API.
 *
 * This example is published under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * @author Wikitude
 *
 */

"use strict";
var https = require('https');

class APIError extends Error {
    constructor(message, code) {
        super(message);
        this.code = code;
    }

    toString() {
        return `(${this.code}): ${this.message}`;
    }
}

class ServiceError extends APIError {
    constructor(message, code, reason) {
        super(message, code);
        this.reason = reason;
    }

    toString() {
        return `${this.reason} (${this.code}): ${this.message}`;
    }
}

// The endpoint where the Wikitude Cloud Targets API resides.
var API_ENDPOINT_ROOT       = "api.wikitude.com";

// placeholders used for url-generation
var PLACEHOLDER_TC_ID       = "${TC_ID}";
var PLACEHOLDER_TARGET_ID   = "${TARGET_ID}";
var PLACEHOLDER_GENERATION_ID   = "${GENERATION_ID}";

// paths used for manipulation of target collection and target images
var PATH_ADD_TC      = "/cloudrecognition/targetCollection";
var PATH_GET_TC      = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID;
var PATH_GENERATE_TC = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/generation/cloudarchive";

var PATH_ADD_TARGET  = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target";
var PATH_ADD_TARGETS = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/targets";
var PATH_GET_TARGET  = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target/" + PLACEHOLDER_TARGET_ID;

var PATH_CREATE_OBJECT_TARGETS = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID + "/targets";
var PATH_GET_OBJECT_TARGET  = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID + "/target/" + PLACEHOLDER_TARGET_ID;
var PATH_GET_ALL_OBJECT_TARGETS = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID + "/target";
var PATH_GET_OBJECT_TARGET_GENERATION_INFORMATION = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID + "/generation/target/" + PLACEHOLDER_GENERATION_ID;

var PATH_CREATE_OBJECT_TC = "/cloudrecognition/objectTargetCollection/";
var PATH_GET_OBJECT_TC = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID;
var PATH_GENERATE_WTO = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID + "/generation/wto";
var PATH_WTO_GENERATION_STATUS = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID + "/generation/wto/" + PLACEHOLDER_GENERATION_ID;
var PATH_GET_OBJECT_TC_JOBS = "/cloudrecognition/objectTargetCollection/" + PLACEHOLDER_TC_ID + "/jobs";

var PATH_GET_ALL_PROJECTS = "/cloudrecognition/projects";

var PATH_GENERATE_HEATMAP = "/cloudrecognition/heatmap";

// status codes as returned by the api
var HTTP_OK         = 200;
var HTTP_ACCEPTED   = 202;
var HTTP_NO_CONTENT = 204;

var HEADER_LOCATION = "location";
var CONTENT_TYPE_JSON = "application/json";

// Your API key
var apiToken = null;
// The version of the API we will use
var apiVersion = null;
// interval used to poll status of asynchronous operations
var apiPollInterval = null;

/**
 * @class ManagerAPI
 */
module.exports = class ManagerAPI {
    /**
     * Creates a new ManagerAPI object that offers the service to interact with the Wikitude Cloud Targets API.
     *
     * @param {string} token The token to use when connecting to the endpoint
     * @param {number} version The version of the API we will use
     * @param {number} [pollInterval=10000] in milliseconds used to poll status of asynchronous operations
     */
    constructor(token, version, pollInterval) {
        // save the configured values
        apiToken = token;
        apiVersion = version;
        apiPollInterval = pollInterval || 10000;
    }

    /**
     * Create target collection with given name. Note: response contains unique "id" attribute, which is required for any further modifications
     * @param name of the target collection
     * @returns {Promise}
     *      resolved once target collection was added, value is JSON Object of the created empty target collection
     */
    createTargetCollection(name) {
        var path = PATH_ADD_TC;
        var payload = {name};

        return sendRequest('POST', path, payload);
    }

    /**
     * Retrieve all created and active target collections
     * @returns {Promise}
     *      resolved once target collections' information is available, result is JSONArray of target collection JSONObjects
     */
    getAllTargetCollections() {
        return sendRequest('GET', PATH_ADD_TC);
    }

    /**
     * Rename existing target collection
     * @param {string} tcId id of target collection
     * @param {string} name new name to use for this target collection
     * @returns {Promise}
     *      resolved once target collection was updated, result is JSON Object of the modified  target collection
     */
    renameTargetCollection(tcId, name) {
        var path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId);
        var payload = {name};

        return sendRequest('POST', path, payload);
    }

    /**
     * Receive target collection as JSON Object (without making any modifications)
     * @param {string} tcId id of the target collection
     * @returns {Promise}
     *      resolved once target collection is available, result is JSON Object of target collection
     */
    getTargetCollection(tcId) {
        var path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('GET', path);
    }

    /**
     * Deletes given target collection including all of its target images. Note: this cannot be undone.
     * @param {string} tcId id of target collection
     * @returns {Promise}
     *      resolved once target collection was deleted
     */
    deleteTargetCollection(tcId) {
        var path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('DELETE', path);
    }

    /**
     * retrieve all targets from a target collection by id (NOT name)
     * @param {string} tcId id of target collection
     * @returns {Promise}
     *      resolved once targets are available, result is Array of all targets of requested target collection
     */
    getAllTargets(tcId) {
        var path = PATH_ADD_TARGET.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('GET', path);
    }

    /**
     * Adds targets to existing target collection. Note: You have to call generateTargetCollection to take changes into account
     * @param {string} tcId id of target collection
     * @param targets Array of JSONObjects of targetImages. Must contain 'name' and 'imageUrl' attribute
     * @returns {Promise} JSON representation of the status of the operation
     *      resolved once the operation finished, for the result the service will be polled
     *      Note: Depending on the amount of targets this operation may take from seconds to minutes
     */
    addTargets(tcId, targets) {
        var path = PATH_ADD_TARGETS.replace(PLACEHOLDER_TC_ID, tcId);

        return sendAsyncRequest('POST', path, targets);
    }

    /**
     * Receive existing target image's information
     * @param {string} tcId id of target collection
     * @param {string} targetId id of target
     * @returns {Promise}
     *      resolved once target image was added, result is JSONObject of target collection
     */
    getTarget(tcId, targetId) {
        var path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('GET', path);
    }

    /**
     * Update target JSON properties of existing targetId and targetCollectionId
     * @param {string} tcId id of target collection
     * @param {string} targetId id of target
     * @param {Object} target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
     * @returns {Promise}
     *      resolved once the target was updated, result is a JSON representation of the target as an array
     */
    updateTarget(tcId, targetId, target) {
        var path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('POST', path, target);
    }

    /**
     * Deletes existing target from a target collection
     * @param {string} tcId id of target collection
     * @param {string} targetId id of target
     * @returns {Promise}
     *      resolved once target image was deleted
     */
    deleteTarget(tcId, targetId) {
        var path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('DELETE', path);
    }

    /**
     * Generates target collection. Note: You must call this to put target image changes live. Before calling this target images are only marked ass added/removed internally
     * @param {string} tcId id of target collection
     * @returns {Promise} JSON representation of the status of the operation
     *      resolved once the operation finished, for the result the service will be polled
     *      Note: Depending on the amount of targets this operation may take from seconds to minutes
     */
    generateTargetCollection(tcId) {
        var path = PATH_GENERATE_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendAsyncRequest('POST', path);
    }

    /**
     * Creates a set of up to 10 new Object Targets in an Object Target Collection in your account.
     * @param {string} tcId The id of the Object Target Collection.
     * @param {JSONObject[]} targets An array of Object Targets to create.
     * @returns {Promise} JSON representation of the status of the operation
     *      resolved once the operation finished, for the result the service will be polled
     *      Note: Depending on the amount of targets this operation may take from seconds to minutes
     */
    createObjectTargets(tcId, targets) {
        var path = PATH_CREATE_OBJECT_TARGETS.replace(PLACEHOLDER_TC_ID, tcId);

        return sendAsyncRequest('POST', path, targets);
    }

    /**
     * Delete a particular Object Target from your Object Target Collection.
     * @param {string} tcId The id of the Object Target Collection.
     * @param {string} targetId The id of the Object Target.
     * @returns {Promise}
     *      Resolves with an empty response body.
     */
    deleteObjectTarget(tcId, targetId) {
        var path = PATH_GET_OBJECT_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('DELETE', path);
    }

    /**
     * Request a particular Object Target of an Object Target Collection.
     * @param {string} tcId The id of Object Target Collection.
     * @param {string} targetId The id of the Object Target.
     * @returns {Promise}
     *      Resolves with the particular requested Object Target.
     */
    getObjectTarget(tcId, targetId) {
        var path = PATH_GET_OBJECT_TARGET.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_TARGET_ID, targetId);

        return sendRequest('GET', path);
    }

    /**
     * Request all Object Targets of your account.
     * @param {string} tcId The id of target collection.
     * @returns {Promise}
     *      Resolves with an array of Object Targets of your Object Target Collection.
     */
    getAllObjectTargets(tcId) {
        var path = PATH_GET_ALL_OBJECT_TARGETS.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('GET', path);
    }

    /**
     * Retrieves information status about a particular scheduled Object Target creation.
     * @param {string} tcId The id of target collection.
     * @param {string} generationId The id that identifies the Object Target creation.
     * @returns {Promise}
     *      Resolves with the job status.
     */
    getObjectTargetGenerationInformation(tcId, generationId) {
        var path = PATH_GET_OBJECT_TARGET_GENERATION_INFORMATION.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_GENERATION_ID, generationId);

        return sendRequest('GET', path);
    }

    /**
     * Create a new Object Target Collection in your account.
     * @param {string} name The name of the target collection.
     * @returns {Promise}
     *      resolved once target collection was added, value is JSON Object of the created empty target collection
     */
    createObjectTargetCollection(name) {
        var path = PATH_CREATE_OBJECT_TC;

        return sendRequest('POST', path, {name});
    }

    /**
     * Delete a Object Target Collection and all its Object Targets
     * @param {string} tcId The id of the Object Target Collection.
     * @returns {Promise}
     *      resolved once the Object Target Collection was deleted,
     *      value is an empty response body
     */
    deleteObjectTargetCollection(tcId) {
        var path = PATH_GET_OBJECT_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('DELETE', path);
    }

    /**
     * Request a particular Object Target Collection in your account.
     * @param {string} tcId The id of the Object Target Collection.
     * @returns {Promise}
     *      Resolves with the particular requested Object Target Collection.
     */
    getObjectTargetCollection(tcId) {
        var path = PATH_GET_OBJECT_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('GET', path);
    }

    /**
     * Request all Object Target Collections in your account.
     * @returns {Promise}
     *      Resolves with an array of all Object Target Collections in your account.
     */
    getAllObjectTargetCollections() {
        var path = PATH_CREATE_OBJECT_TC;

        return sendRequest('GET', path);
    }

    /**
     * Generate a Object Target Collection and all its Object Targets as WTO.
     * @param {string} tcId The id of the Object Target Collection.
     * @param {string} sdkVersion Version of the Wikitude SDK to generated the file for. Valid values "7.0".
     * @param {string} [email] Address to send email notification to after generation finished.
     */
    generateWto(tcId, sdkVersion, email) {
        var path = PATH_GENERATE_WTO.replace(PLACEHOLDER_TC_ID, tcId);

        return sendAsyncRequest('POST', path, {sdkVersion, email});
    }

    /**
     * Retrieves information about a particular scheduled wto generation.
     * @param {string} tcId The id of the Object Target Collection.
     * @param {string} generationId The id that identifies the Object Targets creation.
     * @returns {Promise}
     *      Resolves with the list of jobs.
     */
    getWtoGenerationStatus(tcId, generationId) {
        var path = PATH_WTO_GENERATION_STATUS.replace(PLACEHOLDER_TC_ID, tcId).replace(PLACEHOLDER_GENERATION_ID, generationId);

        return sendRequest('GET', path);
    }

    /**
     * Retrieves a list of asynchronous jobs sorted by creation date.
     * @param {string} tcId The id of the Object Target Collection.
     * @returns {Promise}
     *      Resolves with a list of asynchronous jobs.
     */
    getObjectTargetCollectionJobs(tcId) {
        var path = PATH_GET_OBJECT_TC_JOBS.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('GET', path);
    }

    /**
     * Updates an existing Object Target Collection in your account.
     * @param {string} tcId The id of target collection.
     * @param {string} name The name of the target collection.
     * @param {JSONObject} metadata Arbitrary JSON data that should be updated in the Object Target Collection.
     * @returns {Promise}
     *      resolved once the Object Target Collection was updated,
     *      value is the JSON Object of the updated Object Target Collection
     */
    updateObjectTargetCollection(tcId, name, metadata) {
        var path = PATH_GET_OBJECT_TC.replace(PLACEHOLDER_TC_ID, tcId);

        return sendRequest('PUT', path, {name, metadata});
    }

    /**
     * Request all projects in your account.
     * @returns {Promise}
     *      Resolves with an array of all projects in your account.
     */
    getAllProjects() {
        var path = PATH_GET_ALL_PROJECTS;

        return sendRequest('GET', path);
    }

    /**
     * Generates a greyscale image out of the input image,
     * where areas with recognition and tracking relevance are highlighted in color.
     * @param {string} imageUrl The path to the image of which a heatmap should be created.
     * @returns {Promise}
     *      Resolves with the completed heatmap generation job object.
     */
    generateHeatmap(imageUrl) {
        var path = PATH_GENERATE_HEATMAP;

        return sendAsyncRequest('POST', path, {imageUrl});
    }
};

/**
 * HELPER method to send request to the Wikitude Cloud Targets API.
 *
 * @param {string} method
 *            request method (POST, GET, DELETE)
 * @param {string} path
 *            path of api end-point (appended to API_ENDPOINT_ROOT-url)
 * @param [payload]
 *            the JSON object that will be posted into the body
 * @returns {Promise}
 *            resolved once operation finished
 */
function sendRequest(method, path, payload) {
    return (
        sendApiRequest(method, path, payload)
            .then(response => {
                var jsonResponse;

                if ( hasJsonContent(response) ) {
                    jsonResponse = readJsonBody(response);
                }

                return jsonResponse;
            })
    );
}

function sendApiRequest(method, path, payload) {
    // prepare request
    var headers = {
        'X-Version': apiVersion,
        'X-Token': apiToken
    };

    var data;
    if (payload) {
        headers['Content-Type'] = CONTENT_TYPE_JSON;
        data = JSON.stringify(payload);
    }

    return (
        request(method, path, headers, data)
        .then(response => {
            if ( isResponseSuccess(response) ) {
                return response;
            } else {
                return (
                    readAPIError(response)
                        .then(error => {
                            throw error;
                        })
                );
            }
        })
    );
}

function request(method, path, headers, data) {
    return new Promise((fulfil, reject) => {
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

        // Create the request
        var request = https.request(options, fulfil);

        // On error, we reject
        request.on('error', reject);

        // write to body
        request.end(data);
    });
}

function isResponseSuccess(response) {
    var code = response.statusCode;

    return code === HTTP_OK || code === HTTP_ACCEPTED || code === HTTP_NO_CONTENT;
}

function readAPIError(response) {
    if ( hasJsonContent(response) ) {
        return readServiceError(response);
    } else {
        return readGeneralError(response);
    }
}

function hasJsonContent( response ) {
    var headers = response.headers;
    var contentType = headers['content-type'];
    var contentLength = headers['content-length'];

    return contentType === CONTENT_TYPE_JSON && contentLength !== "0";
}

function readServiceError( response ) {
    return (
        readJsonBody(response)
            .then(error => {
                var message = error.message;
                var code = error.code;
                var reason = error.reason;

                return new ServiceError( message, code, reason );
            })
    );
}

function readJsonBody( response ) {
    var body = readBody(response);

    return body.then(body => {
        try {
            return JSON.parse(body);
        } catch (error) {
            throw new Error(body);
        }
    });
}

function readBody( response ) {
    response.setEncoding('utf8');

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

function readGeneralError(response) {
    return (
        readBody(response)
            .then(message => {
                var code = response.statusCode;

                return new APIError(message, code);
            })
    );
}

function sendAsyncRequest( method, path, payload ) {
    return (
        sendApiRequest(method, path, payload)
            .then(response => {
                var location = getLocation(response);
                var initialDelay = Promise.resolve(apiPollInterval);

                if (hasJsonContent(response)) {
                    initialDelay = readJsonBody(response)
                        .then(status => status.estimatedLatency)
                    ;
                }

                return (
                    initialDelay
                        .then(wait)
                        .then(() => pollStatus(location))
                );
            })
    );
}

function getLocation( response ) {
    return response.headers[HEADER_LOCATION];
}

function wait(milliseconds) {
    return new Promise( fulfil => {
        setTimeout(fulfil, milliseconds);
    });
}

function pollStatus(location ) {
    return (
        readStatus(location)
        .then(status => {
            if (isCompleted(status)) {
                return status;
            } else {
                return (
                    wait(apiPollInterval)
                    .then(() => pollStatus(location))
                );
            }
        })
    );
}

function readStatus(location) {
    return sendApiRequest("GET", location).then(readJsonBody);
}

function isCompleted(status) {
    return status.status === "COMPLETED";
}

