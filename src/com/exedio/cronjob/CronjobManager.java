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

import com.exedio.cops.CopsServlet;
import com.exedio.cops.Resource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronjobManager extends CopsServlet
{
	static final Logger logger = LoggerFactory.getLogger( "com.exedio.cronjob" );

	private static final long serialVersionUID =100000000000001L;

	static final Resource stylesheet = new Resource("cronjob.css");
	static final Resource logo = new Resource("logo.png");
	static final Resource shortcutIcon = new Resource("shortcutIcon.png");

	@SuppressWarnings("NonSerializableFieldInSerializableClass")
	@SuppressFBWarnings("SE_BAD_FIELD")
	private List<Handler> handlers;

	@Override
	public void init() throws ServletException
	{
		super.init();
		if (logger.isInfoEnabled())
			logger.info("CronjobManager is starting ... (" + System.identityHashCode(this) + ')');
		final String STORE = "store";
		final String storeNames=getServletConfig().getInitParameter(STORE);
		if (storeNames==null)
		{
			throw new RuntimeException("ERROR: Servlet-Init-Parameter: >> "+STORE+" << was expected but not found");
		}

		final List<CronjobStore> stores = new ArrayList<>();
		for ( final String storeName: storeNames.split(",") )
		{
			final Class<?> storeClass;
			try
			{
				storeClass = Class.forName(storeName);
			}
			catch (final ClassNotFoundException e)
			{
				throw new RuntimeException("ERROR: A class with name: "+storeName+" was not found", e);
			}

			final Constructor<?> storeConstructor;
			try
			{
				storeConstructor = storeClass.getConstructor(ServletConfig.class);
			}
			catch(final NoSuchMethodException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" has no suitable constructor", e);
			}

			final Object o;
			try
			{
				o = storeConstructor.newInstance(getServletConfig());
			}
			catch(final InvocationTargetException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" constructor throw exception", e);
			}
			catch(final InstantiationException e)
			{
				throw new RuntimeException("ERROR: Class "+storeClass+" could not be instantiated (must not be abstract or an interface)", e);
			}
			catch(final IllegalAccessException e)
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

		handlers = new ArrayList<>();
		int idCounter = 1;
		for ( final CronjobStore store: stores )
		{
			final Duration storeInitialDelay = store.getInitialDelay();
			for (final Job job: store.getJobs())
				handlers.add(new Handler(job, idCounter++, storeInitialDelay));
		}

		// start threads at the very end
		// so that errors in code above do not
		// leave running threads
		for(final Handler handler : handlers)
			handler.startThread();

		if (logger.isInfoEnabled())
			logger.info("CronjobManager is started. (" + System.identityHashCode(this) + ')');
	}

	@Override
	public void destroy()
	{
		if (logger.isInfoEnabled())
			logger.info("CronjobManager is terminating ... (" + System.identityHashCode(this) + ')');
		for(final Handler job : handlers)
		{
			job.setActivated(false, new JobStopInfoDestroy(job));
		}
		for(final Handler job : handlers)
		{
			job.stopThread();
		}
		if (logger.isInfoEnabled())
			logger.info("CronjobManager is terminated. (" + System.identityHashCode(this) + ')');
	}

	private static String getImplementationVersion()
	{
		final String iv=CronjobManager.class.getPackage().getImplementationVersion();
		return iv==null ? "" : iv;
	}

	@Override
	protected void doRequest(
			final HttpServletRequest request,
			final HttpServletResponse response)
	throws IOException
	{
		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy
		response.setHeader("Content-Security-Policy",
				"default-src 'none'; " +
				"style-src 'self'; " +
				"img-src 'self'; " +
				"frame-ancestors 'none'; " +
				"block-all-mixed-content; " +
				"base-uri 'none'");

		// Do not leak information to external servers, not even the (typically private) hostname.
		// We need the referer within the servlet, because typically there is a StrictRefererValidationFilter.
		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy
		response.setHeader("Referrer-Policy", "same-origin");

		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options
		response.setHeader("X-Content-Type-Options", "nosniff");

		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options
		response.setHeader("X-Frame-Options", "deny");

		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection
		response.setHeader("X-XSS-Protection", "1; mode=block");

		final PageCop cop = PageCop.getCop(request);

		if("POST".equals(request.getMethod()))
		{
			cop.post(request, handlers);
			response.sendRedirect(cop.getAbsoluteURL(request));
		}

		final Principal principal = request.getUserPrincipal();
		final String authentication = principal!=null ? principal.getName() : null;
		String hostname = null;
		try
		{
			hostname = InetAddress.getLocalHost().getHostName();
		}
		catch(final UnknownHostException ignored)
		{
			// leave hostname==null
		}
		final long now = System.currentTimeMillis();
		response.setContentType("text/html; charset=utf-8");
		final Out out = new Out(request, response);
		Page_Jspm.write(out,
				cop,
				authentication,
				hostname,
				now,
				new SimpleDateFormat("yyyy/MM/dd'&nbsp;'HH:mm:ss.SSS Z (z)").format(new Date(now)),
				handlers,
				getImplementationVersion(),
				System.identityHashCode(this));
		out.sendBody();
	}
}
