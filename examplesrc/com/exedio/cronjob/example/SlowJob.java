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

public class SlowJob extends AbstractJob
{
	SlowJob(final int number, final int initialDelay)
	{
		super("SlowJob" + number, 1000, initialDelay);
	}
	
	@Override
	public void execute()
	{
		System.out.println(name + ".execute start");
		try
		{
			Thread.sleep(10000);
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		System.out.println(name + ".execute ready");
	}
}
