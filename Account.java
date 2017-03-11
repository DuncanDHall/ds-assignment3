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

    public Account (int balance) {
        this.balance = balance;
        accounts = new ArrayList<Account_int>();
    }

    public void receive(int amount) throws RemoteException {
        balance += amount;
    }

    public void connect(Account_int stub) throws  RemoteException {
        System.out.println(accounts);
        accounts.add(stub);
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

    public String generateName(Registry registry) {
        int i = 0;
        while (true) {
            String id = String.format("account_%d", i);
            try {
                connect((Account_int) registry.lookup(id));
            } catch (NotBoundException re) {
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
    	System.out.println((int)(Math.random() * 50000)+5000);
    	String host = (args.length < 1) ? null : args[0];

    	Account account = new Account(200);
        System.out.println(account.accounts.toString());
        System.out.println(account.balance);

        try {
            Registry registry = LocateRegistry.getRegistry(1099);

            Account_int stub = (Account_int) UnicastRemoteObject.exportObject(account, 0);

            String id = account.generateName(registry);
            registry.rebind(id, stub);

            System.err.println("Account ready");


            System.out.println(id);


//            System.out.println("shutting down client...");

//            UnicastRemoteObject.unexportObject(account, true);
        } catch (Exception e) {
            System.err.println("Account exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
