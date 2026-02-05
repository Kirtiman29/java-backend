package com.rdc.order.service;

import com.rdc.order.entity.Order;
import com.rdc.order.util.IndianNumberToWords;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceGeneratorService {

    private final InvoicePdfService invoicePdfService;

    public byte[] generateInvoicePdf(Order order) {
        enrichOrderForInvoice(order);
        return invoicePdfService.generatePdf(order);
    }

    /**
     * ✅ UPDATED: Inclusive GST Calculation
     * Extracts base price and tax from the final total paid.
     */
    private void enrichOrderForInvoice(Order order) {
        // The total price already includes GST (e.g., 705000 cents)
        long grandTotal = order.getTotalPriceCents();

        // 1. Calculate Base Price (Sub-total) = Total / 1.18
        long subTotal = Math.round(grandTotal / 1.18);

        // 2. Calculate CGST & SGST (9% each of base)
        long totalGst = grandTotal - subTotal;
        long cgst = totalGst / 2;
        long sgst = totalGst - cgst; // Balance to ensure precision

        // 3. Set values for the PDF template [cite: 165, 166]
        order.setSubTotalCents(subTotal);
        order.setCgstCents(cgst);
        order.setSgstCents(sgst);
        order.setTotalAmountCents(grandTotal);

        // 4. Convert to words [cite: 166, 167]
        long grandTotalInRupees = grandTotal / 100;
        String words = IndianNumberToWords.convert(grandTotalInRupees);
        order.setAmountInWords(words);
    }
}