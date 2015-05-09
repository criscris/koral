package xyz.koral.internal;

public class KoralError extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	
    public KoralError(String message) 
    {
        super(message);
    }
    
    public KoralError(String message, Throwable cause) 
    {
        super(message, cause);
    }

    public KoralError(Throwable cause) 
    {
        super(cause);
    }
}
