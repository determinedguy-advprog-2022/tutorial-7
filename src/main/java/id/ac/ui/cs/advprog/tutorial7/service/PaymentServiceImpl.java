package id.ac.ui.cs.advprog.tutorial7.service;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import id.ac.ui.cs.advprog.tutorial7.core.bankapi.BankApi;
import id.ac.ui.cs.advprog.tutorial7.core.miscapi.HolidayApi;
import id.ac.ui.cs.advprog.tutorial7.core.vaapi.VAHelper;
import id.ac.ui.cs.advprog.tutorial7.core.vaapi.VirtualAccount;
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
        
        if(holidayApi.isHoliday(day)) return new PaymentResponse(0, "Cannot pay on holidays");

        int vaAmount;
        BankApi bankApi;
        try {
            vaAmount = vaHelper.getVAAmount(va);
            bankApi = vaHelper.getBankByVA(va);
        } catch(NoSuchElementException e) {
            return new PaymentResponse(0, "VA number not found");
        }

        if(bankApi.isBankClosed(time, vaAmount)) return new PaymentResponse(0, "Bank already closed, please try again tomorrow");
        
        String errorMsg = vaHelper.validatePayment(va, vaAmount, payAmount);
        if(!errorMsg.equals("")) return new PaymentResponse(0, errorMsg);

        boolean paymentSuccessfull = bankApi.pay(payAmount);
        vaHelper.logVAPayment(va, paymentSuccessfull);
        if(!paymentSuccessfull) return new PaymentResponse(0, "Payment unsuccessfull, please try again");
        else return new PaymentResponse(1, "Payment successfull");

        

    }
    
}
