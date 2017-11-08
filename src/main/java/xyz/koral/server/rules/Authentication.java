package xyz.koral.server.rules;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import xyz.koral.HTML;
import xyz.koral.server.HttpCode;
import xyz.koral.server.KoralServlet.Action;
import xyz.koral.server.KoralServlet.Verb;
import xyz.koral.server.Method;
import xyz.koral.server.ServerUtil;
import xyz.koral.server.config.InvitedUser;
import xyz.koral.server.config.User;

public class Authentication 
{
	public static long signUpTokenExpirationInSecs = 24 * 3600;
	
	public static String authenticate(HttpServletRequest req)
	{
		HttpSession session = req.getSession(false);
		if (session == null) return null;
		return (String) session.getAttribute("user");
	}
	
	public static Method signUpPage = new Method(
		Arrays.asList(
				r -> r.verb == Verb.GET,
				r -> r.action == Action.signUp,
				r -> r.config.tokenToInvitedUser != null && 
					 r.req.getParameter("signUpToken") != null && 
					 r.config.tokenToInvitedUser.containsKey(r.req.getParameter("signUpToken"))), 
		(req, resp) -> 
		{
			String html = HTML
			.koral("Sign up", req.config.koralClientURI)
			.header()
			.body()
			.add("div", null, "class", "koralSignUp")
			.create();
			resp.setContentType(ServerUtil.htmlMediaType);
			resp.getWriter().append(html).close();
		});
	
	public static Method signUp = new Method(
		Arrays.asList(
				r -> r.verb == Verb.POST,
				r -> r.action == Action.signUp), 
		(req, resp) -> 
		{
			SignUpData d = ServerUtil.body(req.req, SignUpData.class);
			if (d == null)
			{
				resp.sendError(HttpCode.Bad_request_400);
				return;
			}
			if (!isValidPassword(d.password))
			{
				resp.sendError(HttpCode.Bad_request_400, "Password too short.");
				return;
			}
			
			InvitedUser iu = null;
			if (req.config.tokenToInvitedUser != null && d.signUpToken != null)
			{
				iu = req.config.tokenToInvitedUser.get(d.signUpToken);
			}
			if (iu == null)
			{
				resp.sendError(HttpCode.Unauthorized_401, "Invalid sign up token.");
				return;
			}
			
			if (iu.username == null || iu.username.length() == 0)
			{
				resp.sendError(HttpCode.Internal_server_error_500);
				return;
			}
			if (!iu.username.equals(d.username))
			{
				resp.sendError(HttpCode.Unauthorized_401, "Wrong user name.");
				return;
			}
			
			if (iu.timestamp + signUpTokenExpirationInSecs * 1000L < System.currentTimeMillis())
			{
				resp.sendError(HttpCode.Unauthorized_401, "User sign up is expired.");
				return;
			}
			
			User user = new User();
			user.email = iu.email;
			user.projects = iu.projects;
			
			try
			{
				user.salt = Password.generateSalt_base64();
				user.passwordHash = Password.hash(d.password, user.salt);
			}
			catch (Exception ex)
			{
				resp.sendError(HttpCode.Internal_server_error_500);
				return;
			}

			req.config.tokenToInvitedUser.remove(d.signUpToken);
			if (req.config.users == null) req.config.users = new HashMap<>();
			req.config.users.put(iu.username, user);
			req.configModified = true;
		});
	
	public static Method signIn = new Method(
		Arrays.asList(
				r -> r.verb == Verb.POST,
				r -> r.action == Action.signIn), 
		(req, resp) -> 
		{
			SignInData d = ServerUtil.body(req.req, SignInData.class);
			if (d == null)
			{
				resp.sendError(HttpCode.Bad_request_400);
				return;
			}
			if (d.username == null || d.password == null)
			{
				resp.sendError(HttpCode.Bad_request_400);
				return;
			}
			
			User user = req.config.users.get(d.username);
			if (user == null)
			{
				resp.sendError(HttpCode.Unauthorized_401);
				return;
			}
			
			String hash = null;
			try
			{
				hash = Password.hash(d.password, user.salt);
			}
			catch (Exception ex)
			{
				resp.sendError(HttpCode.Internal_server_error_500);
				return;
			}
			
			if (!user.passwordHash.equals(hash))
			{
				resp.sendError(HttpCode.Unauthorized_401);
				return;
			}
			
			HttpSession session = req.req.getSession(true);
			session.setMaxInactiveInterval(120 * 3600); // 5 days
			session.setAttribute("user", d.username);
			Cookie cookie = new Cookie("user", d.username);
			resp.addCookie(cookie);
		});
	
	public static Method signOut = new Method(
		Arrays.asList(
				r -> r.verb == Verb.GET,
				r -> r.action == Action.signOut), 
		(req, resp) -> 
		{
			HttpSession session = req.req.getSession(false);
			if (session == null)
			{
				resp.sendError(HttpCode.Bad_request_400, "Session is expired.");
				return;
			}
			session.invalidate();
		});
	
	
	public static boolean isValidPassword(String password)
	{
		return password != null && password.length() >= 4;
	}
}

class SignUpData
{
	public String username;
	public String password;
	public String signUpToken;
}

class SignInData
{
	public String username;
	public String password;
}

class Password
{
    public static String hash(String password, String salt_base64) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 1000;
        char[] chars = password.toCharArray();
        byte[] salt = Base64.getDecoder().decode(salt_base64);
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
     
    public static String generateSalt_base64() throws NoSuchAlgorithmException
    {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
