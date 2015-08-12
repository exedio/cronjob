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

import com.exedio.cope.util.JobStop;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;

final class Handler
{
	private static final Logger logger = CronjobManager.logger;

	private static final int DURATION_BETWEEN_CHECKS = 2705;
	private final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");

	final String id;
	final Job job;
	final String jobName;
	final boolean activeInitially;
	private boolean initialized = false;
	private RunContext runContext = null;
	private Date lastTimeStarted;
	private long lastInterruptRequest;
	private long interruptMaximum = 0;
	private long interruptTotal = 0;
	private int interruptCount = 0;
	private Throwable lastException;
	private int lastRunResult = 0;
	private boolean lastRunSuccessful;
	private boolean activated;
	private JobStopInfo deactivateInfo = null;
	private int successfulRuns;
	private long averageTimeNeeded;
	private long timeNeeded;
	private boolean runNow;
	private int fails;
	private RunningThread runningThread;
	private final Date createdAt;
	private final long initialDelayinMS;
	private final long stopTimeout;

	/**
	 * After construction use #startThread() to start running of the job.
	 */
	Handler(final Job job, final int id, final long initialDelayInMS)
	{
		this.id = "cronjob_"+String.valueOf(id);
		this.job=job;
		this.jobName = job.getName();
		this.activeInitially = job.isActiveInitially();
		lastTimeStarted=null;
		lastException=null;
		lastRunSuccessful=true;
		activated=activeInitially;
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
			final long last = lastTimeStarted.getTime();
			final long now = new Date().getTime();
			if ((now-last)>=(job.getMinutesBetweenExecutions()*1000l*60))
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

	void stopIfRequested() throws JobStop
	{
		final long now = System.currentTimeMillis();
		registerInterruptRequest(now);
		lastInterruptRequest = now;
		if(!activated)
		{
			if (deactivateInfo != null)
				throw deactivateInfo.newJobStop();
			else
				// cronjob has never been activated (set to 'on')
				throw new JobStop("cronjob not active ('on')");
		}
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
			if(!initialized)
			{
				try
				{
					job.init();
					initialized = true;
				}
				catch(final Exception e)
				{
					atCatch(e);
					return;
				}
				catch(final AssertionError e)
				{
					atCatch(e);
					return;
				}
			}

			runContext = new RunContext(this);
			lastTimeStarted=new Date();
			lastInterruptRequest = 0;
			final long msb=lastTimeStarted.getTime();
			//System.out.println("\nStarting Cronjob: "+getDisplayedName()+" at "+DATE_FORMAT.format(lastTimeStarted));
			try
			{
				job.run(runContext);
				lastRunResult = runContext.getProgress();
				final Date finished =new Date();
				lastRunSuccessful=true;
				//System.out.println("Finished Cronjob: "+getDisplayedName()+" at "+DATE_FORMAT.format(finished)+"\n");
				final long msa=finished.getTime();
				timeNeeded=msa-msb;
				updateAverageTimeNeeded(timeNeeded);
				registerInterruptRequest(finished.getTime());
			}
			catch (final Exception e)
			{
				atCatch(e, msb);
			}
			catch (final AssertionError e)
			{
				atCatch(e, msb);
			}
			finally
			{
				runContext = null;
			}
		}
	}

	private void updateAverageTimeNeeded(final long timeNeeded) // TODO why this parameter is needed here?
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

	void destroyIfInitialized()
	{
		if(initialized)
		{
			try
			{
				job.destroy();
				initialized = false;
			}
			catch(final Exception e)
			{
				atCatch(e);
				return;
			}
			catch(final AssertionError e)
			{
				atCatch(e);
				return;
			}
		}
	}

	private void atCatch(final Throwable e)
	{
		lastException=e;
		fails++;
		final Date failedAt= new Date();
		log(e, failedAt);
	}

	private void atCatch(final Throwable e, final long msb)
	{
		lastException=e;
		fails++;
		final Date failedAt= new Date();
		timeNeeded=failedAt.getTime()-msb;
		log(e, failedAt);
		lastRunSuccessful=false;
		registerInterruptRequest(failedAt.getTime());
	}

	private boolean canExecuteJob()
	{
		if(runNow && runContext==null)
		{
			runNow=false;
			return true;
		}
		boolean result = true;
		if ((new Date().getTime()-createdAt.getTime())<initialDelayinMS)
		{
			result=false;
		}
		if(runContext!=null)
		{
			runContext = null;
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

	private void log(final Throwable e, final Date failedAt)
	{
		logger.error("Execution of Cronjob: " + jobName + " FAILED at "+DATE_FORMAT.format(failedAt)+" !!!");
		logger.error("******************** CronjobException - START ********************");
		logger.error( "", e ); // prints stack trace
		logger.error("******************** CronjobException - END **********************");
	}

	void setActivated(final boolean activated, final JobStopInfo info)
	{
		this.activated = activated;

		if(!activated)
			deactivateInfo = info;
		else
			deactivateInfo = null;

		if (logger.isInfoEnabled())
			logger.info("Cronjob: " + jobName + " was "+(activated ? "" : "de")+"activated at "+DATE_FORMAT.format(new Date()));
	}

	@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR") // TODO
	void runNow()
	{
		runNow=true;
		runningThread.notifyWaiter();
		try
		{
			Thread.sleep(100); //This for updating the running Parameter
		}
		catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}

	boolean wasLastRunSuccessful() { return lastRunSuccessful;	}
	int getLastRunResult() { return lastRunResult; }
	void removeLastException(){lastException=null;}
	boolean isActivated(){	return activated;	}
	boolean isInitialized(){	return initialized;	}
	long getAverageTimeNeeded(){ return averageTimeNeeded;}
	long getTimeNeeded() {return timeNeeded;}
	int getSuccessfulRuns(){return successfulRuns;}
	int getNumberOfFails(){return fails;}
	Throwable getLastException(){return lastException;}
	RunContext getRunContext() {return runContext;}
	long getInitialDelayInMilliSeconds() {return job.getInitialDelayInMilliSeconds();}
	int getMinutesBetweenExecutions() {return job.getMinutesBetweenExecutions();}
	Date getLastTimeStarted() {return lastTimeStarted;}
	long getStopTimeout() {return stopTimeout;}
	Thread.State        getThreadState()     { final Thread t = runningThread; return t!=null ? t.getState()      : null; }
	StackTraceElement[] getThreadStackTrace(){ final Thread t = runningThread; return t!=null ? t.getStackTrace() : null; }

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
				logger.info("waiting for job:"+jobName+" to terminate");
				runningThread.stopRunning();
				if (runningThread.isAlive())
				{
					runningThread.notifyWaiter();
				}
				runningThread.join(stopTimeout);
				logger.info("job:"+jobName+" joined");
				if(runningThread.isAlive())
				{
					logger.info("job:"+jobName+" stopping forcefully");
					stop(runningThread);
				}
				logger.info("job:"+jobName+" terminated");
			}
			catch (final InterruptedException ex)
			{
				logger.error( "", ex );
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

		@SuppressFBWarnings("NO_NOTIFY_NOT_NOTIFYALL") // TODO
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
		@SuppressFBWarnings("UW_UNCOND_WAIT") // TODO
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
				catch (final InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
			destroyIfInitialized();
		}
	}
}
