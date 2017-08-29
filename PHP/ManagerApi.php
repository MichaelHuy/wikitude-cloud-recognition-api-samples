<?php
/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Cloud Targets API.
 *
 * This example is published under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * @author Wikitude
 *
 */

class APIException extends Exception {
    public function __construct($message, $code) {
        parent::__construct($message, $code);
    }

    public function __toString() {
        return "{$this->code}: {$this->message}";
    }
}

class ServiceException extends APIException {
    private $reason = null;

    public function __construct($message, $code, $reason) {
        parent::__construct($message, $code);
        $this->reason = $reason;
    }

    public function __toString() {
        return "{$this->reason} ({$this->code}): {$this->message}";
    }
}

/**
 * @class ManagerAPI
 */
class ManagerAPI
{
    // The endpoint where the Wikitude Cloud Targets API resides.
    private $API_HOST = 'https://api.wikitude.com';

    // placeholders used for url-generation
    private $PLACEHOLDER_TC_ID       = '${TC_ID}';
    private $PLACEHOLDER_TARGET_ID   = '${TARGET_ID}';
    private $PLACEHOLDER_GENERATION_ID   = '${GENERATION_ID}';

    // paths used for manipulation of target collection and target images
    private $PATH_ADD_TC      = '/cloudrecognition/targetCollection';
    private $PATH_GET_TC      = '/cloudrecognition/targetCollection/${TC_ID}';
    private $PATH_GENERATE_TC = '/cloudrecognition/targetCollection/${TC_ID}/generation/cloudarchive';
    
    private $PATH_ADD_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target';
    private $PATH_ADD_TARGETS = '/cloudrecognition/targetCollection/${TC_ID}/targets';
    private $PATH_GET_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target/${TARGET_ID}';

    private $PATH_CREATE_OBJECT_TARGETS = '/cloudrecognition/objectTargetCollection/${TC_ID}/targets';
    private $PATH_GET_OBJECT_TARGET  = '/cloudrecognition/objectTargetCollection/${TC_ID}/target/${TARGET_ID}';
    private $PATH_GET_ALL_OBJECT_TARGETS = '/cloudrecognition/objectTargetCollection/${TC_ID}/target';
    private $PATH_GET_OBJECT_TARGET_GENERATION_INFORMATION = '/cloudrecognition/objectTargetCollection/${TC_ID}/generation/target/${GENERATION_ID}';

    private $PATH_CREATE_OBJECT_TC = '/cloudrecognition/objectTargetCollection/';
    private $PATH_GET_OBJECT_TC = '/cloudrecognition/objectTargetCollection/${TC_ID}';
    private $PATH_GENERATE_WTO = '/cloudrecognition/objectTargetCollection/${TC_ID}/generation/wto';
    private $PATH_WTO_GENERATION_STATUS = '/cloudrecognition/objectTargetCollection/${TC_ID}/generation/wto/${GENERATION_ID}';
    private $PATH_GET_OBJECT_TC_JOBS = '/cloudrecognition/objectTargetCollection/${TC_ID}/jobs';

    private $PATH_GET_ALL_PROJECTS = '/cloudrecognition/projects';

    private $PATH_GENERATE_HEATMAP = '/cloudrecognition/heatmap';

    // status codes as returned by the api
    private $HTTP_OK         = 200;
    private $HTTP_ACCEPTED   = 202;
    private $HTTP_NO_CONTENT = 204;

    // The token to use when connecting to the endpoint
    private $token = null;
    // The version of the API we will use
    private $version = null;
    // Current API host
    private $apiRoot = null;
    // interval used to poll status of asynchronous operations
    private $pollInterval = null;

    /**
     * Creates a new ManagerAPI object that offers the service to interact with the Wikitude Cloud Targets API.
     *
     * @param string $token The token to use when connecting to the endpoint
     * @param string $version The version of the API we will use
     * @param int $pollInterval in milliseconds used to poll status of asynchronous operations
     */
    function __construct($token, $version = "2", $pollInterval = 10000){
        //initialize the values
        $this->token = $token;
        $this->version = $version;
        $this->apiRoot = $this->API_HOST;
        $this->pollInterval = $pollInterval;
    }

    /**
     * Create target Collection with given name.
     * @param string $tcName of the target collection. Note that response contains an "id" attribute, which acts as unique identifier
     * @return array of the JSON representation of the created empty target collection
     */
    public function createTargetCollection($tcName) {
        $payload = array('name' => $tcName);

        return $this->sendRequest('POST', $this->PATH_ADD_TC, $payload);
    }

