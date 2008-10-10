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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CronjobManager extends HttpServlet
{
	private static final long serialVersionUID =100000000000001L;
	
	private List<ObservedCronjob> observedCronjobs;
	private String storeName;
	private boolean active;
		
	@Override
	public void init() throws ServletException
	{
		super.init();
		System.out.println("CronjobManager is starting ... (" + System.identityHashCode(this) + ')');
		final String STORE = "store";
		storeName=getServletConfig().getInitParameter(STORE);
		if (storeName==null)
		{
			throw new RuntimeException("ERROR: Servlet-Init-Parameter: >> "+STORE+" << was expected but not found");
		}

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
		CronjobStore store = null;		
		if (o instanceof CronjobStore)
		{
			store=(CronjobStore)o;
			observedCronjobs = new ArrayList<ObservedCronjob>();
			active=store.isActive();
			if (store.isActive())
			{
				int idCounter = 1;
				for (final Job job: store.getJobs())
				{
					ObservedCronjob observedCronjob = new ObservedCronjob(job, idCounter++, store.getInitialDelayInMilliSeconds());
					observedCronjobs.add(observedCronjob);
					observedCronjob.startThread();
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
		for (final ObservedCronjob job :observedCronjobs)
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
	static final String TRUE = "true";
	static final String FALSE = "false";
	static final String AUTO_REFRESH = "autoRefresh";
	static final String START_CRONJOB = "Start";
	static final String DELETE_LAST_EXCEPTION = "Delete";
	static final String ENABLE_AUTOREFRESH = "Enable Auto-Refresh";
	static final String DISABLE_AUTOREFRESH = "Disable Auto-Refresh";
	
	private void doRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
	{
		String uri=request.getRequestURI();
		// Start/Activate/Deactivate/removeLastException for selected Cronjob
		
		if("POST".equals(request.getMethod()))
		{
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
			response.sendRedirect(uri);
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
		response.setContentType("text/html; charset=utf-8");
		PrintWriter out = response.getWriter();
		Page_Jspm.write(out,
				uri+"?"+AUTO_REFRESH+"="+(autoRefreshPage ? TRUE : FALSE),
				System.currentTimeMillis(),
				autoRefreshPage,
				enableOrDisableAutoRefreshButton,
				observedCronjobs,
				getImplementationVersion(),
				System.identityHashCode(this),
				active);
		out.close();
	}
}
