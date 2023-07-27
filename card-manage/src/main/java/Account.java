import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class Account {
    private int accountId;
    private int customerId;
    private int accountLimit;
    private int perTransactionLimit;
    private int lastAccountLimit;
    private int lastPerTransactionLimit;
    private LocalDateTime accountLimitUpdateTime;
    private LocalDateTime perTransactionLimitUpdateTime;

    public Account(int accountId, int customerId, int accountLimit, int perTransactionLimit) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.accountLimit = accountLimit;
        this.perTransactionLimit = perTransactionLimit;
        this.lastAccountLimit = accountLimit;
        this.lastPerTransactionLimit = perTransactionLimit;
        this.accountLimitUpdateTime = LocalDateTime.now();
        this.perTransactionLimitUpdateTime = LocalDateTime.now();
    }

    // Getters and setters (omitted for brevity)
}
