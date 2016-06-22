package sharingsystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Hashtable;

public class Connection implements Runnable {
  /**
   * The socket connected to the server.
   */
  private Socket socket;
  /**
   * The table containing the keys and values.
   */
  private Hashtable<String,String> hashtable;
  
  /**
   * The constructor associating the socket and hashtable to existing ones.
   * @param socket - A socket 
   * @param hashtable - A hashtable
   */
  public Connection(Socket socket, Hashtable<String,String> hashtable) {
    this.socket = socket;
    this.hashtable=hashtable;
  }
  /**
   * Put an item in the indexing hashtable represented by its key and value.
   * Sends an error message to the client in case the item could not be added.
   * Sends a success message if the item could be added.
   * @param key - The key of the item to be added.
   * @param value - The value of the item to be added.
   */
  public void register(String key, String value) {
    if(key == null || value == null) {
      returnError();
    }
    else {
      this.hashtable.put(key, value);
      returnSuccess(""+value);
    }
  }
  
  /**
   * Sends the value of a String in the hashtable represented by a key to the client.
   * Sends an error message if their is no value.
   * @param key - The key of the value to be sent.
   */
  public void get(String key) {
    String value =hashtable.get(key);
    if(value != null) {
      returnSuccess(""+value);
    }
    else {
      returnError();
    }
  }
  
  /**
   * Sends a message to tell that the operation did not succeed.
   */
  public void returnError() {
    ObjectOutputStream out;
    String msg_out=Constant.ERROR_HEADER;
    try {
      out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
      byte[] message = new byte[1024];
      message=msg_out.getBytes();
      out.write(message);
      out.flush();
    } catch (IOException e) {
      ;
    }
  }
  
  /**
   * Sends a message to tell the operation was a success with an added another message to the message.
   * The added message is separated by a token.
   */
  public void returnSuccess(String msg) {
    ObjectOutputStream out;
    try {
      out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
      byte[] header = new byte[4];
      byte[] key = new byte[20];
      byte[] value = new byte[1000];
      byte[] message = new byte[1024];
      header=Constant.SUCCESS_HEADER.getBytes();
      key="nokey".getBytes();
      value=msg.getBytes();
      System.arraycopy(header, 0, message, 0, header.length);
      System.arraycopy(key, 0, message, 4, key.length);
      System.arraycopy(value, 0, message, 24, value.length);
      out.write(message);
      out.flush();
    } catch (IOException e) {
      ;
    }
  }
  
  /**
   * Sends a message to tell the operation was a success.
   */
  public void returnSuccess() {
      returnSuccess("Success");
  }
  
  /**
   * The function used to transfer an owned file to another peer.
   * @param filename - The name of the file
   * @param header - The header of the message issuing the download.
   */
  public void upload(String filename, String header) {
    ObjectOutputStream out;
    String directory="";
    if (header.equals(Constant.DOWNLOAD_HEADER)) {
      directory=ConfigurationFile.getShareDirectory();
    }
    else if (header.equals(Constant.DOWNLOAD_REPLICA_HEADER)) {
      directory=ConfigurationFile.getReplicaDirectory();
    }
    try {
      File file = new File(directory+"/"+filename);
      FileInputStream inf=new FileInputStream(file);
      out = new ObjectOutputStream(socket.getOutputStream());
      byte buffer[] = new byte[1024];
      int i;
      while((i=inf.read(buffer))!=-1){
         out.write(buffer,0,i);
      }
      inf.close();
      out.flush();
    } catch (IOException e) {
      System.out.println("An error occurred trying to share a file.");
    }
  }

  /**
   * The function used to download a replicate of a file designed by its name.
   * @param filename - The name of the file
   */
  public void downloadReplicate(String filename) {
    ObjectInputStream in;
    try {
      returnSuccess();
      in=new ObjectInputStream(socket.getInputStream());
      File file = new File(ConfigurationFile.getReplicaDirectory()+"/"+filename);
      FileOutputStream outf =new FileOutputStream(file);
      byte buffer[] = new byte[1024];
      int i;
      while((i=in.read(buffer))!=-1){
          outf.write(buffer,0,i);
          if (i<1024 ) {
            break;
          }
      }
      outf.close();
    } catch (IOException e) {
      
    }
  }
  
  /**
   * This function converts the string used as an input with an abnormal size to the same string with an optimal size.
   * It removes all the '\0' bytes at the end of the string.
   * @param str - The string to convert
   * @return newStr - The converted string
   */
  public String convertString(String str) {
    char arr[] = str.toCharArray();
    int i=0;
    while(arr[i]!='\0') {
      i++;
    }
    String newStr = new String(arr,0,i);
    return newStr;
  }
  
  /**
   * The run() function of the thread. It reads an incoming message and takes the right decision (put, get or del).
   * It returns an error message if the message received could not be decrypted.
   */
  public void run() {
    ObjectInputStream in=null;
    while(true) {
      try {
        in = new ObjectInputStream(socket.getInputStream());
        byte buffer[] = new byte[1024];
        in.read(buffer);
        String header=new String(Arrays.copyOfRange(buffer,0,4));
        String keyR=new String(Arrays.copyOfRange(buffer,4,24));
        String valueR=new String(Arrays.copyOfRange(buffer,24,1024));
        String key = convertString(keyR);
        String value = convertString(valueR);
        if (header.equals(Constant.REGISTER_HEADER)) {
          register(key,value);
        }
        else if(header.equals(Constant.SEARCH_HEADER)) {
          get(key);
        }
        else if(header.equals(Constant.DOWNLOAD_HEADER) || header.equals(Constant.DOWNLOAD_REPLICA_HEADER)) {
          upload(value,header);
        }
        else if(header.equals(Constant.REPLICATE_HEADER)) {
          downloadReplicate(value);
        }
        else {
          returnError();
        }
      } catch (IOException e) {
        try {
          socket.close();
        } catch (IOException e1) {
        }
        break;
      }
    }
  }

}
