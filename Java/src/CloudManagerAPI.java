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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

// please find library at https://code.google.com/p/org-json-java/downloads/list and include in your project's build path
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CloudManagerAPI {
	public class APIException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private final int code;

		public APIException(String message, int code) {
			super(message);
			this.code = code;
		}

		public String getMessage() {
			final String message = super.getMessage();

			return String.format("(%s): %s", code, message);
		}

		public int getCode() {
		    return this.code;
		}
	}

	public class ServiceException extends APIException {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private final String reason;

		public ServiceException(String message, int code, String reason) {
			super(message, code);
			this.reason = reason;
		}

		public String getMessage() {
			final String message = super.getMessage();
			final int code = this.getCode();

			return String.format("%s (%s): %s", reason, code, message);
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
	private static final int API_DEFAULT_POLL_INTERVAL = 10000;

	private static final String PLACEHOLDER_TC_ID 		= "${TC_ID}";
	private static final String PLACEHOLDER_TARGET_ID 	= "${TARGET_ID}";

	private static final String PATH_ADD_TC         = "/cloudrecognition/targetCollection";
	private static final String PATH_GET_TC         = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID;
	private static final String PATH_GENERATE_TC    = "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/generation/cloudarchive";

	private static final String PATH_ADD_TARGET 	= "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target";
	private static final String PATH_ADD_TARGETS	= "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/targets";
	private static final String PATH_GET_TARGET 	= "/cloudrecognition/targetCollection/" + PLACEHOLDER_TC_ID + "/target/" + PLACEHOLDER_TARGET_ID;

	private static final String HEADER_KEY_TOKEN = "X-Token";
	private static final String HEADER_KEY_VERSION = "X-Version";

	private static final String STATUS_COMPLETED = "COMPLETED";

	// The token to use when connecting to the endpoint
	private final String apiToken;
	// The version of the API we will use
	private final int apiVersion;
	// The interval used to poll asynchronous endpoints
	private final int apiPollInterval;

    private enum Method {
        GET, POST, DELETE
    }

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
        final String response = sendRequest(Method.POST, PATH_ADD_TC, tcJSONObject);

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
        final String response = this.sendRequest(Method.GET, PATH_ADD_TC);

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

		final String path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(Method.POST, path, tcJSONObject);
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
		final String path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(Method.GET, path);

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
		final String path = PATH_GET_TC.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		this.sendRequest(Method.DELETE, path);

		return true;
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
		final String path = PATH_ADD_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(Method.GET, path);

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
		final String path = PATH_ADD_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));
		final String responseString = this.sendRequest(Method.POST, path, target);

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
		final String path = PATH_ADD_TARGETS.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8"));

		return this.sendAsyncRequest(Method.POST, path, targets);
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
	public JSONObject getTarget(final String tcId, final String targetId) throws IOException, JSONException, APIException {
		final String path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		final String responseString = this.sendRequest(Method.GET, path);

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
	public JSONObject updateTarget(final String tcId, final String targetId, final JSONObject target) throws IOException, JSONException, APIException {
		final String path = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		final String responseString = this.sendRequest(Method.POST, path, target);

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
	public boolean deleteTarget(final String tcId, final String targetId) throws IOException, JSONException, APIException {
		final String requestUrl = PATH_GET_TARGET.replace(PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8")).replace(PLACEHOLDER_TARGET_ID,  URLEncoder.encode(targetId, "UTF-8"));
		this.sendRequest(Method.DELETE, requestUrl);

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
	public JSONObject generateTargetCollection(final String tcId) throws IOException, JSONException, APIException, InterruptedException {
		final String requestUrl = PATH_GENERATE_TC.replace (PLACEHOLDER_TC_ID, URLEncoder.encode(tcId, "UTF-8") );

		return sendAsyncRequest(Method.POST, requestUrl);
	}

	/**
	 * Send the POST request to the Wikitude Cloud Targets API.
	 *
	 * <b>Remark</b>: We are not using any external libraries for sending HTTP
	 * requests, to be as independent as possible. Libraries like Apache
	 * HttpComponents make it a lot easier to interact with HTTP connections.
	 *
     * @param method
     * 			  The http method to use ('GET', 'POST', 'DELETE')
	 * @param path
	 *            The path to request
	 * @param payload
	 * 			  The JSONObject to send in body, set null if none should be used
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
	private <TPayload> String sendRequest(final Method method, final String path, final TPayload payload) throws IOException, JSONException, APIException {
        HttpURLConnection connection = sendAPIRequest(method, path, payload);
        String response = null;

        if ( hasJsonContent(connection) ) {
            response = readInput(connection);
        }

        return response;
    }

    private String sendRequest(final Method method, final String path) throws IOException, JSONException, APIException {
        return sendRequest(method, path, null);
    }

    private <TPayload> HttpURLConnection sendAPIRequest(final Method method, final String path, final TPayload payload) throws IOException, JSONException, APIException {
	    URL url = new URL(API_ENDPOINT_ROOT + path);

        HttpURLConnection connection = openConnection(url, method);

        // append JSON body, if set
        if (payload != null) {
            writePayload(connection, payload.toString());
        }

        if ( isResponseSuccess(connection)) {
            return connection;
        } else {
            throw readAPIException(connection);
        }
    }

    private HttpURLConnection openConnection(final URL url, final Method method) throws IOException {
        // open the connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // use supplied method and configure the connection
        connection.setRequestMethod(method.toString());
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        // set the request headers
        connection.setRequestProperty(HEADER_KEY_TOKEN, apiToken);
        connection.setRequestProperty(HEADER_KEY_VERSION, "" + apiVersion);

        return connection;
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

	private boolean isResponseSuccess(HttpURLConnection connection) throws IOException {
		int statusCode = connection.getResponseCode();

        return statusCode == HttpsURLConnection.HTTP_OK || statusCode == HttpsURLConnection.HTTP_ACCEPTED || statusCode == HttpURLConnection.HTTP_NO_CONTENT;
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
	    if ( hasJsonContent(connection) ) {
            return readServiceException(connection);
        } else {
            return readGeneralError(connection);
        }
	}

    private boolean hasJsonContent(HttpURLConnection connection) {
        final String contentType = connection.getHeaderField("Content-Type");
        final String contentLength = connection.getHeaderField("Content-Length");

        return "application/json".equals(contentType) && !"0".equals(contentLength);
    }

    private APIException readServiceException(HttpURLConnection connection) throws IOException, JSONException {
        final String strError = readError(connection);
        final JSONObject error = new JSONObject(strError);
        final int code = error.getInt("code");
        final String reason = error.getString("reason");
        final String message = error.getString("message");

        return new ServiceException(message, code, reason);
    }

    private String readError(HttpURLConnection connection) throws IOException {
        return readBody(connection.getErrorStream());
    }

    private APIException readGeneralError(HttpURLConnection connection) throws IOException, JSONException {
        final String message = readError(connection);
        int code = connection.getResponseCode();

        return new APIException(message, code);
    }

    private JSONObject sendAsyncRequest(final Method method, final String path) throws IOException, JSONException, APIException, InterruptedException {
        return sendAsyncRequest(method, path, null);
    }

    private <TPayload> JSONObject sendAsyncRequest(final Method method, final String path, final TPayload payload) throws IOException, JSONException, APIException, InterruptedException {
        final HttpURLConnection connection = sendAPIRequest(method, path, payload);
        final String location = getLocation(connection);

        int initialDelay = apiPollInterval;
        if (hasJsonContent(connection)) {
            final JSONObject status = new JSONObject(readInput(connection));
            initialDelay = status.getInt("estimatedLatency");
        }

        wait(initialDelay);

        return pollStatus(location);
	}

	private String getLocation(HttpURLConnection connection) {
        return connection.getHeaderField("Location");
    }

    private void wait(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    private JSONObject pollStatus(final String location) throws InterruptedException, APIException, IOException, JSONException {
        while (true) {
            final JSONObject status = readStatus(location);
            if (isCompleted(status)) {
                return status;
            }
            wait(apiPollInterval);
        }
    }

    private JSONObject readStatus( final String location ) throws JSONException, APIException, IOException {
        HttpURLConnection connection = sendAPIRequest(Method.GET, location, null);

        return readJsonObjectBody(connection);
    }

    private JSONObject readJsonObjectBody(final HttpURLConnection connection) throws IOException, JSONException {
        final String body = readInput(connection);

        return new JSONObject(body);
    }

    private boolean isCompleted(final JSONObject status) throws JSONException {
        return STATUS_COMPLETED.equals(status.getString("status"));
    }
}
