package xyz.koral.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSource implements Source
{
	File file;
	
	public FileSource(File file)
	{
		this.file = file;
	}
	
	public InputStream createInputStream() 
	{
		try
		{
			return new FileInputStream(file);
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}

	public InputStream createInputStream(long offset, long maxBytes) 
	{
		if (offset == 0 && maxBytes >= file.length()) return createInputStream();
		try
		{
			return new PartInputStream(new FileInputStream(file), offset, maxBytes);
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}

	public Path getFile() 
	{
		return Paths.get(file.toURI());
	}
}
