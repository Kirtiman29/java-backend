package com.rdc.admin.util;

import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;
import org.springframework.stereotype.Component;

@Component
public class DesignMapper {

    public DesignResponse toResponse(Design design) {
        if (design == null) return null;

        DesignResponse response = new DesignResponse();
        response.setId(design.getId());
        response.setTitle(design.getTitle());
        response.setSlug(design.getSlug());
        response.setDescription(design.getDescription());

        // FIX: Using new pricing field
        response.setFinalPriceCents(design.getFinalPriceCents());
        response.setBasePriceCents(design.getBasePriceCents());

        // FIX: Mapping new status flags
        response.setActive(design.getActive());
        response.setDraft(design.getDraft());

        // FIX: Mapping new section flags
        response.setTrending(design.getTrending());
        response.setEditorsPick(design.getEditorsPick());
        response.setNewArrival(design.getNewArrival());

        response.setTags(design.getTags());
        response.setCreatedAt(design.getCreatedAt());
        response.setUpdatedAt(design.getUpdatedAt());

        return response;
    }
}