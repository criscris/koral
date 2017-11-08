package xyz.koral;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.RServe.RConnection;
import org.rosuda.REngine.RServe.RserveException;
import org.rosuda.REngine.RServe.StartRserve;

import xyz.koral.table.Table;

/**
 * in your R IDE:
 * install.packages("Rserve")
 * library(Rserve)
 * Rserve(args='--vanilla')
 * 
 * 
 * [also check out JRI/rJava]
 */
public class R 
{
	public RConnection con;
	
	public R()
	{
        try 
        {
        	StartRserve.checkLocalRserve();
        	
        	// make a new local connection on default port (6311)   
			con = new RConnection();
			checkRServeTermination();
		} 
        catch (RserveException ex) 
        {
			throw new KoralError(ex);
		}  
	}
	
	public R set(String varName, Table table)
	{
		if (table.nrows() > Integer.MAX_VALUE || table.ncols() > Integer.MAX_VALUE) 
			throw new KoralError("Data dimensions (" + table.nrows() + "," + table.ncols() + ") exceeds " + Integer.MAX_VALUE);
		
		RList list = new RList();
		
		for (int i=0; i<table.ncols(); i++)
		{
			Table col = table.cols(i);
			if (col.isNumeric())
			{
				list.add(new REXPDouble(col.toArrayD()));
			}
			else
			{
				list.add(new REXPString(col.toArrayS()));
			}
		}
		
		for (int i=0; i<table.ncols(); i++)
		{
			list.setKeyAt(i, table.getColName(i));
		}

		try 
		{
			con.assign(varName, REXP.createDataFrame(list));
		} 
		catch (RserveException | REXPMismatchException ex) 
		{
			throw new KoralError(ex);
		}
		
		return this;
	}
	
	final static String defaultErrorMsg = "Error in try({ : ";
	public R exec(String code) 
	{
		try 
		{
			REXP r = con.eval("try({" + code + "}, silent=TRUE)");
			if (r != null && r.inherits("try-error")) // in contrast to RConnection javadoc it may return null also for successful calls
			{
				close();
				String s = r.asString();
				if (s.startsWith(defaultErrorMsg)) s = s.substring(defaultErrorMsg.length());
				throw new KoralError(code + ": " + s);
			}
		} 
		catch (RserveException | REXPMismatchException ex) 
		{
			close();
			throw new KoralError(ex);
		}
		return this;
	}
	
	public String print(String expression)
	{
		try 
		{
			return con.eval("paste(capture.output(print(" + expression + ")),collapse='\\n')").asString();
		} 
		catch (RserveException | REXPMismatchException ex) 
		{
			close();
			throw new KoralError(ex);
		} 
	}
	
	public double getD(String varName)
	{
		try 
		{
			return con.eval(varName).asDouble();
		} 
		catch (RserveException | REXPMismatchException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	public Table get(String varName)
	{    
		System.out.println("R get " + varName);
		try 
		{
			REXP rexp = con.eval(varName);
			if (rexp.isList())
			{
				RList list = rexp.asList();
				List<Table> cols = new ArrayList<>();
				System.out.println(varName + ": list size=" + list.size());
		        for (int i=0; i<list.size(); i++)
		        {
		        	REXP col = list.at(i);
		        	Table col_ = col.isNumeric() ? 
		        		Table.numeric(DoubleStream.of(col.asDoubles())) :
		        		Table.text(Stream.of(col.asStrings()));

		            String columnLabel = list.keyAt(i);
		            if (columnLabel != null) col_.setColNames_m(columnLabel);
		            cols.add(col_);
		        }
		        return Table.colBind_m(cols);
			}
			else if (rexp.isVector())
			{
				if (rexp.isNumeric())
				{
					double[][] r = rexp.asDoubleMatrix();
					System.out.println(r.length + "*" + r[0].length + " matrix");
					
					Table m = Table.numeric(r.length, r[0].length);
					for (int i=0; i<r.length; i++)
					{
						double[] row = r[i];
						for (int j=0; j<row.length; j++)
						{
							m.set_m(i, j, row[j]);
						}
					}
					return m;
				}
				else Table.text(Stream.of(rexp.asStrings()));				
			}
		} 
		catch (RserveException | REXPMismatchException ex) 
		{
			close();
			throw new KoralError(ex);
		}
		return null;
	}
	
	public void close()
	{
		con.close();
	}
	
	private static boolean isRunning = false;
	static synchronized void checkRServeTermination()
	{
		if (isRunning) return;
		isRunning = true;
		new Thread(() -> 
		{
			while (isRunning)
			{
				try 
				{
					Thread.sleep(500);
				} 
				catch (Exception e) 
				{
					
				}
				
				if (Notifier.isJVMTerminating()) 
				{
					try 
					{
						new RConnection().shutdown();
					} 
					catch (Exception e) 
					{
						e.printStackTrace();
					}
					isRunning = false;
				}
			}
		}).start();
	}
}
