/*
 * Copyright (C) 2004-2006  exedio GmbH (www.exedio.com)
 */

package com.exedio.cronjob;

public interface Cronjob
{
	void executeJob() throws Exception;
	int getMinutesBetweenTwoJobs();
	String getName();
	int getInitialDelayInMilliSeconds();	
}
