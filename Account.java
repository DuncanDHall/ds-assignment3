import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Account implements Account_server_int {

    int balance;
    ArrayList<Account_int> accounts;
    String id;

    // snapshot stuff
    HashMap<String, SnapshotAssistant> activeSnapshots;

    public Account (int balance) {
        this.balance = balance;
        accounts = new ArrayList<>();
        try {
            id = "account@" + InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println("Account failed to discover it's name:");
            e.printStackTrace();
        }

        activeSnapshots = new HashMap<>();
    }

    public void connectAccount(Account_int accountStub) throws RemoteException {
        accounts.add(accountStub);
    }

    public void rename(String newName) throws RemoteException {
        this.id = newName;
    }

    public String getID() throws RemoteException {
    	return id;
    }

    public void receive(int amount) throws RemoteException {
        System.out.println("Recieved $" + amount);
        balance += amount;
    }

    public void leaderIs(String accountID) throws RemoteException {
        if (accountID.compareTo(this.getID()) < 0) {
            System.out.println("me: " + this.getID() + " > " + accountID);
            getNextAccount().leaderIs(this.getID());
            return;
        }
        if (accountID.compareTo(this.getID()) == 0) {
            System.out.println("I'm the leader");
            // start leading
            leadSnapshot();
        }
        else {
            System.out.println("me: " + this.getID() + " < " + accountID);
            getNextAccount().leaderIs(accountID);
        }
    }

    public void leaderIs() throws RemoteException {
        getNextAccount().leaderIs(this.getID());
    }

    public void snapshot(String snapshotID, SnapshotAssistant sa) throws RemoteException {
        //TODO
    }

    public void passSnapshotLog(Account_int sender, String snapshotID, String logEntry, int amount) throws RemoteException {
        SnapshotAssistant sa = activeSnapshots.get(snapshotID);
        sa.log(logEntry, amount);
        boolean snapshotComplete = sa.heardFrom(sender);
        if (snapshotComplete) sa.saveSnapshot();

        //TODO
    }

    private void leadSnapshot() {
        //TODO
        SnapshotAssistant sa = new SnapshotAssistant(this, accounts);
        sa.propagate();
    }

    private Account_int getNextAccount() throws RemoteException {
    	//if accounts is empty return reference to self
    	if(accounts.isEmpty()) {
    	    return this;
    	}

    	// else return the smallest greater account, or if there is none, the smallest overall
    	Account_int leastGreater = null;
    	Account_int least = accounts.get(0);

    	for (Account_int accountStub: accounts) {
    	    // updating leastGreatest
    	    if (accountStub.getID().compareTo(this.getID()) > 0) {
    	        if (leastGreater == null || accountStub.getID().compareTo(leastGreater.getID()) < 0) {
    	            leastGreater = accountStub;
                }
            }

            // updating least
            if (accountStub.getID().compareTo(least.getID()) < 0) least = accountStub;
        }

        return (leastGreater == null)? least: leastGreater;
    }

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
                    System.out.println("Attempting transfer...");
        			try {
        				if (accounts.size() != 0 && balance > 0) {
        					int stubIndex = (int)(Math.random() * accounts.size());
	        				Account_int stub = accounts.get(stubIndex);
	        				balance -= amount;
	        				stub.receive(amount);
	        				System.out.println("\t...sent $"+amount+" to "+accounts.get(stubIndex).getID());
        				} else {
                            System.out.println("\t...no other accounts found");
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
            Account_server_int stub = (Account_server_int) UnicastRemoteObject.exportObject(account, 0);

            Registry registry = LocateRegistry.getRegistry(host_ip,1099);
            Server_int server_stub = (Server_int) registry.lookup("server");

            System.out.println("Connecting to other accounts...");

            server_stub.connect(stub);

            System.err.println("Account ready: " + account.getID());

            account.leaderIs();
//            account.transfer();

        } catch (RemoteException e) {
            System.err.println("Account error: rmi connection issues:");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Account error: no 'server' bound in this rmi registry:");
            e.printStackTrace();
        }
    }

}
