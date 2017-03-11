import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account_int extends Remote{
	String getID() throws RemoteException;

	// transfer functionality
    void receive(int amount) throws RemoteException;
    void connect(Account_int stub) throws RemoteException;

    // leader election
    void leaderIs(String accountID) throws RemoteException;

    // snapshot
}
