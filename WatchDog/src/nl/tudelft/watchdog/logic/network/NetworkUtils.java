package nl.tudelft.watchdog.logic.network;

import java.io.IOException;

import nl.tudelft.watchdog.logic.exceptions.ServerCommunicationException;
import nl.tudelft.watchdog.logic.logging.WatchDogLogger;
import nl.tudelft.watchdog.ui.preferences.Preferences;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/** Utility functions for accessing the network. */
public class NetworkUtils {

	/**
	 * An enum denoting the three possible different connection outcomes:
	 * successful, unsuccessful, or a network error.
	 */
	public enum Connection {
		/** Server returned expected answer. */
		SUCCESSFUL,
		/** Server returned something, but not the expected answer. */
		UNSUCCESSFUL,
		/** Network error. */
		NETWORK_ERROR
	};

	/**
	 * Checks whether the given url is reachable and exists. The connection
	 * timeout is set to 5 seconds.
	 * 
	 * @return if the url exists and returns a 200 status code.
	 *         <code>false</code> if the url does not return 200. In case of
	 *         NetworkFailure, throws an execpetion.
	 */
	public static Connection urlExistsAndReturnsStatus200(String url) {
		HttpClient client = createAuthenticatedHttpClient();
		HttpGet get;

		try {
			get = new HttpGet(url);
		} catch (IllegalArgumentException e) {
			return Connection.UNSUCCESSFUL;
		}
		try {
			HttpResponse response = client.execute(get);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_OK) {
				return Connection.SUCCESSFUL;
			} else if (statusCode == HttpStatus.SC_NOT_FOUND) {
				return Connection.UNSUCCESSFUL;
			}
			return Connection.NETWORK_ERROR;
		} catch (IOException exception) {
			// intentionally empty
		}
		// TODO (MMB) throw network access fail exception
		// this return is present just to fulfill the method requirements.
		return Connection.NETWORK_ERROR;
	}

	/**
	 * Opens an HTTP connection to the server, and transmits the supplied json
	 * data to the server. In case of error, the exact problem is logged.
	 * 
	 * @return The inputstream from the response.
	 * @throws ServerCommunicationException
	 */
	public static HttpEntity transferJson(String url, String jsonData)
			throws ServerCommunicationException, IllegalArgumentException {
		HttpClient client = createHTTPClient();
		HttpPost post = new HttpPost(url);
		String errorMessage = "";

		try {
			StringEntity input = new StringEntity(jsonData);
			input.setContentType("application/json");
			post.setEntity(input);

			HttpResponse response = client.execute(post);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
				return response.getEntity();
			} else {
				// server returns not created
				errorMessage = "Failed to execute Json request on server (status code: "
						+ response.getStatusLine().getStatusCode()
						+ "). "
						+ readResponse(response.getEntity());

			}
		} catch (IOException e) {
			// server unreachable case
			errorMessage = "Failed to commuincate with server. "
					+ e.getMessage();
		}
		WatchDogLogger.getInstance().logInfo(errorMessage);
		throw new ServerCommunicationException(errorMessage);
	}

	/** @return the base URL for new user registration. */
	public static String buildNewUserURL() {
		return getServerURI() + "user";
	}

	/** @return the base URL for new user registration. */
	public static String buildNewProjectURL() {
		return getServerURI() + "project";
	}

	/** @return the base URL for (existing) user-based operations. */
	public static String buildExistingUserURL(String id) {
		return getServerURI() + "user/" + id;
	}

	/** @return the base URL for user-based operations. */
	public static String buildProjectURL(String id) {
		return getServerURI() + "project/" + id;
	}

	/** @return the URL to post new intervals to the server to for this user. */
	public static String buildIntervalsPostURL(String userid, String projectid) {
		return buildExistingUserURL(userid) + "/" + projectid + "/intervals";
	}

	/**
	 * Wrapper function for Appache's {@link EntityUtils#toString()}, taking
	 * care of the exceptions (which should never happen).
	 */
	public static String readResponse(HttpEntity entity) {
		try {
			return EntityUtils.toString(entity);
		} catch (ParseException | IOException exception) {
			WatchDogLogger.getInstance().logSevere(exception);
		}
		return "";
	}

	private static String getServerURI() {
		return Preferences.getInstance().getServerURI();
	}

	/** Builds the correct HTTPClient according to the Preferences. */
	private static HttpClient createHTTPClient() {
		if (Preferences.getInstance().isAuthenticationEnabled()) {
			return createAuthenticatedHttpClient();
		}
		return createNormalHttpClient();
	}

	/** Creates an HTTP client that uses a normal connection. */
	private static HttpClient createNormalHttpClient() {
		return createPlainHttpClientBuilder().build();
	}

	/** Creates a vanilla HTTP client builder with some timeouts. */
	private static HttpClientBuilder createPlainHttpClientBuilder() {
		int connectionTimeout = 5000;
		RequestConfig config = RequestConfig.custom()
				.setConnectionRequestTimeout(connectionTimeout)
				.setConnectTimeout(connectionTimeout)
				.setSocketTimeout(connectionTimeout).build();
		HttpClientBuilder client = HttpClientBuilder.create()
				.setDefaultRequestConfig(config);
		return client;
	}

	/** Creates an HTTP client that uses an authenticated connection. */
	private static HttpClient createAuthenticatedHttpClient() {
		CredentialsProvider provider = new BasicCredentialsProvider();
		byte[] password = { 104, 110, 115, 112, 113, 115, 122, 110, 112, 113 };
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
				"watchdogplugin", new String(password));
		provider.setCredentials(AuthScope.ANY, credentials);
		HttpClient client = createPlainHttpClientBuilder()
				.setDefaultCredentialsProvider(provider).build();
		return client;
	}

}