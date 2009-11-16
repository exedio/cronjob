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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.exedio.cope.util.Interrupter;

final class Handler implements Interrupter
{
	private final int DURATION_BETWEEN_CHECKS=2705;
	private final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
	private final DateFormat DATE_FORMAT_SIMPLE = new SimpleDateFormat("HH:mm:ss");
	
	final String id;
	final Job job;
	final String jobName;
	private boolean running;
	private Date lastTimeStarted;
	private long lastInterruptRequest;
	private long interruptMaximum = 0;
	private long interruptTotal = 0;
	private int interruptCount = 0;
	private Exception lastException;
	private int lastRunResult = 0;
	private boolean lastRunSuccessful;
	private boolean activated;
	private int successfulRuns;
	private long averageTimeNeeded;
	private long timeNeeded;
	private boolean runNow;
	private int fails;
	private RunningThread runningThread;
	private Date createdAt;
	private int initialDelayinMS;
	private final int stopTimeout;
	
	/**
	 * After construction use #startThread() to start running of the job.
	 */
	Handler(final Job job, final int id, final int initialDelayInMS, final boolean active)
	{
		this.id = "cronjob_"+String.valueOf(id);
		this.job=job;
		this.jobName = job.getName();
		running=false;
		lastTimeStarted=null;
		lastException=null;
		lastRunSuccessful=true;
		activated=active;
		successfulRuns=0;
		averageTimeNeeded=0;
		timeNeeded=0;
		fails=0;
		runNow=false;
		createdAt=new Date();
		this.initialDelayinMS=initialDelayInMS+job.getInitialDelayInMilliSeconds();
		this.stopTimeout = job.getStopTimeout();
	}
	
	private boolean timeForExcecution()
	{
		if (lastTimeStarted == null)
		{
			return true;
		}
		else
		{
			long last = lastTimeStarted.getTime();
			long now = new Date().getTime();
			if ((now-last)>=(job.getMinutesBetweenExecutions()*1000*60))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	long getLastInterruptRequest()
	{
		return lastInterruptRequest;
	}
	
	long getInterruptMaximum()
	{
		return interruptMaximum;
	}
	
	long getInterruptAverage()
	{
		return (interruptCount>0) ? (interruptTotal / interruptCount) : 0;
	}
	
	public boolean isRequested()
	{
		final long now = System.currentTimeMillis();
		registerInterruptRequest(now);
		lastInterruptRequest = now;
		return !activated;
	}
	
	private void registerInterruptRequest(final long now)
	{
		final long last = (lastInterruptRequest!=0) ? lastInterruptRequest : lastTimeStarted.getTime();
		final long elapsed = (int)(now - last);
		
		if(interruptMaximum<elapsed)
			interruptMaximum = elapsed;
		interruptTotal += elapsed;
		interruptCount++;
	}
	
	void tryToExecute()
	{
		if (canExecuteJob())
		{
			running=true;
			lastTimeStarted=new Date();
			lastInterruptRequest = 0;
			long msb=lastTimeStarted.getTime();
			//System.out.println("\nStarting Cronjob: "+getDisplayedName()+" at "+DATE_FORMAT.format(lastTimeStarted));
			try
			{
				lastRunResult = job.run(this);
				Date finished =new Date();
				lastRunSuccessful=true;
				//System.out.println("Finished Cronjob: "+getDisplayedName()+" at "+DATE_FORMAT.format(finished)+"\n");
				long msa=finished.getTime();
				timeNeeded=msa-msb;
				updateAverageTimeNeeded(timeNeeded);
				registerInterruptRequest(finished.getTime());
			}
			catch (Exception e)
			{
				lastException=e;
				fails++;
				Date failedAt= new Date();
				timeNeeded=failedAt.getTime()-msb;
				System.out.println("Execution of Cronjob: " + jobName + " FAILED at "+DATE_FORMAT.format(failedAt)+" !!!");
				System.out.println("******************** CronjobException - START ********************");
				e.printStackTrace();
				System.out.println("******************** CronjobException - END **********************");
				lastRunSuccessful=false;
				registerInterruptRequest(failedAt.getTime());
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
	
	private boolean canExecuteJob()
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
		System.out.println("Cronjob: " + jobName + " was deactivated at "+DATE_FORMAT.format(new Date()));
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
	
	boolean wasLastRunSuccessful() { return lastRunSuccessful;	}
	int getLastRunResult() { return lastRunResult; }
	void removeLastException(){lastException=null;}
	boolean isActivated(){	return activated;	}
	long getAverageTimeNeeded(){ return averageTimeNeeded;}
	long getTimeNeeded() {return timeNeeded;}
	int getSuccessfulRuns(){return successfulRuns;}
	int getNumberOfFails(){return fails;}
	Exception getLastException(){return lastException;}
	boolean isRunning() {return running;}
	int getInitialDelayInMilliSeconds() {return job.getInitialDelayInMilliSeconds();}
	int getMinutesBetweenExecutions() {return job.getMinutesBetweenExecutions();}
	Date getLastTimeStarted() {return lastTimeStarted;}
	int getStopTimeout() {return stopTimeout;}
	
	void startThread()
	{
		runningThread = new RunningThread();
		runningThread.setName("exedio cronjob: " + jobName);
		runningThread.start();
	}
	
	void stopThread()
	{
		if (runningThread!=null)
		{
			try
			{
				System.out.println("waiting for job:"+jobName+" to terminate");
				runningThread.stopRunning();
				if (runningThread.isAlive())
				{
					runningThread.notifyWaiter();
				}
				runningThread.join(stopTimeout);
				System.out.println("job:"+jobName+" joined");
				if(runningThread.isAlive())
				{
					System.out.println("job:"+jobName+" stopping forcefully");
					stop(runningThread);
				}
				System.out.println("job:"+jobName+" terminated");
			}
			catch (InterruptedException ex)
			{
				Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
	@SuppressWarnings("deprecation") // OK: last resort
	private static final void stop(final Thread t)
	{
		t.stop();
	}
	
	class RunningThread extends Thread
	{
		private boolean doRun = true;
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
		
		void stopRunning()
		{
			doRun = false;
		}
		
		@Override
		public void run()
		{
			while (doRun)
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
					throw new RuntimeException(e);
				}
			}
		}
	}
}
