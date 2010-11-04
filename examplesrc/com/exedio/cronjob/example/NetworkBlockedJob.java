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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.exedio.cronjob.ExperimentalTaskContext;

final class NetworkBlockedJob extends AbstractJob
{
	NetworkBlockedJob()
	{
		super("NetworkBlocked", 60, 24*60*60*1000);
	}
	
	@Override
	public void run(final ExperimentalTaskContext ctx) throws IOException
	{
		System.out.println(name + ".run start");
		final URL url = new URL("http://www.exedio.com:1234/");
		final HttpURLConnection con = (HttpURLConnection)url.openConnection();
		final int responseCode = con.getResponseCode();
		System.out.println(name + ".run ready (" + responseCode +')');
		ctx.notifyProgress(result++);
	}
}
