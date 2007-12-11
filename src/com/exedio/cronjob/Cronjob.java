/*
 * Copyright (C) 2004-2006  exedio GmbH (www.exedio.com)
 */

package com.exedio.cronjob;

public interface Cronjob
{
	public void executeJob() throws Exception;
	public int getMinutesBetweenTwoJobs();
	public String getName();
	public int getInitialDelayInMilliSeconds();	
}
