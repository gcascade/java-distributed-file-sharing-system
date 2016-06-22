package sharingsystem;

import java.util.Scanner;

public class Client {

  /**
   * The function checks if a String represents an integer.
   * @param string - The string to be checked.
   * @return result - True of false. The String represents an integer or not.
   */
  public static boolean isInt(String string) {
    try {
      @SuppressWarnings("unused")
      int number = Integer.parseInt(string);
    }
    catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  /**
   * The main function.
   * The user is asked for his server number before the program run the Server thread and the Action thread.
   * @param args
   */
  @SuppressWarnings("resource")
  public static void main (String args[]) {
    Scanner sc = new Scanner(System.in);
    int clientNumber=0;
    int max =ConfigurationFile.getClientNumber();
    boolean confirmed = false;
    while(clientNumber < 1 || clientNumber > max || !confirmed){
      System.out.println("Please enter you client number. (Between 1 and "
    +max+").");
      String number=sc.next();
      if(isInt(number)) {
        clientNumber=Integer.parseInt(number);
      }
      if(clientNumber >= 1 && clientNumber <=max && isInt(number)) {
        System.out.println("Your client number is "+clientNumber+". Are you sure (y/n) ?");
        String answer = sc.next();
        if(answer.equals("y") || answer.equals("Y")) {
          confirmed = true;
        }
      }
    }
    Thread server = new Thread (new Server(clientNumber));
    server.start();
    Thread action = new Thread(new Action(clientNumber));
    action.start();
    
  }
}
