package g52.training.event;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import g52.training.service.RabbitMQSender;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id", scope = CreateAccountPaymentSuccessEvent.class)
public class CreateAccountPaymentSuccessEvent implements FireEvent {

    private String requestId;
    private String userId;
    private String userName;
    private Long createdAt;
    private RabbitMQSender<CreateAccountPaymentSuccessEvent> createAccountPaymentSuccessSender;

    public CreateAccountPaymentSuccessEvent() {
    }

    public CreateAccountPaymentSuccessEvent(String requestId, String userId, String userName, Long createdAt, RabbitMQSender createAccountPaymentSuccessSender) {
        this.requestId = requestId;
        this.userId = userId;
        this.userName = userName;
        this.createdAt = createdAt;
        this.createAccountPaymentSuccessSender = createAccountPaymentSuccessSender;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public void fire() {
        createAccountPaymentSuccessSender.send(this);
    }
}
