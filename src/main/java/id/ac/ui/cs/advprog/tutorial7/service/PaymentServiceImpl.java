package id.ac.ui.cs.advprog.tutorial7.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
        CompletableFuture<Boolean> isHolidayFuture = CompletableFuture.supplyAsync(() -> holidayApi.isHoliday(day));
        CompletableFuture<Integer> vaAmountFuture = CompletableFuture.supplyAsync(() -> vaHelper.getVAAmount(va));
        CompletableFuture<BankApi> bankFuture = CompletableFuture.supplyAsync(() -> vaHelper.getBankByVA(va));
        CompletableFuture<String> paymentValidateFuture;
        CompletableFuture<Boolean> bankClosedFuture;

        int vaAmount;
        BankApi bankApi;
        boolean paymentSuccessful = false;

        try {
            bankApi = bankFuture.get();
            bankClosedFuture = CompletableFuture.supplyAsync(() -> bankApi.isBankClosed(time, payAmount));
            vaAmount = vaAmountFuture.get();
            paymentValidateFuture = CompletableFuture.supplyAsync(() -> vaHelper.validatePayment(va, vaAmount, payAmount));

            if (isHolidayFuture.get()) {
                return new PaymentResponse(0, "Cannot pay on holidays");
            }

            if ((bankClosedFuture.get())) {
                return new PaymentResponse(0, "Bank already closed, please try again tomorrow");
            }

            String errorMsg = paymentValidateFuture.get();

            if (!errorMsg.equals("")) {
                return new PaymentResponse(0, errorMsg);
            }

            CompletableFuture<Boolean> bankPaymentFuture = CompletableFuture.supplyAsync(() -> bankApi.pay(payAmount));
            paymentSuccessful = bankPaymentFuture.get();

        } catch (NoSuchElementException e) {
            return new PaymentResponse(0, "VA number not found");
        } catch (InterruptedException | ExecutionException ignored){

        }

        boolean paymentSuccessfulFinal = paymentSuccessful;
        CompletableFuture.runAsync(() -> vaHelper.logVAPayment(va, paymentSuccessfulFinal));

        if (paymentSuccessful) {
            return new PaymentResponse(1, "Payment successful");
        }
        return new PaymentResponse(0, "Payment unsuccessful, please try again");
    }
    
}
