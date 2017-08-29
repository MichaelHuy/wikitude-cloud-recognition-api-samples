<?php
// A simple example how to connect with the Wikitude Cloud Manager API using Python.
// This example is published under Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0.html
// @author Wikitude

include('./ManagerAPI.php');

# The token to use when connecting to the endpoint
$token = "<enter-your-manager-token-here>";
$userEmail = "<enter-your-email-here>";
$EXAMPLE_OBJECT_VIDEO = "<enter-path-to-object-video-here>";

$api = new ManagerAPI($token, "3");

$EXAMPLE_IMAGE_URLS = array(
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg",
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg"
);

try {
    // create a target collection with the name testTargetCollection
    // and store the id which is of the target collection which is returned
    print "</br></br>Create Image Target Collection</br>\n";
    $tcResult = $api->createTargetCollection("testTargetCollection");
    print json_encode($tcResult);
    $tcId = $tcResult['id'];

    // add multiple targets to the target collection which was created in the previous step
    print "\n</br></br>Add Multiple Targets</br>\n";
    $targets = array(
        array(
            "name" => "TC2",
            "imageUrl" => $EXAMPLE_IMAGE_URLS[1]
        )
    );
    $newTargetsResult = $api->addTargets($tcId, $targets);
    print json_encode($newTargetsResult);

    // publish the target collection. After the target collection has been published it can be used for recognition
    print "\n</br></br>Publish Image Target Collection</br>\n";
    print json_encode($api->generateTargetCollection($tcId));

    // clean up
    print "\n</br></br>Delete Image Target Collection</br>\n";
    $api->deleteTargetCollection($tcId);
    print "deleted: $tcId\n";

} catch( APIException $e) {
    print $e;
}

try {
    // create an Object Target Collection with the name testObjectTargetCollection
    // and store the id which is of the target collection which is returned
    print "</br></br>Create Object Target Collection</br>\n";
    $tcResult = $api->createObjectTargetCollection("testObjectTargetCollection");
    print json_encode($tcResult);
    $tcId = $tcResult['id'];

    // add multiple targets to the Object Target Collection which was created in the previous step
    print "\n</br></br>Add Multiple Targets</br>\n";
    $targets = array(
        array(
            "name" => "TC2",
            "resource" => array(
                "uri" => $EXAMPLE_OBJECT_VIDEO,
                "fov" => 60
            )
        )
    );
    $newTargetsResult = $api->createObjectTargets($tcId, $targets);

    print "\nDone creating targets";
    print json_encode($newTargetsResult);

    // create a WTO file
    print "\n</br></br>Create WTO for Object Target Collection</br>\n";
    print json_encode($api->generateWto($tcId, "7.0", $userEmail));

    // clean up
    print "\n</br></br>Delete Object Target Collection</br>\n";
    $api->deleteObjectTargetCollection($tcId);
    print "deleted: $tcId\n";
} catch( APIException $e) {
    print $e;
}

?>