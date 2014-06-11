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

import java.util.ArrayList;

import java.util.logging.Logger;

public class ZoneMonitor implements Runnable
{
  private static Logger logger_ = Logger.getLogger(ZoneMonitor.class.getName());

  private Admin admin_ = null;
  private ZoneGSCMonitor zoneGSCMonitor_ = null;

  private Admin admin()
    {
    if (admin_ == null)
      {
      String lookupGroups = System.getenv("LOOKUPGROUPS");
      String lookupLocators = System.getenv("LOOKUPLOCATORS");

      AdminFactory factory = new AdminFactory();
      if (lookupGroups != null)
        factory.addGroups(lookupGroups);
      if (lookupLocators != null)
        factory.addLocators(lookupLocators);

      admin_ = factory.createAdmin();
      }

    return admin_;
    }


  /**
   * The full constructor for the ZoneMonitor class.
   */
  public ZoneMonitor(String zoneName,int gscCount)
    {
    zoneGSCMonitor_ = new ZoneGSCMonitor(admin(),zoneName,gscCount);
    }


  /**
   * Run until done.
   */
  public void run()
    {
    Thread zoneGSCThread = new Thread(zoneGSCMonitor_);
    zoneGSCThread.start();

    while (!zoneGSCMonitor_.isDone())
      {
      try { zoneGSCThread.join(); }
      catch(InterruptedException ignore) { }
      }

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

    System.exit(0);
    }
}  // end ZoneMonitor
