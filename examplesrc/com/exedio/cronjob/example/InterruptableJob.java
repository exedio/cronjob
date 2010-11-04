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

import com.exedio.cronjob.ExperimentalTaskContext;

final class InterruptableJob extends AbstractJob
{
	InterruptableJob()
	{
		super("Interruptable", 1000, 0);
	}
	
	@Override
	public void run(final ExperimentalTaskContext ctx)
	{
		System.out.println(name + ".run start");
		try
		{
			for(int i = 0; i<10; i++)
			{
				Thread.sleep(1000);
				System.out.println(name + ".run slept " + i);
				ctx.notifyProgress(result++);
				if(ctx.requestsStop())
				{
					System.out.println(name + ".run interrupted");
					return;
				}
			}
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		System.out.println(name + ".run ready");
		ctx.notifyProgress(result++);
	}
}
