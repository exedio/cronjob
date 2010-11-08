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

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.exedio.cops.Cop;

abstract class PageCop extends Cop
{
	private static final String AUTO_REFRESH = "autoRefresh";

	static final String ACTIVATE="on";
	static final String DEACTIVATE="off";
	static final String START_CRONJOB = "Start";
	static final String DELETE_LAST_EXCEPTION = "Delete";
	static final String ALL = "all";

	final boolean autoRefresh;

	PageCop(
			final String pathInfo,
			final boolean autoRefresh)
	{
		super(pathInfo);

		this.autoRefresh = autoRefresh;

		addParameter(AUTO_REFRESH, autoRefresh);
	}

	static PageCop getCop(final HttpServletRequest request)
	{
		final boolean autoRefresh = getBooleanParameter(request, AUTO_REFRESH);
		final String pathInfo = request.getPathInfo();

		if(('/' + JobCop.PATH_INFO).equals(pathInfo))
			return JobCop.getCop(autoRefresh, request);
		return HomeCop.getCop(autoRefresh, request);
	}

	final HomeCop toHome()
	{
		return new HomeCop(autoRefresh, false, false);
	}

	void post(final HttpServletRequest request, final List<Handler> handlers)
	{
		for(final Handler handler : handlers)
		{
			final String[] params = request.getParameterValues(handler.id);
			if (params!=null)
			{
				final List<String> paramsAsList = Arrays.asList(params);
				if (paramsAsList.contains(START_CRONJOB))
				{
					handler.runNow();
				}
				else if (paramsAsList.contains(DELETE_LAST_EXCEPTION))
				{
					handler.removeLastException();
				}
				else if (paramsAsList.contains(ACTIVATE))
				{
					handler.setActivated(true);
				}
				else if (paramsAsList.contains(DEACTIVATE))
				{
					handler.setActivated(false);
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
				for(final Handler handler : handlers)
					handler.setActivated(true);
			}
			else if(paramsAsList.contains(DEACTIVATE))
			{
				for(final Handler handler : handlers)
					handler.setActivated(false);
			}
			else
			{
				throw new RuntimeException(paramsAsList.toString());
			}
		}
	}

	abstract PageCop toAutoRefresh(boolean autoRefresh);
	abstract void write(Out out, long now, List<Handler> handlers);
}
