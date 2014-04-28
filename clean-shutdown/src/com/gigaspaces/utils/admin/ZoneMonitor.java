/**
 * ZoneMonitor monitors the Admin API for changes to the GigaSpaces XAP
 * infrastructure related to a particular zone.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 GigaSpaces Technologies Inc.  All rights reserved.
 * @version 1
 */

package com.gigaspaces.utils.admin;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerAddedEventListener;
import org.openspaces.admin.zone.Zone;

import java.util.ArrayList;

import java.util.logging.Logger;

public class ZoneMonitor
  implements Runnable,
             GridServiceContainerAddedEventListener
{
  private static final long MONITOR_INTERVAL = 2000;  // 2 seconds

  private static Logger logger_ = Logger.getLogger(ZoneMonitor.class.getName());
  private static Object threadMonitor_ = new Object();

  private Admin admin_ = null;
  private String zoneName_ = null;
  private int gscCount_ = 0;
  private ArrayList<GridServiceContainer> gscs_
    = new ArrayList<GridServiceContainer>();

  /**
   * The full constructor for the ZoneMonitor class.
   */
  public ZoneMonitor(String zoneName,int gscCount)
    {
    zoneName_ = zoneName;
    gscCount_ = gscCount;
    
    String lookupGroups = System.getenv("LOOKUPGROUPS");
    String lookupLocators = System.getenv("LOOKUPLOCATORS");

    AdminFactory factory = new AdminFactory();
    if (lookupGroups != null)
      factory.addGroups(lookupGroups);
    if (lookupLocators != null)
      factory.addLocators(lookupLocators);

    admin_ = factory.createAdmin();
    }


  public boolean isDone() { return (gscs_.size() >= gscCount_); }


  /**
   * Method called when a GSC is added.  This method is inherited from
   * the GridServiceContainerAddedEventListener interface.
   */
  public void gridServiceContainerAdded(GridServiceContainer gsc)
    {
    logger_.info("GridServiceContainer found.");
    for (Zone zone : gsc.getZones().values())
      {
      logger_.info("  " + zone.getName());
      if (zoneName_.equals(zone.getName()))
        {
        synchronized(gscs_) { gscs_.add(gsc); }
        logger_.info("GridServiceContainer added.");
        }
      }
    }


  /**
   * Run until done.
   */
  public void run()
    {
    admin_.addEventListener(this);

    while (!isDone())
      {
      try { Thread.sleep(MONITOR_INTERVAL); }
      catch(InterruptedException e) { }
      }

    admin_.removeEventListener(this);
    admin_.close();
    }

  
  /**
   * The command line harness for the ZoneMonitor class.
   *
   * @param args The command line arguments passed in.
   */
  public static void main(String args[])
    {
    if (args.length == 2)
      {
      ZoneMonitor monitor = new ZoneMonitor(args[0],Integer.parseInt(args[1]));
      monitor.run();
      logger_.info("At least "
                   + args[1]
                   + " GSCs are available in zone "
                   + args[0]);
      }
    else
      logger_.info("Usage:  java "
                   + ZoneMonitor.class.getName()
                   + " <zone-name> <gsc-count>");
    }
}  // end ZoneMonitor
