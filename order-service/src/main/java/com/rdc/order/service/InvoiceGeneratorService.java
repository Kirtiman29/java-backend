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

    public byte[] generateInvoicePdf(Order order, List<OrderItem> items) {
        log.info("🛠️ Enriching Order #{} with Tax calculations", order.getId());

        enrichOrderForInvoice(order);
        return invoicePdfService.generatePdf(order, items);
    }

    private void enrichOrderForInvoice(Order order) {
        long grandTotal = order.getTotalPriceCents();

        long subTotal = Math.round(grandTotal / 1.18);

        long gstTotal = grandTotal - subTotal;
        long cgst = gstTotal / 2;
        long sgst = gstTotal - cgst;

        order.setSubTotalCents(subTotal);
        order.setCgstCents(cgst);
        order.setSgstCents(sgst);
        order.setTotalAmountCents(grandTotal);

        long rupees = grandTotal / 100;
        order.setAmountInWords(IndianNumberToWords.convert(rupees));

        log.info("✅ Tax enriched: Taxable={}, CGST={}, SGST={}, Total={}", subTotal, cgst, sgst, grandTotal);
    }
}