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

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletConfig;

import com.exedio.cronjob.CronjobStore;
import com.exedio.cronjob.Job;

public final class ExampleStore2 implements CronjobStore
{
	public ExampleStore2(final ServletConfig config)
	{
		System.out.println("ExampleStore constructor");
	}
	
	public List<? extends Job> getJobs()
	{
		System.out.println("ExampleStore.getJobs");
		return Arrays.asList(new NormalJob(10));
	}

	public long getInitialDelayInMilliSeconds()
	{
		System.out.println("ExampleStore.getInitialDelayInMilliSeconds");
		return 1000;
	}

	public boolean isActive()
	{
		System.out.println("ExampleStore.isActive");
		return true;
	}
}
