package com.rdc.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/offers")
public class OfferController {

    // private final OfferService offerService; // Inject service

    // POST /api/admin/offers — create offer
    @PostMapping
    public ResponseEntity<?> createOffer(@RequestBody Object request) {
        // return ResponseEntity.status(HttpStatus.CREATED).body(offerService.createOffer(request));
        return ResponseEntity.status(HttpStatus.CREATED).body("Offer created (placeholder)");
    }

    // GET /api/admin/offers
    @GetMapping
    public List<?> listOffers() {
        // return offerService.findAll();
        return List.of("Offer 1", "Offer 2");
    }

    // PUT /api/admin/offers/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOffer(@PathVariable Long id, @RequestBody Object request) {
        // return ResponseEntity.ok(offerService.updateOffer(id, request));
        return ResponseEntity.ok("Offer updated (placeholder)");
    }

    // DELETE /api/admin/offers/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOffer(@PathVariable Long id) {
        // offerService.delete(id);
    }
}