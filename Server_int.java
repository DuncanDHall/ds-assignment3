import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Created by duncan on 3/16/17.
 */
public interface Server_int extends Remote {
    String generateAccountID(String ip) throws RemoteException;
    boolean connect(Account_int stub, String accountID) throws RemoteException;
    ArrayList<Account_int> getAccounts() throws RemoteException;
}
