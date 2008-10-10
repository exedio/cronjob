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

import com.exedio.cronjob.Job;

public class AbstractJob implements Job
{
	private final String name;
	
	AbstractJob(final String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		System.out.println(name + ".getName");
		return name;
	}

	public void execute() throws Exception
	{
		System.out.println(name + ".execute");
	}

	public int getMinutesBetweenExecutions()
	{
		System.out.println(name + ".getMinutesBetweenExecutions");
		return 1;
	}
	
	public int getInitialDelayInMilliSeconds()
	{
		System.out.println(name + ".getInitialDelayInMilliSeconds");
		return 1000;
	}
}
