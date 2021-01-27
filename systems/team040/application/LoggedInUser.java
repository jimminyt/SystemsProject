package systems.team040.application;

import systems.team040.functions.AccountType;

class LoggedInUser {
    private static LoggedInUser instance;
    private String username;
    private AccountType accountType;

    static LoggedInUser getInstance() {
        return instance == null ? (instance = new LoggedInUser()) : instance;
    }

    static void login(String username, AccountType type) {
        LoggedInUser user = getInstance();
        user.accountType = type;
        user.username = username;
    }

    static void logout() {
        instance = null;
    }

    private LoggedInUser() {}

    String getUsername() {
        return username;
    }

    AccountType getAccountType() {
        return accountType;
    }
}
