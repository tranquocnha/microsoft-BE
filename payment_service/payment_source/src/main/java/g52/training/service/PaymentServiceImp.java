package g52.training.service;

import com.fpt.g52.common_service.notification.model.shared.payload.rabbitmq.OrderPayload;
import g52.training.dto.createpay.CreatePayReqDto;
import g52.training.dto.createpay.CreatePayResponseDto;
import g52.training.dto.deposit.DepositReqDto;
import g52.training.dto.history.History;
import g52.training.dto.history.HistoryResponseDto;
import g52.training.dto.registbooking.RegistBookingReqDto;
import g52.training.dto.registbooking.RegistBookingResponseDto;
import g52.training.dto.viewamount.ViewAmountResponseDto;
import g52.training.entity.AccountEntity;
import g52.training.entity.PaymentHistoryEntity;
import g52.training.mapper.NotifyPayloadMapper;
import g52.training.mapper.PaymentHistoryMapper;
import g52.training.repository.AccountJpaRepository;
import g52.training.repository.PaymentHistoryJpaRepository;
import g52.training.valueobject.PaymentStatus;
import g52.training.valueobject.PaymentsHistoryOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImp {

    private final PaymentHistoryJpaRepository paymentHistoryJpaRepository;
    private final AccountJpaRepository accountJpaRepository;
    private final NotifyMQSender notifyMQSender;

    @Value("${booking.host}")
    private String bookingHost;
    private final String NOTIFY_PAID_API = "/api/bookings/{id}/paycomplete";

    public boolean registPayment(String userName) {
        Optional<AccountEntity> optionalAccountEntity = accountJpaRepository.findAccountEntityByAccount(userName);
        if (optionalAccountEntity.isEmpty()) {
            AccountEntity accountEntity = new AccountEntity();
            accountEntity.setAccount(userName);
            accountEntity.setAmount(BigDecimal.ZERO);
            accountEntity.setCreatedAt(ZonedDateTime.now());
            accountEntity.setUpdatedAt(accountEntity.getCreatedAt());
            accountJpaRepository.save(accountEntity);
            return true;
        }
        return false;
    }

    public CreatePayResponseDto makePayment(final String account, CreatePayReqDto createPayDto) {
        Optional<AccountEntity> optionalAccountEntity = accountJpaRepository.findAccountEntityByAccount(account);
        CreatePayResponseDto responseDto = new CreatePayResponseDto();
        responseDto.setBookingId(createPayDto.getBookingId());
        responseDto.setPrice(createPayDto.getPrice());
        responseDto.setCreatedAt(ZonedDateTime.now().toEpochSecond());
        if (optionalAccountEntity.isEmpty()) {
            throw new IllegalArgumentException("account is not exist or not regist payment!");
        } else if (optionalAccountEntity.get().getAmount().compareTo(createPayDto.getPrice()) < 1) {
            responseDto.setStatus(PaymentStatus.FAILED);
            responseDto.setMessage("Amount of account is not enough for payment please deposit!");
        } else {
            responseDto.setStatus(PaymentStatus.SUCCESS);
            responseDto.setMessage("Pay successfully!");
        }
        if (PaymentStatus.SUCCESS.equals(responseDto.getStatus())){
            BigDecimal newAmount = optionalAccountEntity.get().getAmount().subtract(createPayDto.getPrice());
            optionalAccountEntity.get().setAmount(newAmount);
            accountJpaRepository.save(optionalAccountEntity.get());
        }
        paymentHistoryJpaRepository.save(PaymentHistoryMapper.convertCreatePayResponseDto(createPayDto, responseDto, optionalAccountEntity.get(), PaymentsHistoryOperator.PAY));

        String bookingNotify = bookingHost + NOTIFY_PAID_API.replace("{id}", createPayDto.getBookingId());
        System.out.println("Send notice to bookingNotify " + bookingNotify);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(bookingNotify, null);
        System.out.println("End send notice to bookingNotify");

        return responseDto;
    }

    public void deposit(String account, DepositReqDto reqDto) {
        Optional<AccountEntity> optionalAccountEntity = accountJpaRepository.findAccountEntityByAccount(account);

        CreatePayReqDto createPayReqDto = new CreatePayReqDto();
        createPayReqDto.setPrice(reqDto.getPrice());

        CreatePayResponseDto payResponseDto = new CreatePayResponseDto();
        payResponseDto.setCreatedAt(ZonedDateTime.now().toEpochSecond());
        if (optionalAccountEntity.isEmpty()) {
            throw new IllegalArgumentException("account is not exist or not created!");
        } else if (reqDto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price can not negative!");
        } else {
            payResponseDto.setStatus(PaymentStatus.SUCCESS);
            payResponseDto.setMessage("Deposit successfully!");
        }
        payResponseDto.setPrice(reqDto.getPrice());
        BigDecimal newAmount = optionalAccountEntity.get().getAmount().add(reqDto.getPrice());
        optionalAccountEntity.get().setAmount(newAmount);
        accountJpaRepository.save(optionalAccountEntity.get());
        paymentHistoryJpaRepository.save(PaymentHistoryMapper.convertCreatePayResponseDto(createPayReqDto, payResponseDto, optionalAccountEntity.get(), PaymentsHistoryOperator.DEPOSIT));
    }

    public HistoryResponseDto getHistory(String account) {
        Optional<AccountEntity> optionalAccountEntity = accountJpaRepository.findAccountEntityByAccount(account);
        if (optionalAccountEntity.isEmpty()){
            throw new IllegalArgumentException("account is not exist or not created!");
        }
        List<History> list =
                paymentHistoryJpaRepository.findAllByAccount(account).stream().map(PaymentHistoryMapper::convertCreatePayResponseDto).collect(Collectors.toList());
        HistoryResponseDto historyResponseDto = new HistoryResponseDto();
        historyResponseDto.setHistories(list);

        ViewAmountResponseDto viewAmountResponseDto = viewAmount(account);
        historyResponseDto.setViewAccount(viewAmountResponseDto);
        return historyResponseDto;
    }

    public ViewAmountResponseDto viewAmount(String account) {
        ViewAmountResponseDto viewAmountResponseDto = new ViewAmountResponseDto();
        Optional<AccountEntity> optionalAccountEntity = accountJpaRepository.findAccountEntityByAccount(account);
        if (optionalAccountEntity.isEmpty()){
            throw new IllegalArgumentException("account is not exist or not created!");
        }
        viewAmountResponseDto.setAccount(optionalAccountEntity.get().getAccount());
        viewAmountResponseDto.setAmount(optionalAccountEntity.get().getAmount());
        return viewAmountResponseDto;
    }

    public RegistBookingResponseDto registBooking(String account, RegistBookingReqDto reqDto) {
        Optional<AccountEntity> optionalAccountEntity = accountJpaRepository.findAccountEntityByAccount(account);
        if (optionalAccountEntity.isEmpty()){
            throw new IllegalArgumentException("account is not exist or not created!");
        }
        Optional<PaymentHistoryEntity> optionalPaymentHistoryEntity =
                paymentHistoryJpaRepository.findPaymentHistoryEntityByBookingId(reqDto.getBookingId());
        PaymentHistoryEntity paymentHistoryEntity = null;
        if (optionalPaymentHistoryEntity.isEmpty()) {
            paymentHistoryEntity = new PaymentHistoryEntity();
            paymentHistoryEntity.setAccount(account);
            paymentHistoryEntity.setBookingId(reqDto.getBookingId());
            paymentHistoryEntity.setPrice(reqDto.getPrice());
            paymentHistoryEntity.setStatus(PaymentStatus.WAIT);
            paymentHistoryEntity.setOperator(PaymentsHistoryOperator.PAY);
            paymentHistoryJpaRepository.save(paymentHistoryEntity);
        } else {
            paymentHistoryEntity = optionalPaymentHistoryEntity.get();
        }

        RegistBookingResponseDto res = new RegistBookingResponseDto(
                paymentHistoryEntity.getBookingId(),
                paymentHistoryEntity.getPrice(),
                paymentHistoryEntity.getStatus(),
                paymentHistoryEntity.getAccount(),
                optionalAccountEntity.get().getAmount()
        );

        return res;
    }

    public void payBooking(final String account, CreatePayReqDto createPayDto) {
        Optional<PaymentHistoryEntity> paymentHistoryEntityOptional =
                paymentHistoryJpaRepository.findPaymentHistoryEntityByBookingId(createPayDto.getBookingId());
        Optional<AccountEntity> optionalAccountEntity = accountJpaRepository.findAccountEntityByAccount(account);
        if (paymentHistoryEntityOptional.isPresent()){
            PaymentHistoryEntity entity = paymentHistoryEntityOptional.get();
            if (PaymentStatus.WAIT.equals(entity.getStatus()) || PaymentStatus.FAILED.equals(entity.getStatus())) {
                if (optionalAccountEntity.get().getAmount().compareTo(createPayDto.getPrice()) < 1) {
                    entity.setStatus(PaymentStatus.FAILED);
                    entity.setMessage("Amount of account is not enough for payment please deposit!");
                    paymentHistoryJpaRepository.save(entity);
                }else {
                    entity.setStatus(PaymentStatus.SUCCESS);
                    entity.setMessage("Booking payed successfully!");
                    paymentHistoryJpaRepository.save(entity);
                    BigDecimal newAmount = optionalAccountEntity.get().getAmount().subtract(createPayDto.getPrice());
                    optionalAccountEntity.get().setAmount(newAmount);
                    accountJpaRepository.save(optionalAccountEntity.get());
                }
                sendNotify(entity);

            } else {
                throw new IllegalArgumentException("BookingID has status = " + entity.getStatus() + " cant pay, please create new booking");
            }
        } else {
            throw new IllegalArgumentException("BookingID not exist ");
        }
    }

    public void cancelBooking(final String account, CreatePayReqDto createPayDto) {
        Optional<PaymentHistoryEntity> paymentHistoryEntityOptional =
                paymentHistoryJpaRepository.findPaymentHistoryEntityByBookingId(createPayDto.getBookingId());
        if (paymentHistoryEntityOptional.isPresent()){
            PaymentHistoryEntity entity = paymentHistoryEntityOptional.get();
            if (!entity.getAccount().equals(account)) {
                throw new IllegalArgumentException("BookingID not exist for this account");
            }
            if (PaymentStatus.WAIT.equals(entity.getStatus())) {
                entity.setStatus(PaymentStatus.CANCELED);
                entity.setMessage("Booking canceled!");
                paymentHistoryJpaRepository.save(entity);

                sendNotify(entity);
            } else {
                throw new IllegalArgumentException("BookingID has status = " + entity.getStatus() + " cant pay, please create new booking");
            }
        } else {
            throw new IllegalArgumentException("BookingID not exist ");
        }
    }

    private void sendNotify(PaymentHistoryEntity entity) {
        try{
            System.out.println("Send to MQ");
            OrderPayload orderPayload = NotifyPayloadMapper.convertToOrderPayload(entity);
            System.out.println("Payload : " + orderPayload);
            notifyMQSender.send(orderPayload);
            System.out.println("Send to MQ Success");
        }catch (Exception ex) {
            System.out.println("Send notice to MQ error");
            System.out.println(ex);
        }

        try {
            String bookingNotify = bookingHost + NOTIFY_PAID_API.replace("{id}", entity.getBookingId());
            System.out.println("Send notice to bookingNotify " + bookingNotify);
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.put(bookingNotify, null);
            System.out.println("End send notice to bookingNotify");
        }catch (Exception ex){
            System.out.println("Send notice to bookingNotify error");
            System.out.println(ex);
        }
    }
}
