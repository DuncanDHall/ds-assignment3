import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by duncan on 3/17/17.
 */
public class SnapshotAssistant {

    String id;
    Account_int leader;
    HashSet<Account_int> downStreamAccounts;

    StringBuilder log;
    int volume;  // total amount of money tracked in snapshot

    public SnapshotAssistant(Account_int leader, ArrayList<Account_int> accounts) {
        this.id = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date());
        this.leader = leader;
        downStreamAccounts = new HashSet<>();
        try {

            for (Account_int a: accounts) downStreamAccounts.add(a);
            log = new StringBuilder("Snapshot id: "+id+"\nLeader: "+leader.getID()+"\n");

        } catch (RemoteException e) {
            System.err.println("SnapshotAssistant error while getting IDs:");
            e.printStackTrace();
        }
    }

    public SnapshotAssistant(SnapshotAssistant another) {
        // used to copy an existing snapshot
        this.id = another.id;
        this.leader = another.leader;
        this.downStreamAccounts = another.downStreamAccounts;
    }

    public String getID() {
        return id;
    }

    public void log(String logEntry, int amount) {
        log.append(logEntry+"\n");
        volume += amount;
    }

    public boolean heardFrom(Account_int sender) {
        // returns true if heard from all other accounts.
        try {
            downStreamAccounts.remove(sender.getID());
        } catch (RemoteException e) {
            System.err.println("Snapshot error - getting an ID");
            e.printStackTrace();
        }
        return downStreamAccounts.isEmpty();
    }

    public void saveSnapshot() {
        // create final entry in log for total volume:
        log.append("Total volume: $" + volume);

        // create a new file named with the snapshot timestamp
        File logFile = new File(id);

        //write log to that file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(log.toString());
        } catch (IOException e) {
            System.err.println("Error while writing to snapshot log file " + id);
            e.printStackTrace();
        }

        // monitor snapshot volume
        System.out.println("Snapshot complete with total volume: $" + volume);
    }

    public void propagate() {
        for (Account_int account: downStreamAccounts){
            try {
                account.snapshot(this.getID(), this);
            } catch (RemoteException e) {
                System.err.println("Error while propigating snapshot:")
                e.printStackTrace();
            }
        }
    }
}
