/**
 * ZoneShutdown shuts down all GigaSpaces infrastructure components and
 * processing units associated with a zone.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 GigaSpaces Technologies Inc.  All rights reserved.
 * @version 1
 */

package com.gigaspaces.utils.admin;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.events.GridServiceContainerAddedEventListener;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitAddedEventListener;
import org.openspaces.admin.zone.Zone;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.logging.Logger;

public class ZoneShutdown
  implements Runnable,
             GridServiceContainerAddedEventListener,
             ProcessingUnitAddedEventListener
{
  private static final long SHUTDOWN_ATTEMPT_INTERVAL = 2000;  // 2 seconds
  private static final long UPDATE_SETTLE_INTERVAL = 3000;  // 3 seconds
  
  private static Logger logger_
    = Logger.getLogger(ZoneShutdown.class.getName());

  private String zoneName_ = null;
  private boolean done_ = false;
  private Admin admin_ = null;
  private long lastUpdate_ = 0;
  private HashSet<GridServiceAgent> gsas_ = new HashSet<GridServiceAgent>();
  private ArrayList<GridServiceContainer> gscs_
    = new ArrayList<GridServiceContainer>();
  private ArrayList<ProcessingUnit> pus_ = new ArrayList<ProcessingUnit>();

  /**
   * The full constructor for the ZoneShutdown class.
   */
  public ZoneShutdown(String zoneName)
    {
    zoneName_ = zoneName;

    String lookupGroups = System.getenv("LOOKUPGROUPS");
    String lookupLocators = System.getenv("LOOKUPLOCATORS");

    AdminFactory factory = new AdminFactory();
    if (lookupGroups != null)
      factory.addGroups(lookupGroups);
    if (lookupLocators != null)
      factory.addLocators(lookupLocators);

    admin_ = factory.createAdmin();
    }


  public boolean isDone() { return done_; }
  public void done() { done_ = true; }


  /**
   * Update the last update timestamp.
   */
  private void updated()
    {
    lastUpdate_ = System.currentTimeMillis();
    }


  /**
   * Determine if there is enough information to shutdown the zone.
   */
  private boolean readyToShutdown()
    {
    return ((lastUpdate_ != 0)
            && ((System.currentTimeMillis() - lastUpdate_)
                > UPDATE_SETTLE_INTERVAL)
            && (gscs_.size() > 0)
            && (pus_.size() > 0));
    }


  /**
   * Get the processing units associated with the specified GSC.
   */
  private ArrayList<ProcessingUnit> processingUnits(GridServiceContainer gsc)
    {
    ArrayList<ProcessingUnit> pus = new ArrayList<ProcessingUnit>();
    ProcessingUnitInstance[] instances = gsc.getProcessingUnitInstances();

    for (ProcessingUnitInstance instance : instances)
      pus.add(instance.getProcessingUnit());

    return pus;
    }


  /**
   * Undeploy all processing units running in GSCs in the specified zone.
   */
  private void undeployProcessingUnits()
    {
    synchronized(gscs_)
      {
      for (GridServiceContainer gsc : gscs_)
        for (ProcessingUnit pu : processingUnits(gsc))
          {
          logger_.info("Undeploying ProcessingUnit " + pu.getName());
          pu.undeployAndWait();
          logger_.info("ProcessingUnit " + pu.getName() + " undeployed.");
          }
      }
    }


  /**
   * Kill any GSCs that have no remaining processing units.
   */
  private void killGSCs()
    {
    synchronized(gscs_)
      {
      for (GridServiceContainer gsc : gscs_)
        {
        gsas_.add(gsc.getGridServiceAgent());
        if (processingUnits(gsc).isEmpty())
          gsc.kill();
        }
      }
    }


  /**
   * Shutdown any GSAs that have no remaining GSCs.
   */
  private void shutdownGSAs()
    {
    synchronized(gsas_)
      {
      for (GridServiceAgent gsa : gsas_)
        gsa.shutdown();
      }
    }


  /**
   * If ready, shutdown all components associated with the zone.
   */
  private boolean attemptShutdown()
    {
    if (readyToShutdown())
      {
      undeployProcessingUnits();
      killGSCs();
      // shutdownGSAs();
      done();
      }

    return isDone();
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

    updated();
    }


  /**
   * Method called when a PU is added.  This method is inherited from
   * the ProcessingUnitAddedEventListener interface.
   */
  public void processingUnitAdded(ProcessingUnit pu)
    {
    synchronized(pus_) { pus_.add(pu); }
    logger_.info("ProcessingUnit " + pu.getName() + " added.");
    
    updated();
    }


  /**
   * Run until done.
   */
  public void run()
    {
    admin_.addEventListener(this);

    while (!isDone())
      {
      try
        {
        Thread.sleep(SHUTDOWN_ATTEMPT_INTERVAL);
        if (attemptShutdown())
          {
          admin_.removeEventListener(this);
          admin_.close();
          break;
          }
        }
      catch(InterruptedException e) { }
      }
    }


  /**
   * The command line harness for the ZoneShutdown class.
   *
   * @param args The command line arguments passed in.
   */
  public static void main(String args[])
    {
    if (args.length == 1)
      {
      ZoneShutdown shutdown = new ZoneShutdown(args[0]);
      shutdown.run();
      }
    else
      logger_.info("Usage:  java "
                   + ZoneShutdown.class.getName()
                   + " <zone-name>");
    }
}  // end ZoneShutdown
