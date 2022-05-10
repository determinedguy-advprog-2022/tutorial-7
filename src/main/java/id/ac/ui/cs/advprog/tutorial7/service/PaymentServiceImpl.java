package id.ac.ui.cs.advprog.tutorial7.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import id.ac.ui.cs.advprog.tutorial7.core.bankapi.BankApi;
import id.ac.ui.cs.advprog.tutorial7.core.miscapi.HolidayApi;
import id.ac.ui.cs.advprog.tutorial7.core.vaapi.VAHelper;
import id.ac.ui.cs.advprog.tutorial7.model.PaymentResponse;

@Service
public class PaymentServiceImpl implements PaymentService{

    HolidayApi holidayApi = new HolidayApi();
    VAHelper vaHelper = new VAHelper();

    @Override
    public String createNewVA(int vaAmount, String bank) {
        
        String va = vaHelper.createNewVA(vaAmount, bank);
        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            return null;
        }
    
        return va;
    }

    @Override
    public PaymentResponse pay(String va, int payAmount, String day, String time) {
        AtomicInteger vaAmount = new AtomicInteger();
        AtomicReference<BankApi> bankApi = new AtomicReference<>();

        PaymentResponse successStatus = new PaymentResponse(1, "");

        CompletableFuture<PaymentResponse> holidayStatus = CompletableFuture.supplyAsync(() -> {
                if (holidayApi.isHoliday(day)) {
                    return new PaymentResponse(0, "Cannot pay on holidays");
                }
                return successStatus;
            }
        );

        CompletableFuture<PaymentResponse> vaStatus = CompletableFuture.supplyAsync(() -> {
                try {
                    vaAmount.set(vaHelper.getVAAmount(va));
                    bankApi.set(vaHelper.getBankByVA(va));
                } catch (NoSuchElementException e) {
                    return new PaymentResponse(0, "VA number not found");
                }
                return successStatus;
            }
        );

        CompletableFuture<PaymentResponse> bankStatus = CompletableFuture.supplyAsync(() -> {
                if (bankApi.get().isBankClosed(time, vaAmount.get())) {
                    return new PaymentResponse(0, "Bank already closed, please try again tomorrow");
                }
                return successStatus;
            }
        );

        CompletableFuture<PaymentResponse> paymentValidation = CompletableFuture.supplyAsync(() -> {
            String errorMsg = vaHelper.validatePayment(va, vaAmount.get(), payAmount);
                if (!errorMsg.equals("")) {
                    return new PaymentResponse(0, errorMsg);
                }
                return successStatus;
            }
        );

        CompletableFuture<PaymentResponse> paymentStatus = CompletableFuture.supplyAsync(() -> {
                boolean paymentSuccessful = bankApi.get().pay(payAmount);
                vaHelper.logVAPayment(va, paymentSuccessful);
                if(!paymentSuccessful) {
                    return new PaymentResponse(0, "Payment unsuccessful, please try again");
                }
                return new PaymentResponse(1, "Payment successful");
            }
        );

        // https://stackoverflow.com/a/67445305
        CompletableFuture<Void> combinedFuture =
                CompletableFuture.allOf(holidayStatus, vaStatus, bankStatus, paymentValidation, paymentStatus);

        combinedFuture.join();

        List<PaymentResponse> answers = Stream.of(holidayStatus, vaStatus, bankStatus, paymentValidation, paymentStatus)
                .map(CompletableFuture::join).toList();

        if (answers.get(0).ok == 0) {
            return answers.get(0);
        } else if (answers.get(1).ok == 0) {
            return answers.get(1);
        } else if (answers.get(2).ok == 0) {
            return answers.get(2);
        } else if (answers.get(3).ok == 0) {
            return answers.get(3);
        }
        return answers.get(4);

        // Check if it is holiday
        // if(holidayApi.isHoliday(day)) return new PaymentResponse(0, "Cannot pay on holidays");

        // Validate VA number
//        int vaAmount;
//        BankApi bankApi;
//
//        try {
//            vaAmount = vaHelper.getVAAmount(va);
//            bankApi = vaHelper.getBankByVA(va);
//        } catch(NoSuchElementException e) {
//            return new PaymentResponse(0, "VA number not found");
//        }

        // Check if bank is open or closed
        // if(bankApi.isBankClosed(time, vaAmount)) return new PaymentResponse(0, "Bank already closed, please try again tomorrow");

        // Validate payment
//        String errorMsg = vaHelper.validatePayment(va, vaAmount.get(), payAmount);
//        if(!errorMsg.equals("")) return new PaymentResponse(0, errorMsg);

        // Check if payment is valid
//        boolean paymentSuccessful = bankApi.get().pay(payAmount);
//        vaHelper.logVAPayment(va, paymentSuccessful);
//        if(!paymentSuccessful) return new PaymentResponse(0, "Payment unsuccessful, please try again");
//        else return new PaymentResponse(1, "Payment successful");

    }
    
}
