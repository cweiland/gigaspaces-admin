/**
 * SpaceStatisticsFileLogger logs space statistics to a file.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 Patrick May
 * @version 1
 */

package com.gigaspaces.examples.monitor;

import java.util.logging.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceStatistics;

public class SpaceStatisticsFileLogger implements SpaceStatisticsLogger
{
  private static Logger logger_
    = Logger.getLogger(SpaceStatisticsFileLogger.class.getName());

  private String fileName_ = null;

  /**
   * The full constructor for the SpaceStatisticsFileLogger class.
   */
  public SpaceStatisticsFileLogger(String fileName)
    {
    fileName_ = fileName;
    }


  /**
   * Log the space statistics.  This method is inherited from
   * SpaceStatisticsLogger.
   */
  public void log(Space space,SpaceStatistics statistics)
    {
    SpaceStatisticsParser parser = new SpaceStatisticsParser(space,statistics);
    PrintWriter writer = null;

    try
      {
      writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName_,
                                                                 true)));
      for (String metric : parser.parsedStatistics())
        writer.println(metric);
      }
    catch (IOException ioe)
      {
      logger_.severe("Unable to open file:  " + fileName_);
      }
    finally
      {
      try { writer.close(); }
      catch (Exception ignore) { }
      }
    }
}  // end SpaceStatisticsFileLogger
