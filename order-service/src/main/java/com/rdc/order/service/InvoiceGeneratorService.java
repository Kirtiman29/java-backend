package com.rdc.order.service;

import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.util.IndianNumberToWords;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceGeneratorService {

    private final InvoicePdfService invoicePdfService;

    /**
     * ✅ FIXED: Now accepts 2 arguments (Order + Items List)
     * This prevents the "Expected 1 argument but found 2" compiler error
     * and avoids Hibernate LazyInitialization/Orphan errors.
     */
    public byte[] generateInvoicePdf(Order order, List<OrderItem> items) {
        log.info("🛠️ Enriching Order #{} with Tax calculations", order.getId());

        enrichOrderForInvoice(order);

        // Pass both the order (for header info) and items (for the table) to the PDF service
        return invoicePdfService.generatePdf(order, items);
    }

    /**
     * ✅ Inclusive GST Calculation Logic
     * Extracts Taxable Value, CGST, and SGST from the already inclusive total.
     */
    private void enrichOrderForInvoice(Order order) {
        // Grand Total is already inclusive (e.g., 10000 cents)
        long grandTotal = order.getTotalPriceCents();

        // 1. Calculate Taxable Value (Base = Total / 1.18)
        // We use double to maintain precision during the division
        long subTotal = Math.round(grandTotal / 1.18);

        // 2. Calculate GST Components
        long gstTotal = grandTotal - subTotal;
        long cgst = gstTotal / 2;
        long sgst = gstTotal - cgst; // Balance ensures no rounding cents are lost

        // 3. Set transient fields for Thymeleaf rendering
        order.setSubTotalCents(subTotal);
        order.setCgstCents(cgst);
        order.setSgstCents(sgst);
        order.setTotalAmountCents(grandTotal);

        // 4. Convert to Rupees for Words (e.g., 10000 cents -> 100 Rupees)
        long rupees = grandTotal / 100;
        order.setAmountInWords(IndianNumberToWords.convert(rupees));

        log.info("✅ Tax enriched: Taxable={}, CGST={}, SGST={}, Total={}", subTotal, cgst, sgst, grandTotal);
    }
}