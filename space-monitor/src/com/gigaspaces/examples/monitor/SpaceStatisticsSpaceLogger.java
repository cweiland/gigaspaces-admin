/**
 * SpaceStatisticsSpaceLogger logs space statistics to a metrics space.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 Patrick May
 * @version 1
 */

package com.gigaspaces.examples.monitor;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;

import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceStatistics;

import java.util.logging.Logger;

public class SpaceStatisticsSpaceLogger implements SpaceStatisticsLogger
{
  private static Logger logger_
    = Logger.getLogger(SpaceStatisticsSpaceLogger.class.getName());

  private String spaceURL_ = null;
  private GigaSpace gigaSpace_ = null;

  /**
   * Return a proxy to the GigaSpace, creating it if necessary.
   */
  private GigaSpace space()
    {
    if (gigaSpace_ == null)
      {
      UrlSpaceConfigurer urlSpaceConfigurer
        = new UrlSpaceConfigurer(spaceURL_);
      GigaSpaceConfigurer gigaSpaceConfigurer
        = new GigaSpaceConfigurer(urlSpaceConfigurer.space());

      gigaSpace_ = gigaSpaceConfigurer.gigaSpace();
      }

    return gigaSpace_;
    }


  /**
   * The full constructor for the SpaceStatisticsSpaceLogger class.
   */
  public SpaceStatisticsSpaceLogger(String spaceURL)
    {
    spaceURL_ = spaceURL;
    }


  /**
   * Log the space statistics.  This method is inherited from
   * SpaceStatisticsLogger.
   */
  public void log(Space space,SpaceStatistics statistics)
    {
    SpaceStatisticsParser parser = new SpaceStatisticsParser(space,statistics);

    try
      {
      for (String metric : parser.parsedStatistics())
        space().write(new SpaceMetric(metric));
      }
    catch (Exception e)
      {
      logger_.severe("Unable to write metric:  " + e);
      }
    }
}  // end SpaceStatisticsSpaceLogger
