package com.rdc.order.service;

import com.rdc.order.entity.Order;
import com.rdc.order.util.IndianNumberToWords;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceGeneratorService {

    private final InvoicePdfService invoicePdfService;

    /**
     * Orchestrates the enrichment of order data and PDF generation.
     */
    public byte[] generateInvoicePdf(Order order) {
        enrichOrderForInvoice(order);
        return invoicePdfService.generatePdf(order);
    }

    /**
     * Calculates taxes and totals specifically for the GST Invoice view.
     * These values are persisted to the unique invoice columns in the database.
     */
    private void enrichOrderForInvoice(Order order) {
        // Calculate the base subtotal from items
        long subTotal = order.getItems().stream()
                .mapToLong(item -> item.getPriceCents() * item.getQuantity())
                .sum();

        // Standard GST Calculation: 9% CGST and 9% SGST
        long cgst = Math.round(subTotal * 0.09);
        long sgst = Math.round(subTotal * 0.09);
        long grandTotal = subTotal + cgst + sgst;

        // Set values to the order entity for Thymeleaf to access
        order.setSubTotalCents(subTotal);
        order.setCgstCents(cgst);
        order.setSgstCents(sgst);
        order.setTotalAmountCents(grandTotal);

        // ✅ FIXED: Convert grand total (in Rupees) to professional English words
        long grandTotalInRupees = grandTotal / 100;
        String words = IndianNumberToWords.convert(grandTotalInRupees);
        order.setAmountInWords(words);
    }
}