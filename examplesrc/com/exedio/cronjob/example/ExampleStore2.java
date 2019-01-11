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

import com.exedio.cronjob.CronjobStore;
import com.exedio.cronjob.Job;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletConfig;

public final class ExampleStore2 implements CronjobStore
{
	/**
	 * @param config is used by other subclasses for initialization
	 */
	public ExampleStore2(final ServletConfig config)
	{
		System.out.println("ExampleStore2 constructor >" + config.getServletName() + '<');
	}

	@Override
	public List<? extends Job> getJobs()
	{
		System.out.println("ExampleStore2.getJobs");
		final NormalJob job = new NormalJob(10);
		job.activeInitially = false;
		return Collections.singletonList(job);
	}

	@Override
	public Duration getInitialDelay()
	{
		System.out.println("ExampleStore2.getInitialDelay");
		return ofSeconds(1);
	}
}
