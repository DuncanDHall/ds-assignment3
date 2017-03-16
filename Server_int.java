import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by duncan on 3/16/17.
 */
public interface Server_int extends Remote {
    void connect(Account_server_int stub) throws RemoteException;
}
