import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by duncan on 3/16/17.
 */
public class Server implements Server_int {

    ArrayList<Account_server_int> accounts;
    HashSet<String> accountNames;

    public Server() {
        accounts = new ArrayList<>();
        accountNames = new HashSet<>();
    }

    public boolean connect(Account_server_int stub) throws RemoteException {
        // check if account name is correct pattern:
        if (!validateName(stub.getID())) return false;

        // rename if duplicate
        while (accountNames.contains(stub.getID())) {
            stub.rename(reformatName(stub.getID()));
        }

        for (Account_server_int account: accounts) {
            // add existing account stubs to new account
            stub.connectAccount(account);
            // add new account to existing account
            account.connectAccount(stub);
        }
        accounts.add(stub);
        accountNames.add(stub.getID());
        System.out.println("Added " + stub.getID() + " to account registry");
        return true;
    }

    private String reformatName(String id) {
        StringBuilder sb = new StringBuilder(id).insert(7, 't');
        return sb.toString();
    }


    private boolean validateName(String id) {
        Pattern p = Pattern.compile("account\\d*@.+");
        Matcher m = p.matcher(id);
        return m.matches();
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
