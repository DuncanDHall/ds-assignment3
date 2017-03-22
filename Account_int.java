import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account_int extends Remote {
    // necessary only for comparing identity between references (see Account.equals() method)
    String getID() throws RemoteException;

	// transfer functionality
    void receiveTransfer(Account_int sender, int amount) throws RemoteException;

    // leader election
    void leaderIs(String accountID) throws RemoteException;
    void leaderIs() throws RemoteException;

//    String stall() throws RemoteException;

    // snapshot
    void receiveMarker(Account_int sender, Account_int leader, String snapshotID) throws RemoteException;

    // logging states (invoked on leader only)
    void logState(Account_int sender, String snapshotID, String entry, int totalVolume) throws RemoteException;

}
