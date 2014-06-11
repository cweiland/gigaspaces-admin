/**
 * ZoneGSCMonitor monitors the Admin API for changes to
 * GSCs in the GigaSpaces XAP infrastructure.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 GigaSpaces Technologies Inc.  All rights reserved.
 * @version 1
 */

package com.gigaspaces.utils.admin;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminEventListener;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerAddedEventListener;
import org.openspaces.admin.zone.Zone;

import java.util.ArrayList;

import java.util.logging.Logger;

public class ZoneGSCMonitor
  extends AbstractGridComponentMonitor
  implements GridServiceContainerAddedEventListener
{
  private static final long MONITOR_INTERVAL = 2000;  // 2 seconds

  private static Logger logger_
    = Logger.getLogger(ZoneGSCMonitor.class.getName());

  private String zoneName_ = null;
  private int gscCount_ = 0;
  private ArrayList<GridServiceContainer> gscs_
    = new ArrayList<GridServiceContainer>();

  /**
   * Inherited from AbstractGridComponentMonitor.
   */
  protected AdminEventListener eventListener() { return this; }

  /**
   * The full constructor for the ZoneGSCMonitor class.
   */
  public ZoneGSCMonitor(Admin admin,String zoneName,int gscCount)
    {
    super(admin);
    zoneName_ = zoneName;
    gscCount_ = gscCount;
    }


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

    if (gscs_.size() >= gscCount_)
      done();
    }
}  // end ZoneGSCMonitor