    /**
     * Retrieve all created and active target collections
     * @return array containing JSONObjects of all targetCollection that were created
     */
    public function getAllTargetCollections() {
        return $this->sendRequest('GET', $this->PATH_ADD_TC);
    }

    /**
     * Rename existing target collection
     * @param string $tcId id of target collection
     * @param string $tcName new name to use for this target collection
     * @return array the updated JSON representation as an array of the modified target collection
     */
    public function renameTargetCollection($tcId, $tcName) {
        $payload = array('name' => $tcName);
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);

        return $this->sendRequest('POST', $path,$payload);
    }

    /**
     * Receive JSON representation of existing target collection (without making any modifications)
     * @param string $tcId id of the target collection
     * @return array of the JSON representation of target collection
     */
    public function getTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);

        return $this->sendRequest('GET', $path);
    }

    /**
     * deletes existing target collection by id (NOT name)
     * @param string $tcId id of target collection
     * @return true on successful deletion, false otherwise
     */
    public function deleteTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        $this->sendRequest('DELETE', $path);

        return true;
    }

    /**
     * retrieve all targets from a target collection by id (NOT name)
     * @param string $tcId id of target collection
     * @return array of all targets of the requested target collection
     */
    public function getAllTargets($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGET);

        return $this->sendRequest('GET', $path);
    }

    /**
     * adds multiple targets to an existing target collection
     * @param string $tcId id of the target collection to add targets to
     * @param array $targets array of targets
     * @return array representation of the status of the operation
     *      Note: this method will wait until the operation is finished, depending on the amount of targets this
     *      operation may take seconds to minutes
     */
    public function addTargets($tcId, $targets) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGETS);

        return $this->sendAsyncRequest('POST', $path, $targets);
    }

    /**
     * Get target JSON of existing targetId and targetCollectionId
     * @param string $tcId id of target collection
     * @param string $targetId id of target
     * @return array JSON representation of target as an array
     */
    public function getTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));

        return $this->sendRequest('GET', $path);
    }

    /**
     * Update target JSON properties of existing targetId and targetCollectionId
     * @param string $tcId id of target collection
     * @param string $targetId id of target
     * @param array $target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
     * @return array JSON representation of target as an array
     */
    public function updateTarget($tcId, $targetId, $target) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));

        return $this->sendRequest('POST', $path, $target);
    }

    /**
     * Deletes existing target from a target collection
     * @param string $tcId id of target collection
     * @param string $targetId id of target
     * @return true after successful deletion
     */
    public function deleteTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        $this->sendRequest('DELETE', $path);
        return true;
    }

    /***
     * Gives command to start generation of given target collection. Note: Added targets will only be analyzed after generation.
     * @param string $tcId id of target collection
     * @return array representation of the status of the operation
     *      Note: this method will wait until the operation is finished, depending on the amount of targets this
     *      operation may take seconds to minutes
     */
    public function generateTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GENERATE_TC);
        return $this->sendAsyncRequest('POST', $path);
    }

    /**
     * Creates a set of up to 10 new Object Targets in an Object Target Collection in your account.
     * @param string tcId The id of the Object Target Collection.
     * @param array targets An array of Object Targets to create.
     * @return array JSON representation of the status of the operation
     *      resolved once the operation finished, for the result the service will be polled
     *      Note: Depending on the amount of targets this operation may take from seconds to minutes
     */
    public function createObjectTargets($tcId, $targets) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_CREATE_OBJECT_TARGETS);
        return $this->sendAsyncRequest('POST', $path, $targets);
    }

    /**
     * Delete a particular Object Target from your Object Target Collection.
     * @param string tcId The id of the Object Target Collection.
     * @param string targetId The id of the Object Target.
     * @return empty response body.
     */
    public function deleteObjectTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, $this->PATH_GET_OBJECT_TARGET));
        return $this->sendRequest('DELETE', $path);
    }

    /**
     * Request a particular Object Target of an Object Target Collection.
     * @param string tcId The id of Object Target Collection.
     * @param string targetId The id of the Object Target.
     * @return the particular requested Object Target.
     */
    public function getObjectTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, $this->PATH_GET_OBJECT_TARGET));
        return $this->sendRequest('GET', $path);
    }

    /**
     * Request all Object Targets of your account.
     * @param string tcId The id of target collection.
     * @return array of Object Targets of your Object Target Collection.
     */
    public function getAllObjectTargets($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_ALL_OBJECT_TARGETS);
        return $this->sendRequest('GET', $path);
    }

    /**
     * Retrieves information status about a particular scheduled Object Target creation.
     * @param string tcId The id of target collection.
     * @param string generationId The id that identifies the Object Target creation.
     * @return the job status.
     */
    public function getObjectTargetGenerationInformation($tcId, $generationId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, str_replace($this->PLACEHOLDER_GENERATION_ID, $generationId, $this->PATH_GET_OBJECT_TARGET_GENERATION_INFORMATION));
        return $this->sendRequest('GET', $path);
    }

    /**
     * Create a new Object Target Collection in your account.
     * @param {string} name The name of the target collection.
     * @return JSON Object of the created empty target collection
     */
    public function createObjectTargetCollection($name) {
        print "createObjectTargetCollection\n";
        print $this->PATH_CREATE_OBJECT_TC;

        $path = $this->PATH_CREATE_OBJECT_TC;
        $payload = array('name' => $name);
        return $this->sendRequest('POST', $path, $payload);
    }

    /**
     * Delete a Object Target Collection and all its Object Targets
     * @param string tcId The id of the Object Target Collection.
     * @return resolved once the Object Target Collection was deleted,
     *      value is an empty response body
     */
    public function deleteObjectTargetCollection($tcId) {
        print "\ndeleteObjectTargetCollection\n";

        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_OBJECT_TC);
        return $this->sendRequest('DELETE', $path);
    }

    /**
     * Request a particular Object Target Collection in your account.
     * @param string tcId The id of the Object Target Collection.
     * @return the particular requested Object Target Collection.
     */
    public function getObjectTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_OBJECT_TC);
        return $this->sendRequest('GET', $path);
    }

    /**
     * Request all Object Target Collections in your account.
     * @return array of all Object Target Collections in your account.
     */
    public function getAllObjectTargetCollections() {
        $path = PATH_CREATE_OBJECT_TC;
        return $this->sendRequest('GET', $path);
    }

    /**
     * Generate a Object Target Collection and all its Object Targets as WTO.
     * @param string tcId The id of the Object Target Collection.
     * @param string sdkVersion Version of the Wikitude SDK to generated the file for. Valid values "7.0".
     * @param string [email] Address to send email notification to after generation finished.
     * @return JSON representation of the WTO generation job
     */
    public function generateWto($tcId, $sdkVersion, $email) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GENERATE_WTO);
        $payload = array('sdkVersion' => $sdkVersion, 'email' => $email);
        return $this->sendAsyncRequest('POST', $path, $payload);
    }

    /**
     * Retrieves information about a particular scheduled wto generation.
     * @param string tcId The id of the Object Target Collection.
     * @param string generationId The id that identifies the Object Targets creation.
     * @return the list of jobs.
     */
    public function getWtoGenerationStatus($tcId, $generationId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, str_replace($this->PLACEHOLDER_GENERATION_ID, $generationId, $this->PATH_WTO_GENERATION_STATUS));
        return sendRequest('GET', $path);
    }

    /**
     * Retrieves a list of asynchronous jobs sorted by creation date.
     * @param string tcId The id of the Object Target Collection.
     * @return list of asynchronous jobs.
     */
    public function getObjectTargetCollectionJobs($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_OBJECT_TC_JOBS);
        return sendRequest('GET', $path);
    }

    /**
     * Updates an existing Object Target Collection in your account.
     * @param string tcId The id of target collection.
     * @param string name The name of the target collection.
     * @param JSONObject metadata Arbitrary JSON data that should be updated in the Object Target Collection.
     * @return resolved once the Object Target Collection was updated,
     *      value is the JSON Object of the updated Object Target Collection
     */
    public function updateObjectTargetCollection($tcId, $name, $metadata) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_OBJECT_TC);
        $payload = array('name' => $name, 'metadata' => $metadata);
        return $this->sendRequest('PUT', $path, $payload);
    }

    /**
     * Request all projects in your account.
     * @return array of all projects in your account.
     */
    public function getAllProjects() {
        $path = $this->PATH_GET_ALL_PROJECTS;
        return $this->sendRequest('GET', $path);
    }

    /**
     * Generates a greyscale image out of the input image,
     * where areas with recognition and tracking relevance are highlighted in color.
     * @param string imageUrl The path to the image of which a heatmap should be created.
     * @return completed heatmap generation job object.
     */
    public function generateHeatmap($imageUrl) {
        $path = $this->PATH_GENERATE_HEATMAP;
        $payload = array('imageUrl' => $imageUrl);
        return $this->sendAsyncRequest('POST', $path, $payload);
    }

    /**
     * HELPER method to send request to the Wikitude Cloud Targets API.
     *
     * @param method
     *            the HTTP-method which will be used when sending the request
     * @param path
     *          the path to the service which is defined in the private variables
     * @param payload
     *            the array which will be converted to a JSON object which will be posted into the body
     * @return array|null
     */
    private function sendRequest($method, $path, $payload = null) {
        $response = $this->sendAPIRequest($method, $path, $payload);
        $jsonResponse = null;

        if ( $this->hasJsonContent($response) ) {
            $jsonResponse = $this->readJsonBody($response);
        }

        return $jsonResponse;
    }

    private function sendAPIRequest($method, $path, $payload = null) {
        // create url
        $url = $this->apiRoot . $path;

        // prepare the request
        $headers = array(
            "Content-Type: application/json",
            "X-Version: {$this->version}",
            "X-Token: {$this->token}"
        );

        $data = null;
        if ( $payload ) {
            $data = json_encode($payload);
        }

        $response = $this->request($url, $method, $headers, $data);

        if ($response["body"] === false) {
            throw new APIException("Unexpected Error", $response["code"]);
        } else {
            if ( $this->isResponseSuccess($response) ) {
                return $response;
            } else {
                throw $this->readAPIException( $response );
            }
        }
    }

    private function request($url, $method, $headers, $data = null) {
        //prepare the request
        $curl = curl_init($url);

        $responseHeaders = array();
        $addHeaderLine = function ( $curl, $line ) use (&$responseHeaders) {
            $parts = explode(": ", $line, 2);
            $name = $parts[0];
            $value = "";
            if ( isset($parts[1])) {
                $value = $parts[1];
            }
            $value = trim($value, "\n\r");
            if ( $value ) {
                $responseHeaders[$name] = $value;
            }

            return strlen($line);
        };

        //configure the request
        curl_setopt_array($curl, array(
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADERFUNCTION => $addHeaderLine
        ));

        if ( $data ) {
            curl_setopt($curl, CURLOPT_POSTFIELDS, $data);
        }

        $body = curl_exec($curl);
        $info = curl_getinfo($curl);
        curl_close($curl);

        $code = $info["http_code"];

        return array(
            "code" => $code,
            "headers" => $responseHeaders,
            "body" => $body
        );
    }

    private function isResponseSuccess( $response ) {
        $code = $response["code"];

        return $code == $this->HTTP_OK || $code == $this->HTTP_ACCEPTED || $code == $this->HTTP_NO_CONTENT;
    }

    private function readAPIException( $response ) {
        if ( $this->hasJsonContent( $response ) ) {
            return $this->readServiceException($response);
        } else {
            return $this->readGeneralException($response);
        }
    }

    private function hasJsonContent( $response ) {
        $headers = $response["headers"];
        $contentType = $headers["Content-Type"];
        $contentLength = $headers["Content-Length"];

        return $contentType == "application/json" && $contentLength != "0";
    }

    private function readServiceException( $response ) {
        $json = $this->readJsonBody($response);
        $code = $json["code"];
        $reason = $json["reason"];
        $message = $json["message"];

        return new ServiceException($message, $code, $reason);
    }

    private function readJsonBody($response) {
        return json_decode($response["body"], true);
    }

    private function readGeneralException( $response ) {
        $code = $response["code"];
        $message = $response["body"];

        return new APIException($message, $code);
    }

    private function sendAsyncRequest($method, $path, $payload = null) {
        $response = $this->sendAPIRequest($method, $path, $payload);
        $location = $this->getLocation($response);
        $initialDelay = $this->pollInterval;

        if ( $this->hasJsonContent($response) ) {
            $status = $this->readJsonBody($response);
            $initialDelay = $status["estimatedLatency"];
        }
        $this->wait($initialDelay);

        return $this->pollStatus($location);
    }

    private function getLocation($response) {
        return $response["headers"]["Location"];
    }

    private function wait($milliseconds) {
        $microseconds = $milliseconds * 1000;
        usleep($microseconds);
    }

    private function pollStatus($location) {
        while (true) {
            $status = $this->readStatus($location);
            if ($this->isCompleted($status) ) {
                return $status;
            }
            $this->wait($this->pollInterval);
        };
    }

    private function readStatus($location) {
        $response = $this->sendAPIRequest('GET', $location);

        return $this->readJsonBody($response);
    }

    private function isCompleted($status) {
        return $status["status"] == "COMPLETED";
    }
}

?>