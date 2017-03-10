import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Account implements Account_int {

    private int balance;

    public void Account (int balance) {
        this.balance = balance;
    }

    void transfer(int amount) throws RemoteException {
        // TODO
    }

    public static void main(String[] args) {

    }
}
