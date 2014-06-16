/**
 * SpaceStatisticsLogger is an interface that must be implemented by all
 * statistics loggers that can be passed to the SpaceMonitor.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 Patrick May
 * @version 1
 */

package com.gigaspaces.examples.monitor;

import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceStatistics;

public interface SpaceStatisticsLogger
{
  public void log(Space space,SpaceStatistics statistics);
}  // end SpaceStatisticsLogger
