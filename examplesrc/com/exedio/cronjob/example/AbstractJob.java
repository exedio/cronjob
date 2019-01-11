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

package com.exedio.cronjob.example;

import static java.time.Duration.ofSeconds;

import com.exedio.cope.util.JobContext;
import com.exedio.cronjob.Job;
import java.time.Duration;

class AbstractJob implements Job
{
	protected boolean activeInitially = true;
	protected final String name;
	protected final Duration intervalBetweenExecutions;
	protected final Duration initialDelay;
	protected int result = 0;

	AbstractJob(final String name, final Duration intervalBetweenExecutions, final Duration initialDelay)
	{
		this.name = name;
		this.intervalBetweenExecutions = intervalBetweenExecutions;
		this.initialDelay = initialDelay;
	}

	@Override
	public String getName()
	{
		//System.out.println(name + ".getName");
		return name;
	}

	@Override
	public void init() throws Exception
	{
		System.out.println(name + ".init");
	}

	@Override
	public void destroy() throws Exception
	{
		System.out.println(name + ".destroy");
	}

	@Override
	public void run(final JobContext ctx) throws Exception
	{
		System.out.println(name + ".run" + (ctx.supportsProgress()?"":" NO PROGRESS"));
		ctx.incrementProgress(result++);
	}

	@Override
	public boolean isActiveInitially()
	{
		return activeInitially;
	}

	@Override
	public Duration getIntervalBetweenExecutions()
	{
		//System.out.println(name + ".getIntervalBetweenExecutions");
		return intervalBetweenExecutions;
	}

	@Override
	public Duration getInitialDelay()
	{
		//System.out.println(name + ".getInitialDelay"+initialDelay);
		return initialDelay;
	}

	@Override
	public Duration getStopTimeout()
	{
		return ofSeconds(5);
	}
}
