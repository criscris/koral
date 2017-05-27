package xyz.koral;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;

public interface Git 
{
	static void commit(File dir, String filePath, String author, String message)
	{
		try 
		{
			org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.init().setDirectory(dir).call();
			git.add().addFilepattern(filePath).call();
			git.commit().setMessage(message).setAuthor(author, "").call();
			git.close();
		} 
		catch (GitAPIException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	static List<String> history(File dir, String filePath)
	{
		return  IO.exec(dir, "git log -p -- " + filePath);
	}
	
	static String historyHTML(File dir, String filePath, String koralBaseURI)
	{
		boolean isMeta = true;
		HTML html = HTML.koral(filePath, koralBaseURI);
		html.add("div", null, "class", "koralCommitHistory");
		html.child();
		for (String line : history(dir, filePath))
		{

			if (!isMeta && line.startsWith("commit ")) isMeta = true;
			
			String clazz = isMeta ? "meta" : "content";
			
			if (!isMeta)
			{
				if (line.startsWith("+")) clazz += " plus";
				else if (line.startsWith("-")) clazz += " minus";
			}
			
			if (line.startsWith("@@")) 
			{
				isMeta = false;
				clazz = "change";
			}
			
			if (line != null && line.length() > 0) html.add("div", line, "class", clazz);
		}
		return html.create();
	}
}
