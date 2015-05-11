package xyz.koral.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BinaryFiles 
{
	public static byte[] getData(InputStream is) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		copy(is, bos, true, true);
		return bos.toByteArray();
	}
	
	public static long copy(InputStream in, OutputStream out, boolean closeInputStream, boolean closeOutputStream) throws IOException
	{
		try 
		{
			long byteCount = 0;
			byte[] buffer = new byte[16384];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) 
			{
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
			}
			out.flush();
			return byteCount;
		}
		finally 
		{
			if (closeInputStream) in.close();
			if (closeOutputStream) out.close();
		}
	}
	
	public static List<File> listFilesRecursively(File dir,  String endsWith)
	{
		List<File> acceptedFiles = new ArrayList<>();
		listFilesRecursively(dir, endsWith, acceptedFiles);
		return acceptedFiles;
	}
	
	static void listFilesRecursively(File dir, String endsWith, List<File> acceptedFiles)
	{
		for (File file : dir.listFiles())
		{
			if (file.isFile())
			{
				if (endsWith == null || file.getName().endsWith(endsWith)) 
				{
					acceptedFiles.add(file);
				}
			}
			else if (file.isDirectory())
			{
				listFilesRecursively(file, endsWith, acceptedFiles);
			}
		}
	}
}
