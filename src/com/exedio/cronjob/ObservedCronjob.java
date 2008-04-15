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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

final class ObservedCronjob
{
	private final int DURATION_BETWEEN_CHECKS=2705;
	private final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
	private final DateFormat DATE_FORMAT_SIMPLE = new SimpleDateFormat("HH:mm:ss");
	
	private String id=null;
	private Cronjob cronjob;
	private boolean running;
	private Date lastTimeStarted;
	private Exception lastException;
	private boolean lastExecutionSuccessful;
	private boolean activated;
	private int successfulRuns;
	private long averageTimeNeeded;
	private long timeNeeded;
	private boolean runNow;
	private int fails;
	private RunningThread runningThread;
	private Date createdAt;
	private int initialDelayinMS;
	
	/**
	 * After construction use #startThread() to start running of the job.
	 */
	ObservedCronjob(final Cronjob cronjob, final String id, final int initialDelayInMS)
	{
		this.id=id;
		this.cronjob=cronjob;
		running=false;
		lastTimeStarted=null;
		lastException=null;
		lastExecutionSuccessful=true;
		activated=true;
		successfulRuns=0;
		averageTimeNeeded=0;
		timeNeeded=0;
		fails=0;
		runNow=false;
		createdAt=new Date();
		this.initialDelayinMS=initialDelayInMS+cronjob.getInitialDelayInMilliSeconds();
	}
	
	private boolean timeForExcecution()
	{
		if (lastTimeStarted == null)
		{
			return true;
		}
		else
		{
			Long last = lastTimeStarted.getTime();
			Long now = new Date().getTime();
			if ((now-last)>=(cronjob.getMinutesBetweenTwoJobs()*1000*60))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	String getDisplayedName()
	{
		return cronjob.getClass().getName()+((cronjob.getName()!=null) ? " ("+cronjob.getName()+")" : " (name not specified)");
	}
	
	private void tryToExecute()
	{
		if (canExecuteChronjob())
		{
			running=true;
			lastTimeStarted=new Date();
			long msb=lastTimeStarted.getTime();
			System.out.println("\nStarting Cronjob: "+getDisplayedName()+" at "+DATE_FORMAT.format(lastTimeStarted));
			try
			{
				cronjob.executeJob();
				Date finished =new Date();
				lastExecutionSuccessful=true;
				System.out.println("Finished Cronjob: "+getDisplayedName()+" at "+DATE_FORMAT.format(finished)+"\n");
				long msa=finished.getTime();
				timeNeeded=msa-msb;
				updateAverageTimeNeeded(timeNeeded);
			}
			catch (Exception e)
			{
				lastException=e;
				fails++;
				Date failedAt= new Date();
				timeNeeded=failedAt.getTime()-msb;
				System.out.println("Execution of Cronjob: "+getDisplayedName()+" FAILED at "+DATE_FORMAT.format(failedAt)+" !!!");
				System.out.println("******************** CronjobException - START ********************");
				e.printStackTrace();
				System.out.println("******************** CronjobException - END **********************");
				lastExecutionSuccessful=false;
			}
			finally
			{
				running=false;
			}
		}
	}
	
	private void updateAverageTimeNeeded(final long timeNeeded)
	{
		if (successfulRuns==0)
		{
			averageTimeNeeded=timeNeeded;
		}
		else
		{
			averageTimeNeeded=(((successfulRuns*averageTimeNeeded) + timeNeeded)/(successfulRuns+1));
		}
		successfulRuns++;
	}
	
	private boolean canExecuteChronjob()
	{
		if (runNow && !running)
		{
			runNow=false;
			return true;
		}
		boolean result = true;
		if ((new Date().getTime()-createdAt.getTime())<initialDelayinMS)
		{
			result=false;
		}
		if (running)
		{
			result=false;
		}
		if (!timeForExcecution())
		{
			result=false;
		}
		if (!activated)
		{
			result=false;
		}
		return result;
	}
	
	private boolean isToday(final Date date)
	{
		Calendar c = new GregorianCalendar();
		c.getInstance();
		c.set(Calendar.HOUR_OF_DAY,0);
		c.set(Calendar.MINUTE,0);
		c.set(Calendar.SECOND,0);
		c.set(Calendar.MILLISECOND,0);
		Date lastMidnight=c.getTime();
		c.add(Calendar.DATE,1);
		Date nextMidnight = c.getTime();
		if (date.after(lastMidnight) && nextMidnight.after(date))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	String getLastTimeStartedAsString()
	{
		if (lastTimeStarted!=null)
		{
			if (isToday(lastTimeStarted))
				return DATE_FORMAT_SIMPLE.format(lastTimeStarted);	
			else
				return DATE_FORMAT.format(lastTimeStarted);	
		}
		else
		{
			return "x";
		}
	}
	
	void setActivated(boolean activated)
	{
		this.activated = activated;
		System.out.println("Cronjob: "+getDisplayedName()+" was deactivated at "+DATE_FORMAT.format(new Date()));
	}
	
	void runNow()
	{
		runNow=true;
		runningThread.notifyWaiter();
		try
		{
			Thread.sleep(100); //This for updating the running Parameter
		} 
		catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}
	
	String getId() {return id;	}
	boolean wasLastExecutionSuccessful()	{return lastExecutionSuccessful;	}
	void removeLastException(){lastException=null;}
	boolean isActivated(){	return activated;	}
	long getAverageTimeNeeded(){ return averageTimeNeeded;}
	long getTimeNeeded() {return timeNeeded;}
	int getSuccessfulRuns(){return successfulRuns;}
	int getNumberOfFails(){return fails;}
	Exception getLastException(){return lastException;}
	boolean isRunning() {return running;}
	int getMinutesBetweenTwoJobs() {return cronjob.getMinutesBetweenTwoJobs();}
	Date getLastTimeStarted() {return lastTimeStarted;}
	
	void startThread()
	{
		runningThread = new RunningThread();
		runningThread.start();
	}
	
	class RunningThread extends Thread
	{
		private void doTheWork()
		{
			tryToExecute();
		}
		
		void notifyWaiter()
		{
			synchronized(WAITER)
			{
				WAITER.notify();
			}
		}
		
		private final Object WAITER = new Object();
		
		@Override
		public void run()
		{
			while (true)
			{
				doTheWork();
				try
				{
					synchronized(WAITER)
					{
						WAITER.wait(DURATION_BETWEEN_CHECKS);
					}
				}
				catch (InterruptedException e)
				{
					throw new RuntimeException("nexpected interrupt");
				}	
			}
		}
	}
}
