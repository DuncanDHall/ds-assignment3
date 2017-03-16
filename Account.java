import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Account implements Account_server_int {

    int balance;
    ArrayList<Account_int> accounts;
    String id;

    public Account (int balance) {
        this.balance = balance;
        accounts = new ArrayList<Account_int>();
        id = "account_";
    }

    public void connectAccount(Account_int accountStub) throws RemoteException {
        accounts.add(accountStub);
    }

    public String getID() throws RemoteException {
    	return id;
    }

    public void receive(int amount) throws RemoteException {
        balance += amount;
    }

//    public void leaderIs(String accountID, Registry registry) throws RemoteException {
//        if (accountID.charAt(id.length()-1) < id.charAt(id.length()-1)) return;
//        else if (accountID.charAt(id.length()-1) > id.charAt(id.length()-1))  {
//            // get next account and pass accountID
//            System.out.println(id + "I think I'm the leader...");
//            Account_int nextStub = getNextAccount(registry);
//            nextStub.leaderIs(accountID, registry);
//        }
//        else {
//            // start snapshot
//            System.out.println("I'm the leader");
//        }
//    }
//
//    public void startLeading(Registry registry) throws RemoteException {
//        Account_int nextStub = getNextAccount(registry);
//        nextStub.leaderIs(id, registry);
//    }

//    private Account_int getNextAccount(Registry registry) throws RemoteException {
//    	//if accounts is empty return own stub
//    	if(accounts.isEmpty()) {
//    		try {
//    			return (Account_int) registry.lookup(id);
//    		} catch(NotBoundException e) {
//    			System.out.println("registry.lookup not f exception");
//    		}
//    	}
//    	int nextInt = id.charAt(id.length()-1)+1;
//    	if(accounts.size() <= nextInt) {
//    		//return account 0
//    		String nextId = "account_0";
//    		try {
//    			return (Account_int) registry.lookup(nextId);
//    		} catch(NotBoundException e) {
//    			System.out.println("registry.lookup not bound exception");
//    		}
//    	} else {
//    		//return account next int
//    		String nextId = "account_"+nextInt;
//    		try {
//    			return (Account_int) registry.lookup(nextId);
//    		} catch(NotBoundException e) {
//    			System.out.println("registry.lookup not bound exception");
//    		}
//    	}
//    	return null;
//    }

    @Override
    public String toString() { 
    	return "Account object with id: " + id;
    }

    public void transfer() throws RemoteException { 
        int time = (int)(Math.random() * 2000)+1000;
        final int amount = (int)(Math.random() * balance)+1;
        //int pid = (int)(Math.random() * 4)+1;
        new Timer().schedule (
        	new TimerTask() {
        		@Override
        		public void run() {
        			try {
        				if (accounts.size() != 0 && balance > 0) {
        					int stubIndex = (int)(Math.random() * accounts.size());
	        				Account_int stub = accounts.get(stubIndex);
	        				balance -= amount;
	        				stub.receive(amount);
	        				System.out.println("Sent $"+amount+" to "+accounts.get(stubIndex).getID());
        				}
        				transfer();
    				} catch (RemoteException e) {
                        System.out.println("Unable to connect to target account while transferring:");
                        e.printStackTrace();
                    }
                }
        	}, time
        );
    }


    public static void main(String[] args) {

//    	String host = (args.length < 1) ? null : args[0];

        System.out.println("Account initializing...");
        Account account = new Account(200);

        try {
            System.out.println("What ip is the server's rmi registry located on?");
            Scanner scan = new Scanner(System.in);
            String host_ip = scan.nextLine();

            System.out.println("Connecting to rmi registry...");
            Account_int stub = (Account_int) UnicastRemoteObject.exportObject(account, 0);

            Registry registry = LocateRegistry.getRegistry(host_ip,1099);
            Server_int server_stub = (Server_int) registry.lookup("server");

            System.err.println("Account ready");

//            account.leaderIs(id, registry);
//            System.out.println(id);
            account.transfer();

        } catch (RemoteException e) {
            System.err.println("Account error: rmi connection issues:");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Account error: no 'server' bound in this rmi registry:");
            e.printStackTrace();
        }
    }

}
