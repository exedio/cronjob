/*
 * Copyright (C) 2004-2006  exedio GmbH (www.exedio.com)
 */
package com.exedio.cronjob;

public interface Cronjob
{
	public void excecuteJob() throws Exception;
	public int getMinutesBetweenTwoJobs();
	public String getName();
}
