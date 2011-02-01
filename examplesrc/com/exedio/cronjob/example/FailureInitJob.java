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

final class FailureInitJob extends AbstractJob
{
	private int fail = 0;
	private boolean active = false;

	FailureInitJob(final int number)
	{
		super("FailureInit" + number, 1, 1000);
		activeInitially = false;
	}

	@Override
	public void init()
	{
		if(active)
			throw new IllegalStateException("init: already initialized");
		if(fail++<3)
			throw new RuntimeException("example exception from " + name + "#init");

		active = true;
		fail = 0;
	}

	@Override
	public void destroy()
	{
		if(!active)
			throw new IllegalStateException("destroy: not yet initialized");
		if(fail++<3)
			throw new RuntimeException("example exception from " + name + "#destroy");

		active = false;
		fail = 0;
	}

	@Override
	public void run(final JobContext ctx)
	{
		if(!active)
			throw new IllegalStateException("run: not yet initialized");
		if(fail++<3)
			throw new RuntimeException("example exception from " + name + "#run");

		fail = 0;
	}
}
