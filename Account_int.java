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
    void snapshot(Account_int sender) throws RemoteException;
    void passSnapshotLog(String logEntry) throws RemoteException;
}
