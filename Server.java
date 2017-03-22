import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

/**
 * Created by duncan on 3/16/17.
 */
public class Server implements Server_int {

    ArrayList<Account_int> accounts;
    int accountNum;

    public Server() {
        accounts = new ArrayList<>();
        accountNum = 0;
    }

    @Override
    public String generateAccountID(String ip) throws RemoteException {
        return "account" + accountNum + "@" + ip;
    }

    @Override
    public boolean connect(Account_int stub, String accountID) throws RemoteException {
        accounts.add(stub);
        accountNum++;
        System.out.println("Account " + accountID + " was connected.");
        return true;
    }

    @Override
    public ArrayList<Account_int> getAccounts() throws RemoteException {
        return accounts;
    }

    public static void main(String[] args) {
        System.out.println("Server initializing...");
        Server server = new Server();

        try {
            System.out.println("Connecting to rmi registry...");
            Registry registry = LocateRegistry.getRegistry(1099);
            Server_int stub = (Server_int) UnicastRemoteObject.exportObject(server, 0);

            registry.rebind("server", stub);

            System.out.println("Server ready!");
            System.out.println("ip: " + InetAddress.getLocalHost());
        } catch (RemoteException e) {
            System.err.println("Server crashed due to rmi connection issues:");
            e.printStackTrace();
        } catch (UnknownHostException e) {
            System.err.println("Server failed startup while attempting to access ip:");
            e.printStackTrace();
        }
    }
}
