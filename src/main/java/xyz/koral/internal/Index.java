package xyz.koral.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.w3c.dom.Element;

public class Index 
{
	File file;
	File indexFile;
	
	public Index(File file)
	{
		this.file = file;
		String filename = file.getName();
		int i1 = filename.lastIndexOf(".");
		String name = filename.substring(0, i1);
		String extension = filename.substring(i1);
		indexFile = new File(file.getParentFile(), ".index/" + name + ".idx" + extension);
	}
	
	public File getIndexFile()
	{
		try
		{
			if (!indexFile.exists() || 
					Files.getLastModifiedTime(Paths.get(indexFile.toURI())).compareTo(Files.getLastModifiedTime(Paths.get(file.toURI()))) < 0)
			{
				createIndex();
			}
			
			return indexFile;
		}
		catch (IOException ex)
		{
			throw new KoralError(ex);
		}
	}
	
	private void createIndex()
	{
		try 
		{
			long time = System.currentTimeMillis();
			System.out.print("Indexing " + file.getName());
			XmlDocument xml = XmlDocument.skipChildren(new FileInputStream(file), "array");
			System.out.print(" " + (System.currentTimeMillis() - time) + " ms. Keying... ");
			time = System.currentTimeMillis();
			for (Element array : xml.xpath("//array"))
			{
				new KeyIndices(array, new FileInputStream(file));
				System.out.print(".");
			}
			System.out.println((System.currentTimeMillis() - time) + " ms.");
			
			indexFile.getParentFile().mkdirs();
			xml.save(new FileOutputStream(indexFile));
		} 
		catch (FileNotFoundException ex) 
		{
			throw new KoralError(ex);
		}
	}
}
