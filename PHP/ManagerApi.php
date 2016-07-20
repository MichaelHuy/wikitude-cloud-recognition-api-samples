<?php

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
 *
 */
class ManagerAPI
{
    // the API host live
    private $API_HOST = 'https://api.wikitude.com';

    private $PLACEHOLDER_TC_ID       = '${TC_ID}';
    private $PLACEHOLDER_TARGET_ID   = '${TARGET_ID}';

    private $PATH_ADD_TC      = '/cloudrecognition/targetCollection';
    private $PATH_GET_TC      = '/cloudrecognition/targetCollection/${TC_ID}';
    private $PATH_GENERATE_TC = '/cloudrecognition/targetCollection/${TC_ID}/generation/cloudarchive';
    
    private $PATH_ADD_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target';
    private $PATH_ADD_TARGETS = '/cloudrecognition/targetCollection/${TC_ID}/targets';
    private $PATH_GET_TARGET  = '/cloudrecognition/targetCollection/${TC_ID}/target/${TARGET_ID}';

    private $HTTP_OK         = 200;
    private $HTTP_ACCEPTED   = 202;
    private $HTTP_NO_CONTENT = 204;

    // Your API key
    private $token = null;
    // The version of the API we will use
    private $version = null;
    // Current API host (stage/live)
    private $apiRoot = null;
    // interval used to poll status of asynchronous operations
    private $pollInterval = null;

    /**
     * ManagerAPI constructor.
     * @param string $token Your API key
     * @param string $version of the API we will use
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
        return $this->sendHttpRequest('POST', $this->PATH_ADD_TC, $payload);
    }

    /**
     * Retrieve all created and active target collections
     * @return array containing JSONObjects of all targetCollection that were created
     */
    public function getAllTargetCollections() {
        return $this->sendHttpRequest('GET', $this->PATH_ADD_TC);
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
        return $this->sendHttpRequest('POST', $path,$payload);
    }

    /**
     * Receive JSON representation of existing target collection (without making any modifications)
     * @param string $tcId id of the target collection
     * @return array of the JSON representation of target collection
     */
    public function getTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        return $this->sendHttpRequest('GET', $path);
    }

    /**
     * deletes existing target collection by id (NOT name)
     * @param string $tcId id of target collection
     * @return true on successful deletion, false otherwise
     */
    public function deleteTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        $this->sendHttpRequest('DELETE', $path);
        return true;
    }

    /**
     * retrieve all targets from a target collection by id (NOT name)
     * @param string $tcId id of target collection
     * @return array of all targets of the requested target collection
     */
    public function getAllTargets($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGET);
        return $this->sendHttpRequest('GET', $path);
    }

    /**
     * adds a target to an existing target collection
     * @param string $tcId id of the target collection to add target to
     * @param array $target array representation of target, e.g. array("name" => "TC1","imageUrl" => "http://myurl.com/image.jpeg");
     * @return array representation of created target (includes unique "id"-attribute)
     */
    public function addTarget($tcId, $target) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGET);
        return $this->sendHttpRequest('POST', $path, $target);
    }

    /**
     * adds multiple targets to an existing target collection
     * @param string $tcId id of the target collection to add targets to
     * @param array $targets array of targets
     * @return array representation of created target (includes unique "id"-attribute)
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
        return $this->sendHttpRequest('GET', $path);
    }

    /**
     * Update target JSON properties of existing targetId and targetCollectionId
     * @param string $tcId id of target collection
     * @param string $targetId id of target
     * @param string $target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
     * @return array JSON representation of target as an array
     */
    public function updateTarget($tcId, $targetId, $target) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        return $this->sendHttpRequest('POST', $path, $target);
    }

    /**
     * Delete existing target from a collection
     * @param string $tcId id of target collection
     * @param string $targetId id of target
     * @return true after successful deletion
     */
    public function deleteTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        $this->sendHttpRequest('DELETE', $path);
        return true;
    }

    /***
     * Gives command to start generation of given target collection. Note: Added targets will only be analyzed after generation.
     * @param string $tcId id of target collection
     * @return true on successful generation start. It will not wait until the generation is finished. The generation
     *      will take some time, depending on the amount of targets that have to be generated
     */
    public function generateTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GENERATE_TC);
        return $this->sendAsyncRequest('POST', $path);
    }

    /**
     * Send the POST request to the Wikitude Cloud Targets API.
     *
     * @param method
     *            the HTTP-method which will be used when sending the request
     * @param path
     *            the path to the service which is defined in the private variables
     * @param payload
     *            the array which will be converted to a JSON object which will be posted into the body
     * @return array|null
     */
    private function sendHttpRequest($method, $path, $payload = null) {
        $response = $this->sendAPIRequest($method, $path, $payload);

        return $this->readJsonBody($response);
    }

    private function sendAPIRequest($method, $path, $payload = null) {
        // create url
        $url = $this->apiRoot . $path;

        //prepare the request
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
            list($name, $value) = explode(": ", $line, 2);
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

        if ( $this->hasJsonContent($response) ) {
            $status = $this->readJsonBody($response);
            $estimatedLatency = $status["estimatedLatency"];
            $this->wait($estimatedLatency);
        }

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
        $status = null;
        do {
            $this->wait($this->pollInterval);
            $status = $this->readStatus($location);
        } while( $this->isNotCompleted($status) );

        return $status;
    }

    private function readStatus($location) {
        $response = $this->sendAPIRequest('GET', $location);

        return $this->readJsonBody($response);
    }

    private function isNotCompleted($status) {
        return $status["status"] != "COMPLETED";
    }
}

?>