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

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

final class JobStopInfoRequest extends JobStopInfo
{
	private final String authentication;
	private final String address;
	private final String agent;

	JobStopInfoRequest(
			final Handler handler,
			final String action,
			final HttpServletRequest request)
	{
		super(handler, action);
		final Principal principal = request.getUserPrincipal();
		this.authentication = principal!=null ? principal.getName() : null;
		this.address = request.getRemoteAddr();
		this.agent = request.getHeader("User-Agent");
	}

	@Override
	void completeMessage(final StringBuilder bf)
	{
		if(authentication!=null)
			bf.append(" autenticated as ").
				append(authentication);

		if(address!=null)
			bf.append(" from address ").
				append(address);

		if(agent!=null)
			bf.append(" using agent ").
				append(agent);
	}
}
