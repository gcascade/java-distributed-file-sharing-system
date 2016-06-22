package sharingsystem;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationFile {

  /**
   * The number of clients
   */
  private static int clientNumber;
  /**
   * Clients' IP addresses.
   */
  private static String clientIp[];
  /**
   * Clients' port numbers.
   */
  private static int clientPort[];
  
  /**
   * The three directories used to share and download files.
   */
  private static String downloadDirectory;
  private static String shareDirectory;
  private static String replicaDirectory;
  
  /**
   * The status of replication.
   * true if enabled.
   * false if disabled.
   */
  private static boolean replica;
  
  /**
   * The number of replica
   */
  private static int replicaNumber;
  /**
   * The function loads the 'config.properties' file and sets the static attributes.
   */
  public static void loadFile() {
    String filename = "config.properties";
    Properties prop = new Properties();
    try {
      InputStream in = new FileInputStream(filename);
      prop.load(in);
      clientNumber=Integer.parseInt(prop.getProperty("numberOfClient"));
      if(clientNumber < 1) {
        clientNumber=1;
      }
      replicaNumber=Integer.parseInt(prop.getProperty("replicaNumber"));
      clientIp=new String[clientNumber];
      clientPort = new int[clientNumber];
      for(int i=0;i<clientNumber;i++) {
        clientIp[i]=prop.getProperty("client"+(i+1));
        clientPort[i]=Integer.parseInt(prop.getProperty("client"+(i+1)+"port"));
      }
      downloadDirectory=prop.getProperty("downloadDirectory");
      shareDirectory=prop.getProperty("shareDirectory");
      replicaDirectory=prop.getProperty("replicaDirectory");
      if(prop.getProperty("replica")!=null) {
        if(prop.getProperty("replica").equals("1")) {
          replica=true;
        }
        else {
          replica=false;
        }
      }
      else {
        replica=false;
      }
    } catch (FileNotFoundException e) {
      System.out.println("Configuration file was not found.");
    } catch (IOException e) {
      System.out.println("Configuration file could not be opened");
    } catch(NullPointerException e) {
      System.out.println("Something in the configuration file could not be read");
    }


  }
  /**
   * Load the file to return the clients' IP addresses.
   * @return clientIp - The clients' IP addresses.
   */
  public static String[] getClientIp(){
    loadFile();
    return clientIp;
  }

  /**
   * Load the file to return the clients' port numbers.
   * @return clientPort - The clients' port numbers.
   */
  public static int[] getClientPort() {
    loadFile();
    return clientPort;
  }
  
  /**
   * Load the file to return the name of the directory where downloaded files should be stored.
   * @return downloadDirectory - The name of the directory
   */
  public static String getDownloadDirectory() {
    loadFile();
    return downloadDirectory;
  }
  
  /**
   * Load the file to return the name of the directory containing the files to be shared.
   * @return shareDirectory - The name of the directory.
   */
  public static String getShareDirectory() {
    loadFile();
    return shareDirectory;
  }
  
  /**
   * Load the file to return the name of the directory containing the replicated files to be shared.
   * @return replicaDirectory - The name of the directory.
   */
  public static String getReplicaDirectory() {
    loadFile();
    return replicaDirectory;
  }
  
  /**
   * Load the file to return the status of replication (Enabled/Disabled)
   * @return replica - True if enabled, false if disabled.
   */
  public static boolean isReplicaEnabled() {
    loadFile();
    return replica;
  }
  
  /**
   * Load the file to return the number of replica
   * @return replicaNumber - The number of replica
   */
  public static int getReplicaNumber() {
    loadFile();
    return replicaNumber;
  }
  
  /**
   * Load the file to return the number of clients.
   * @return clientNumber - The number of clients
   */
  public static int getClientNumber() {
    loadFile();
    return clientNumber;
  }
  
}
