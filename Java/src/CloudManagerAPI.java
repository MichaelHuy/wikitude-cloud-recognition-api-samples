

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

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
	public class APIException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private final int code;
		private final String reason;
		
		public APIException(int code, String reason, String message) {
			super(message);
			this.code = code;
			this.reason = reason;
		}
		
		public String getMessage() {
			final String message = super.getMessage();
			
			return String.format("%s (%s): %s", reason, code, message);
		}

		public int getCode() {
		    return this.code;
		}

		public String getReason() {
		    return this.reason;
		}
		
	}

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
	private static final int API_DEFAULT_POLL_INTERVAL = 1000;
	
	private static final String PLACEHOLDER_TC_ID 		= "${TC_ID}";
	private static final String PLACEHOLDER_TARGET_ID 	= "${TARGET_ID}";
	
	private static final String URL_ADD_TC 		= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection";
	private static final String URL_GET_TC 		= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID;
	private static final String URL_GENERATE_TC = API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/generation/cloudarchive";
	
	private static final String URL_ADD_TARGET 	= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target";
	private static final String URL_ADD_TARGETS	= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/targets";
	private static final String URL_GET_TARGET 	= API_ENDPOINT_ROOT + "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target/" + PLACEHOLDER_TARGET_ID;
	
	private static final String HEADER_KEY_TOKEN = "X-Token";
	private static final String HEADER_KEY_VERSION = "X-Version";
	
	private static final String COMPLETED = "COMPLETED";

	// The token to use when connecting to the endpoint
	private final String apiToken;
	// The version of the API we will use
	private final int apiVersion;
	// The interval used to poll asynchronous endpoints
	private final int apiPollInterval;

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
		this(token, version, API_DEFAULT_POLL_INTERVAL);
	}

	/**
	 * Creates a new TargetsAPI object that offers the service to interact with
	 * the Wikitude Cloud Targets API.
	 * 
	 * @param token
	 *            The token to use when connecting to the endpoint
	 * @param version
	 *            The version of the API we will use
	 * @param pollInterval
	 *            The interval for polling asynchronous endpoints
	 */
	public CloudManagerAPI(String token, int version, int pollInterval) {
		this.apiToken = token;
		this.apiVersion = version;
		this.apiPollInterval = pollInterval;
	}

	/**
	 * Create target Collection with given name.
	 * @param tcName target collection's name. Note that response contains an "id" attribute, which acts as unique identifier
	 * @return JSON representation of the created empty target collection
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 * @throws APIException thrown in case service responds with an error
	 */
	public JSONObject createTargetCollection(final String tcName) throws IOException, JSONException, APIException {
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
	 * @throws APIException thrown in case service responds with an error
	 */
	public JSONArray getAllTargetCollections() throws IOException, JSONException, APIException {
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
	 * @throws APIException thrown in case service responds with an error
	 */
	public JSONObject renameTargetCollection(final String tcId, final String newName) throws IOException, JSONException, APIException {
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
	 * @throws APIException thrown in case service responds with an error
	 */
	public JSONObject getTargetCollection(final String tcId) throws IOException, JSONException, APIException {
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
	 * @throws APIException thrown in case service responds with an error
	 */
	public boolean deleteTargetCollection(final String tcId) throws IOException, JSONException, APIException {
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
	 * @throws APIException thrown in case service responds with an error
	 */
	public JSONArray getAllTargets(final String tcId) throws IOException, JSONException, APIException {
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
	 * @throws APIException thrown in case service responds with an error
	 */
	public JSONObject addTarget(final String tcId, final JSONObject target) throws IOException, JSONException, APIException {
		final String requestUrl = URL_ADD_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, target, "POST");

		return new JSONObject(responseString);
	}
	
	/**
	 * adds a target to an existing target collection
	 * @param tcId
	 * @param targets JSON representation of targets, e.g. {"name" : "foo", "imageUrl": "http://myserver.com/path/img.jpg"}
	 * @return JSON representation of created target (includes unique "id"-attribute)
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 * @throws APIException thrown in case service responds with an error
	 * @throws InterruptedException 
	 */
	public JSONObject addTargets(final String tcId, final JSONArray targets) throws IOException, JSONException, APIException, InterruptedException {
		final String requestUrl = URL_ADD_TARGETS.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendAsyncRequest(requestUrl, targets, "POST");

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
	 * @throws APIException thrown in case service responds with an error
	 */
	public JSONObject getTarget(final String tcId, final String targetId) throws FileNotFoundException, UnsupportedEncodingException, IOException, JSONException, APIException {
		final String requestUrl = URL_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, null, "GET"); 
		return new JSONObject(responseString);
	}

	/**
     * Update target JSON properties of existing targetId and targetCollectionId
     * @param tcId id of target collection
     * @param targetId id of target
     * @param target JSON representation of the target's properties that shall be updated, e.g. { "physicalHeight": 200 }
     * @return JSON representation of target as an array
     * @throws FileNotFoundException in case target does not exist
	 * @throws UnsupportedEncodingException in case utf-8 encoder is not possible in your JRE
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 * @throws APIException thrown in case service responds with an error
     */
	public JSONObject updateTarget(final String tcId, final String targetId, final JSONObject target) throws FileNotFoundException, UnsupportedEncodingException, IOException, JSONException, APIException {
		final String requestUrl = URL_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		final String responseString = this.sendRequest(requestUrl, target, "POST"); 
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
	 * @throws APIException thrown in case service responds with an error
	 */
	public boolean deleteTarget(final String tcId, final String targetId) throws FileNotFoundException, UnsupportedEncodingException, IOException, JSONException, APIException {
		final String requestUrl = URL_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		this.sendRequest(requestUrl, null, "DELETE");
		
		return true;
	}
	
	/***
	 * Gives command to start generation of given target collection. Note: Added targets will only be analized after generation.
	 * @param tcId id of target collection
	 * @return true on successful generation start. Generation will take some time
	 * @throws FileNotFoundException in case target does not exist
	 * @throws UnsupportedEncodingException in case utf-8 encoder is not possible in your JRE
	 * @throws IOException thrown in case of network problems
	 * @throws JSONException thrown in case server response is no valid JSON
	 * @throws APIException thrown in case service responds with an error
	 * @throws InterruptedException 
	 */
	public JSONObject generateTargetCollection(final String tcId) throws FileNotFoundException, UnsupportedEncodingException, IOException, JSONException, APIException, InterruptedException {
		final String requestUrl = URL_GENERATE_TC.replace (PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8") );

		return new JSONObject(sendAsyncRequest(requestUrl, "POST"));
	}
	
	/**
	 * Send the POST request to the Wikitude Cloud Targets API.
	 * 
	 * <b>Remark</b>: We are not using any external libraries for sending HTTP
	 * requests, to be as independent as possible. Libraries like Apache
	 * HttpComponents make it a lot easier to interact with HTTP connections.
	 * 
	 * @param url
	 *            The url to request
	 * @param  payload
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
	 * @throws JSONException 
	 * @throws APIException 
	 * 
	 */
	private String sendRequest(final String url, final JSONObject payload, final String method) throws IOException, FileNotFoundException, JSONException, APIException {
		try {
			HttpURLConnection connection = openConnection(url, method);
			
			// append JSON body, if set
			if (payload != null) {
				writePayload(connection, payload);
			}
			
			if ( isResponseStatusSuccess(connection)) {
				return readResponse(connection);
			} else {
				throw readAPIException(connection);
			}

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
		}
	}

	private HttpURLConnection openConnection(final String urlStr, final String method) throws MalformedURLException, IOException, ProtocolException {
		
		// create the URL object from the endpoint
		URL url = new URL(urlStr);

		// open the connection
		HttpURLConnection connection = (HttpURLConnection) url
				.openConnection();

		// use supplied method and configure the connection
		connection.setRequestMethod(method);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);

		// set the request headers
		connection.setRequestProperty(HEADER_KEY_TOKEN, apiToken);
		connection.setRequestProperty(HEADER_KEY_VERSION, "" + apiVersion);
		
		return connection;
	}

	private void writePayload(HttpURLConnection connection, final JSONObject payload) throws IOException {
		writePayload(connection, payload.toString());
	}

	private void writePayload(HttpURLConnection connection, final String payload) throws IOException {
		OutputStreamWriter writer = null;
		try {
			final String contentLength = String.valueOf(payload.length());
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", contentLength);

			// construct the writer and write request
			writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(payload);
			System.out.println("payload: " + payload);
			writer.flush();
		} finally {
			closeStream(writer);
		}
	}

	private void closeStream(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			// intentionally left blank
		}
	}

	private boolean isResponseStatusSuccess(HttpURLConnection connection) throws IOException {
		int statusCode = connection.getResponseCode();
		boolean success = statusCode == HttpsURLConnection.HTTP_OK || statusCode == HttpsURLConnection.HTTP_ACCEPTED;
		return success;
	}

	private String readResponse(HttpURLConnection connection) throws IOException {
		String response = null;
		if (hasResponseBody(connection)) {
			response = readInput(connection);
		}
		return response;
	}

	private boolean hasResponseBody(HttpURLConnection connection) throws IOException {
		int statusCode = connection.getResponseCode();
		
		return statusCode != HttpURLConnection.HTTP_ACCEPTED || statusCode != HttpURLConnection.HTTP_NO_CONTENT;
	}

	private String readInput(HttpURLConnection connection) throws IOException {
		return readBody(connection.getInputStream());
	}

	private String readBody(InputStream inputStream) throws IOException {
		BufferedReader reader = null;
		String response = null;
		try {
			// listen on the server response
			reader = new BufferedReader(new InputStreamReader(inputStream));
			// construct the server response and return
			StringBuilder sb = new StringBuilder();
			for (String line; (line = reader.readLine()) != null;) {
				sb.append(line);
			}
			response = sb.toString();
		} finally {
			closeStream(reader);
		}
		
		return response;
	}
	
	private APIException readAPIException(HttpURLConnection connection) throws IOException, JSONException {
		final String strError = readError(connection);
		final JSONObject error = new JSONObject(strError);
		final int code = error.getInt("code");
		final String reason = error.getString("reason");
		final String message = error.getString("message");

		return new APIException(code, reason, message);
	}

	private String readError(HttpURLConnection connection) throws IOException {
		return readBody(connection.getErrorStream());
	}
	
	private String sendAsyncRequest(final String url, final JSONObject payload, final String method) throws IOException, FileNotFoundException, JSONException, APIException, InterruptedException {
		return sendAsyncRequest(url, payload.toString(), method);
	}
	
	private String sendAsyncRequest(final String url, final JSONArray payload, final String method) throws IOException, FileNotFoundException, JSONException, APIException, InterruptedException {
		return sendAsyncRequest(url, payload.toString(), method);
	}

	private String sendAsyncRequest(final String url, final String payload, final String method) throws IOException, FileNotFoundException, JSONException, APIException, InterruptedException {
		try {
			HttpURLConnection connection = openConnection(url, method);
			
			// append JSON body, if set
			if (payload != null) {
				writePayload(connection, payload);
			}
			
			if (isResponseStatusSuccess(connection)) {
				return readProgress(connection);
			} else {
				throw readAPIException(connection);
			}

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
		}
	}


	private String sendAsyncRequest(final String url, final String method) throws IOException, FileNotFoundException, JSONException, APIException, InterruptedException {
		try {
			HttpURLConnection connection = openConnection(url, method);
			
			if (isResponseStatusSuccess(connection)) {
				return readProgress(connection);
			} else {
				throw readAPIException(connection);
			}

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
		}
	}
	private String readProgress(HttpURLConnection connection) throws FileNotFoundException, InterruptedException, JSONException, IOException, APIException {
		String location = API_ENDPOINT_ROOT + connection.getHeaderField("Location");
		return poll( location );
	}
	
	private String poll(String location) throws InterruptedException, FileNotFoundException, JSONException, IOException, APIException {
		JSONObject progress;
		String response;
		do {
			Thread.sleep(apiPollInterval);
			response = sendRequest(location, null, "GET");
			progress = new JSONObject(response);
		} while( !COMPLETED.equals(progress.getString("status")));
		
		return response;
	}
}
