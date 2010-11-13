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

final class JobCop extends PageCop
{
	static final String PATH_INFO = "job.html";
	private static final String JOB = "j";

	static final String SAMPLE = "sample";

	final String id;

	JobCop(
			final boolean autoRefresh,
			final String id)
	{
		super(PATH_INFO, autoRefresh);

		this.id = id;

		addParameter(JOB, id);
	}

	static JobCop getCop(final boolean autoRefresh, final HttpServletRequest request)
	{
		return new JobCop(
				autoRefresh,
				request.getParameter(JOB));
	}

	@Override
	JobCop toAutoRefresh(final boolean autoRefresh)
	{
		return new JobCop(autoRefresh, id);
	}

	@Override
	void post(final HttpServletRequest request, final List<Handler> handlers)
	{
		if(request.getParameter(SAMPLE)!=null)
		{
			final Handler handler = handler(handlers);
			final RunContext ctx = handler.getRunContext();
			if(ctx!=null)
				ctx.sample();
			return;
		}
		super.post(request, handlers);
	}

	@Override
	void write(final Out out, final long now, final List<Handler> handlers)
	{
		Job_Jspm.write(out, now, handler(handlers));
	}

	private Handler handler(final List<Handler> handlers)
	{
		for(final Handler handler : handlers)
			if(id.equals(handler.id))
				return handler;

		throw new RuntimeException(id);
	}
}
