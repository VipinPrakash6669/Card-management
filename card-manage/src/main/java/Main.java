import configuration.DbConfiguration;
import enums.LimitType;
import enums.OfferStatus;
import pojo.Account;
import pojo.LimitOffer;
import service.AccountService;

import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static DbConfiguration dbConfiguration = new DbConfiguration();

    public static void main(String[] args) {
        // Set up the database connection details

        AccountService accountService = new AccountService(dbConfiguration.url, dbConfiguration.username, dbConfiguration.password);

        // Create an account for limit offer generation
        Account account1 = accountService.createAccount(1001, 5000, 1000);

        Account account2 = accountService.createAccount(1002, 6800, 2000);

        // Create a limit offer for the account
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime offerActivationTime = now.minusDays(2);
        LocalDateTime offerExpiryTime = now.plusDays(15);
        accountService.createLimitOffer(account1.getAccountId(), LimitType.ACCOUNT, 7000, offerActivationTime, offerExpiryTime);
        accountService.createLimitOffer(account2.getAccountId(), LimitType.ACCOUNT, 9000, offerActivationTime, offerExpiryTime);

        // Create a limit offer for the account with limit type PER_TRANSACTION
        accountService.createLimitOffer(account2.getAccountId(), LimitType.PER_TRANSACTION, 1500, offerActivationTime, offerExpiryTime);

        accountService.createLimitOffer(account2.getAccountId(), LimitType.PER_TRANSACTION, 2500, offerActivationTime, offerExpiryTime);

        // Fetch active offers for the account ID and active date
        LocalDateTime activeDate = now;
        List<LimitOffer> activeOffers = accountService.getActiveLimitOffers(account1.getAccountId(), activeDate);
        List<LimitOffer> activeOffers1 = accountService.getActiveLimitOffers(account2.getAccountId(), activeDate);

        System.out.println("Active Limit Offers:");
        for (LimitOffer activeOffer : activeOffers) {
            System.out.println("Limit Offer ID: " + activeOffer.getLimitOfferId());
            System.out.println("Account ID: " + activeOffer.getAccountId());
            System.out.println("Limit Type: " + activeOffer.getLimitType());
            System.out.println("New Limit: " + activeOffer.getNewLimit());
            System.out.println("Offer Activation Time: " + activeOffer.getOfferActivationTime());
            System.out.println("Offer Expiry Time: " + activeOffer.getOfferExpiryTime());
        }

        // Update the status of the first active offer to ACCEPTED
        if (!activeOffers.isEmpty()) {
            LimitOffer firstOffer = activeOffers.get(0);
            accountService.acceptLimitOffer(firstOffer.getLimitOfferId());
        }
        System.out.println("--------------------------------------");
        //offer rejected case for second offer
        if (!activeOffers1.isEmpty()) {
            LimitOffer firstOffer = activeOffers1.get(0);
            accountService.acceptLimitOffer(firstOffer.getLimitOfferId());
//            accountService.rejectLimitOffer(firstOffer.getLimitOfferId());
        }
        // Fetch and print account details for a given account id
        int accountIdToFetch = account1.getAccountId();
        Account fetchedAccount = accountService.getAccount(accountIdToFetch);
        accountService.printAccountDetails(fetchedAccount);
    }
}
