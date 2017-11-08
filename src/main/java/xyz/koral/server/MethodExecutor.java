package xyz.koral.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public interface MethodExecutor
{
	void exec(Request req, HttpServletResponse resp) throws ServletException, IOException;
}

