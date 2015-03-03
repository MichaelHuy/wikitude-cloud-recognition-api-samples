

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

// please find library at https://code.google.com/p/org-json-java/downloads/list and include in your project's build path
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * TargetsAPI shows a simple example how to interact with the Wikitude Cloud
 * Targets API.
 * 
 * This example is published under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * @author Wikitude
 * 
 */
public class CloudManagerAPI {

	// The endpoint where the Wikitude Cloud Targets API resides.
	// *********************************************************************
	// ***************************** IMPORTANT *****************************
	// *********************************************************************
	// In case you get the following error when running the code:
	// "unable to find valid certification path to requested target",
	// your JVM does not recognize the SSL Certificate Authority (CA) Wikitude
	// is using. In this case, you either need to switch to http connections
	// (which causes your API Token to be transmitted in plain text over the
	// net), or you add the certificate to your certificate store. A tutorial
	// how to do that can be found at
	// https://blogs.oracle.com/gc/entry/unable_to_find_valid_certification
	private static final String API_ENDPOINT_ROOT = "https://api.wikitude.com";
	
	private static final String PLACEHOLDER_TC_ID 		= "${TC_ID}";
	private static final String PLACEHOLDER_TARGET_ID 	= "${TARGET_ID}";
	
	private static final String URL_ADD_TC 		= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection";
	private static final String URL_GET_TC 		= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID;
	private static final String URL_GENERATE_TC = API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/generation";
	
	private static final String URL_ADD_TARGET 	= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target";
	private static final String URL_GET_TARGET 	= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target/" + PLACEHOLDER_TARGET_ID;
	
	private static final String HEADER_KEY_TOKEN = "X-Token";
	private static final String HEADER_KEY_VERSION = "X-Version";
	

	// The token to use when connecting to the endpoint
	private final String apiToken;
	// The version of the API we will use
	private final int apiVersion;

	/**
	 * Creates a new TargetsAPI object that offers the service to interact with
	 * the Wikitude Cloud Targets API.
	 * 
	 * @param token
	 *            The token to use when connecting to the endpoint
	 * @param version
	 *            The version of the API we will use
	 */
	public CloudManagerAPI(String token, int version) {
		this.apiToken = token;
		this.apiVersion = version;
	}

