import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.lang.Math;
import java.util.Timer;
import java.util.TimerTask;

public class Account implements Account_int {

    private int balance;

    public void Account (int balance) {
        this.balance = balance;
    }

    public void receive(int amount) throws RemoteException {
        balance += amount;
    }

    private void transfer(Account_int stub) throws RemoteException {
        int time = (int)(Math.random() * 45000)+5000;
        int amount = (int)(Math.random() * balance)+1;
        balance -= amount;
        //int pid = (int)(Math.random() * 4)+1;
        new Timer().schedule (
        	new TimerTask() {
        		@Override
        		public void run() {
        			try {
        				stub.receive(amount);
        				} catch(Exception e) {
                        System.out.println("registerName exception");
                    }
        		}
        	}, time
        );
       

    }

    public static void main(String[] args) {
    	System.out.println((int)(Math.random() * 50000)+5000);
    	String host = (args.length < 1) ? null : args[0];

    	 try {
            Registry registry = LocateRegistry.getRegistry(host);
            Account_int stub = (Account_int) registry.lookup("im_server");
            Client_int clientStub = (Client_int) UnicastRemoteObject.exportObject(client, 0);

            System.err.println("Client ready");

            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine();
            while(client.executeCommand(input, stub, clientStub)) {
                input = sc.nextLine();
//                executeCommand(input, stub, clientStub);
            }


            System.out.println("shutting down client...");
            sc.close();
            UnicastRemoteObject.unexportObject(client, true);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
