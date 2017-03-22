import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Account implements Account_int {

    int balance;
    ArrayList<Account_int> accounts;

    String id;
    Server_int server_stub;
    HashMap<String, ArrayList<Account_int>> activeSnapshots;

    public Account(int balance) {
        this.balance = balance;
        accounts = new ArrayList<>();
        accounts.add(this);
        activeSnapshots = new HashMap<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Account_int) {
            try {
                return this.getID().equals(((Account_int) obj).getID());
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        } else return false;
    }

    @Override
    public String getID() throws RemoteException {
        return id;
    }

    @Override
    public void receiveTransfer(Account_int sender, int amount) throws RemoteException {
        System.out.println("Received $" + amount + "from" + sender);
        balance += amount;
    }

    @Override
    public void leaderIs(String accountID) throws RemoteException {
        if (accountID.compareTo(id) < 0) {
            System.out.println("me: " + id + " > " + accountID);
            leaderIs(id);
        }
        if (accountID.compareTo(id) == 0) {
            System.out.println("I'm the leader");
            // start leading

        } else {
            getNextAccount().leaderIs(accountID);
            System.out.println("me: " + id + " < " + accountID);
        }
    }

    @Override
    public void leaderIs() throws RemoteException {
        getNextAccount().leaderIs(id);
    }

    @Override
    public void receiveMarker(Account_int sender, Account_int leader, String snapshotID) throws RemoteException {
        if(activeSnapshots.containsKey(snapshotID)) {

        } else {
            ArrayList<Account_int> unheardFromAccounts = new ArrayList<Account_int>(accounts);
            //remove yourself from accounts
            unheardFromAccounts.remove(sender);
            unheardFromAccounts.remove(this);
            System.out.println(unheardFromAccounts);
            activeSnapshots.put(snapshotID, unheardFromAccounts);
        }
    }

    @Override
    public void logState(Account_int sender, String entry) throws RemoteException {
        // TODO
    }

    private Account_int getNextAccount() {
        int i = 0;
        while (!this.equals(accounts.get(i))) i++;
        return accounts.get((i + 1) % accounts.size());
    }

    @Override
    public String toString() {
        return "Account object with id: " + id;
    }

    public void transfer() throws RemoteException {

        int time = (int) (Math.random() * 2000) + 1000;
        final int amount = (int) (Math.random() * balance) + 1;

        final Account_int this_account = this;

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Attempting transfer...");
                        try {
                            if (accounts.size() != 0 && balance > 0) {
                                int stubIndex = (int) (Math.random() * accounts.size());
                                Account_int stub = accounts.get(stubIndex);
                                balance -= amount;
                                stub.receiveTransfer(this_account, amount);
                                System.out.println("\t...sent $" + amount + " to " + accounts.get(stubIndex));
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


    private void connectToServer() throws RemoteException, NotBoundException {
        System.out.println("What ip is the server's rmi registry located on?");
        Scanner scan = new Scanner(System.in);
        String host_ip = scan.nextLine();

        System.out.println("Connecting to rmi registry...");
        Registry registry = LocateRegistry.getRegistry(host_ip,1099);
        server_stub = (Server_int) registry.lookup("server");

        Account_int stub = (Account_int) UnicastRemoteObject.exportObject(this, 0);
        try {
            id = server_stub.generateAccountID(InetAddress.getLocalHost().toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        server_stub.connect(this, id);
    }

    private void refreshAccounts() throws RemoteException {
        accounts = server_stub.getAccounts();
    }


    public static void main(String[] args) {

//    	String host = (args.length < 1) ? null : args[0];

        System.out.println("Account initializing...");
        final Account account = new Account(200);
        final Account account2 = new Account(200);

        try {


//            server_stub.connect(stub);
            account.connectToServer();
            account2.connectToServer();
            // TODO: every once in a while refresh account list

            System.err.println("Account ready: " + account);

            Timer accountRefreshTimer = new Timer();
            account.receiveMarker(account2, account, "snapshot_test");

//            accountRefreshTimer.schedule( new TimerTask() {
//                @Override
//                public void run() {
//                    try {
//                        account.refreshAccounts();
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }, 0, 1000);
//            System.out.println("starting timetest");


//            account.refreshAccounts();
//            account.timeTest();

//            account.leaderIs();
//            account.transfer();

        } catch (RemoteException e) {
            System.err.println("Account error: rmi connection issues:");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Account error: no 'server' bound in this rmi registry:");
            e.printStackTrace();
        }
    }


//    @Override
//    public String stall() throws RemoteException {
//        System.out.println("before");
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return "done";
////        int j = 2;
////        while (j++ < 1000000000) {
////            int s = j;
////            while (s > 0) {
////                s--;
////            }
////        }
//    }
//
//
//    private void timeTest() throws RemoteException {
//        Account_int next = getNextAccount();
//        System.out.println("before before");
//        if (!this.equals(next)) {
//            System.out.println("before");
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        System.out.println(accounts.get(0).stall());
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
//            System.out.println("after");
//            System.out.println("response: " + accounts.get(0).getID());
//        }
//    }

}
