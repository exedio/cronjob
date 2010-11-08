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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.exedio.cops.Cop;

abstract class PageCop extends Cop
{
	private static final String AUTO_REFRESH = "autoRefresh";

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

	void post(@SuppressWarnings("unused") final HttpServletRequest request, @SuppressWarnings("unused") final List<Handler> handlers)
	{
		// empty TODO
	}

	abstract PageCop toAutoRefresh(boolean autoRefresh);
	abstract void write(Out out, long now, List<Handler> handlers);
}
