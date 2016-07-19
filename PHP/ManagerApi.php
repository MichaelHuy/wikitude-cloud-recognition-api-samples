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
    private $API_HOST = 'https://api.wikitude.com/cloudrecognition';

    private $PLACEHOLDER_TC_ID       = '${TC_ID}';
    private $PLACEHOLDER_TARGET_ID   = '${TARGET_ID}';

    private $PATH_ADD_TC      = '/targetCollection';
    private $PATH_GET_TC      = '/targetCollection/${TC_ID}';
    private $PATH_GENERATE_TC = '/targetCollection/${TC_ID}/generation/cloudarchive';
    
    private $PATH_ADD_TARGET  = '/targetCollection/${TC_ID}/target';
    private $PATH_GET_TARGET  = '/targetCollection/${TC_ID}/target/${TARGET_ID}';

    // Your API key
    private $token = null;
    // The version of the API we will use
    private $version = null;
    // Current API host (stage/live)
    private $apiRoot = null;

    // Constructor
    function __construct($token, $version){
        //initialize the values
        $this->apiToken = $token;
        $this->apiVersion = $version;
        $this->apiRoot = $this->API_HOST;
    }

    /**
     * Create target Collection with given name.
     * @param tcName target collection's name. Note that response contains an "id" attribute, which acts as unique identifier
     * @return array of the JSON representation of the created empty target collection
     */
    public function createTargetCollection($tcName) {
        $payload = array('name' => $tcName);
        return $this->sendHttpRequest('POST', $this->PATH_ADD_TC, $payload);
    }

    /**
     * Retrieve all created and active target collections
     * @return Array containing JSONObjects of all targetCollection that were created
     */
    public function getAllTargetCollections() {
        return $this->sendHttpRequest('GET', $this->PATH_ADD_TC);
    }

    /**
     * Rename existing target collection
     * @param tcId id of target collection
     * @param tcName new name to use for this target collection
     * @return the updated JSON representation as an array of the modified target collection
     */
    public function renameTargetCollection($tcId, $tcName) {
        $payload = array('name' => $tcName);
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        return $this->sendHttpRequest('POST', $path,$payload);
    }

    /**
     * Receive JSON representation of existing target collection (without making any modifications)
     * @param tcId id of the target collection
     * @return array of the JSON representation of target collection
     */
    public function getTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        return $this->sendHttpRequest('GET', $path);
    }

    /**
     * deletes existing target collection by id (NOT name)
     * @param tcId id of target collection
     * @return true on successful deletion, false otherwise
     */
    public function deleteTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        $this->sendHttpRequest('DELETE', $path);
        return true;
    }

    /**
     * retrieve all targets from a target collection by id (NOT name)
     * @param tcId id of target collection
     * @return array of all targets of the requested target collection
     */
    public function getAllTargets($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGET);
        return $this->sendHttpRequest('GET', $path);
    }

    /**
     * adds a target to an existing target collection
     * @param tcId
     * @param target array representation of target, e.g. array("name" => "TC1","imageUrl" => "http://myurl.com/image.jpeg");
     * @return array representation of created target (includes unique "id"-attribute)
     */
    public function addTarget($tcId, $target) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGET);
        return $this->sendHttpRequest('POST', $path, $target);
    }

    /**
     * Get target JSON of existing targetId and targetCollectionId
     * @param tcId id of target collection
     * @param targetId id of target
     * @return JSON representation of target as an array
     */
    public function getTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        return $this->sendHttpRequest('GET', $path);
    }

    /**
     * Update target JSON properties of existing targetId and targetCollectionId
     * @param tcId id of target collection
     * @param targetId id of target
     * @param target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
     * @return JSON representation of target as an array
     */
    public function updateTarget($tcId, $targetId, $target) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        return $this->sendHttpRequest('POST', $path, $target);
    }

    /**
     * Delete existing target from a collection
     * @param tcId id of target collection
     * @param targetId id of target
     * @return true after successful deletion
     */
    public function deleteTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        $this->sendHttpRequest('DELETE', $path);
        return true;
    }

    /***
     * Gives command to start generation of given target collection. Note: Added targets will only be analized after generation.
     * @param tcId id of target collection
     * @return true on successful generation start. It will not wait until the generation is finished. The generation will take some time, depending on the amount of targets that have to be generated
     */
    public function generateTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GENERATE_TC);
        $this->sendHttpRequest('POST', $path);
        return true;
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
     */
    private function sendHttpRequest($method, $path, $payload = null) {
        // create url
        $url = $this->apiRoot . $path;

        //prepare the request
        $curl = $this->createRequest( $url, $method );

        if ( $payload ) {
            curl_setopt($curl, CURLOPT_POSTFIELDS, json_encode($payload));
        }

        $curl_response = curl_exec($curl);
        $info = curl_getinfo($curl);
        $statusCode = $info["http_code"];
        curl_close($curl);

        if ($curl_response === false) {
            throw new APIException("Unexpected Error", $statusCode);
        } else {
            //parse the result
            $response = json_decode($curl_response , true);

            if ( $this->isResponseStatusSuccess($statusCode) ) {
                return $response;
            } else {
                throw readServiceException( $response );
            }
        }
    }

    private function createRequest( $url, $method ) {
        $curl = curl_init($url);

        //configure the request
        curl_setopt_array($curl, array(
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_HTTPHEADER => array(
                "Content-Type: application/json",
                "X-Version: {$this->apiVersion}",
                "X-Token: {$this->apiToken}"
            ),
            CURLOPT_RETURNTRANSFER => true
        ));

        return $curl;
    }

    private function isResponseStatusSuccess( $statusCode ) {
        return $statusCode == 200 || $statusCode == 202;
    }

    private function readServiceException( $response ) {
        $code = $response["code"];
        $reason = $response["reason"];
        $message = $response["message"];

        return new ServiceException($message, $code, $reason);
    }
}

?>