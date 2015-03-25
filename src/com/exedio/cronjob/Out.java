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

import com.exedio.cops.BodySender;
import com.exedio.cops.Cop;
import com.exedio.cops.CopsServlet;
import com.exedio.cops.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final class Out
{
	private final StringBuilder bf;
	private final long now = System.currentTimeMillis();
	private final SimpleDateFormat dateFormatFull  = new SimpleDateFormat("yyyy/MM/dd'&nbsp;'HH:mm:ss'<small>'.SSS'</small>'");
	private final SimpleDateFormat dateFormatYear  = new SimpleDateFormat(     "MM/dd'&nbsp;'HH:mm:ss'<small>'.SSS'</small>'");
	private final SimpleDateFormat dateFormatToday = new SimpleDateFormat(                  "HH:mm:ss'<small>'.SSS'</small>'");

	private final HttpServletRequest request;
	private final HttpServletResponse response;

	Out(
			final HttpServletRequest request,
			final HttpServletResponse response)
	{
		this.bf = new StringBuilder();
		this.request = request;
		this.response = response;
	}

	void writeStatic(final String s)
	{
		bf.append(s);
	}

	void write(final String s)
	{
		bf.append(s);
	}

	void write(final Date d)
	{
		if(d==null)
			return;

		final long millis = d.getTime();
		final SimpleDateFormat df;
		if( (now-deltaToday) < millis && millis < (now+deltaToday) )
			df = dateFormatToday;
		else if( (now-deltaYear) < millis && millis < (now+deltaYear) )
			df = dateFormatYear;
		else
			df = dateFormatFull;

		bf.append(df.format(d));
	}

	private static final long deltaYear  = 1000l * 60 * 60 * 24 * 90; // 90 days
	private static final long deltaToday = 1000l * 60 * 60 * 6; // 6 hours


	void write(final boolean b)
	{
		bf.append(b);
	}

	void write(final int i)
	{
		bf.append(i);
	}

	void write(final long l)
	{
		bf.append(l);
	}

	void write(final Resource resource)
	{
		bf.append(resource.getURL(request));
	}

	void write(final Cop cop)
	{
		bf.append(cop.getURL(request));
	}

	void sendBody() throws IOException
	{
		BodySender.send(response, bf, CopsServlet.UTF8);
	}
}
