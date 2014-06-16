/**
 * SpaceMetric is a wrapper around the metrics returned SpaceMonitor to
 * allow them to be written to a space.
 *
 * @author Patrick May (patrick.may@gigaspaces.com)
 * @author &copy; 2014 Patrick May
 * @version 1
 */

package com.gigaspaces.examples.monitor;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;

import com.gigaspaces.metadata.index.SpaceIndexType;

import java.util.Date;

@SpaceClass
public class SpaceMetric
{
  private String rawMetric_ = null;
  private Date timestamp_ = null;
  private String spaceName_ = null;
  private String metricType_ = null;

  /**
   * The default constructor for the SpaceMetric class.
   */
  public SpaceMetric()
    {
    }

  
  /**
   * The full constructor for the SpaceMetric class.
   *
   * @param rawMetric The CSV formatted metric from SpaceMonitor
   */
  public SpaceMetric(String rawMetric)
    {
    rawMetric_ = new String(rawMetric);

    String[] components = rawMetric.split(",");
    timestamp_ = new Date(Long.parseLong(components[0]));
    spaceName_ = new String(components[1]);
    metricType_ = new String(components[2]);
    }


  // Accessors / mutators required by GigaSpaces
  @SpaceId(autoGenerate = false)
  public String getRawMetric() { return rawMetric_; }
  public void setRawMetric(String rawMetric) { rawMetric_ = rawMetric; }

  @SpaceIndex(type = SpaceIndexType.EXTENDED)
  public Date getTimestamp() { return timestamp_; }
  public void setTimestamp(Date timestamp) { timestamp_ = timestamp; }

  public String getSpaceName() { return spaceName_; }
  public void setSpaceName(String spaceName) { spaceName_ = spaceName; }

  @SpaceIndex
  public String getMetricType() { return metricType_; }
  public void setMetricType(String metricType) { metricType_ = metricType; }
}  // end SpaceMetric
