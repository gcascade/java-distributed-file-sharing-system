package sharingsystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Action implements Runnable {
  
  /**
   * The list of sockets.
   */
  private List<Socket> socket;
  /**
   * The Id of the peer
   */
  private int myId;
  /**
   * Constructor used to assign an Id while preparing the socket list.
   * @param serverId - The Id of the peer
   */
  public Action(int serverId) {
    this.socket = new ArrayList<Socket>();
    this.myId = serverId;
  }
  /**
   * Default constructor. Get the socket list ready.
   */
  public Action() {
    this.socket = new ArrayList<Socket>();
    this.myId = 0;
  }

  /**
   * The function initiates a socket connection to a server among the 8 listed in the configuration file.
   * @param serverNb - The number of the server to connect.
   * @return
   */
  public Socket connectToServer(int serverNb) {
    Socket socket=null;
    try {
      socket = new Socket(InetAddress.getByName(ConfigurationFile.getClientIp()[serverNb-1]),
          ConfigurationFile.getClientPort()[serverNb-1]);
    } catch (UnknownHostException e) {
      System.out.println("Could not connect to server. Unknown host.");
    } catch (IOException e) {
      System.out.println("An error occurred creating a socket.");
    }
    return socket;
  }
  
  /**
   * The function finds the server having the corresponding key.
   * @param key - The key.
   * @return - serverNb - The number of the server.
   */
  public int getServer(String key) {
    int hashcode = Integer.parseInt(key);
    int serverNb=0;
    for(int i=0;i<ConfigurationFile.getClientNumber();i++) {
      if(hashcode < (i+1)*Constant.MAX/ConfigurationFile.getClientNumber()) {
        serverNb=i+1;
        break;
      }
    }
    return serverNb;
  }
  
  /**
   * The function sends a message to a server through a socket. The first 4 bits correspond to the header.
   * The next 20 bits correspond to the key and the last 1000 bits correspond to the value.
   * @param socket - The socket connected to the server.
   * @param header - The header sent before the message.
   * @param key - The key.
   * @param value - The value.
   * @return result - True or false depending if the message was sent.
   */
  public boolean sendMessage(Socket socket,String header, String key, String value) {
    boolean result=false;
    ObjectOutputStream out;
    try {
      out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
      byte[] headerByte = new byte[4];
      byte[] keyByte = new byte[20];
      byte[] valueByte = new byte[1000];
      byte[] message = new byte[1024];
      if (!(header.equals(Constant.REGISTER_HEADER) || header.equals(Constant.DOWNLOAD_HEADER) ||
          header.equals(Constant.SEARCH_HEADER) || header.equals(Constant.REPLICATE_HEADER)
          || header.equals(Constant.DOWNLOAD_REPLICA_HEADER) || header.equals(Constant.SUCCESS_HEADER))) {
        return false;
      }
      headerByte=header.getBytes();
      keyByte=key.getBytes();
      valueByte=value.getBytes();
      System.arraycopy(headerByte, 0, message, 0, headerByte.length);
      System.arraycopy(keyByte, 0, message, 4, keyByte.length);
      System.arraycopy(valueByte, 0, message, 24, valueByte.length);
      out.write(message);
      out.flush();
      return true;
    } catch (IOException e) {
      System.out.println("The client tried to send a message but it failed.");
    }
    return result;
  }
  
  /**
   * The function receives a message from a server and returns it as a String.
   * @param socket - The socket connected to the server.
   * @return message - The message from the server.
   */
  public String receiveMessage(Socket socket) {
    ObjectInputStream in;
    String message="";
    try {
      in = new ObjectInputStream (socket.getInputStream());
      byte buffer[] = new byte[1024];
      in.read(buffer);
      String header=new String(Arrays.copyOfRange(buffer,0,4));
      String key=new String(Arrays.copyOfRange(buffer,4,24));
      String value=new String(Arrays.copyOfRange(buffer,24,1024));
      message=header+Constant.TOKEN+convertString(key)+Constant.TOKEN+convertString(value);
    } catch (IOException e) {
      System.out.println("The client tried to receive a message but no message was received.");
    }
    return message;
  }
  
  /**
   * The hash function. It transforms a value into a key.
   * @param value - The value.
   * @return key - The key.
   */
  public String hash(String value) {
    String key=null;
    int nb=value.hashCode()%Constant.MAX;
    if(nb < 0) {
      nb=-nb;
    }
    key = Integer.toString(nb);
    return key;
  }
  
  /**
   * The method asks for the user to choose a file to download.
   * @return filename - The file name
   */
  @SuppressWarnings("resource")
  public String chooseFile() {
    System.out.println("Enter the name of the file you wish to download.");
    String filename="";
    Scanner sc = new Scanner(System.in);
    filename=sc.nextLine();
    while(filename.equals("")) {
      filename=sc.nextLine();
    }
    System.out.println("You will search and download "+filename+". Are you sure ?(Type y to continue. Other to cancel)");
    String ans = sc.nextLine();
    if(ans.equals("y") || ans.equals("Y")) {
      return filename;
    }
    else {
      return null;
    }
  }
  
  /**
   * The method registers files that are in the shared directory defined in the configuration file.
   */
  public void register(OutputStream[] out) {
    File shareDirectory = new File(ConfigurationFile.getShareDirectory());
    registerDirectory(shareDirectory,out);
  }
  
  /**
   * The recursive method registerDirectory check the content of a directory and registers it to the decentralized indexing server.
   * Directories are not registered but their content is checked.
   * Found files are registered through the registerFile method.
   * @param shareDirectory - The directory to check for registration
   */
  public void registerDirectory (File shareDirectory, OutputStream[] out) {
    if (shareDirectory.isDirectory()) {
      File[] list = shareDirectory.listFiles();
      for (int i = 0; i<list.length;i++) {
        registerDirectory(list[i],out);
        if(list[i].isFile()) {
          try {
            registerFile(list[i],out);
          } catch (ServerNotFoundException e) {
            System.out.println("A server could not be found.");
          } catch (RegistrationException e) {
            System.out.println("Some files were not registered.");
          }
        }
      }
    }
  }
  
  /**
   * The method registers a file used as an input.
   * The file is replicated to another peer in the system if the replication was enabled in configuration file.
   * @param file - The file to register
   * @throws ServerNotFoundException - If the server found by hashing the name of the file does not exist.
   * @throws RegistrationException - If the file could not be registered.
   */
  public void registerFile(File file, OutputStream[] out) throws ServerNotFoundException, RegistrationException {
    String filename=file.getName();
    String key=hash(filename);
    int serverNb=getServer(key);
    if(serverNb==0) {
      throw new ServerNotFoundException();
    }
    Socket socket = this.socket.get(serverNb-1);
    String replicaId="";
    if(ConfigurationFile.isReplicaEnabled()) {
      for (int i=0;i<ConfigurationFile.getReplicaNumber();i++) {
        replicaId+=Constant.TOKEN+getReplicaId(Integer.parseInt(key),myId)[i];
      }
    }
    boolean result=false;
    try {
      result = sendMessage(socket,Constant.REGISTER_HEADER,key,""+myId+Constant.TOKEN+replicaId);
    }
    catch (NullPointerException e) {
      ;
    }
    if (result == false) {
      throw new RegistrationException();
    }
    try{
      if(ConfigurationFile.isReplicaEnabled()) {
        replicateFile(file,out);
      }
    }
    catch (RegistrationException e) {
      System.out.println("Warning : Could not upload a replica of the file "+filename+" during registration.");
    }
  }
  
  /**
   * The function replicates a file by sending it to another peer.
   * @param file - The file to replicate
   * @throws RegistrationException - If the file could not be replicated.
   */
  public void replicateFile(File file, OutputStream[] out) throws RegistrationException {
    String filename = file.getName();
    String key = hash(filename);
    int replicaId[]=getReplicaId(Integer.parseInt(key),myId);
    for (int i=0;i<replicaId.length;i++) {
      Socket socket = this.socket.get(replicaId[i]-1);
      try {
        sendMessage(socket,Constant.REGISTER_HEADER,key,""+myId);
      }
      catch(NullPointerException e) {
        throw new RegistrationException();
      }
      boolean result=false;
      try {
        result=sendMessage(socket,Constant.REPLICATE_HEADER,key,filename);
      }
      catch(NullPointerException e) {
        throw new RegistrationException();
      }
      if (result == false) {
        throw new RegistrationException();
      }
      String msg_in = receiveMessage(socket);
      StringTokenizer st = new StringTokenizer(msg_in,Constant.TOKEN);
      String header = st.nextToken();
      if (!header.equals(Constant.SUCCESS_HEADER)) {
        throw new RegistrationException();
      }
      else {
        try {
          FileInputStream inf=new FileInputStream(file);
          out[replicaId[i]-1] = new ObjectOutputStream(socket.getOutputStream());
          byte buffer[] = new byte[1024];
          int j;
          while((j=inf.read(buffer))!=-1){
             out[replicaId[i]-1].write(buffer,0,j);
          }
          inf.close();
          out[replicaId[i]-1].flush();
          sendMessage(socket,Constant.SUCCESS_HEADER,key,filename);
        } catch (IOException e) {
          throw new RegistrationException();
        }
    }
    
    }
  }
    
  /**
   * The function contacts the decentralized indexing server to find the original and replicate peers having the file.
   * If the supposed peer holding the index is down, and if replication is enabled, replica(s) will be contacted.
   * @param key - The key used to register the file.
   * @return peerId - An array having the ids of the peer holding the file. The original peer is always the first one in the array.
   */
  public int[] findPeerToDownload(String key) {
    int peerId[] = new int[ConfigurationFile.getReplicaNumber()+1];
    int indexId = getServer(key);
    peerId[0]=0;
    Socket socket = this.socket.get(indexId-1);
    System.out.println("Querying the location of the file...");
    sendMessage(socket,Constant.SEARCH_HEADER,key,"");
    String msg_in ="";
    msg_in=receiveMessage(socket);
    StringTokenizer st = new StringTokenizer(msg_in,Constant.TOKEN);
    if(st.nextToken().equals(Constant.SUCCESS_HEADER)) {
      st.nextToken();
      int i=0;
      while(st.hasMoreTokens()) {
        peerId[i]=Integer.parseInt(st.nextToken());
        i++;
      }
    }
    else {
      if(ConfigurationFile.isReplicaEnabled()) {
        int replicaNb=ConfigurationFile.getReplicaNumber();
        boolean found=false;
        for(int i=0;i<replicaNb;i++) {
          sendMessage(socket,Constant.SEARCH_HEADER,key,"");
          msg_in=receiveMessage(socket);
          st=new StringTokenizer(msg_in,Constant.TOKEN);
          if(st.nextToken().equals(Constant.SUCCESS_HEADER)) {
            found=true;
            st.nextToken();
            int j=0;
            while(st.hasMoreTokens()) {
              peerId[j]=Integer.parseInt(st.nextToken());
              j++;
            }
          }
          if(found==true) {
            break;
          }
        }
      }
    }
    return peerId;
  }
  
  /**
   * The function generate the id(s) of the peer(s) which holds or will hold the replica of a file designed by its key.
   * The peer having the replica cannot be the peer which has the original file.
   * @param key - The key of the file.
   * @param myId - The id of the peer replicating the file.
   * @return replicaId - The ids of the peer having the replica.
   */
  public int[] getReplicaId(int key, int myId) {
    int replicaNb = ConfigurationFile.getReplicaNumber();
    if(replicaNb!=0) {
      int replicaId[] = new int[replicaNb];
      for(int i=0;i<replicaNb;i++) {
        replicaId[i]=(myId+i+1)%ConfigurationFile.getClientNumber();
        if(replicaId[i]==0) {
          replicaId[i]=ConfigurationFile.getClientNumber();
        }
      }
      return replicaId;
    }
    else {
      int def[] = new int[1];
      def[0]=0;
      return def;
    }
  }
  
  /**
   * The function used to download a file. It throws an exception if the download failed.
   * @param filename - The file to download.
   * @param peerId - The id of the peer having the file.
   * @param in - An array of ObjectInputStream, used to allow multiple consecutive downloads.
   * @param action - The header to be sent. Download an original file or a replica.
   * @throws DownloadException - If the download failed.
   */
  public void download (String filename, int peerId, InputStream[] in, String action) throws DownloadException {
    String key = hash(filename);
    if(peerId==0) {
      throw new DownloadException();
    }
    try {
      sendMessage(socket.get(peerId-1),action,key,filename);
      System.out.println("Starting download...");
      in[peerId-1] = new ObjectInputStream(socket.get(peerId-1).getInputStream());
      FileOutputStream outf =new FileOutputStream(new File(ConfigurationFile.getDownloadDirectory()+"/"+filename));
      byte buffer[] = new byte[1024];
      int i;
      while((i=in[peerId-1].read(buffer))!=-1){
          outf.write(buffer,0,i);
          if(i<1024) {
            break;
          }
      }
      outf.close(); 
      System.out.println("Download finished.");
    } catch (IOException e) {
      throw new DownloadException();
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
   * The run() function asks the user for an action. Initiate connections with the servers if this was not already done.
   * It connects to one server to do a put(), get() or del() operation.
   * It is never ending.
   */
  @SuppressWarnings("resource")
  public void run() {
    InputStream in[] = new InputStream[8];
    OutputStream out[] = new OutputStream[8];
    boolean registered=false;
    while(true) {
      System.out.println("");
      System.out.println("Start ?");
      Scanner sc = new Scanner(System.in);
      sc.nextLine();
      if(socket.isEmpty()) {
        for(int i=0;i<ConfigurationFile.getClientNumber();i++) {
          socket.add(connectToServer(i+1));
        }
      }
      if(registered==false) {
        register(out);
      }
      registered = true;
      String filename = chooseFile();
      if(filename!=null) {
        String key = hash(filename);
        int peerId[] = findPeerToDownload(key);
        if (peerId[0]==0) {
          System.out.println("The file "+filename+ " could not be found. It may not exist or the connection has terminated.");
        }
        else {
          System.out.println("The file was found on peer "+peerId[0]+".");
          try {
            download(filename,peerId[0],in,Constant.DOWNLOAD_HEADER);
          } catch (DownloadException e) {
            System.out.println("The download failed. The system will now try to download on another server.");
            boolean downloadSucceeded=false;
            int i=1;
            while(i<peerId.length && downloadSucceeded==false) {
              try {
                if(i>ConfigurationFile.getReplicaNumber()) {
                  break;
                }
                i++;
                download(filename,peerId[i],in,Constant.DOWNLOAD_REPLICA_HEADER);
                downloadSucceeded=true;
              } catch (DownloadException e1) {
                System.out.println("The download failed. The system will now try to download on another server.");
              }
            }
            if(downloadSucceeded==false) {
              System.out.println("The file could not be downloaded.");
            }
          }
        }
      }
    }
  }

}
