package sharingsystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

public class Server implements Runnable {
/**
 * The client number
 */
  private int clientNumber;
  /**
   * A hashtable to store filenames and corresponding server IDs.
   */
  private Hashtable<String,String> hashtable;
  
  /**
   * Constructor to initialize a server with the right server number.
   * It gets the hashtable ready.
   * @param clientNumber
   */
  public Server(int clientNumber) {
    this.clientNumber = clientNumber;
    hashtable = new Hashtable<String,String>();
  }
  /**
   * The default constructor initialize the server number with a wrong number.
   * Should never be called.
   */
  public Server() {
    this(0);
    hashtable = new Hashtable<String,String>();
  }
  
  /**
   * Initialize the server to listen on the right port.
   * When someone connects, run the Connection thread.
   */
  @SuppressWarnings("resource")
  public void run() {
    try {
      ServerSocket server = new ServerSocket(ConfigurationFile.getClientPort()[clientNumber-1]);
      while(true) {
        Socket socket =server.accept();
        Thread thread = new Thread(new Connection(socket,hashtable));
        thread.start();
      }
    }
    catch(IOException e) {
      System.out.println("Server socket could not be created. Check configuration file.");
    }
  }

}
