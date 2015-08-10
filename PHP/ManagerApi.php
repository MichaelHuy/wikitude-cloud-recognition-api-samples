<?php

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
     * Send the POST request to the Wikitude Cloud Targets API.
     * 
     * @param payload
     *            the array which will be converted to a JSON object which will be posted into the body
     * @param method
     *            the HTTP-method which will be used when sending the request
     * @param path
     *            the path to the service which is defined in the private variables
     */
    private function sendHttpRequest($payload, $method, $path) {
        // create uri
        $uri = $this->apiRoot . $path;
        
        //configure the request
        $options = array(
            'http' => array(
                'method' => $method,
                'content' => ($payload == null ? '' : json_encode($payload)),
                // 'ignore_errors' => '1',
                'header'=>  "Content-Type: application/json\r\n" .
                            "X-Version: " . $this->apiVersion . "\r\n" .
                            "X-Token: " . $this->apiToken . "\r\n"
            )
        );

        //prepare the request
        $context  = stream_context_create($options);

        $result = @file_get_contents($uri, false, $context);
        //parse the result
        $response = json_decode($result , true);
        
        //return the response
        return $response;
    }

    /**
     * Create target Collection with given name.
     * @param tcName target collection's name. Note that response contains an "id" attribute, which acts as unique identifier
     * @return array of the JSON representation of the created empty target collection
     */
    public function createTargetCollection($tcName) {
        $payload = array('name' => $tcName);
        return $this->sendHttpRequest($payload, 'POST', $this->PATH_ADD_TC);
    }

    /**
     * Retrieve all created and active target collections
     * @return Array containing JSONObjects of all taregtCollection that were created
     */
    public function getAllTargetCollections() {
        return $this->sendHttpRequest(null, 'GET', $this->PATH_ADD_TC);
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
        return $this->sendHttpRequest($payload, 'POST', $path);
    }

    /**
     * Receive JSON representation of existing target collection (without making any modifications)
     * @param tcId id of the target collection
     * @return array of the JSON representation of target collection
     */
    public function getTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        return $this->sendHttpRequest(null, 'GET', $path);
    }

    /**
     * deletes existing target collection by id (NOT name)
     * @param tcId id of target collection
     * @return true on successful deletion, false otherwise
     */
    public function deleteTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TC);
        return ($this->sendHttpRequest("", 'DELETE', $path) == null ? true : false);
    }

    /**
     * retrieve all targets from a target collection by id (NOT name)
     * @param tcId id of target collection
     * @return array of all targets of the requested target collection
     */
    public function getAllTargets($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGET);
        return $this->sendHttpRequest(null, 'GET', $path);
    }

    /**
     * adds a target to an existing target collection
     * @param tcId
     * @param target array representation of target, e.g. array("name" => "TC1","imageUrl" => "http://myurl.com/image.jpeg");
     * @return array representation of created target (includes unique "id"-attribute)
     */
    public function addTarget($tcId, $target) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_ADD_TARGET);
        return $this->sendHttpRequest($target, 'POST', $path);
    }

    /**
     * Get target JSON of existing targetId and targetCollectionId
     * @param tcId id of target collection
     * @param targetId id of target
     * @return JSON representation of target as an array
     */
    public function getTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        return $this->sendHttpRequest(null, 'GET', $path);
    }

    /**
     * Delete existing target from a collection
     * @param tcId id of target collection
     * @param targetId id of target
     * @return true after successful deletion
     */
    public function deleteTarget($tcId, $targetId) {
        $path = str_replace($this->PLACEHOLDER_TARGET_ID, $targetId, str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GET_TARGET));
        return ($this->sendHttpRequest(null, 'DELETE', $path) == null ? true : false);
    }

    /***
     * Gives command to start generation of given target collection. Note: Added targets will only be analized after generation.
     * @param tcId id of target collection
     * @return true on successful generation start. It will not wait until the generation is finished. The generation will take some time, depending on the amount of targets that have to be generated
     */
    public function generateTargetCollection($tcId) {
        $path = str_replace($this->PLACEHOLDER_TC_ID, $tcId, $this->PATH_GENERATE_TC);
        return $this->sendHttpRequest(null, 'POST', $path);
    }
}

?>








