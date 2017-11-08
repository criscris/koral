package xyz.koral.server.config;

import java.util.Set;

public class User 
{
	public String email;
	public String passwordHash;
	public String salt;
	public Set<String> projects;
}
