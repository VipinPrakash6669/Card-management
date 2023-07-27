import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

enum LimitType {
    ACCOUNT,
    PER_TRANSACTION
}

enum OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

@Getter
@Setter
class LimitOffer {
    private int limitOfferId;
    private int accountId;
    private LimitType limitType;
    private int newLimit;
    private LocalDateTime offerActivationTime;
    private LocalDateTime offerExpiryTime;
    private OfferStatus status;

    public LimitOffer(int limitOfferId, int accountId, LimitType limitType, int newLimit, LocalDateTime offerActivationTime, LocalDateTime offerExpiryTime, OfferStatus status) {
        this.limitOfferId = limitOfferId;
        this.accountId = accountId;
        this.limitType = limitType;
        this.newLimit = newLimit;
        this.offerActivationTime = offerActivationTime;
        this.offerExpiryTime = offerExpiryTime;
        this.status = status;
    }

    // Getters and setters (omitted for brevity)
}
