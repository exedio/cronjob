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

import com.exedio.cope.util.JobStop;
import java.util.Date;

@SuppressWarnings("AbstractClassWithoutAbstractMethods")
abstract class JobStopInfo
{
	private final String jobName;
	private final String action;
	private final long timeNanos = System.nanoTime();
	private final long timeMillis = System.currentTimeMillis();

	JobStopInfo(final Handler handler, final String action)
	{
		this.jobName = handler.jobName;
		this.action = action;
	}

	final JobStop newJobStop()
	{
		final StringBuilder bf = new StringBuilder();
		bf.append("cronjob ").
			append(jobName).
			append(" was ").
			append(action).
			append(" on ").
			append(new Date(timeMillis)).
			append(" (").
			append(System.nanoTime()-timeNanos).
			append("ns ago)");
		completeMessage(bf);
		return new JobStop(bf.toString());
	}

	void completeMessage(@SuppressWarnings("unused") final StringBuilder bf)
	{
		// nothing
	}
}
