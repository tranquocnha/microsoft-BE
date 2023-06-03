package g52.training.controller;


import g52.training.dto.createpay.CreatePayReqDto;
import g52.training.dto.deposit.DepositReqDto;
import g52.training.dto.deposit.DepositResponseDto;
import g52.training.dto.history.HistoryResponseDto;
import g52.training.service.PaymentServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/payment")
@RequiredArgsConstructor
public class PayController {

    private final PaymentServiceImp paymentServiceImp;

    @PostMapping(value = "/pay-booking")
    public ResponseEntity<HistoryResponseDto> payBooking(@RequestBody CreatePayReqDto createPayDto) {
        paymentServiceImp.registPayment(SecurityContextHolder.getContext().getAuthentication().getName());
        paymentServiceImp.makePayment(SecurityContextHolder.getContext().getAuthentication().getName(), createPayDto);
        return ResponseEntity.ok(paymentServiceImp.getHistory(SecurityContextHolder.getContext().getAuthentication().getName()));
    }

    @PostMapping(value = "/deposit")
    public ResponseEntity<HistoryResponseDto> deposit(@RequestBody DepositReqDto depositReqDto) {
        paymentServiceImp.registPayment(SecurityContextHolder.getContext().getAuthentication().getName());
        paymentServiceImp.deposit(SecurityContextHolder.getContext().getAuthentication().getName(), depositReqDto);
        return ResponseEntity.ok(paymentServiceImp.getHistory(SecurityContextHolder.getContext().getAuthentication().getName()));
    }

    @PostMapping(value = "/regist-payment")
    public ResponseEntity<Boolean> registPayment() {
        return ResponseEntity.ok(paymentServiceImp.registPayment(SecurityContextHolder.getContext().getAuthentication().getName()));
    }


    @GetMapping(value = "/histories")
    public ResponseEntity<HistoryResponseDto> getHistory() {
        paymentServiceImp.registPayment(SecurityContextHolder.getContext().getAuthentication().getName());
        return ResponseEntity.ok(paymentServiceImp.getHistory(SecurityContextHolder.getContext().getAuthentication().getName()));
    }

    @ExceptionHandler
    protected ResponseEntity<String> handleEntityNotFound(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
