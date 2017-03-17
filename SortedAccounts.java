import java.util.ArrayList;

/**
 * Created by duncan on 3/16/17.
 * a wrapper around an arraylist that adds so that list is sorted by .getID()
 */
public class SortedAccounts<Account_int> extends ArrayList<Account_int> {
    @Override
    public boolean add(Account_int accountStub) {
        boolean r = super.add(accountStub);
        return r;
    }

    public Account_int getNext(Account_int predecessor) {
        //TODO
        return null;
    }
}
