/**
 * SpaceMonitor monitors a running GigaSpaces XAP space, periodically
 * emitting metrics in CSV format.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 Patrick May
 * @version 1
 */

package com.gigaspaces.examples.monitor;

import java.util.logging.Logger;

import java.util.concurrent.TimeUnit;

import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.Admin;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.Spaces;
import org.openspaces.admin.space.SpaceStatistics;
import org.openspaces.admin.space.events.SpaceStatisticsChangedEventListener;
import org.openspaces.admin.space.events.SpaceStatisticsChangedEvent;

public class SpaceMonitor implements Runnable
{
  private static final long DEFAULT_STATISTICS_INTERVAL = 10;  // seconds

  private static Logger logger_
    = Logger.getLogger(SpaceMonitor.class.getName());
  private static Object threadMonitor = new Object();

  SpaceStatisticsLogger statisticsLogger_ = null;
  long interval_ = DEFAULT_STATISTICS_INTERVAL;
  String lookupGroups_ = null;
  String lookupLocators_ = null;

  /**
   * SpaceStatisticsListener listens for space statistics updates.
   */
  private class SpaceStatisticsListener
    implements Runnable, SpaceStatisticsChangedEventListener
  {
    private boolean done_ = false;

    /**
     * The full constructor for the SpaceStatisticsListener class.
     */
    public SpaceStatisticsListener()
      {
      }


    /**
     * Method called when space statistics have changed.  This method is
     * inherited from the SpaceStatisticsChangedEventListener interface.
     */
    public void spaceStatisticsChanged(SpaceStatisticsChangedEvent event)
      {
      if (!event.getStatistics().isNA())
        statisticsLogger_.log(event.getSpace(),event.getStatistics());
      else
        logger_.warning("Statistics are not available.");
      }


    public boolean isDone() { return done_; }


    /**
     * Run until done.
     */
    public void run()
      {
      while (!isDone())
        {
        synchronized(threadMonitor)
          {
          try { threadMonitor.wait(); }
          catch(InterruptedException e) { }
          }
        }
      }


    /**
     * Stop listening.
     */
    public void done()
      {
      done_ = true;
      threadMonitor.notifyAll();
      }
  }  // end SpaceStatisticsListener


  /**
   * The full constructor for the SpaceMonitor class.
   */
  public SpaceMonitor(SpaceStatisticsLogger statisticsLogger,long interval)
    {
    statisticsLogger_ = statisticsLogger;
    interval_ = interval;
    lookupGroups_ = System.getenv("LOOKUPGROUPS");
    lookupLocators_ = System.getenv("LOOKUPLOCATORS");
    }


  /**
   * The full constructor for the SpaceMonitor class.
   */
  public SpaceMonitor(SpaceStatisticsLogger statisticsLogger)
    {
    this(statisticsLogger,DEFAULT_STATISTICS_INTERVAL);
    }


  private void waitForListener(SpaceStatisticsListener listener,
                               Thread listenerThread)
    {
    while (!listener.isDone())
      {
      try
        {
        listenerThread.join();
        }
      catch (InterruptedException ignore)
        {
        logger_.info("Listener interrupted.");
        }
      }
    }


  /**
   * Run the SpaceMonitor.
   */
  public void run()
    {
    AdminFactory factory = new AdminFactory();
    if (lookupGroups_ != null)
      factory.addGroups(lookupGroups_);
    if (lookupLocators_ != null)
      factory.addLocators(lookupLocators_);

    Admin admin = factory.createAdmin();

    Spaces spaces = admin.getSpaces();
    spaces.setStatisticsInterval(interval_,TimeUnit.SECONDS);
    if (!spaces.isMonitoring())
      spaces.startStatisticsMonitor();

    SpaceStatisticsListener listener = new SpaceStatisticsListener();
    Thread listenerThread = new Thread(listener);
    listenerThread.start();
    spaces.getSpaceStatisticsChanged().add(listener);

    waitForListener(listener,listenerThread);

    spaces.stopStatisticsMonitor();
    admin.close();
    }


  /**
   * Run the SpaceMonitor from the command line.
   *
   * @param args The command line arguments passed in.
   */
  public static void main(String args[])
    {
    if ((args.length == 1) || (args.length == 2))
      {
      SpaceMonitor monitor = null;
      if (args.length == 1)
        monitor = new SpaceMonitor(new SpaceStatisticsFileLogger(args[0]));
      else
        monitor = new SpaceMonitor(new SpaceStatisticsFileLogger(args[0]),
                                   Long.parseLong(args[1]));

      monitor.run();
      }
    else
      System.out.println("Usage:  java "
                         + SpaceMonitor.class.getName()
                         + " <logfile-name> [<polling-interval>]");

    System.exit(0);
    }
}  // end SpaceMonitor
