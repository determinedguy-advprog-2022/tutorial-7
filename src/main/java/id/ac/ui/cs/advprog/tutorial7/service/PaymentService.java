package id.ac.ui.cs.advprog.tutorial7.service;

import id.ac.ui.cs.advprog.tutorial7.model.PaymentResponse;

import java.util.concurrent.ExecutionException;

public interface PaymentService {
    String createNewVA(int vaAmount, String bank);
    PaymentResponse pay(String va, int payAmount, String day, String time);
}
