// please find library at https://code.google.com/p/org-json-java/downloads/list and include in your project's build path
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple example how to connect with the Wikitude Cloud Targets API using
 * Java.
 * <p>
 * This example is published under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * @author Wikitude
 */

public class CloudManagerAPITest {

    // The token to use when connecting to the endpoint
    private static final String API_TOKEN = "<enter-your-manager-token-here>";
    private static final String USER_EMAIL = "<enter-your-email-here>";
    private static final String EXAMPLE_OBJECT_VIDEO = "<enter-path-to-object-video-here>";
    

    private static final int API_VERSION = 3;
    
    private static final String[] EXAMPLE_IMAGE_URLS = {
            "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg",
            "http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg"
    };

    public static void main(String args[]) {
        try {
            // create the object
            final CloudManagerAPI api = new CloudManagerAPI(API_TOKEN, API_VERSION);

            // create an empty targetCollection
            final JSONObject createdTargetCollection = api.createTargetCollection("myFirstTc");

            // targetCollection's id, which was created and will be modified in the following lines
            final String currentTcId = createdTargetCollection.getString("id");

            // create empty target collection
            System.out.println("\nCreated Image Target Collection:");
            printTargetCollection(createdTargetCollection);

            // add multiple targets at once to existing targetCollection
            final JSONArray targets = new JSONArray();
            final JSONObject newTarget = new JSONObject();
            newTarget.put("name", "target_1");
            newTarget.put("imageUrl", EXAMPLE_IMAGE_URLS[1]);
            targets.put(newTarget);
            final JSONObject addedTargets = api.addTargets(currentTcId, targets);
            System.out.println("\n\nAdded Targets to tc " + currentTcId);
            System.out.println(addedTargets.toString());

            // generate target collection for using its targets in productive Client API
            final JSONObject generatedTargetCollection = api.generateTargetCollection(currentTcId);
            System.out.println("Published Image Target Collection: " + generatedTargetCollection.toString());

            // clean up
            api.deleteTargetCollection(currentTcId);
            System.out.println("Deleted Image Target Collection: " + currentTcId);
        } catch (final Exception e) {
            System.out.println("Unexpected exception occurred '" + e.getMessage() + "'");
            e.printStackTrace();
        }

        try {
            // create the object
            final CloudManagerAPI api = new CloudManagerAPI(API_TOKEN, API_VERSION);

            // create an empty targetCollection
            final JSONObject createdTargetCollection = api.createObjectTargetCollection("myFirstTc");

            // targetCollection's id, which was created and will be modified in the following lines
            final String currentTcId = createdTargetCollection.getString("id");

            // create empty target collection
            System.out.println("\nCreated Object Target Collection:");
            printTargetCollection(createdTargetCollection);

            // add multiple targets at once to existing targetCollection
            final JSONArray targets = new JSONArray();
            final JSONObject newTarget = new JSONObject();
            newTarget.put("name", "New Object Target");
            final JSONObject newResource = new JSONObject();
            newResource.put("uri", EXAMPLE_OBJECT_VIDEO);
            newResource.put("fov", 60);
            newTarget.put("resource", newResource);
            targets.put(newTarget);
            final JSONObject addedTargets = api.createObjectTargets(currentTcId, targets);
            System.out.println("\n\nAdded Targets to tc " + currentTcId);
            System.out.println(addedTargets.toString());

            // generate WTO
            final JSONObject generatedWTO = api.generateWto(currentTcId, "7.0", USER_EMAIL);
            System.out.println("\n\nCreated WTO for Object Target Collection " + currentTcId);

            // clean up
            api.deleteObjectTargetCollection(currentTcId);
            System.out.println("Deleted Object Target Collection: " + currentTcId);
        } catch (final Exception e) {
            System.out.println("Unexpected exception occurred '" + e.getMessage() + "'");
            e.printStackTrace();
        }
    }


    /**
     * Helper method to print out basic information of given target collection JSONObject
     *
     * @param tc JSONObject of a valid target collection
     * @throws JSONException thrown if mandatory values are not set
     */
    private static void printTargetCollection(final JSONObject tc) throws JSONException {
        System.out.println("************************");
        System.out.println(" - tc id:      " + tc.getString("id"));
        System.out.println(" - tc name:    " + tc.getString("name"));
        System.out.println("************************");
    }

    /**
     * Helper method to print out basic information of given target JSONObject
     *
     * @param target target to print
     * @throws JSONException thrown if mandatory values are not set
     */
    private static void printTarget(final JSONObject target) throws JSONException {
        System.out.println("________________________");
        System.out.println(" - target id:      " + target.getString("id"));
        System.out.println(" - target name:    " + target.getString("name"));
        System.out.println("________________________");
    }

}
