
/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Targets API.
 * 
 * This example is published under Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * @author Wikitude
 */

 var express = require('express');
 var app = express();

// tests Wikitude TargetApi usage
app.get('/testTargetApi', function(req, res) {
    var ManagerApi = require('./ManagerAPI.js');

    // create API using own token and version
    var api = new ManagerApi('<enter-your-token-here>', 2);

    // function called once target collection was created
    var testTargetCollection = function(createdTargetCollection) {

        // create new target, generate target collection, receive all meta information and delete complete target collection
        var imageUrlNewTarget = "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg";

        // 1) Add a target image to the collection (Note this happens in parallel to the previous deletion test)
        api.addTarget(createdTargetCollection.id, {'name': 'myTarget1', 'imageUrl': imageUrlNewTarget}, function(err, createdTarget) {
            if (err) {
                console.log("ERROR OCCURRED: " + err);
                return;
            }
            console.log("id of created target: " + createdTarget.id);

            // 2) generate target collection
            api.generateTargetCollection(createdTargetCollection.id, function(err, result) {
                console.log("generated targetCollection " + createdTargetCollection.id + "? " + (err ? "NO" : "YES"));
            });
        });
    };

    // create target collection and write JSON of target collection to response
    api.createTargetCollection('firstOne', function(err, result) {
        if (err) {
            res.status(500);
            res.send();
        } else {
            testTargetCollection(result);
            res.json(result);
            res.status(200);
        }
    });

});

var server = app.listen(3000, function () { 
    console.log('Example app listening now');
});