import java.io.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by duncan on 3/17/17.
 */
public class SnapshotAssistant implements Serializable {

    String id;
    Account_int leader;
    String ownerID;
    HashMap<String, Account_int> downStreamAccounts;

    ArrayList<Integer> incomingTransfers;

    StringBuilder myLog;
    StringBuilder leaderLog;
    int balance;
    int volume;  // total amount of money tracked in snapshot

    public SnapshotAssistant(Account_int leader, String ownerID, int balance, ArrayList<Account_int> accounts) {
        this.id = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());
        this.leader = leader;
        this.ownerID = ownerID;
        this.balance = balance;
        downStreamAccounts = new HashMap<>();
        try {
            for (Account_int a: accounts) downStreamAccounts.put(a.getID(), a);

            // leader specific:
            String leaderID = leader.getID();
            if (leaderID == ownerID) {
                leaderLog = new StringBuilder();
                leaderLog.append("Snapshot log " + id + " lead by " + leaderID + ":\n");
            }
        } catch (RemoteException e) {
            System.err.println("SnapshotAssistant error while getting IDs:");
            e.printStackTrace();
        }
        myLog = new StringBuilder("<" + ownerID + " | ");
        incomingTransfers = new ArrayList<>();
    }

    public SnapshotAssistant(SnapshotAssistant another, String ownerID, int balance, ArrayList<Account_int> accounts) {
        // used to create a snapshot with the same leader and id.
        this.id = another.id;
        this.leader = another.leader;
        this.ownerID = ownerID;
        this.balance = balance;
        downStreamAccounts = new HashMap<>();
        try {
            for (Account_int a: accounts) downStreamAccounts.put(a.getID(), a);
        } catch (RemoteException e) {
            System.err.println("SnapshotAssistant error while getting IDs:");
            e.printStackTrace();
        }
        myLog = new StringBuilder("<" + ownerID + " | ");
        incomingTransfers = new ArrayList<>();
    }

    public String getID() {
        return id;
    }

    public void logTransfer(int amount) {
        volume += amount;
        incomingTransfers.add(amount);
    }

    public boolean heardFrom(String senderID) {
        // returns true if heard from all other accounts.
        downStreamAccounts.remove(senderID);
        return downStreamAccounts.isEmpty();
    }

    public void reportToLeader() {

        myLog.append("balance: $" + balance + " | transfers:");
        for (int amount: incomingTransfers) myLog.append(" $" + amount);
        myLog.append(">\n");

        try {
            leader.passSnapshotLog(id, myLog.toString(), volume);
        } catch (RemoteException e) {
            System.err.println("Unable to pass state log to leader:");
            e.printStackTrace();
        }
    }

    public void saveSnapshot() {
        leaderLog.append("Total volume: " + volume);

        // make sure snapshot/ directory exists
        File dir = new File("snapshots");
        if (!dir.exists()) dir.mkdir();

        // create a new file named with the snapshot timestamp
        String pathname = "snapshots/" + id;
        File logFile = new File(pathname);

        //write log to that file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(leaderLog.toString());
        } catch (IOException e) {
            System.err.println("Error while writing to snapshot log file " + id);
            e.printStackTrace();
        }

        // monitor snapshot volume
        System.out.println("Snapshot complete with total volume: $" + volume);
    }

    public void propagate() {
        for (String accountID: downStreamAccounts.keySet()){
            try {
                String id = this.getID();
                downStreamAccounts.get(accountID).snapshot(ownerID, id, this);
            } catch (RemoteException e) {
                System.err.println("Error while propagating snapshot:");
                e.printStackTrace();
            }
        }
    }

    public void appendToLeaderLog(String logEntry, int amount) {
        leaderLog.append(logEntry);
        volume += amount;
    }
}
