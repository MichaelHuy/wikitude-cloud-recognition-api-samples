<?php
// A simple example how to connect with the Wikitude Cloud Manager API using Python.
// This example is published under Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0.html
// @author Wikitude

include('./ManagerAPI.php');

# The token to use when connecting to the endpoint
$token = "<enter-your-token-here>";
$api = new ManagerAPI($token, "1");

// create a target colection with the name testTargetCollection 
// and store the id which is of the target collection which is returned
print "</br></br>CREATE TARGETCOLLECTION</br>";
$tcResult = $api->createTargetCollection("testTargetCollection");
print json_encode($tcResult);
$tcId = $tcResult['id'];

// add a target to the target collection which was created in the previous step
print "</br></br>ADD TARGET</br>";
$target = array(
    "name" => "TC1",
    "imageUrl" => "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg"
);
$newTargetResult = $api->addTarget($tcId, $target);
print json_encode($newTargetResult);

$targetId = $newTargetResult['id'];

// publish the target collection. After the target collection has been published it can be used for recognition
print "</br></br>PUBLISH TARGETCOLLECTION</br>";
print json_encode($api->generateTargetCollection($tcId));

?>