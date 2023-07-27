package service;

import enums.LimitType;
import enums.OfferStatus;
import lombok.Getter;
import lombok.Setter;
import pojo.Account;
import pojo.LimitOffer;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public
class AccountService {
    private Connection connection;

    public AccountService(String url, String username, String password) {
        try {
            // Set up the database connection
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Account createAccount(int customerId, int accountLimit, int perTransactionLimit) {
        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO accounts (customer_id, account_limit, per_transaction_limit, last_account_limit, last_per_transaction_limit, account_limit_update_time, per_transaction_limit_update_time) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, customerId);
            stmt.setInt(2, accountLimit);
            stmt.setInt(3, perTransactionLimit);
            stmt.setInt(4, accountLimit);
            stmt.setInt(5, perTransactionLimit);
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            // Get the generated account_id
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int accountId = generatedKeys.getInt(1);
                return new Account(accountId, customerId, accountLimit, perTransactionLimit);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Account getAccount(int accountId) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM accounts WHERE account_id = ?");
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int customerId = rs.getInt("customer_id");
                int accountLimit = rs.getInt("account_limit");
                int perTransactionLimit = rs.getInt("per_transaction_limit");
                int lastAccountLimit = rs.getInt("last_account_limit");
                int lastPerTransactionLimit = rs.getInt("last_per_transaction_limit");
                LocalDateTime accountLimitUpdateTime = rs.getTimestamp("account_limit_update_time").toLocalDateTime();
                LocalDateTime perTransactionLimitUpdateTime = rs.getTimestamp("per_transaction_limit_update_time").toLocalDateTime();
                return new Account(accountId, customerId, accountLimit, perTransactionLimit);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void createLimitOffer(int accountId, LimitType limitType, int newLimit, LocalDateTime offerActivationTime, LocalDateTime offerExpiryTime) {
        Account account = getAccount(accountId);
        if (account == null) {
            System.out.println("Account not found.");
            return;
        }

        int currentLimit = limitType == LimitType.ACCOUNT ? account.getAccountLimit() : account.getPerTransactionLimit();

        if (newLimit <= currentLimit) {
            System.out.println("New limit must be greater than the current limit.");
            return;
        }
        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO limit_offers (account_id, limit_type, new_limit, offer_activation_time, offer_expiry_time, status) VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, accountId);
            stmt.setString(2, limitType.toString());
            stmt.setInt(3, newLimit);
            stmt.setTimestamp(4, Timestamp.valueOf(offerActivationTime));
            stmt.setTimestamp(5, Timestamp.valueOf(offerExpiryTime));
            stmt.setString(6, OfferStatus.PENDING.toString());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<LimitOffer> getActiveLimitOffers(int accountId, LocalDateTime activeDate) {
        List<LimitOffer> activeOffers = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM limit_offers WHERE account_id = ? AND status = ? AND offer_activation_time < ? AND offer_expiry_time > ?");
            stmt.setInt(1, accountId);
            stmt.setString(2, OfferStatus.PENDING.toString());
            stmt.setTimestamp(3, Timestamp.valueOf(activeDate));
            stmt.setTimestamp(4, Timestamp.valueOf(activeDate));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int limitOfferId = rs.getInt("limit_offer_id");
                LimitType limitType = LimitType.valueOf(rs.getString("limit_type"));
                int newLimit = rs.getInt("new_limit");
                LocalDateTime offerActivationTime = rs.getTimestamp("offer_activation_time").toLocalDateTime();
                LocalDateTime offerExpiryTime = rs.getTimestamp("offer_expiry_time").toLocalDateTime();
                OfferStatus status = OfferStatus.valueOf(rs.getString("status"));
                LimitOffer offer = new LimitOffer(limitOfferId, accountId, limitType, newLimit, offerActivationTime, offerExpiryTime, status);
                activeOffers.add(offer);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return activeOffers;
    }

    public void acceptLimitOffer(int limitOfferId) {
        try {
            PreparedStatement getOfferStmt = connection.prepareStatement("SELECT * FROM limit_offers WHERE limit_offer_id = ?");
            getOfferStmt.setInt(1, limitOfferId);
            ResultSet offerRs = getOfferStmt.executeQuery();

            if (offerRs.next()) {
                int accountId = offerRs.getInt("account_id");
                LimitType limitType = LimitType.valueOf(offerRs.getString("limit_type"));
                int newLimit = offerRs.getInt("new_limit");

                // Get the account details
                Account account = getAccount(accountId);
                printAccountDetails(account);
                if (account == null) {
                    System.out.println("Account not found.");
                    return;
                }

                // Update the account's limit values and limit update times
                if (limitType == LimitType.ACCOUNT) {
                    account.setLastAccountLimit(account.getAccountLimit());
                    account.setAccountLimit(newLimit);
                    account.setAccountLimitUpdateTime(LocalDateTime.now());
                } else if (limitType == LimitType.PER_TRANSACTION) {
                    account.setLastPerTransactionLimit(account.getPerTransactionLimit());
                    account.setPerTransactionLimit(newLimit);
                    account.setPerTransactionLimitUpdateTime(LocalDateTime.now());
                }

                // Update the limit offer status to ACCEPTED in the database
                PreparedStatement updateOfferStmt = connection.prepareStatement("UPDATE limit_offers SET status = ? WHERE limit_offer_id = ?");
                updateOfferStmt.setString(1, OfferStatus.ACCEPTED.toString());
                updateOfferStmt.setInt(2, limitOfferId);
                updateOfferStmt.executeUpdate();

                PreparedStatement updateAccountStmt;
                if (limitType == LimitType.ACCOUNT) {
                    updateAccountStmt = connection.prepareStatement("UPDATE accounts SET account_limit = ?, last_account_limit = ?, account_limit_update_time = ? WHERE account_id = ?");
                    updateAccountStmt.setInt(1, account.getAccountLimit());
                    updateAccountStmt.setInt(2, account.getLastAccountLimit());
                    updateAccountStmt.setTimestamp(3, Timestamp.valueOf(account.getAccountLimitUpdateTime()));
                    updateAccountStmt.setInt(4, accountId);
                } else {
                    updateAccountStmt = connection.prepareStatement("UPDATE accounts SET per_transaction_limit = ?, last_per_transaction_limit = ?, per_transaction_limit_update_time = ? WHERE account_id = ?");
                    updateAccountStmt.setInt(1, account.getPerTransactionLimit());
                    updateAccountStmt.setInt(2, account.getLastPerTransactionLimit());
                    updateAccountStmt.setTimestamp(3, Timestamp.valueOf(account.getPerTransactionLimitUpdateTime()));
                    updateAccountStmt.setInt(4, accountId);
                }
                updateAccountStmt.executeUpdate();

                System.out.println("Limit offer with ID " + limitOfferId + " has been accepted.");
                printAccountDetails(account);
            } else {
                System.out.println("Limit offer not found.");
            }

            offerRs.close();
            getOfferStmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void rejectLimitOffer(int limitOfferId) {
        try {
            // Update the limit offer status to REJECTED in the database
            PreparedStatement updateOfferStmt = connection.prepareStatement("UPDATE limit_offers SET status = ? WHERE limit_offer_id = ?");
            updateOfferStmt.setString(1, OfferStatus.REJECTED.toString());
            updateOfferStmt.setInt(2, limitOfferId);
            updateOfferStmt.executeUpdate();

            System.out.println("Limit offer with ID " + limitOfferId + " has been rejected.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void printAccountDetails(Account account) {
        if (account == null) {
            System.out.println("Account not found.");
            return;
        }
        System.out.println("-----------Account details----------------");
        System.out.println("Account ID: " + account.getAccountId());
        System.out.println("Customer ID: " + account.getCustomerId());
        System.out.println("Account Limit: " + account.getAccountLimit());
        System.out.println("Per Transaction Limit: " + account.getPerTransactionLimit());
        System.out.println("Last Account Limit: " + account.getLastAccountLimit());
        System.out.println("Last Per Transaction Limit: " + account.getLastPerTransactionLimit());
        System.out.println("Account Limit Update Time: " + account.getAccountLimitUpdateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("Per Transaction Limit Update Time: " + account.getPerTransactionLimitUpdateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
