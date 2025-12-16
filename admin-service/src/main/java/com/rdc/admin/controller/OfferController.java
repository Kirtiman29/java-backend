package com.rdc.admin.controller;

import com.rdc.admin.dto.OfferCreateRequest;
import com.rdc.admin.dto.OfferDto;
import com.rdc.admin.dto.OfferUpdateRequest;
import com.rdc.admin.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    public ResponseEntity<OfferDto> createOffer(@Valid @RequestBody OfferCreateRequest request) {
        OfferDto newOffer = offerService.createOffer(request);
        return new ResponseEntity<>(newOffer, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OfferDto>> getAllOffers() {
        List<OfferDto> offers = offerService.findAll();
        return ResponseEntity.ok(offers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferDto> getOfferById(@PathVariable Long id) {
        OfferDto offer = offerService.getOfferById(id);
        return ResponseEntity.ok(offer);
    }

    @PutMapping("/{id}") // Using PUT for full replacement/update
    public ResponseEntity<OfferDto> updateOffer(@PathVariable Long id, @Valid @RequestBody OfferUpdateRequest request) {
        OfferDto updatedOffer = offerService.updateOffer(id, request);
        return ResponseEntity.ok(updatedOffer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
}