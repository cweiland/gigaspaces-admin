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
    logger_.info("ZoneShutdown.processingUnits() called.");
    ArrayList<ProcessingUnit> pus = new ArrayList<ProcessingUnit>();
    ProcessingUnitInstance[] instances = gsc.getProcessingUnitInstances();

    for (ProcessingUnitInstance instance : instances)
      pus.add(instance.getProcessingUnit());

    logger_.info("ZoneShutdown.processingUnits() returning.");
    return pus;
    }


  /**
   * Undeploy all processing units running in GSCs in the specified zone.
   */
  private void undeployProcessingUnits()
    {
    logger_.info("ZoneShutdown.undeployProcessingUnits() called.");
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
    logger_.info("ZoneShutdown.undeployProcessingUnits() returning.");
    }


  /**
   * Kill any GSCs that have no remaining processing units.
   */
  private void killGSCs()
    {
    logger_.info("ZoneShutdown.killGSCs() called.");
    synchronized(gscs_)
      {
      for (GridServiceContainer gsc : gscs_)
        {
        gsas_.add(gsc.getGridServiceAgent());
        if (processingUnits(gsc).isEmpty())
          {
          logger_.info("ZoneShutdown.killGSCs(): killing GSC.");
          gsc.kill();
          logger_.info("ZoneShutdown.killGSCs(): killed GSC.");
          }
        }
      }
    logger_.info("ZoneShutdown.killGSCs() returning.");
    }


  /**
   * Shutdown any GSAs that have no remaining GSCs.
   */
  private void shutdownGSAs()
    {
    logger_.info("ZoneShutdown.shutdownGSAs() called.");
    synchronized(gsas_)
      {
      for (GridServiceAgent gsa : gsas_)
        {
        logger_.info("ZoneShutdown.shutdownGSAs(): shutting down GSA.");
        gsa.shutdown();
        logger_.info("ZoneShutdown.shutdownGSAs(): GSA shut down.");
        }
      }
    logger_.info("ZoneShutdown.shutdownGSAs() returning.");
    }


  /**
   * If ready, shutdown all components associated with the zone.
   */
  private boolean attemptShutdown()
    {
    logger_.info("ZoneShutdown.attemptShutdown() called.");
    if (readyToShutdown())
      {
      undeployProcessingUnits();
      killGSCs();
      // shutdownGSAs();
      done();
      }

    logger_.info("ZoneShutdown.attemptShutdown() returning.");
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
    logger_.info("ProcessingUnit " + pu.getName() + " added.");
    synchronized(pus_) { pus_.add(pu); }
    
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
          logger_.info("ZoneShutdown.run():  removing event listener.");
          admin_.removeEventListener(this);
          logger_.info("ZoneShutdown.run():  closing admin.");
          admin_.close();
          logger_.info("ZoneShutdown.run():  done.");
          break;
          }
        }
      catch(InterruptedException e) { }
      }

    logger_.info("ZoneShutdown.run() returning.");
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
