/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Targets API.
 * 
 * This example is published under Apache License, Version 2.0 http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * @author Wikitude
 */

const ManagerApi = require('./ManagerAPI.js');

const token = '<enter-your-manager-token-here>';
const userEmail = '<enter-your-email-here>';
const EXAMPLE_OBJECT_VIDEO = '<enter-path-to-object-video-here>';

// create API using own token and version
const api = new ManagerApi(token, 3);
const EXAMPLE_IMAGE_URLS = [
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg",
    "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg"
];

api.createTargetCollection('Image Target Collection')
    .then(targetCollection => {
        const tcId = targetCollection.id;
        console.log(`created targetCollection: ${tcId}`);

        return api.renameTargetCollection(tcId, "Renamed Image Target Collection")
            .then(targetCollection => {
                console.log(`Updated Image Target Collection name to be ${targetCollection.name}`);

                const target = {name: "myTarget0", imageUrl: EXAMPLE_IMAGE_URLS[0]};

                return api.addTargets(tcId, [target]);
            })
            .then(targets => {
                console.log(`Created Image Targets ${targets}`);

                return api.generateTargetCollection(tcId);
            })
            .then(archive => {
                console.log(`Generated cloud archive: ${archive.id}`);

                return api.deleteTargetCollection(tcId);
            })
            .then(() => console.log(`Removed Image Target Collection: ${tcId}`));
    })
    .catch(error => {
        console.error("ERROR OCCURRED:", error.message, error);
    });

api.createObjectTargetCollection('Object Target Collection')
    .then(objectTargetCollection => {
        const tcId = objectTargetCollection.id;
        console.log(`created targetCollection with id ${tcId}`);

        return api.updateObjectTargetCollection(tcId, 'New OTC Name')
            .then(targetCollection => {
                console.log(`Updated Object Target Collection name to be ${targetCollection.name}`);

                const newObjectTarget = {
                    name: 'New Object Target',
                    resource: {
                        uri: EXAMPLE_OBJECT_VIDEO,
                        fov: 60
                    },
                    metadata: {my: 'meta data'}
                };

                return api.createObjectTargets(targetCollection.id, [newObjectTarget]);
            })
            .then(targets => {
                console.log(`Create Object Targets ${targets}`);

                return api.generateWto(tcId, '7.0', userEmail);
            })
            .then(response => {
                console.log('Generated WTO file');

                return api.deleteObjectTargetCollection(tcId)
            })
            .then(() => console.log(`Removed Image Target Collection: ${tcId}`));
    })
    .catch(error => {
        console.error("ERROR OCCURRED:", error.message, error);
    });
