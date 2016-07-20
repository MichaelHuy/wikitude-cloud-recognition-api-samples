<?php
// A simple example how to connect with the Wikitude Cloud Manager API using Python.
// This example is published under Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0.html
// @author Wikitude

include('./ManagerAPI.php');

# The token to use when connecting to the endpoint
$token = "<enter-your-token-here>";
$api = new ManagerAPI($token, "2");

$EXAMPLE_IMAGE_URLS = array(
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg",
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg"
);

try {
    // create a target collection with the name testTargetCollection
    // and store the id which is of the target collection which is returned
    print "</br></br>CREATE TARGETCOLLECTION</br>\n";
    $tcResult = $api->createTargetCollection("testTargetCollection");
    print json_encode($tcResult);
    $tcId = $tcResult['id'];

    // add a target to the target collection
    print "\n</br></br>ADD TARGET</br>\n";
    $target = array(
        "name" => "TC1",
        "imageUrl" => $EXAMPLE_IMAGE_URLS[0]
    );
    $newTargetResult = $api->addTarget($tcId, $target);
    print json_encode($newTargetResult);

    // add multiple targets to the target collection which was created in the previous step
    print "\n</br></br>ADD MULTIPLE TARGETS</br>\n";
    $targets = array(
        array(
            "name" => "TC2",
            "imageUrl" => $EXAMPLE_IMAGE_URLS[1]
        )
    );
    $newTargetsResult = $api->addTargets($tcId, $targets);
    print json_encode($newTargetsResult);

    // publish the target collection. After the target collection has been published it can be used for recognition
    print "\n</br></br>PUBLISH TARGETCOLLECTION</br>\n";
    print json_encode($api->generateTargetCollection($tcId));

    // clean up
    print "\n</br></br>DELETE TARGETCOLLECTION</br>\n";
    $api->deleteTargetCollection($tcId);
    print "deleted: $tcId\n";

} catch( APIException $e) {
    print $e;
}

?>