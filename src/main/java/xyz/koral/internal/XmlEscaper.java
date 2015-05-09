package xyz.koral.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;


public class XmlEscaper 
{	
	public String escapeContent(String text)
	{
		if (text == null) return null;
		StringWriter writer = new StringWriter();
		try 
		{
			escapeContent(text, writer);
		} 
		catch (IOException e) 
		{
			// StringWriter does not throw IOException
		}
		return writer.toString();
	}
    
    public void escapeContent(String in, Writer out) throws IOException 
    {
    	for (char c : in.toCharArray())
    	{
    		if (c > 0x7F)
    		{
                out.write("&#");
                out.write(Integer.toString(c, 10));
                out.write(';');
    		}
    		else
    		{
        		switch (c)
        		{
        		case '&': out.write("&amp;"); break;
        		case '<': out.write("&lt;"); break;
        		case '>': out.write("&gt;"); break;
        		case '|': out.write("&#124;"); break; // pipe
        		case '`': out.write("&#96;"); break; // grave
        		case '~': out.write("&#126;"); break; // tilde
        		case '^': out.write("&#94;"); break; // circumflex
        		case '\\': out.write("&#92;"); break; // backslash	
        		
        		default: out.write(c);
        		}
    		}
    	}
    }
    
    /**
     * TODO: minimize number of StringBuffer.append calls by only looking for the next &
     * @return
     */
	public String unescape(String text)
	{
		if (text.indexOf('&') == -1) return text;
		
		StringBuilder sb = new StringBuilder(text.length());
		
    	char[] in = text.toCharArray();
    	int i = 0;
    	while (i < in.length - 3)
    	{
    		if (in[i] == '&')
    		{
    			if (in[i+1] == 'l' && in[i+2] == 't' && in[i+3] == ';')
    			{
    				sb.append('<');
    				i += 4;
    				continue;
    			}
    			else if (in[i+1] == 'g' && in[i+2] == 't' && in[i+3] == ';')
    			{
    				sb.append('>');
    				i += 4;
    				continue;
    			}
    			else if (in[i+1] == 'a' && in[i+2] == 'm' && in[i+3] == 'p' && i < in.length - 4 && in[i+4] == ';')
    			{
    				sb.append('&');
    				i += 5;
    				continue;
    			}
    			else if (in[i+1] == 'q' && in[i+2] == 'u' && in[i+3] == 'o' && i < in.length - 5 && in[i+4] == 't' && in[i+5] == ';')
    			{
    				sb.append('\"');
    				i += 6;
    				continue;
    			}
    			else if (in[i+1] == 'a' && in[i+2] == 'p' && in[i+3] == 'o' && i < in.length - 5 && in[i+4] == 's' && in[i+5] == ';')
        		{
    				sb.append('\'');
        			i += 6;
        			continue;
        		}
    			else if (in[i+1] == '#')
    			{
    				if (in[i+2] == 'x' || in[i+2] == 'X')
    				{
    					int s = i+3;
    					while (s < in.length - 1 && (in[s] >= '0' && in[s] <= '9') || (in[s] >= 'a' && in[s] <= 'f') || (in[s] >= 'A' && in[s] <= 'F'))
    					{
    						s++;
    					}
    					if (s > i+3 && in[s] == ';')
    					{
    						try
    						{
    							int unicode = Integer.parseInt(text.substring(i+3, s), 16);
    				            if (unicode > 0xFFFF) sb.append(Character.toChars(unicode));
    				            else sb.append((char) unicode);
    							i = s+1;
    							continue;
    						}
    						catch (NumberFormatException ex) {}
    					}
    				}
    				else
    				{
    					int s = i+2;
    					while (s < in.length - 1 && in[s] >= '0' && in[s] <= '9')
    					{
    						s++;
    					}
    					if (s > i+2 && in[s] == ';')
    					{
    						try
    						{
    							int unicode = Integer.parseInt(text.substring(i+2, s), 10);
    				            if (unicode > 0xFFFF) sb.append(Character.toChars(unicode));
    				            else sb.append((char) unicode);
    							i = s+1;
    							continue;
    						}
    						catch (NumberFormatException ex) {}
    					}
    				}
    			}
    		}

    		sb.append(in[i]);
    		i++;
    	}
    	for (; i<in.length; i++) sb.append(in[i]);
		
		return sb.toString();
	}
}


