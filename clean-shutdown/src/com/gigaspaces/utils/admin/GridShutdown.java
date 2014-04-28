package com.gigaspaces.utils.admin;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.ProcessingUnit;

import com.gigaspaces.cluster.replication.async.mirror.MirrorStatistics;
import com.j_spaces.core.filters.ReplicationStatistics;
import com.j_spaces.core.filters.ReplicationStatistics.OutgoingReplication;

public class GridShutdown
{
  private static final String DEFAULT_PROPERTIES_FILE = "shutdown.properties";

  private Properties properties_ = new Properties();
  private String lookupGroups_ = null;
  private int numberOfAgentsToShutdown_ = 0;
  private int transactionTimeout_ = 0;
  private Admin admin_ = null;
  
  private void loadProperties(String propertiesFile) throws Exception
    {
    try
      {
      InputStream in = new FileInputStream(propertiesFile);
      properties_.load(in);
      in.close();
      }
    catch (IOException e)
      {
      throw new Exception("Config file '" + propertiesFile + "' not found");
      }
    }

  
  public GridShutdown(String propertiesFile) throws Exception
    {
    loadProperties(propertiesFile);
    lookupGroups_ = properties_.getProperty("lookupgroups");
    numberOfAgentsToShutdown_
      = Integer.parseInt(properties_.getProperty("numberOfGSAgents"));
    transactionTimeout_
      = Integer.parseInt(properties_.getProperty("transactionTimeout"));

    if ((lookupGroups_ == null || lookupGroups_.length() == 0))
      throw new Exception("Invalid Input parameters ");

    admin_ = new AdminFactory().addGroup(lookupGroups_).createAdmin();
    }

  
  public GridShutdown() throws Exception
    {
    this(DEFAULT_PROPERTIES_FILE);
    }


  private ArrayList<SpaceInstance> findAllMirrors()
    {
    ArrayList<SpaceInstance> mirrors = new ArrayList<SpaceInstance>();

    for (Space space : admin_.getSpaces())
      for (SpaceInstance spaceInstance : space)
        {
        MirrorStatistics mirrorStat
          = spaceInstance.getStatistics().getMirrorStatistics();

        if (mirrorStat != null)
          mirrors.add(spaceInstance);
        }

    return mirrors;
    }

  
  private void logMirrorStatistics(SpaceInstance spaceInstance,
                                   MirrorStatistics statistics)
    {
    System.out.println("	Mirror Stats:"
                       + spaceInstance.getSpace().getName());
    System.out.println("		total operation count:"
                       + statistics.getOperationCount());
    System.out.println("		successful operation count:"
                       + statistics.getSuccessfulOperationCount());
    System.out.println("		failed operation count:"
                       + statistics.getFailedOperationCount());
    System.out.println("		in progress operation count:"
                       + statistics.getInProgressOperationCount());
    }


  private boolean mirrorsReadyForShutdown()
    {
    System.out.println("Testing mirrors");

    List<SpaceInstance> mirrors = findAllMirrors();

    List<String> pending = new ArrayList<String>();

    for (SpaceInstance spaceInstance : mirrors)
      {
      MirrorStatistics mirrorStatistics
        = spaceInstance.getStatistics().getMirrorStatistics();
      logMirrorStatistics(spaceInstance,mirrorStatistics);

      if (mirrorStatistics.getInProgressOperationCount() > 0)
        pending.add(spaceInstance.getSpace().getName());
      }

    for (String name : pending)
      System.out.println("Mirror :" + name + " needs to flush entries.");

    return pending.isEmpty();
    }


  private List<SpaceInstance> findRedoLogs()
    {
    List<SpaceInstance> list = new ArrayList<SpaceInstance>();

    for (Space space : admin_.getSpaces())
      for (SpaceInstance spaceInstance : space)
        {
        ReplicationStatistics statistics
          = spaceInstance.getStatistics().getReplicationStatistics();

        if (statistics != null)
          list.add(spaceInstance);
        }

    return list;
    }


  private void logRedoLogStatistics(SpaceInstance spaceInstance,
                                    OutgoingReplication statistics)
    {
    System.out.println("	Outgoing Replication:"
                       + spaceInstance.getSpace().getName());
    System.out.println("		Redo log size:"
                       + statistics.getRedoLogSize());
    System.out.println("		memory packet count:"
                       + statistics.getRedoLogMemoryPacketCount());
    System.out.println("		external storage packet count:"
                       + statistics.getRedoLogExternalStoragePacketCount());
    System.out.println("		extenral storage space used:"
                       + statistics.getRedoLogExternalStorageSpaceUsed());
    }


  private boolean redoLogsReadyForShutdown()
    {
    System.out.println("Testing Redo logs");

    List<SpaceInstance> redos = findRedoLogs();

    List<String> pending = new ArrayList<String>();

    for (SpaceInstance spaceInstance : redos)
      {
      ReplicationStatistics statistics
        = spaceInstance.getStatistics().getReplicationStatistics();

      if (statistics != null)
        {
        OutgoingReplication redoLogStatistics
          = spaceInstance
            .getStatistics()
            .getReplicationStatistics()
            .getOutgoingReplication();
        logRedoLogStatistics(spaceInstance,redoLogStatistics);

        if (redoLogStatistics.getRedoLogSize() > 0)
          pending.add(spaceInstance.getSpace().getName());
          }
      }

    for (String name : pending)
      System.out.println("Space :" + name + " needs to replicate entries");

    return pending.isEmpty();
    }


  private void undeployProcessingUnits()
    {
    ProcessingUnits units = admin_.getProcessingUnits();
    for (ProcessingUnit unit : units)
      unit.undeployAndWait();
    }


  private void shutdownAgents()
    {
    System.out.println("Shutting down grid service agents");

    for (GridServiceAgent agent : admin_.getGridServiceAgents().getAgents())
      {
      System.out.println("Shutting down "
                         + agent
                         + " on "
                         + agent.getMachine().getHostName());
      agent.shutdown();
      }
    }
  

  public void shutdown() throws Exception
    {
    System.out.println("Starting Shutdown of "
                       + numberOfAgentsToShutdown_
                       + " agents");

    // wait to find all agents
    Thread.sleep(3000);
    admin_.getGridServiceAgents().waitFor(numberOfAgentsToShutdown_);

    System.out.println("Waiting for transactions to complete...");
    Thread.sleep(transactionTimeout_);

    if (mirrorsReadyForShutdown())
      if (redoLogsReadyForShutdown())
        {
        undeployProcessingUnits();
        shutdownAgents();
        }
      else
        System.out.println(
          "Some of the spaces have not finished replicating the content");
    else
      System.out.println(
        "Some of the mirrors are not finished flushing the content");
    }

  
  public static void main(String[] args) throws Exception
    {
    long startTime = System.currentTimeMillis();

    if ((args.length == 0) || (args.length == 1))
      {
      GridShutdown gridShutdown = null;
      if (args.length == 0)
        gridShutdown = new GridShutdown();
      else
        gridShutdown = new GridShutdown(args[0]);

      Scanner keyboard = new Scanner(System.in);
      System.out.println(
        "Are you sure you want to shut down the entire service grid?");
      String next = keyboard.next();

      if (! next.equals("Y"))
        System.exit(0);

      gridShutdown.shutdown();

      System.out.println("Shutdown completed in : "
                         + (System.currentTimeMillis() - startTime) / 1000
                         + " seconds");
      System.exit(0);
      }
    else
      System.out.println("Usage:  ");
    }
}
