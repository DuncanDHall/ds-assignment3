import java.rmi.RemoteException;

/**
 * Created by duncan on 3/16/17.
 */
public interface Account_server_int extends Account_int {
    String getID() throws RemoteException;
    // called by server telling accounts to remember stubs to other accounts
    void connectAccount(Account_int accountStub) throws RemoteException;
}
