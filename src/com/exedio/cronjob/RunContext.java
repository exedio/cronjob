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

import com.exedio.cope.util.EmptyJobContext;
import com.exedio.cope.util.JobStop;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RunContext extends EmptyJobContext
{
	private final Handler handler;
	final long nanos = System.nanoTime();

	@SuppressFBWarnings("VO_VOLATILE_INCREMENT")
	private volatile int progress = 0;
	private final ArrayList<Sample> samples = new ArrayList<>();

	RunContext(final Handler handler)
	{
		this.handler = handler;
	}

	@Override
	public void stopIfRequested() throws JobStop
	{
		handler.stopIfRequested();
	}

	@Override
	@Deprecated
	public boolean requestedToStop()
	{
		try
		{
			handler.stopIfRequested();
			return false;
		}
		catch(final JobStop js)
		{
			return true;
		}
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
				? Collections.emptyList()
				: new ArrayList<>(samples);
		}
	}
}
