
import java.io.FileNotFoundException;

// // please find library at https://code.google.com/p/org-json-java/downloads/list and include in your project's build path
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * A simple example how to connect with the Wikitude Cloud Targets API using
 * Java.
 * 
 * This example is published under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * @author Wikitude
 * 
 */

public class CloudManagerAPITest {

	// The token to use when connecting to the endpoint
	private static final String API_TOKEN = "INSERT_YOUR_TOKEN_HERE";
	// The version of the API we will use
	private static final int API_VERSION = 1;
	// The sample image URLs we are using in this example that will be used as targets inside a target collection
	private static final String[] EXAMPLE_IMAGE_URLS = {
			"http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/surfer.jpeg",
			"http://s3-eu-west-1.amazonaws.com/web-api-hosting/examples_data/biker.jpeg" };

	public static void main(String args[]) {
		try {
			// create the object
			final CloudManagerAPI api = new CloudManagerAPI(API_TOKEN, API_VERSION);			
			
			// create an empty targetCollection
			final JSONObject createdTargetCollection = api.createTargetCollection("myFirstTc");
			
			// targetCollection's id, which was created and will be modified in the following lines
			final String currentTcId = createdTargetCollection.getString("id");
			
			// create empty target collection
			System.out.println("\nCREATED TARGET-COLLECTION:");
			printTargetCollection(createdTargetCollection);
			
			// helper to hold information of last added target image
			JSONObject lastAddedTarget = null;
			
			// add target to existing targetCollection
			for (int i=0; i<EXAMPLE_IMAGE_URLS.length; i++) {
				
				// create target image JSON with basic information
				final JSONObject newTarget = new JSONObject();
				newTarget.put("name", "target_" + i);
				newTarget.put("imageUrl", EXAMPLE_IMAGE_URLS[i]);
				
				final JSONObject updatedTargetCollectionWithNewTarget = api.addTarget(currentTcId, newTarget);
				System.out.println("\n\nADDED TARGET #" + i +" to tc " + currentTcId);
				printTargetCollection(updatedTargetCollectionWithNewTarget);
				final JSONArray existingTarget = updatedTargetCollectionWithNewTarget.getJSONArray("targets");
				
				// for test purpose the last in the JSONArray is used, which might not equal the very recent target image
				lastAddedTarget = existingTarget.getJSONObject(existingTarget.length()-1);
			}
			
			// generate target collection for using its targets in productive Client API
			System.out.println("\n\nGENERATING TARGET COLLECTION " + currentTcId);
			final boolean generatedTargetCollection = api.generateTargetCollection(currentTcId);
			System.out.println(" - " + (generatedTargetCollection ? "OK" : "NG"));
			
		} catch (final Exception e) {
			System.out.println("Unexpected exception occurred '" + e.getMessage() + "'");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Helper method to print out basic information of given target collection JSONObject
	 * @param tc JSONObject of a valid target collection
	 * @throws JSONException thrown if mandatory values are not set
	 */
	private static void printTargetCollection(final JSONObject tc) throws JSONException {
		System.out.println("************************");
		System.out.println(" - tc id:      " + tc.getString("id"));
		System.out.println(" - tc name:    " + tc.getString("name"));
		final JSONArray targets =  tc.getJSONArray("targets");
		System.out.println(" - targets: " + targets.length());
		for (int i=0; i< targets.length(); i++) {
			final JSONObject currentTarget = targets.getJSONObject(i);
			printTarget(currentTarget);
		}
		System.out.println("************************");
	}
	
	/**
	 * Helper method to print out basic information of given target JSONObject
	 * @param target target to print
	 * @throws JSONException thrown if mandatory values are not set
	 */
	private static void printTarget(final JSONObject target) throws JSONException {
		System.out.println("________________________");
		System.out.println(" - target name:    " + target.getString("name"));
		System.out.println(" - target id:      " + target.getString("id"));
		System.out.println("________________________");
	}

}
