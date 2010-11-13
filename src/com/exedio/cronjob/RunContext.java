/*
 * Copyright (C) 2004-2009  exedio GmbH (www.exedio.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exedio.cope.util.EmptyJobContext;

final class RunContext extends EmptyJobContext
{
	private final Handler handler;
	final long nanos = System.nanoTime();

	private volatile int progress = 0;
	private final ArrayList<Sample> samples = new ArrayList<Sample>();

	RunContext(final Handler handler)
	{
		this.handler = handler;
	}

	@Override
	public boolean requestedToStop()
	{
		return handler.requestsStop();
	}

	@Override
	public boolean supportsProgress()
	{
		return true;
	}

	@Override
	public void incrementProgress()
	{
		progress++;
	}

	@Override
	public void incrementProgress(final int delta)
	{
		progress += delta;
	}

	int getProgress()
	{
		return progress;
	}

	void sample()
	{
		final Sample sample = new Sample(this, progress);

		synchronized(samples)
		{
			samples.add(sample);
		}
	}

	List<Sample> getSamples()
	{
		synchronized(samples)
		{
			return
				samples.isEmpty()
				? Collections.<Sample>emptyList()
				: new ArrayList<Sample>(samples);
		}
	}
}