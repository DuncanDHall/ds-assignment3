import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.lang.Math;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

public class Account implements Account_int {

    int balance;
    ArrayList<Account_int> accounts;
    String id;

    public Account (int balance) {
        this.balance = balance;
        accounts = new ArrayList<Account_int>();
        id = "account_";
    }

    public String getID() throws RemoteException {
    	return id;
    }

    public void receive(int amount) throws RemoteException {
        balance += amount;
    }

    public void connect(Account_int stub) throws RemoteException {
        accounts.add(stub);
    }

    public void leaderIs(String accountID, Registry registry) throws RemoteException {
        if (accountID.charAt(id.length()-1) < id.charAt(id.length()-1)) return;
        else if (accountID.charAt(id.length()-1) > id.charAt(id.length()-1))  {
            // get next account and pass accountID
            Account_int nextStub = getNextAccount(registry);
            nextStub.leaderIs(accountID, registry);
        }
        else {
            // start snapshot
            System.out.println("I'm the leader");
        }
    }

    public void startLeading(Registry registry) throws RemoteException {
        Account_int nextStub = getNextAccount(registry);
        nextStub.leaderIs(id, registry);
    }

    private Account_int getNextAccount(Registry registry) throws RemoteException {
    	//if accounts is empty return own stub
    	if(accounts.isEmpty()) {
    		try {
    			return (Account_int) registry.lookup(id);
    		} catch(NotBoundException e) {
    			System.out.println("registry.lookup not f exception");
    		}
    	}
    	int nextInt = id.charAt(id.length()-1)+1;
    	if(accounts.size() <= nextInt) {
    		//return account 0
    		String nextId = "account_0";
    		try {
    			return (Account_int) registry.lookup(nextId);
    		} catch(NotBoundException e) {
    			System.out.println("registry.lookup not bound exception");
    		}
    	} else {
    		//return account next int
    		String nextId = "account_"+nextInt;
    		try {
    			return (Account_int) registry.lookup(nextId);
    		} catch(NotBoundException e) {
    			System.out.println("registry.lookup not bound exception");
    		}
    	}
    	return null;
    }

    @Override
    public String toString() { 
    	return id;
    }

    public void transfer() throws RemoteException { 
        int time = (int)(Math.random() * 2000)+1000;
        int amount = (int)(Math.random() * balance)+1;
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
	        				System.out.println("sent "+amount+" of money to "+accounts.get(stubIndex).getID());
        				}
        				transfer();
    				} catch(Exception e) {
                        System.out.println("registerName exception");
                    }
        		}
        	}, time
        );
    }

    public String generateName(Registry registry, Account_int stub) {
        int i = 0;
        while (true) {
            String id = String.format("account_%d", i);
            try {
            	Account_int other_stub =(Account_int) registry.lookup(id);
            	other_stub.connect(stub);
            	connect(other_stub);
            } catch (NotBoundException re) {
            	this.id = id;
                return id;

            } catch (Exception e) {
                System.err.println("Bad exception: " + e.toString());
                e.printStackTrace();
                return "bad_account";
            }
            i++;
        }
    }

    public static void main(String[] args) {

    	// int i = 0;
    	// while (true) {
    	// 	System.out.println(i);
    	// 	i++;
    	// }

    	//System.out.println((int)(Math.random() * 50000)+5000);
    	String host = (args.length < 1) ? null : args[0];

    	Account account = new Account(200);
        //System.out.println(account.accounts.toString());
        //System.out.println(account.balance);

        try {
            Registry registry = LocateRegistry.getRegistry(1099);

            Account_int stub = (Account_int) UnicastRemoteObject.exportObject(account, 0);

            String id = account.generateName(registry, stub);
            registry.rebind(id, stub);

            System.err.println("Account ready");

            account.leaderIs(id, registry);
            System.out.println(id);
            account.transfer();

//            System.out.println("shutting down client...");

//            UnicastRemoteObject.unexportObject(account, true);
        } catch (Exception e) {
            System.err.println("Account exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
