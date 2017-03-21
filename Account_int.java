import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account_int extends Remote {

	// transfer functionality
    void receive(int amount) throws RemoteException;

    // leader election
    void leaderIs(String accountID) throws RemoteException;
    void leaderIs() throws RemoteException;

//    void stall() throws RemoteException;

    // snapshot
    void receiveMarker(Account_int sender, Account_int leader, String snapshotID) throws RemoteException;

    // logging states (invoked on leader only)
    void logState(Account_int sender, String entry) throws RemoteException;
}
