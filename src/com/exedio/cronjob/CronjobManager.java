/*
 * Copyright (C) 2006-2008  exedio GmbH (www.exedio.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.exedio.cronjob;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CronjobManager extends HttpServlet
{
	private static final long serialVersionUID =100000000000001L;
	
	private List<Handler> handlers;
		
	@Override
	public void init() throws ServletException
	{
		super.init();
		System.out.println("CronjobManager is starting ... (" + System.identityHashCode(this) + ')');
		final String STORE = "store";
		String storeNames=getServletConfig().getInitParameter(STORE);
		if (storeNames==null)
		{
			throw new RuntimeException("ERROR: Servlet-Init-Parameter: >> "+STORE+" << was expected but not found");
		}

		List<CronjobStore> stores = new ArrayList<CronjobStore>();
		for ( String storeName: storeNames.split(",") )
		{
			final Class<?> storeClass;
			try
			{
				storeClass = Class.forName(storeName);
			}
			catch (ClassNotFoundException e)
			{
				throw new RuntimeException("ERROR: A class with name: "+storeName+" was not found", e);
			}

			final Constructor<?> storeConstructor;
			try
			{
				storeConstructor = storeClass.getConstructor(ServletConfig.class);
			}
			catch(NoSuchMethodException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" has no suitable constructor", e);
			}

			final Object o;
			try
			{
				o = storeConstructor.newInstance(getServletConfig());
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
			
			if (o instanceof CronjobStore)
			{
				stores.add( (CronjobStore)o );
			}
			else
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" must implement the CronjobStore-interface");
			}
		}

		handlers = new ArrayList<Handler>();
		int idCounter = 1;
		for ( CronjobStore store: stores )
		{
			final long storeInitialDelay = store.getInitialDelayInMilliSeconds();
			for (final Job job: store.getJobs())
			{
				final Handler handler = new Handler(job, idCounter++, storeInitialDelay);
				handlers.add(handler);
				handler.startThread();
			}
		}
		System.out.println("CronjobManager is started. (" + System.identityHashCode(this) + ')');
	}
	
	@Override
	public void destroy()
	{
		System.out.println("CronjobManager is terminating ... (" + System.identityHashCode(this) + ')');
		for(final Handler job : handlers)
		{
			job.setActivated(false);
		}
		for(final Handler job : handlers)
		{
			job.stopThread();
		}
		System.out.println("CronjobManager is terminated. (" + System.identityHashCode(this) + ')');
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
	
	static final String ACTIVATE="on";
	static final String DEACTIVATE="off";
	static final String AUTO_REFRESH = "autoRefresh";
	static final String START_CRONJOB = "Start";
	static final String DELETE_LAST_EXCEPTION = "Delete";
	static final String ALL = "all";
	
	private void doRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException
	{
		final boolean autoRefreshPage = request.getParameter(AUTO_REFRESH)!=null;
		final String uriNoAutoRefresh = request.getContextPath() + request.getServletPath();
		final String uriAutoRefresh = uriNoAutoRefresh + '?' + AUTO_REFRESH+"=t";
		final String uri = autoRefreshPage ? uriAutoRefresh : uriNoAutoRefresh;
		
		if("POST".equals(request.getMethod()))
		{
			for(final Handler job : handlers)
			{
				final String[] params = request.getParameterValues(job.id);
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
			final String[] params = request.getParameterValues(ALL);
			if(params!=null)
			{
				final List<String> paramsAsList = Arrays.asList(params);
				if(paramsAsList.contains(ACTIVATE))
				{
					for(final Handler job : handlers)
						job.setActivated(true);
				}
				else if(paramsAsList.contains(DEACTIVATE))
				{
					for(final Handler job : handlers)
						job.setActivated(false);
				}
				else
				{
					throw new RuntimeException(paramsAsList.toString());
				}
			}
			response.sendRedirect(uri);
		}
		
		final Principal principal = request.getUserPrincipal();
		final String authentication = principal!=null ? principal.getName() : null;
		String hostname = null;
		try
		{
			hostname = InetAddress.getLocalHost().getHostName();
		}
		catch(UnknownHostException e)
		{
			// leave hostname==null
		}
		final long now = System.currentTimeMillis();
		response.setContentType("text/html; charset=utf-8");
		PrintWriter out = response.getWriter();
		Page_Jspm.write(out,
				uri,
				uriNoAutoRefresh,
				uriAutoRefresh,
				authentication,
				hostname,
				now,
				new SimpleDateFormat("yyyy/MM/dd'&nbsp;'HH:mm:ss.SSS Z (z)").format(new Date(now)),
				autoRefreshPage,
				handlers,
				getImplementationVersion(),
				System.identityHashCode(this));
		out.close();
	}
}
