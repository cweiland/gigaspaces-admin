/**
 * SpaceStatisticsParser parses space statistics into comma-separated
 * value strings.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 Patrick May
 * @version 1
 */

package com.gigaspaces.examples.monitor;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Map;

import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceRuntimeDetails;
import org.openspaces.admin.space.SpaceStatistics;

public class SpaceStatisticsParser
{
  private static Logger logger_
    = Logger.getLogger(SpaceStatisticsParser.class.getName());

  private Space space_ = null;
  private SpaceStatistics rawStatistics_ = null;
  private ArrayList<String> parsedStatistics_ = new ArrayList<String>();


  /**
   * Format the passed data into a comma-separated value string.
   */
  private String csvFormat(String metricType,Object... metricData)
    {
    StringBuilder csvString = new StringBuilder();

    csvString.append(rawStatistics_.getTimestamp())
      .append(',')
      .append(space_.getName())
      .append(',')
      .append(metricType);

    for (Object data : metricData)
      csvString.append(',').append(data);

    return csvString.toString();
    }


  /**
   * Parse the space statistics into comma-separated value strings.
   */
  private void parse()
    {
    if (!rawStatistics_.isNA())
      {
      // space metrics
      parsedStatistics_.add(csvFormat("instance-count",
                                      space_.getNumberOfInstances()));
      parsedStatistics_.add(csvFormat("backup-count",
                                      space_.getNumberOfBackups()));

      // object type counts
      Map<String,Integer> countPerClassName
        = space_.getRuntimeDetails().getCountPerClassName();
      for (Map.Entry<String,Integer> entry : countPerClassName.entrySet())
        parsedStatistics_.add(csvFormat("class-count",
                                        entry.getKey(),
                                        entry.getValue().toString()));

      // space statistics metrics
      parsedStatistics_.add(csvFormat("object-count",
                                      rawStatistics_.getObjectCount()));
      parsedStatistics_
        .add(csvFormat("active-connection-count",
                       rawStatistics_.getActiveConnectionCount()));
      parsedStatistics_
        .add(csvFormat("active-transaction-count",
                       rawStatistics_.getActiveTransactionCount()));
      parsedStatistics_.add(csvFormat("change-count",
                                      rawStatistics_.getChangeCount()));
      parsedStatistics_.add(csvFormat("change-per-second",
                                      rawStatistics_.getChangePerSecond()));
      parsedStatistics_.add(csvFormat("execute-count",
                                      rawStatistics_.getExecuteCount()));
      parsedStatistics_.add(csvFormat("execute-per-second",
                                      rawStatistics_.getExecutePerSecond()));
      parsedStatistics_.add(csvFormat("notify-ack-count",
                                      rawStatistics_.getNotifyAckCount()));
      parsedStatistics_.add(csvFormat("notify-ack-per-second",
                                      rawStatistics_.getNotifyAckPerSecond()));
      parsedStatistics_
        .add(csvFormat("notify-registration-count",
                       rawStatistics_.getNotifyRegistrationCount()));
      parsedStatistics_
        .add(csvFormat("notify-registration-per-second",
                       rawStatistics_.getNotifyRegistrationPerSecond()));
      parsedStatistics_.add(csvFormat("notify-trigger-count",
                                      rawStatistics_.getNotifyTriggerCount()));
      parsedStatistics_
        .add(csvFormat("notify-trigger-per-second",
                       rawStatistics_.getNotifyTriggerPerSecond()));
      parsedStatistics_.add(csvFormat("read-count",
                                      rawStatistics_.getReadCount()));
      parsedStatistics_.add(csvFormat("read-per-second",
                                      rawStatistics_.getReadPerSecond()));
      parsedStatistics_.add(csvFormat("remove-count",
                                      rawStatistics_.getRemoveCount()));
      parsedStatistics_.add(csvFormat("remove-per-second",
                                      rawStatistics_.getRemovePerSecond()));
      parsedStatistics_.add(csvFormat("take-count",
                                      rawStatistics_.getTakeCount()));
      parsedStatistics_.add(csvFormat("take-per-second",
                                      rawStatistics_.getTakePerSecond()));
      parsedStatistics_.add(csvFormat("update-count",
                                      rawStatistics_.getUpdateCount()));
      parsedStatistics_.add(csvFormat("update-per-second",
                                      rawStatistics_.getUpdatePerSecond()));
      parsedStatistics_.add(csvFormat("write-count",
                                      rawStatistics_.getWriteCount()));
      parsedStatistics_.add(csvFormat("write-per-second",
                                      rawStatistics_.getWritePerSecond()));
      }
    else
      logger_.warning("Space statistics unavailable.");
    }


  /**
   * The full constructor for the SpaceStatisticsParser class.
   */
  public SpaceStatisticsParser(Space space,SpaceStatistics statistics)
    {
    space_ = space;
    rawStatistics_ = statistics;

    parse();
    }


  /**
   * Parse the space statistics.
   */
  public String[] parsedStatistics()
    {
    return parsedStatistics_.toArray(new String[0]);
    }
}  // end SpaceStatisticsParser
