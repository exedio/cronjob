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

import com.exedio.cope.util.JobContext;
import com.exedio.cronjob.Job;

class AbstractJob implements Job
{
	protected boolean activeInitially = true;
	protected final String name;
	protected final int minutesBetweenExecutions;
	protected final long initialDelay;
	protected int result = 0;
	
	AbstractJob(final String name, final int minutesBetweenExecutions, final long initialDelay)
	{
		this.name = name;
		this.minutesBetweenExecutions = minutesBetweenExecutions;
		this.initialDelay = initialDelay;
	}
	
	public String getName()
	{
		//System.out.println(name + ".getName");
		return name;
	}

	public void run(JobContext ctx) throws Exception
	{
		System.out.println(name + ".run" + (ctx.supportsProgress()?"":" NO PROGRESS"));
		ctx.incrementProgress(result++);
	}

	public boolean isActiveInitially()
	{
		return activeInitially;
	}
	
	public int getMinutesBetweenExecutions()
	{
		//System.out.println(name + ".getMinutesBetweenExecutions");
		return minutesBetweenExecutions;
	}
	
	public long getInitialDelayInMilliSeconds()
	{
		//System.out.println(name + ".getInitialDelayInMilliSeconds"+initialDelay);
		return initialDelay;
	}
	
	public long getStopTimeout()
	{
		return 5000;
	}
}
