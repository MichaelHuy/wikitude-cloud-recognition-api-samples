
/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Targets API.
 * 
 * This example is published under Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * @author Wikitude
 */
   
 // [...]

    // tests Wikitude TargetApi usage
    app.get('/testTargetApi', function(req, res) {
        var ManagerApi = require('./ManagerAPI.js');

        // create API using own token and version
        var api = new ManagerApi('INSERT_YOUR_TOKEN_HERE', 1);

        // function called once target collection was created
        var testTargetCollection = function(createdTargetCollection) {

            // rename target collection
            api.renameTargetCollection(createdTargetCollection.id, 'newName', function(err, updatedTargetCollection) {
                console.log("updated name : " + updatedTargetCollection.name);
            });

            // add target to empty collection and delete it right afterwards
            var imageUrlDeleteTestTarget = "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg";
            api.addTarget(createdTargetCollection.id, {'name': 'myTarget1', 'imageUrl': imageUrlDeleteTestTarget}, function(err, createdTarget) {
                console.log("id of created target: " + createdTarget.id);
                api.deleteTarget(createdTargetCollection.id, createdTarget.id, function(err, result) {
                    console.log("target deletion applied " + createdTarget.id + ": " + (err ? "NO" : "YES"));
                });
            });

            // create new target, generate target collection, receive all meta information and delete complete target collection
            var imageUrlNewTarget = "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg";

            // 1) Add a target image to the collection (Note this happens in parallel to the previous deletion test)
            api.addTarget(createdTargetCollection.id, {'name': 'myTarget2', 'imageUrl': imageUrlNewTarget}, function(err, createdTarget) {
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


// [...]