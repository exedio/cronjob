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

import com.exedio.cronjob.Interrupter;

public class InterruptableSlowerJob extends AbstractJob
{
	InterruptableSlowerJob()
	{
		super("InterruptableSlowerJob", 1000, 0);
	}
	
	@Override
	public void execute(final Interrupter interrupter)
	{
		System.out.println(name + ".execute start");
		try
		{
			Thread.sleep(1000);
			System.out.println(name + ".execute slept 1");
			if(interrupter.isRequested())
			{
				System.out.println(name + ".execute interrupted");
				return;
			}
			Thread.sleep(3000);
			System.out.println(name + ".execute slept 2");
			if(interrupter.isRequested())
			{
				System.out.println(name + ".execute interrupted");
				return;
			}
			Thread.sleep(5000);
			System.out.println(name + ".execute slept 3");
			if(interrupter.isRequested())
			{
				System.out.println(name + ".execute interrupted");
				return;
			}
			Thread.sleep(8000);
			System.out.println(name + ".execute slept 8");
			if(interrupter.isRequested())
			{
				System.out.println(name + ".execute interrupted");
				return;
			}
			Thread.sleep(10000);
			System.out.println(name + ".execute slept 10");
		}
		catch(InterruptedException e)
		{
			throw new RuntimeException(e);
		}
		System.out.println(name + ".execute ready");
	}
}
