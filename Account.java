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

    // snapshot-specific tracking

    // map form: <snapshotID, [unheardFromAccounts]>
    HashMap<String, HashSet<Account_int>> activeSnapshots;
    HashMap<String, StringBuilder> leaderLogs;
    HashMap<String, StringBuilder> logEntries;
    HashMap<String, HashSet<Account_int>> unloggedFromAccounts;
    HashMap<String, Integer> snapshotVolumes;
    HashMap<String, Integer> leaderVolumes;

    public Account(int balance) {
        this.balance = balance;

        accounts = new ArrayList<>();
        accounts.add(this);

        activeSnapshots = new HashMap<>();
        leaderLogs = new HashMap<>();  // leader
        logEntries = new HashMap<>();  // non-leader
        unloggedFromAccounts = new HashMap<>();
        snapshotVolumes = new HashMap<>();
        leaderVolumes = new HashMap<>();

    }

    @Override
    public boolean myEquals(Object obj) {
        if (obj instanceof Account_int) {
            String a, b;
            try {
                a = this.getID();
                b = ((Account_int) obj).getID();
                return a.equals(b);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
//            try {
//                return this.getID().equals(((Account_int) obj).getID());
//            } catch (RemoteException e) {
//                e.printStackTrace();
//                return false;
//            }
        } else return false;
    }

    @Override
    public String getID() throws RemoteException {
        return id;
    }

    @Override
    public void receiveTransfer(Account_int sender, int amount) throws RemoteException {
        balance += amount;
        //log transfers
        for(String snapshotID: snapshotVolumes.keySet()) {
            if (activeSnapshots.get(snapshotID).contains(sender)) {
                snapshotVolumes.put(snapshotID, (snapshotVolumes.get(snapshotID)+amount));
                logEntries.get(snapshotID).append("$"+amount + " from " + sender.getID() + ", ");
            }
        }
        System.out.println("Received $" + amount + " from " + sender.getID());
    }

    @Override
    public void leaderIs(String accountID) throws RemoteException {
        refreshAccounts();
        if (accountID.compareTo(id) < 0) {
//            getNextAccount().leaderIs(id);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getNextAccount().leaderIs(id);
                    } catch(RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
//            System.out.println("me: " + id + " > " + accountID);
        }
        else if (accountID.equals(id)) {
            System.out.println("I'm the leader");
            String snapshotID = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        leaderIs("");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }, 3000);

            // start snapshot
            this.receiveMarker(this,this,snapshotID);
        }
        else {
//            getNextAccount().leaderIs(accountID);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getNextAccount().leaderIs(accountID);
                    } catch(RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
//            System.out.println("me: " + id + " < " + accountID);
        }
    }

    @Override
    public void receiveMarker(Account_int sender, Account_int leader, String snapshotID) throws RemoteException {

        //TODO - remove this
        System.out.println(activeSnapshots);

        // Not first time seeing a marker from this snapshot //
        if (activeSnapshots.containsKey(snapshotID)) {
            System.out.println("here");

            // stop recording (remove sender from snapshot set)
            activeSnapshots.get(snapshotID).remove(sender);
        }

        // First time seeing a marker from this snapshot //
        else {
            // start recording
            HashSet<Account_int> unheardFromAccounts = new HashSet<Account_int>(accounts);
            // heard from sender and self
            for (Account_int account: new ArrayList<Account_int>(unheardFromAccounts)) {
                if (this.myEquals(account) || sender.myEquals(account)) unheardFromAccounts.remove(account);
            }
            activeSnapshots.put(snapshotID, unheardFromAccounts);

            StringBuilder entry = new StringBuilder("\n" + id + " | balance: $" + balance + " | transfers: ");
            logEntries.put(snapshotID, entry);

            snapshotVolumes.put(snapshotID, balance);

            if(this.myEquals(leader)) {
                StringBuilder logHeader = new StringBuilder("Snapshot log " + snapshotID + " lead by " + id + ":\n");
                leaderLogs.put(snapshotID, logHeader);
                unloggedFromAccounts.put(snapshotID, new HashSet<Account_int>(accounts));
            }

            // propagate
            try {
                Thread.sleep(1500);
            } catch(InterruptedException e) {
                System.out.println("Error while sleeping snapshot propagation");
                e.printStackTrace();
            }
            for (Account_int account: accounts) {
                if (!this.myEquals(account)) {
                    System.out.println("propagating snapshot to" + account.getID());
                    // new thread to prevent blocking
                    final Account_int _this = this;
                    final Account_int _leader = leader;
                    final String _snapshotID = snapshotID;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                account.receiveMarker(_this, _leader, _snapshotID);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        }

        // if all accounts are heard from, log
        if (activeSnapshots.get(snapshotID).isEmpty()) {
            final int volume = snapshotVolumes.get(snapshotID);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        leader.logState(sender, snapshotID, logEntries.get(snapshotID).toString(), volume);
                    } catch (RemoteException e) {
                        System.err.println("unable to log state");
                        e.printStackTrace();
                    }
                }
            }).start();
            activeSnapshots.remove(snapshotID);
            snapshotVolumes.remove(snapshotID);
        }
    }

    @Override
    public void logState(Account_int sender, String snapshotID, String entry, int totalVolume) throws RemoteException {
        StringBuilder log = leaderLogs.get(snapshotID).append(entry);
        leaderVolumes.put(snapshotID, leaderVolumes.getOrDefault(snapshotID,0)+totalVolume);
        System.out.println(unloggedFromAccounts);
        System.out.println("flag: " + unloggedFromAccounts.get(snapshotID).remove(sender));
        System.out.println(unloggedFromAccounts);
        //all logs received
        if (unloggedFromAccounts.get(snapshotID).isEmpty()) {
            System.out.println("made it");

            //append volume, write it to file
            StringBuilder currentLog = leaderLogs.get(snapshotID);
            currentLog.append("\nTotal volume: $" + leaderVolumes.get(snapshotID) + "\n");

            // make sure snapshot/ directory exists
            File dir = new File("snapshots");
            if (!dir.exists()) dir.mkdir();

            // create a new file named with the snapshot timestamp
            String pathname = "snapshots/" + snapshotID + ".txt";
            File logFile = new File(pathname);

            //write log to that file
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(logFile));
                writer.write(currentLog.toString());
                writer.close();
            } catch (IOException e) {
                System.err.println("Error while writing to snapshot log file " + id);
                e.printStackTrace();
            }

            // remove snapshot from hashes:
            //TODO -rm print
            System.out.println(activeSnapshots.remove(snapshotID));
            leaderLogs.remove(snapshotID);

            // monitor snapshot volume
            System.out.println("Snapshot complete with total volume: $" + totalVolume);
            }
        
    }

    private Account_int getNextAccount() {
        try {
            refreshAccounts(); // get updated ordering from server - a bit hacky...
        } catch (RemoteException e) {
            System.err.println("Unable to refresh accounts:");
            e.printStackTrace();
        }
        int i = 0;
        while (!this.myEquals(accounts.get(i))) i++;
        return accounts.get((i + 1) % accounts.size());
    }

    @Override
    public String toString() {
        return "Account object with id: " + id;
    }

    public void transfer() throws RemoteException {

        int time = (int) (Math.random() * 2000) + 1000;
        final int amount = (int) (Math.random() * balance) + 1;

        final Account_int _this = this;

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Attempting transfer...");
                        try {
                            if (accounts.size() != 0 && balance > 0) {
                                int stubIndex = (int) (Math.random() * accounts.size());
                                Account_int stub = accounts.get(stubIndex);
                                
                                if (!_this.myEquals(stub)) {
                                    balance -= amount;
                                    stub.receiveTransfer(_this, amount);
                                    System.out.println("\t...sent $" + amount + " to " + accounts.get(stubIndex).getID());
                                }
                                
                            } else {
                                System.out.println("\t...no other accounts found");
                            }
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

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        account.transfer();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 1000);



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
//        if (!this.myEquals(next)) {
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
