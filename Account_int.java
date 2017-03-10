import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account_int extends Remote{
    void transfer(int amount) throws RemoteException;
}
