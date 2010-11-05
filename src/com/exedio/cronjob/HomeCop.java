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

import javax.servlet.http.HttpServletRequest;

import com.exedio.cops.Cop;

final class HomeCop extends Cop
{
	private static final String AUTO_REFRESH = "autoRefresh";
	private static final String SHOW_CONFIGURATION = "cf";
	private static final String SHOW_LAST_RUN = "lr";

	final boolean autoRefresh;
	final boolean showConfiguration;
	final boolean showLastRun;

	HomeCop(
			final boolean autoRefresh,
			final boolean showConfiguration,
			final boolean showLastRun)
	{
		super("");

		this.autoRefresh = autoRefresh;
		this.showConfiguration = showConfiguration;
		this.showLastRun = showLastRun;

		addParameter(AUTO_REFRESH, autoRefresh);
		addParameter(SHOW_CONFIGURATION, showConfiguration);
		addParameter(SHOW_LAST_RUN, showLastRun);
	}

	static HomeCop getCop(final HttpServletRequest request)
	{
		return new HomeCop(
				getBooleanParameter(request, AUTO_REFRESH),
				getBooleanParameter(request, SHOW_CONFIGURATION),
				getBooleanParameter(request, SHOW_LAST_RUN));
	}

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
}
