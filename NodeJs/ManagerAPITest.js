
/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Targets API.
 * 
 * This example is published under Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * @author Wikitude
 */

var token = '<enter-your-token-here>';

var ManagerApi = require('./ManagerAPI.js');

// create API using own token and version
var api = new ManagerApi(token, 2);

// create target collection and write JSON of target collection to response
api.createTargetCollection()
    .then(createdTargetCollection => {
        var targetCollectionId = createdTargetCollection.id;

        // create new target, generate target collection, receive all meta information and delete complete target collection
        var imageUrl = "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg";
        var name = "myTarget1";

        return (
            // 1) Add a target image to the collection (Note this happens in parallel to the previous deletion test)
            api.addTarget(targetCollectionId, { name, imageUrl })
            .then(target => {
                console.log(`id of created target: ${target.id}`);
            })
            // 2) generate target collection
            .then(() => api.generateTargetCollection(targetCollectionId))
            .then(() => {
                console.log(`generated cloud archive for targetCollection: ${targetCollectionId}`);
            })
            .then(() => createdTargetCollection)
        );
    })
    .then(createdTargetCollection => {
        res.status(200);
        res.json(createdTargetCollection)
    })
    .catch(error => {
        console.log("ERROR OCCURRED: ", error);
        res.status(500);
        res.send();
    })
;
