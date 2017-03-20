import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account_int extends Remote {
	String getID() throws RemoteException;

	// transfer functionality
    void receive(int amount) throws RemoteException;

    // leader election
    void leaderIs(String accountID) throws RemoteException;
    void leaderIs() throws RemoteException;

    // snapshot
    void snapshot(String sender, String snapshotID, SnapshotAssistant sa) throws RemoteException;
//    void passSnapshotLog(Account_int sender, String snapshotID, String logEntry, int amount) throws RemoteException;
    void passSnapshotLog(String snapshotID, String logEntry, int totalVolume) throws RemoteException;
}
