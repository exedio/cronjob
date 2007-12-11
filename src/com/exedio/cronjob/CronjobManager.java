/*
 * Copyright (C) 2004-2006  exedio GmbH (www.exedio.com)
 */

package com.exedio.cronjob;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CronjobManager extends HttpServlet
{
	private static final long serialVersionUID =100000000000001L;
	
	private List<Cronjob> allCronjobs;
	private final int DURATION_BETWEEN_CHECKS=2705;
	private List<ObservedCronjob> observedCronjobs;
	private int idCounter;
	private String storeName;
	private boolean active;
		
	@Override
	public void init() throws ServletException
	{
		super.init();
		try
		{
			System.out.println("CronjobManager is starting ... (" + System.identityHashCode(this) + ')');
			final String STORE = "store";
			idCounter=0;
			storeName=getServletConfig().getInitParameter(STORE);
			if (storeName==null)
			{
				throw new RuntimeException("ERROR: Servlet-Init-Parameter: >> "+STORE+" << was expected but not found");
			}
	
			final Class storeClass;
			try
			{
				storeClass = Class.forName(storeName);
			}
			catch (ClassNotFoundException e)
			{
				throw new RuntimeException("ERROR: A class with name: "+storeName+" was not found", e);
			}
	
			final Constructor storeConstructor;
			try
			{
				storeConstructor = storeClass.getConstructor(ServletContext.class);
			}
			catch(NoSuchMethodException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" has no suitable constructor", e);
			}
			
			final Object o;
			try
			{
				o = storeConstructor.newInstance(getServletContext());
			}
			catch(InvocationTargetException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" constructor throw exception", e);
			}
			catch(InstantiationException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" could not be instantiated (must not be abstract or an interface)", e);
			}
			catch(IllegalAccessException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" or its null-constructor could not be accessed ", e);
			}		
			CronjobStore store = null;		
			if (o instanceof CronjobStore)
			{
				store=(CronjobStore)o;
				observedCronjobs = new ArrayList<ObservedCronjob>();
				active=store.isActive();
				if (store.isActive())
				{
					for (final Cronjob job: store.getAllCronjobs())
					{
						observedCronjobs.add(new ObservedCronjob(job, getNewId(),store.getInitialDelayInMilliSeconds()));
					}
				}
				else
				{
					System.out.println("INFO: No cronjobs will be executed, "+storeName+".isActive() returned false");
				}
			}
			else
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" must implement the CronjobStore-interface");
			}		
		}
		catch(RuntimeException e)
		{
			// tomcat does not print stack trace or exception message, so we do
			System.err.println("RuntimeException in CronjobManager.init");
			e.printStackTrace();
			throw e;
		}
		catch(Error e)
		{
			// tomcat does not print stack trace or exception message, so we do
			System.err.println("Error in CronjobManager.init");
			e.printStackTrace();
			throw e;
		}
		System.out.println("CronjobManager is started. (" + System.identityHashCode(this) + ')');
	}
	
	@Override
	public void destroy()
	{
		System.out.println("CronjobManager is terminating ... (" + System.identityHashCode(this) + ')');
		for (final ObservedCronjob job :observedCronjobs)
		{
			job.setActivated(false);
		}
		System.out.println("CronjobManager is terminated. (" + System.identityHashCode(this) + ')');
	}
	
	private String getNewId()
	{
		idCounter++;
		return "cronjob_"+String.valueOf(idCounter);
	}
	
	private String getImplementationVersion()
	{
		String iv=CronjobManager.class.getPackage().getImplementationVersion();		
		return iv==null ? "" : iv;
	}
	
	@Override
	protected final void doGet(
		final HttpServletRequest request,
		final HttpServletResponse response)	throws ServletException, IOException
	{
		doRequest(request, response);
	}

	@Override
	protected final void doPost(
		final HttpServletRequest request,
		final HttpServletResponse response) throws ServletException, IOException
	{
		doRequest(request, response);
	}
	
	private void doRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		String uri=request.getRequestURI();
		final String ACTIVATE="activate";
		final String DEACTIVATE="deactivate";
		final String TRUE = "true";
		final String FALSE = "false";
		final String AUTO_REFRESH = "autoRefresh";
		final String START_CRONJOB = "Start";
		final String DELETE_LAST_EXCEPTION = "Delete";
		final String ENABLE_AUTOREFRESH = "Enable Auto-Refresh";
		final String DISABLE_AUTOREFRESH = "Disable Auto-Refresh";
		// Start/Activate/Deactivate/removeLastException for selected Cronjob
		
		for (final ObservedCronjob job : observedCronjobs)
		{
			String[] params=request.getParameterValues(job.getId());
			if (params!=null)
			{
				List<String> paramsAsList = Arrays.asList(params);
				if (paramsAsList.contains(START_CRONJOB))
				{
					job.runNow();
				}
				else if (paramsAsList.contains(DELETE_LAST_EXCEPTION))
				{
					job.removeLastException();
				}
				else if (paramsAsList.contains(ACTIVATE))
				{
					job.setActivated(true);
				}
				else if (paramsAsList.contains(DEACTIVATE))
				{
					job.setActivated(false);
				}
				else{/* NOTHING */}
			}
		}
		
		// AutoRefresh Page 
		String refreshStatus = (request.getParameter(AUTO_REFRESH)==null) ? FALSE : request.getParameter(AUTO_REFRESH);
		boolean autoRefreshPage = refreshStatus.equals(TRUE) ? true : false;
		String enableOrDisableAutoRefreshButton = autoRefreshPage ? DISABLE_AUTOREFRESH : ENABLE_AUTOREFRESH;
		if (request.getParameter(ENABLE_AUTOREFRESH)!=null)
		{
			autoRefreshPage=true;
			enableOrDisableAutoRefreshButton=DISABLE_AUTOREFRESH;
		}
		if (request.getParameter(DISABLE_AUTOREFRESH)!=null)
		{
			autoRefreshPage=false;
			enableOrDisableAutoRefreshButton=ENABLE_AUTOREFRESH;
		}
		
		// Page-Content-Generation
		response.setContentType("text/html; charset=utf-8");
		PrintWriter out = response.getWriter();
		String result="<html>\n<head>\n<title>Cronjob Manager</title>";
		uri=uri+"?"+AUTO_REFRESH+"="+(autoRefreshPage ? TRUE : FALSE);
		if (autoRefreshPage)
		{			
			result+="\n<meta http-equiv=\"refresh\" content=\"5; URL="+uri+"\">";
		}
		result+="\n<style type=\"text/css\">\n th, td \n{ \npadding: 0 5 0 5\n}\n</style>";
		result+="</head>\n<body><form action=\""+uri+"\" method=\"post\">\n<table width=100%><tr><td align=center>\n<h1>exedio cronjob</h1>";			
		if (!observedCronjobs.isEmpty())
		{	
			result+="<table><tr><td><input type=submit name=\""+enableOrDisableAutoRefreshButton+"\" value=\""+enableOrDisableAutoRefreshButton+"\"/>" +
				"</td>" +
				"</tr></table><br>"+
				"<table border=1 width=1% cellpadding=0 cellspacing=0>"+
				"<tr>" +
				"<th rowspan=2>&nbsp;</th>" +
				"<th rowspan=2>&nbsp;</th>" +
				"<th rowspan=2>Name</th>" +
				"<th rowspan=2><nobr>Intervall (m)</nobr></th>" +
				"<th rowspan=2>Running</th>" +
				"<th colspan=3>Last Execution</th>" +			
				"<th rowspan=2>Successful Executions</th>" +
				"<th rowspan=2>Fails</th>" +
				"<th rowspan=2>Average Duration</th>" +
				"</tr>"+
				"<tr>"+
				"<th>Status</th>" +		
				"<th>Start</th>" +		
				"<th>Duration</th>" +		
				"</tr>";
			for (final ObservedCronjob job : observedCronjobs)
			{
				String activate = "<input type=submit name=\""+job.getId()+"\" value=\""+ACTIVATE+"\" "+(job.isActivated() ? "disabled readonly" : "")+"/>";
				String deactivate = "<input type=submit name=\""+job.getId()+"\" value=\""+DEACTIVATE+"\" "+(job.isActivated() ? "" : "disabled readonly")+"/>";

				result+="<tr>"+
					"<td align=center><input type=submit name=\""+job.getId()+"\" value=\""+START_CRONJOB+"\"/></td>"+
					"<td align=center><nobr>"+activate+" "+deactivate+"</nobr></td>"+
					"<td align=center>"+job.getDisplayedName()+"</td>"+
					"<td align=center>"+String.valueOf(job.getMinutesBetweenTwoJobs())+"</td>"+
					"<td align=center>"+(job.isRunning() ? "since "+(new Date().getTime()-job.getLastTimeStarted().getTime())+" ms" : "no")+"</td>"+
					"<td align=center style=\"font-weight:bold; color:"+((job.wasLastExecutionSuccessful()) ? "green" : "red")+"\">"+((job.wasLastExecutionSuccessful()) ? "OK" : "FAILED")+"</td>"+
					"<td align=center><nobr>"+job.getLastTimeStartedAsString()+"</nobr></td>"+
					"<td align=center><nobr>"+job.getTimeNeeded()+"</nobr></td>"+
					"<td align=center><nobr>"+job.getSuccessfulRuns()+"</nobr></td>"+
					"<td align=center><nobr>"+job.getNumberOfFails()+"</nobr></td>"+
					"<td align=center><nobr>"+job.getAverageTimeNeeded()+"</nobr></td>"+
					"</tr>";;
			}
			result+="</table><br>";
			for (final ObservedCronjob job : observedCronjobs)
			{
				List<StackTraceElement> elements= new ArrayList<StackTraceElement>();
				if (job.getLastException()!=null)
				{
					result+="<br><table width=100% cellpadding=0 cellspacing=0>";
					result+="<tr><td><h3>Stacktrace of last exception occured on Cronjob: "+job.getDisplayedName()+"&nbsp;&nbsp;" +
						"<input type=submit name=\""+job.getId()+"\" value=\""+DELETE_LAST_EXCEPTION+"\"/></h3></td></tr>";
					result+="<tr><td>"+job.getLastException().getMessage()+"</td></tr>";
					elements = Arrays.asList(job.getLastException().getStackTrace());
					for (final StackTraceElement el : elements)
					{
						result+="<tr><td align=left>"+el.toString()+"</td></tr>";
					}
					result+="</table>";
				}
			}
		}
		else
		{
			result+="<table width=100%>";
			if (active)
			{
				result+="<tr><td align=center><b>There are currently no cronjobs installed.</b></td></tr>"+
					"<tr><td>&nbsp;</td></tr>"+
					"<tr><td align=left>"+
					"To install a new cronjob, just follow the instuctions below:<br><br>"+
					"&nbsp;&nbsp;&nbsp;1. The cronjob-class has to implement the Cronjob-interface<br>"+
					"&nbsp;&nbsp;&nbsp;2. An instance of the cronjob-class must be added to the method getAllCronjobs() in the class:<b> "+storeName +"</b><br>"+
					"</td></tr>";
			}
			else
			{
				result+="<tr><td align=center><b>The cronjobs are not activated.</b></td></tr>"+
					"<tr><td>&nbsp;</td></tr>"+
					"<tr><td align=left>"+
					"To activate the cronjobs, the class:<b> "+storeName +"</b> has to return true in its isActive() method"+
					"</td></tr>";
			}
			result+="</table>";
		}
		result+="<br><table width=100%><tr><td align=right style=\"font-size:12 \"><hr width=100%>"+
			"exedio cronjob - "+getImplementationVersion()+" - &copy;  <a href=\"http://www.exedio.com\">exedio</a>" +
			" - Gesellschaft f&uuml;r Softwareentwicklung mbH (" + System.identityHashCode(this) + ")</td></tr></table>";
		result+="</td></tr></table></form></body></html>";
		out.println(result);
		out.close();
	}
}
