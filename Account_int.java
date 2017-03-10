import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account_int extends Remote{
    void receive(int amount) throws RemoteException;
    void connect(Account_int stub) throws  RemoteException;
}