	/**
	 * Create target Collection with given name.
	 * @param tcName target collection's name. Note that response contains an "id" attribute, which acts as unique identifier
	 * @return JSON representation of the created empty target collection
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public JSONObject createTargetCollection(final String tcName) throws IOException, JSONException {
		final JSONObject tcJSONObject = new JSONObject();
		tcJSONObject.put("name", tcName);
		
		final String requestUrl = URL_ADD_TC;
		final String response = this.sendRequest(requestUrl, tcJSONObject, "POST");
		return new JSONObject(response);
	}
	
	
	/**
	 * Retrieve all created and active target collections
	 * @return JSONArray containing JSONObjects of all taregtCollection that were created
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public JSONArray getAllTargetCollections() throws IOException, JSONException {
		final String requestUrl = URL_ADD_TC;
		final String response = this.sendRequest(requestUrl, null, "GET");
		return new JSONArray(response);
	}
	
	/**
	 * Rename existing target collection
	 * @param tcId id of target collection
	 * @param newName new name to use for this target collection
	 * @return the updated JSON representation of the modified target collection
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public JSONObject renameTargetCollection(final String tcId, final String newName) throws IOException, JSONException {
		final JSONObject tcJSONObject = new JSONObject();
		tcJSONObject.put("name", newName);
		
		final String requestUrl = URL_GET_TC.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, tcJSONObject, "POST"); 
		return new JSONObject(responseString);
	}
	
	/**
	 * Receive JSON representation of existing target collection (without making any modifications)
	 * @param tcId id of the target collection
	 * @return JSON representation of target collection
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public JSONObject getTargetCollection(final String tcId) throws IOException, JSONException {
		final String requestUrl = URL_GET_TC.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, null, "GET"); 
		return new JSONObject(responseString);
	}	

	/**
	 * deletes existing target collection by id (NOT name)
	 * @param tcId id of target collection
	 * @return true on successful deletion, false otherwise
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public boolean deleteTargetCollection(final String tcId) throws IOException, JSONException {
		final String requestUrl = URL_GET_TC.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, null, "DELETE"); 
		return responseString!=null && responseString.isEmpty();
	}
	
	/**
	 * Receive target collection's target images
	 * @param tcId id of target collection
	 * @return JSONArray of targets within given target collectino
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public JSONArray getAllTargets(final String tcId) throws IOException, JSONException {
		final String requestUrl = URL_ADD_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, null, "GET"); 
		return new JSONArray(responseString);
	}

	/**
	 * adds a target to an existing target collection
	 * @param tcId
	 * @param target JSON representation of target, e.g. {"name" : "foo", "imageUrl": "http://myserver.com/path/img.jpg"}
	 * @return JSON representation of created target (includes unique "id"-attribute)
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public JSONObject addTarget(final String tcId, final JSONObject target) throws IOException, JSONException {
		final String requestUrl = URL_ADD_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, target, "POST");
		System.out.println("responseString: " + responseString);
		return new JSONObject(responseString);
	}
	
	/**
	 * Get target JSON of existing targetId and targetCollectionId
	 * @param tcId id of target collection
	 * @param targetId id of target
	 * @return JSON representation of target
	 * @throws FileNotFoundException in case target does not exist
	 * @throws UnsupportedEncodingException in case utf-8 encoder is not possible in your JRE
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public JSONObject getTarget(final String tcId, final String targetId) throws FileNotFoundException, UnsupportedEncodingException, IOException, JSONException {
		final String requestUrl = URL_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, null, "GET"); 
		return new JSONObject(responseString);
	}
	
	/**
	 * Delete existing target from a collection
	 * @param tcId id of target collection
	 * @param targetId id of target
	 * @return true after successful deletion
	 * @throws FileNotFoundException in case target does not exist
	 * @throws UnsupportedEncodingException in case utf-8 encoder is not possible in your JRE
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public boolean deleteTarget(final String tcId, final String targetId) throws FileNotFoundException, UnsupportedEncodingException, IOException, JSONException {
		final String requestUrl = URL_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, null, "DELETE"); 
		return responseString!=null && responseString.isEmpty();
	}
	
	/***
	 * Gives command to start generation of given target collection. Note: Added targets will only be analized after generation.
	 * @param tcId id of target collection
	 * @return true on successful generation start. Generation will take some time
	 * @throws FileNotFoundException in case target does not exist
	 * @throws UnsupportedEncodingException in case utf-8 encoder is not possible in your JRE
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 */
	public boolean generateTargetCollection(final String tcId) throws FileNotFoundException, UnsupportedEncodingException, IOException, JSONException {
		final String requestUrl = URL_GENERATE_TC.replace (PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8") );
		final String responseString = this.sendRequest( requestUrl, null, "GET");
		return responseString!=null && responseString.isEmpty();
	}
	
	/**
	 * Send the POST request to the Wikitude Cloud Targets API.
	 * 
	 * <b>Remark</b>: We are not using any external libraries for sending HTTP
	 * requests, to be as independent as possible. Libraries like Apache
	 * HttpComponents make it a lot easier to interact with HTTP connections.
	 * 
	 * @param urlStr
	 *            The url to request
	 * @param  body
	 * 			  The JSONObject to send in body, set null if none should be used
	 * @param method
	 * 			  The http method to use ('GET', 'POST', 'DELETE')
	 * @return The response from the server, in JSON format
	 * 
	 * @throws IOException
	 *             when the server cannot serve the request for any reason, or
	 *             anything went wrong during the communication between client
	 *             and server.
	 * @throws FileNotFoundException
	 * 				thrown in case 404 is thrown
	 * 
	 */
	private String sendRequest(final String urlStr, final JSONObject body, final String method) throws IOException, FileNotFoundException {

		BufferedReader reader = null;
		OutputStreamWriter writer = null;

		try {

			// create the URL object from the endpoint
			URL url = new URL(urlStr);

			// open the connection
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			// use POST and configure the connection
			connection.setRequestMethod(method);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			// set the request headers
			connection.setRequestProperty(HEADER_KEY_TOKEN, apiToken);
			connection.setRequestProperty(HEADER_KEY_VERSION, "" + apiVersion);
			
			// append JSON body, if set
			if (body!=null) {
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Content-Length",
						String.valueOf(body.length()));

				// construct the writer and write request
				writer = new OutputStreamWriter(connection.getOutputStream());
				writer.write(body.toString());
				writer.flush();
			}

			// listen on the server response
			reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));

			// construct the server response and return
			StringBuilder sb = new StringBuilder();
			for (String line; (line = reader.readLine()) != null;) {
				sb.append(line);
			}

			// return the result
			return sb.toString();

		} catch (MalformedURLException e) {
			// the URL we specified as end-point was not valid
			System.err.println("The URL is not a valid URL");
			e.printStackTrace();
			return null;
		} catch (ProtocolException e) {
			// this should not happen, it means that we specified a wrong
			// protocol
			System.err
					.println("The HTTP method is not valid.");
			e.printStackTrace();
			return null;
		}  finally {
			// close the reader and writer
			try {
				if (writer!=null) {
					writer.close();
				}
			} catch (Exception e) {
				// intentionally left blank
			}
			try {
				reader.close();
			} catch (Exception e) {
				// intentionally left blank
			}
		}
	}

}
