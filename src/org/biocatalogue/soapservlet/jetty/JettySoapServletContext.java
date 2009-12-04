package org.biocatalogue.soapservlet.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import ca.ucalgary.services.SoapServlet;


public class JettySoapServletContext
{
	public static void main(String[] args) throws Exception
	{
		Server server = new Server();
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(8080);
		server.addConnector(connector);

		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setDirectoriesListed(true);
		resource_handler.setWelcomeFiles(new String[]{ "index.html" });

		resource_handler.setResourceBase("data/");

		ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletContext.setContextPath("/");
		servletContext.addServlet(new ServletHolder(new SoapServlet()),"/servlet/");

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resource_handler, servletContext });
		server.setHandler(handlers);

		server.start();
		server.join();
	}
}


//public class JettySoapServletContext {
//    public static void main(String[] args) throws Exception
//    {
//        Server server = new Server(9999);
// 
//        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
//        context.setContextPath("/");
//        server.setHandler(context);
// 
//        context.addServlet(new ServletHolder(new SoapServlet()),"/servlet/");
//
//        server.start();
//        server.join();
//    }
//}