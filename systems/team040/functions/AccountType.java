package systems.team040.functions;

import java.util.HashMap;
import java.util.Map;

/**
 * enum for different account types and maintains a hashmap so each accounttype can be acquired from it's int
 * value on the database
 */
public enum AccountType {
    Admin(1), Registrar(2), Student(3), Teacher(4);

    public final int value;
    private static Map<Integer, AccountType> map = new HashMap<>();

    static {
        for(AccountType at : AccountType.values()) {
            map.put(at.value, at);
        }
    }

    public static AccountType fromInt(int i) { return map.get(i); }
    AccountType(int i) { this.value = i; }
}
