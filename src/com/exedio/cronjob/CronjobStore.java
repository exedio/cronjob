/*
 * Copyright (C) 2004-2006  exedio GmbH (www.exedio.com)
 */

package com.exedio.cronjob;

import java.util.List;

public interface CronjobStore
{
	List<Cronjob> getAllCronjobs();
	int getInitialDelayInMilliSeconds();
	boolean isActive();
}
