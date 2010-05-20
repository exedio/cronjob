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

import com.exedio.cope.util.Interrupter;

public interface Job
{
	String getName();

	/**
	 * @return
	 *    An arbitrary number, that is displayed by the cronjob maintenance servlet.
	 *    Typically you may want to return something like the number items processed by the job.
	 *    The cronjob library does never use this number for any program logic,
	 *    it just displays the number (and it's average / maximum).
	 */
	int run(Interrupter interrupter) throws Exception;

	int getMinutesBetweenExecutions();
	
	long getInitialDelayInMilliSeconds();

	/**
	 * @return the time (in milliseconds) the job will be given to finish before it
	 *			will be stopped forcefully
	 */
	long getStopTimeout();
}
