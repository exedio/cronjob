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

final class HomeCop extends PageCop
{
	private static final String SHOW_CONFIGURATION = "cf";
	private static final String SHOW_LAST_RUN = "lr";

	final boolean showConfiguration;
	final boolean showLastRun;

	HomeCop(
			final boolean autoRefresh,
			final boolean showConfiguration,
			final boolean showLastRun)
	{
		super("", autoRefresh);

		this.showConfiguration = showConfiguration;
		this.showLastRun = showLastRun;

		addParameter(SHOW_CONFIGURATION, showConfiguration);
		addParameter(SHOW_LAST_RUN, showLastRun);
	}

	static HomeCop getCop(final boolean autoRefresh, final HttpServletRequest request)
	{
		return new HomeCop(
				autoRefresh,
				getBooleanParameter(request, SHOW_CONFIGURATION),
				getBooleanParameter(request, SHOW_LAST_RUN));
	}

	@Override
	HomeCop toAutoRefresh(final boolean autoRefresh)
	{
		return new HomeCop(autoRefresh, showConfiguration, showLastRun);
	}

	HomeCop toShowConfiguration(final boolean showConfiguration)
	{
		return new HomeCop(autoRefresh, showConfiguration, showLastRun);
	}

	HomeCop toShowLastRun(final boolean showLastRun)
	{
		return new HomeCop(autoRefresh, showConfiguration, showLastRun);
	}

	JobCop toJob(final Handler job)
	{
		return new JobCop(autoRefresh, job.id);
	}

	@Override
	void write(final Out out, final long now, final List<Handler> handlers)
	{
		int active = 0;
		int inactive = 0;
		int running = 0;
		for(final Handler job : handlers)
		{
			if(job.isActivated())
				active++;
			else
				inactive++;

			if(job.getRunContext()!=null)
				running++;
		}
		Home_Jspm.write(out, this, active, inactive, running, now, handlers);
	}
}
