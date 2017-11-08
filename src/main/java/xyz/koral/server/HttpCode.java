package xyz.koral.server;

/**
 * source: https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
 *
 */
public class HttpCode 
{
	public static int Ok_200 = 200;
	
	/**
	 * The server cannot or will not process the request due to an apparent client error (e.g., malformed request syntax, size too large, invalid request message framing, or deceptive request routing)
	 */
	public static int Bad_request_400 = 400;
	
	
	/**
	 * Similar to 403 Forbidden, but specifically for use when authentication is required and has failed or has not yet been provided. 
	 * The response must include a WWW-Authenticate header field containing a challenge applicable to the requested resource. 
	 * 401 semantically means "unauthenticated",[33] i.e. the user does not have the necessary credentials.
	 * Note: Some sites issue HTTP 401 when an IP address is banned from the website (usually the website domain) and that specific address is refused permission to access a website.
	 */
	public static int Unauthorized_401 = 401;
	
	
	/**
	 * The requested resource could not be found but may be available in the future. 
	 * Subsequent requests by the client are permissible.
	 */
	public static int Not_found_404 = 404;
	
	
	/**
	 * A generic error message, given when an unexpected condition was encountered and no more specific message is suitable.
	 */
	public static int Internal_server_error_500 = 500;
}
