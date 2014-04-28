/**
 * InfrastructureMonitor monitors the Admin API for changes.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 GigaSpaces Technologies Inc.  All rights reserved.
 * @version 1
 */

package com.gigaspaces.utils.admin;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentLifecycleEventListener;

import java.util.logging.Logger;

public class InfrastructureMonitor
  implements Runnable, GridServiceAgentLifecycleEventListener
{
  private static Logger logger_
    = Logger.getLogger(InfrastructureMonitor.class.getName());
  private static Object threadMonitor_ = new Object();

  private boolean done_ = false;
  private Admin admin_ = null;

  /**
   * The default constructor for the InfrastructureMonitor class.
   */
  public InfrastructureMonitor()
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


  /**
   * Method called when a GSA is added.  This method is inherited from
   * the GridServiceAgentLifecycleEventListener interface.
   */
  public void gridServiceAgentAdded(GridServiceAgent agent)
    {
    logger_.info("GSA added.");
    }


  /**
   * Method called when a GSA is removed.  This method is inherited from
   * the GridServiceAgentLifecycleEventListener interface.
   */
  public void gridServiceAgentRemoved(GridServiceAgent agent)
    {
    logger_.info("GSA removed.");
    }


  public boolean isDone() { return done_; }


  /**
   * Run until done.
   */
  public void run()
    {
    admin_.addEventListener(this);

    while (!isDone())
      {
      synchronized(threadMonitor_)
        {
        try { threadMonitor_.wait(); }
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
    threadMonitor_.notifyAll();
    }


  /**
   * The command line harness for the InfrastructureMonitor class.
   *
   * @param args The command line arguments passed in.
   */
  public static void main(String args[])
    {
    InfrastructureMonitor monitor = new InfrastructureMonitor();
    monitor.run();
    }
}  // end InfrastructureMonitor
