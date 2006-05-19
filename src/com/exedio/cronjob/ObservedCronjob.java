/*
 * Copyright (C) 2004-2006  exedio GmbH (www.exedio.com)
 */
package com.exedio.cronjob;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

class ObservedCronjob
{
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
	
	private final int DURATION_BETWEEN_CHECKS=2705;
	DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss" );
	DateFormat dateFormatOnlyHours = new SimpleDateFormat("HH:mm:ss" );
	
	ObservedCronjob(final Cronjob cronjob, String id)
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
		runningThread = new RunningThread();
		runningThread.start();
	}
	
	private boolean timeForExcecution()
	{
		if (lastTimeStarted==null)
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
	
	String getName()
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
			System.out.println("\nStarting Cronjob: "+getName()+" at "+dateFormat.format(lastTimeStarted));
			try
			{
				cronjob.excecuteJob();
				lastExecutionSuccessful=true;				
				Date finished =new Date();
				System.out.println("Finished Cronjob: "+getName()+" at "+dateFormat.format(finished)+"\n");
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
				System.out.println("Execution of Cronjob: "+getName()+" FAILED at "+dateFormat.format(failedAt)+" !!!");
				System.out.println("******************** CronjobException - START ********************");
				e.printStackTrace();
				System.out.println("******************** CronjobException - END ********************");
				lastExecutionSuccessful=false;				
			}
			finally
			{
				running=false;
			}
		}
	}
	
	private void updateAverageTimeNeeded(long timeNeeded)
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
		boolean result=true;		
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
			return false;
		}
		return result;
	}
	
	private boolean isToday(Date date)
	{
		Calendar c = new GregorianCalendar();
		c.getInstance();
		c.set(Calendar.HOUR_OF_DAY,0);
		c.set(Calendar.MINUTE,0);
		c.set(Calendar.SECOND,0);
		c.set(Calendar.MILLISECOND,0);
		Date midnight=c.getTime();
		c.add(Calendar.DATE,1);
		Date nextMidnigt=c.getTime();
		if (date.after(midnight) && nextMidnigt.after(date))
			return true;
		else
			return false;
	}
	
	boolean isRunning()	{return running;}
	Cronjob getCronjob(){return cronjob;}
	Date getLastTimeStarted(){return lastTimeStarted;}
	String getLastTimeStartedAsString()
	{
		if (lastTimeStarted!=null)
		{
			if (isToday(lastTimeStarted))
				return dateFormatOnlyHours.format(lastTimeStarted);	
			else
				return dateFormat.format(lastTimeStarted);	
		}			
		else
			return "x";
	}
	String getId(){	return id;	}
	boolean wasLastExecutionSuccessful()	{return lastExecutionSuccessful;	}
	void removeLastException(){lastException=null;}
	boolean isActivated(){	return activated;	}
	void setActivated(boolean activated)
	{
		this.activated = activated;
		System.out.println("Cronjob: "+cronjob.getName()+" was deactivated at "+dateFormat.format(new Date()));
	}
	long getAverageTimeNeeded(){ return averageTimeNeeded;}
	long getTimeNeeded() {return timeNeeded;}
	int getSuccessfulRuns(){return successfulRuns;}
	int getNumberOfFails(){return fails;}
	Exception getLastException(){return lastException;}
	void runNow()
	{		
		runNow=true;
		runningThread.notifyWaiter();
		try
		{
			Thread.sleep(100); //This for updating the running Parameter without risk
		} 
		catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}
	
	class RunningThread extends Thread
	{
		private void doSomething()
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
		
		public void run()
		{
			while (true)
			{
				doSomething();
				try
				{
					synchronized(WAITER)
					{
						WAITER.wait(DURATION_BETWEEN_CHECKS);
					}
				}
				catch (InterruptedException e)
				{
					throw new RuntimeException("unexpected interrupt");
				}	
			}
		}
	}
}
