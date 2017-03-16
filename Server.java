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

    ArrayList<Account_server_int> accounts;

    public Server() {
        accounts = new ArrayList<>();
    }

    @Override
    public void connect(Account_server_int stub) throws RemoteException {
        for (Account_server_int account: accounts) {
            // add existing account stubs to new account
            stub.connectAccount(account);
            // add new account to existing account
            account.connectAccount(stub);
        }
        System.out.println("Added " + stub.getID() + " to account registry");
    }

    public static void main(String[] args) {
        System.out.println("Server initializing...");
        Server server = new Server();

        try {
            System.out.println("Connecting to rmi registry...");
            Registry registry = LocateRegistry.getRegistry(1099);
            Server_int stub = (Server_int) UnicastRemoteObject.exportObject(server, 0);
            registry.rebind("server", registry);

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
