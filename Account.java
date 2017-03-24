import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class Account implements Account_int {

    int balance;
    ArrayList<Account_int> accounts;

    String id;
    Server_int server_stub;

    // map form: <snapshotID, [unheardFromAccounts]>
    HashMap<String, HashSet<Account_int>> activeSnapshots;

    HashMap<String, StringBuilder> snapshotLogs;
    HashMap<String, StringBuilder> logEntries;
    HashMap<String, HashSet<Account_int>> unloggedFromAccounts;
    HashMap<String, Integer> snapshotVolumes;
    HashMap<String, Integer> leaderVolumes;

    public Account(int balance) {
        this.balance = balance;
        accounts = new ArrayList<>();
        accounts.add(this);
        activeSnapshots = new HashMap<>();
        snapshotLogs = new HashMap<>();
        logEntries = new HashMap<>();
        unloggedFromAccounts = new HashMap<>();
        snapshotVolumes = new HashMap<>();
        leaderVolumes = new HashMap<>();

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
        //TODO if unheard from sender record
        //log transfers
        for(StringBuilder t: logEntries.values()) {
            t.append("$"+amount + " from " + sender + ", ");
        }
        for(String id: snapshotVolumes.keySet()) {
            snapshotVolumes.put(id, (snapshotVolumes.get(id)+amount));
        }
        balance += amount;
        System.out.println("Received $" + amount + "from" + sender); 
    }

    @Override
    public void leaderIs(String accountID) throws RemoteException {
        refreshAccounts();
        if (accountID.compareTo(id) < 0) {
            System.out.println("me: " + id + " > " + accountID);
            leaderIs(id);
        }
        if (accountID.compareTo(id) == 0) {
            System.out.println("I'm the leader");
            String snapshotID = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());
            this.receiveMarker(this,this,snapshotID);

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
        if (activeSnapshots.containsKey(snapshotID)) {
            // stop recording (remove sender from snapshot set)
            activeSnapshots.get(snapshotID).remove(sender);

            // if all accounts are heard from, log
            if (activeSnapshots.get(snapshotID).isEmpty()) {
                leader.logState(sender, snapshotID, logEntries.get(snapshotID).toString(), snapshotVolumes.get(snapshotID));
                activeSnapshots.remove(snapshotID);
                snapshotVolumes.remove(snapshotID);
            }

        } else { //first marker
            // start recording
            HashSet<Account_int> unheardFromAccounts = new HashSet<Account_int>(accounts);
            // heard from sender and self
            unheardFromAccounts.remove(sender);
            unheardFromAccounts.remove(this);
            activeSnapshots.put(snapshotID, unheardFromAccounts);
            
            logEntries.get(snapshotID).append(id + " | balance: $" + balance + " | transfers: ");
            snapshotVolumes.put(snapshotID, balance);

            if(this.equals(leader)) {
                StringBuilder log = new StringBuilder("Snapshot log " + snapshotID + " lead by " + id + ": \n");
                snapshotLogs.put(snapshotID, log);
                unloggedFromAccounts.put(snapshotID, unheardFromAccounts);
            }

            // propagate
            // TODO - delay propagation with Thread.sleep()
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                System.out.println("interrupted exception on thread");
                e.printStackTrace();
            }
            Account_int thisAccount = this;
            for (Account_int account: unheardFromAccounts) {
                // new thread to prevent blocking
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            account.receiveMarker(thisAccount, leader, snapshotID);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }

        //TODO - finish method
    }

    @Override
    public void logState(Account_int sender, String snapshotID, String entry, int totalVolume) throws RemoteException {
        StringBuilder log = snapshotLogs.get(snapshotID).append(entry);
        leaderVolumes.put(snapshotID, leaderVolumes.getOrDefault(snapshotID,0)+totalVolume);
        //all logs received
        if (unloggedFromAccounts.get(snapshotID).isEmpty()) {
            //append volume, write it to file
            StringBuilder currentLog = snapshotLogs.get(snapshotID);
            currentLog.append("Total volume: " + totalVolume);

            // make sure snapshot/ directory exists
            File dir = new File("snapshots");
            if (!dir.exists()) dir.mkdir();

            // create a new file named with the snapshot timestamp
            String pathname = "snapshots/" + id;
            File logFile = new File(pathname);

            //write log to that file
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
                writer.write(currentLog.toString());
            } catch (IOException e) {
                System.err.println("Error while writing to snapshot log file " + id);
                e.printStackTrace();
            }

            // monitor snapshot volume
            System.out.println("Snapshot complete with total volume: $" + totalVolume);
            }
        
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
        //final Account account2 = new Account(200);

        try {
//            server_stub.connect(stub);
            account.connectToServer();
            //account2.connectToServer();
            // TODO: every once in a while refresh account list

            System.err.println("Account ready: " + account);

            //Timer accountRefreshTimer = new Timer();
            //account.refreshAccounts();
            //account.receiveMarker(account2, account, "snapshot_test");
            account.transfer();
            account.leaderIs();
           // accountRefreshTimer.schedule( new TimerTask() {
           //     @Override
           //     public void run() {
           //         try {
           //             account.refreshAccounts();
           //         } catch (RemoteException e) {
           //             e.printStackTrace();
           //         }
           //     }
           // }, 0, 1000);
//            System.out.println("starting timetest");


//            account.refreshAccounts();
//            account.timeTest();

//            account.leaderIs();
           

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
