/**
 * AbstractGridComponentMonitor provides the basic behavior required by
 * all concrete grid component monitors.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 GigaSpaces Technologies Inc.  All rights reserved.
 * @version 1
 */

package com.gigaspaces.utils.admin;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminEventListener;

import java.util.logging.Logger;

public abstract class AbstractGridComponentMonitor implements Runnable
{
  private static Logger logger_
    = Logger.getLogger(AbstractGridComponentMonitor.class.getName());
  private static Object threadMonitor_ = new Object();

  private boolean done_ = false;
  private Admin admin_ = null;

  /**
   * The full constructor for the AbstractGridComponentMonitor class.
   */
  public AbstractGridComponentMonitor(Admin admin)
    {
    admin_ = admin;
    }


  public boolean isDone() { return done_; }

  protected abstract AdminEventListener eventListener();

  /**
   * Run until done.
   */
  public void run()
    {
    AdminEventListener eventListener = eventListener();
    
    admin_.addEventListener(eventListener);

    while (!isDone())
      {
      synchronized(threadMonitor_)
        {
        try { threadMonitor_.wait(); }
        catch(InterruptedException e) { }
        }
      }

    synchronized(eventListener) { admin_.removeEventListener(eventListener); }
    try { Thread.sleep(2000); }
    catch(InterruptedException ignore) { }
    }


  /**
   * Stop listening.
   */
  public void done()
    {
    done_ = true;
    synchronized(threadMonitor_) { threadMonitor_.notifyAll(); }
    }
}  // end AbstractGridComponentMonitor
