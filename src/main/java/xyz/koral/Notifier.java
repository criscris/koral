package xyz.koral;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Notifier 
{
	Set<Job> jobs = new LinkedHashSet<>();
	PrintWriter writer;
	static long minimumStreamSize = 10000000;
	static long minimumTimeMs = 5000;
	String prefix = "[Notifier]";
	
	long checkIntervalMillis = 500;
	int intermediateReportMultiplier = 5;
	float minimumPercentageDelta = 20f;
	long minimumProgressDelta = 500000000;
	
	protected Notifier()
	{
		setOutputToConsole();
	}
	
	static Notifier instance = null;
	
	public static synchronized Notifier instance()
	{
		if (instance == null) instance = new Notifier();
		return instance;
	}
	
	Runnable jobWatcher = () ->
	{
		long iteration = 1;
		while (jobs.size() > 0)
		{
			try 
			{
				Thread.sleep(checkIntervalMillis);
			} 
			catch (InterruptedException e) 
			{

			}
			
			List<Job> finishedJobs = new ArrayList<>();
			
			float sumPercUnfinished = 0;
			List<Job> unfinishedJobs = new ArrayList<>();
			Set<Job> displayedJobs = new HashSet<>();
			for (Job job : jobs)
			{
				if (job.hasEnded())
				{
					finishedJobs.add(job);
				}
				else if (iteration == 0)
				{
					sumPercUnfinished += job.percentageDone();
					unfinishedJobs.add(job);
				}
			}
			
			
			if (unfinishedJobs.size() > 2)
			{
				float perc = sumPercUnfinished / unfinishedJobs.size();
				println(round(perc, 2) + "% of " + unfinishedJobs.size() + " jobs.");
			}

			for (Job job : finishedJobs)
			{
				displayedJobs.add(job);
				println(job.toString());
				jobs.remove(job);
			}
			
			if (unfinishedJobs.size() <= 2) //  && jobs.size() == 1
			{
				for (Job job : jobs)
				{
					if (job.percentageDelta(false) >= minimumPercentageDelta) 
					{
						job.percentageDelta(true);
						displayedJobs.add(job);
						println(job.toString());
					}
					else if (job.progressDelta(false) >= minimumProgressDelta)
					{
						job.progressDelta(true);
						displayedJobs.add(job);
						println(job.toString());
					}
				}
			}
			
			writer.flush();
			if (isJVMTerminating()) 
			{
				for (Job job : jobs)
				{
					if (!displayedJobs.contains(job)) println(job.toString());
				}
				writer.flush();
				
				break;
			}
			iteration = (iteration + 1) % intermediateReportMultiplier;
		}
		isRunning = false;
	};
	boolean isRunning = false;
	
	void println(String line)
	{
		if (line != null) writer.println(prefix + " " + line);
	}
	
	class StreamJob implements Job
	{
		String label;
		long currentProgress = 0;
		long maxProgress;
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		String progressUnit;
		
		public StreamJob(String label, String progressUnit, long maxProgress)
		{
			this.label = label;
			this.maxProgress = maxProgress;
			this.progressUnit = progressUnit;
		}
		
		public void setProgress(long deltaProgress) 
		{
			currentProgress += deltaProgress;
		}

		public void end() 
		{
			if (endTime == 0) endTime = System.currentTimeMillis();
		}
		
		public long executionTime() 
		{
			long end = endTime > 0 ? endTime : System.currentTimeMillis();
			return end - startTime;
		}

		public long getStartTime() 
		{
			return startTime;
		}

		public String getLabel() 
		{
			return label;
		}
		
		public float percentageDone()
		{
			return maxProgress == Long.MAX_VALUE ? 
					(endTime > 0 ? 100f : 0f) :
					(float) ((double) currentProgress / maxProgress * 100.0);
		}
		
		float lastPercentageDeltaCall = 0f;
		public float percentageDelta(boolean log) 
		{
			float last = lastPercentageDeltaCall;
			float p = percentageDone();
			if (log) lastPercentageDeltaCall = p;
			return p - last;
		}
		
		long lastProgressDeltaCall = 0;
		public long progressDelta(boolean log) 
		{
			long last = lastProgressDeltaCall;
			long p = currentProgress;
			if (log) lastProgressDeltaCall = p;
			return p - last;
		}

		public boolean hasEnded()
		{
			return endTime > 0 || percentageDone() >= 100f;
		}
		
		public String toString()
		{
			long time = executionTime();
			if (time < Notifier.minimumTimeMs || currentProgress < Notifier.minimumStreamSize) return null;

			StringBuilder sb = new StringBuilder(label);
			
			if (progressUnit != null)
			{
				sb.append(" for ");
				if (maxProgress != Long.MAX_VALUE)
				{
					sb.append(valueWithMagnitude(maxProgress, 2));
				}
				else
				{
					if (endTime == 0) sb.append(">");
					sb.append(valueWithMagnitude(currentProgress, 2));
				}
				sb.append(progressUnit);
			}
			
			sb.append(": " + formatTime(time) + ".");
			
			float perc = percentageDone();
			if (maxProgress != Long.MAX_VALUE)
			{
				sb.append(" " + value(perc, 2) + "% done.");
				
				if (perc < 100)
				{
					float p = perc/100;
					float pl = 1f - p;
					double timeLeft = perc == 100 ? 0 :
							(double ) time / (p + 0.0001) * (pl + 0.0001);
					
					sb.append(" " + formatTime(timeLeft) + " left.");
				}
			}
			else
			{
				sb.append(perc >= 100 ? " done." : " pending.");
			}
			
			if (progressUnit != null)
			{
				double unitsPerSecond = (double) currentProgress / ((double) time / 1000.0);
				sb.append(" " + valueWithMagnitude(unitsPerSecond, 1) + progressUnit + "/sec.");
			}
			
			return sb.toString();
		}
	}
	
	static String[] magLabels = {" ", " K", " M", " G", " T"};
	static double[] mags10 = {0, 3, 6, 9, 12};
	public static String valueWithMagnitude(double value, int noOfDigits)
	{
		int magIndex = 0;
		while (magIndex < mags10.length - 2 && value > Math.pow(10, mags10[magIndex+1]))
		{
			magIndex++;
		}
		return value(value / Math.pow(10, mags10[magIndex]), noOfDigits) + magLabels[magIndex];
	}
	
	public static String value(double val, int noOfDigits)
	{
		double d = round(val, noOfDigits);
		String s = "" + d;
		if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
		return s;
	}
	
	
	public static double round(double val, int noOfDigits)
	{
		int d = (int) Math.pow(10, noOfDigits);
		return (double) Math.round(val * d) / d;
	}
	

	static String formatTime(double timeInMs)
	{
		return timeInMs < 100 ? ((long) timeInMs) + " ms" : value(timeInMs/1000, 1) + " secs";
	} 
	
	
	public InputStream start(InputStream is)
	{
		long size = Long.MAX_VALUE;
		String label = "InputStream";
		
		try
		{
			if (is instanceof FileInputStream)
			{
				FileInputStream fis = (FileInputStream) is;
				size = fis.getChannel().size();
				
				label = "FileInputStream";
				
				Field f = fis.getClass().getDeclaredField("path"); 
				f.setAccessible(true);
				String path = (String) f.get(fis);
				if (path != null)
				{
					File file = new File(path);
					label += " (" + file.getName() + ")";
				}
			}
			else if (is instanceof ByteArrayInputStream)
			{
				ByteArrayInputStream bis = (ByteArrayInputStream) is;
				Field f = bis.getClass().getDeclaredField("buf"); 
				f.setAccessible(true);
				byte[] buf = (byte[]) f.get(bis);
				if (buf != null)
				{
					size = buf.length;
					label = "ByteArrayInputStream";
				}
			}
			else if (is instanceof FilterInputStream) // e.g. BufferedInputStream
			{
				FilterInputStream fis = (FilterInputStream) is;
				Field f = fis.getClass().getDeclaredField("in"); 
				f.setAccessible(true);
				InputStream wrappedIs = (InputStream) f.get(fis);
				if (wrappedIs != null) 
				{
					return start(wrappedIs);
				}
			}
		}
		catch (IOException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex)
		{
			throw new KoralError(ex);
		}
		
		if (size < minimumStreamSize) return is;
		String callerSource = getCallerSource();
		if (callerSource != null) label += " (" + callerSource + ")";
		
		StreamJob job = new StreamJob(label, "Bytes", size);
		addJob(job);
		
		return new FilterInputStream(is) 
		{
			public int read() throws IOException 
			{
				int c = in.read();
				if (c >= 0) job.setProgress(1);
				return c;
			}

			public int read(byte[] b, int off, int len) throws IOException 
			{
				int c = in.read(b, off, len);
				if (c >= 0) job.setProgress(c);
				return c;
			}
			
			public long skip(long n) throws IOException // called by fjson
			{
				long c = in.skip(n);
				job.setProgress(c);
				return c;
			}

			public void close() throws IOException 
			{
				job.end();
				in.close();
			}
			
		};
	}
	
	public Reader start(Reader reader)
	{
		long size = Long.MAX_VALUE;
		String label = "Reader";
		
		try
		{
			if (reader instanceof StringReader)
			{
				StringReader s = (StringReader) reader;
				Field f = StringReader.class.getDeclaredField("length"); 
				f.setAccessible(true);
				size = f.getInt(s);
				label = "StringReader";
			}
		}
		catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex)
		{
			throw new KoralError(ex);
		}
		
		if (size < minimumStreamSize) return reader;
		String callerSource = getCallerSource();
		if (callerSource != null) label += " (" + callerSource + ")";
		
		StreamJob job = new StreamJob(label, "Chars", size);
		addJob(job);
		
		return new FilterReader(reader) 
		{
			public int read() throws IOException 
			{
				int c = in.read();
				if (c >= 0) job.setProgress(1);
				return c;
			}

			public int read(char[] cbuf, int off, int len) throws IOException 
			{
				int c = in.read(cbuf, off, len);
				if (c >= 0) job.setProgress(c);
				return c;
			}

			public void close() throws IOException 
			{
				job.end();
				in.close();
				
			}
		};
	}
	
	public OutputStream start(OutputStream os)
	{
		long size = Long.MAX_VALUE;
		String label = "OutputStream";
		
		try
		{
			if (os instanceof FileOutputStream)
			{
				FileOutputStream fos = (FileOutputStream) os;

				label = "FileOutputStream";
				
				Field f = FileOutputStream.class.getDeclaredField("path"); 
				f.setAccessible(true);
				String path = (String) f.get(fos);
				if (path != null)
				{
					File file = new File(path);
					label += " (" + file.getName() + ")";
				}
			}
			else if (os instanceof ByteArrayOutputStream)
			{
				label = "ByteArrayInputStream";
			}
			else if (os instanceof FilterOutputStream) // e.g. BufferedOutputStream
			{
				FilterOutputStream fos = (FilterOutputStream) os;
				Field f = FilterOutputStream.class.getDeclaredField("out"); 
				f.setAccessible(true);
				OutputStream wrappedOs = (OutputStream) f.get(fos);
				if (wrappedOs != null) 
				{
					return start(wrappedOs);
				}
			}
		}
		catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex)
		{
			throw new KoralError(ex);
		}
		
		if (size < minimumStreamSize) return os;
		String callerSource = getCallerSource();
		if (callerSource != null) label += " (" + callerSource + ")";
		
		StreamJob job = new StreamJob(label, "Bytes", size);
		addJob(job);
		
		return new FilterOutputStream(os)
		{
			public void write(int b) throws IOException 
			{
				out.write(b);
				job.setProgress(1);
			}

			public void write(byte[] b, int off, int len) throws IOException 
			{
				out.write(b, off, len);
				job.setProgress(len);
			}

			public void close() throws IOException 
			{
				job.end();
				super.close();
			}
		};
	}
	
	public Writer start(Writer writer)
	{
		long size = Long.MAX_VALUE;
		String label = "Writer";
		
		try
		{
			if (writer instanceof StringWriter)
			{
				label = "StringReader";
			}
		}
		catch (IllegalArgumentException ex)
		{
			throw new KoralError(ex);
		}
		
		if (size < minimumStreamSize) return writer;
		String callerSource = getCallerSource();
		if (callerSource != null) label += " (" + callerSource + ")";
		
		StreamJob job = new StreamJob(label, "Chars", size);
		addJob(job);
		
		return new FilterWriter(writer) 
		{
			public void write(int c) throws IOException 
			{
				out.write(c);
				job.setProgress(1);
			}

			public void write(char[] cbuf, int off, int len) throws IOException 
			{
				out.write(cbuf, off, len);
				job.setProgress(len);
			}

			public void write(String str, int off, int len) throws IOException 
			{
				out.write(str, off, len);
				job.setProgress(len);
			}

			public void close() throws IOException 
			{
				job.end();
				out.close();
			}

		};
	}
	
	/*
	public Job start(String label)
	{
		return null;
	}
	
	public Job start(long maxProgress)
	{
		return null;
	}
	
	public Job start(String label, long maxProgress)
	{
		return null;
	}
	*/
	
	public void endAll()
	{
		jobs = new LinkedHashSet<>();
	}
	
	public void setOutput(Writer writer)
	{
		this.writer = new PrintWriter(writer);
	}
	
	public void setOutputToConsole()
	{
		try 
		{
			writer = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
		} 
		catch (UnsupportedEncodingException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	public void setMinimumStreamSize(long noOfBytes)
	{
		minimumStreamSize = noOfBytes;
	}
	
	public static boolean isJVMTerminating()
	{
		for (Thread t : getJVMThreads())
		{
			if ("DestroyJavaVM".equalsIgnoreCase(t.getName())) return true;
		}
		return false;
	}
	
	void addJob(Job job)
	{	
		jobs.add(job);
		
		synchronized (this) 
		{
			if (!isRunning) 
			{
				isRunning = true;
				new Thread(jobWatcher, "xyz.koral.notification.Notifier").start();
			}
		}
	}
	
	public static List<Thread> getJVMThreads()
	{
		ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
		ThreadGroup parentGroup;
		while ((parentGroup = rootGroup.getParent() ) != null) 
		{
		    rootGroup = parentGroup;
		}
		Thread[] threads = new Thread[ rootGroup.activeCount() ];
		while ( rootGroup.enumerate( threads, true ) == threads.length ) 
		{
		    threads = new Thread[threads.length * 2];
		}
		List<Thread> threadList = new ArrayList<>();
		for (Thread t : threads)
		{
			if (t != null) threadList.add(t);
		}
		return threadList;
	}
	
	public static String getCallerSource()
	{
		String sourceInfo = null;
		StackTraceElement[] st = new Exception().getStackTrace();
		for (int i=0; i<st.length; i++)
		{
			StackTraceElement s = st[i];
			
			String className = s.getClassName();
			if (className.startsWith("xyz.koral.") ||
				className.startsWith("java.util.")) continue;
			String fileName = s.getFileName();
			int lineNumber = s.getLineNumber();
			
			sourceInfo = (s.isNativeMethod() ? "Native method" :
	             (fileName != null && lineNumber >= 0 ?
	              fileName + ":" + lineNumber :
	              (fileName != null ?  fileName : "Unknown source")));
			break;
		}
		return sourceInfo;
	}
}

interface Job 
{
	String getLabel();
	long getStartTime();
	void setProgress(long deltaProgress);
	float percentageDone();
	float percentageDelta(boolean log);
	long progressDelta(boolean log);
	void end();
	boolean hasEnded();
	long executionTime();
}
