package xyz.koral.server.config;

import java.util.Map;

public class ServerConf 
{
	public String koralClientURI;
	public Map<String, InvitedUser> tokenToInvitedUser;
	public Map<String, User> users;
	public Map<String, Project> projects;
	
}
