package com.rdc.admin.util;

import com.rdc.admin.dto.DesignCreateRequest;
import com.rdc.admin.dto.FabricCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CsvParserUtil {

    private static final Set<String> DESIGN_TYPES = Set.of("DIGITAL", "ROTARY", "BOTH");
    private static final Set<String> IMAGE_TYPES = Set.of("VECTOR", "BITMAP");

    public static List<DesignCreateRequest> parse(InputStream inputStream) throws Exception {
        List<DesignCreateRequest> requests = new ArrayList<>();
        try (CSVParser csvParser = createParser(inputStream)) {

            for (CSVRecord row : csvParser) {
                try {
                    DesignCreateRequest req = new DesignCreateRequest();

                    // 1. Identifier (Checks "designIdentifier" then "ID")
                    req.setDesignIdentifier(getSafe(row, "designIdentifier", "ID"));

                    // 2. Text Fields
                    req.setTitle(getSafe(row, "title", "Title"));
                    req.setDescription(getSafe(row, "description", "Description"));

                    // 3. Numbers
                    req.setBasePriceCents(parseLong(getSafe(row, "basePriceCents", "Base Price (Cents)")));
                    req.setDiscountPercent(parseInteger(getSafe(row, "discountPercent", "Discount %")));

                    // 4. Lists (THE FIX FOR YOUR ERROR)
                    req.setTags(parseList(getSafe(row, "tags", "Tags")));
                    req.setSegments(parseList(getSafe(row, "segments", "Segments")));
                    
                    // Checks "categoryIds" then "Category ID"
                    String catIdVal = getSafe(row, "categoryIds", "Category ID");
                    req.setCategoryIds(parseList(catIdVal).stream()
                            .map(Long::valueOf)
                            .collect(Collectors.toList()));

                    // 5. Industrial Specs
                    req.setRepeatSize(getSafe(row, "repeatSize", "Repeat Size"));
                    String rawDesignType = getSafe(row, "designType", "Design Type");
                    String rawImageType = getSafe(row, "imageType", "Image Type");
                    String normalizedDesignType = normalizeValue(rawDesignType);
                    String normalizedImageType = normalizeValue(rawImageType);

                    // Accept swapped CSV columns gracefully when sheet values are reversed.
                    if (IMAGE_TYPES.contains(normalizedDesignType) && DESIGN_TYPES.contains(normalizedImageType)) {
                        String temp = normalizedDesignType;
                        normalizedDesignType = normalizedImageType;
                        normalizedImageType = temp;
                    }

                    req.setDesignType(normalizedDesignType);
                    req.setImageType(normalizedImageType);
                    req.setImageFormat(getSafe(row, "imageFormat", "Image Format"));
                    req.setColorCount(parseInteger(getSafe(row, "colorCount", "Color Count")));
                    req.setResolution(getSafe(row, "resolution", "Resolution"));

                    // 6. Booleans
                    req.setLuxury(Boolean.parseBoolean(getSafe(row, "luxury", "Luxury")));
                    req.setTrending(Boolean.parseBoolean(getSafe(row, "trending", "Trending")));
                    req.setEditorsPick(Boolean.parseBoolean(getSafe(row, "editorsPick", "Editors Pick")));
                    req.setNewArrival(Boolean.parseBoolean(getSafe(row, "newArrival", "New Arrival")));
                    req.setSpecialOffer(Boolean.parseBoolean(getSafe(row, "specialOffer", "Special Offer")));

                    requests.add(req);
                } catch (Exception e) {
                    log.error("Row {} skip error: {}", row.getRecordNumber(), e.getMessage());
                }
            }
        }
        return requests;
    }

    public static List<FabricCreateRequest> parseFabrics(InputStream inputStream) throws Exception {
        List<FabricCreateRequest> requests = new ArrayList<>();
        try (CSVParser csvParser = createParser(inputStream)) {
            for (CSVRecord row : csvParser) {
                try {
                    FabricCreateRequest req = new FabricCreateRequest();
                    req.setFabricIdentifier(getSafe(row, "fabricIdentifier", "ID"));
                    req.setTitle(getSafe(row, "title", "Title"));
                    req.setDescription(getSafe(row, "description", "Description"));
                    req.setPricePerMeter(parseDouble(getSafe(row, "pricePerMeter", "Price Per Meter")));
                    req.setPricePerSwatch(parseDouble(getSafe(row, "pricePerSwatch", "Price Per Swatch")));
                    req.setPricePerQuarter(parseDouble(getSafe(row, "pricePerQuarter", "Price Per Quarter")));
                    req.setPricePerYard(parseDouble(getSafe(row, "pricePerYard", "Price Per Yard")));
                    req.setStockMeters(parseDouble(getSafe(row, "stockMeters", "Stock Meters")));
                    req.setStockQuantity(parseIntegerOrNull(getSafe(row, "stockQuantity", "Stock Quantity")));
                    req.setMaterial(getSafe(row, "material", "Material"));
                    req.setWidth(parseDouble(getSafe(row, "width", "Width")));
                    req.setGsm(parseIntegerOrNull(getSafe(row, "gsm", "GSM")));
                    req.setLength(getSafe(row, "length", "Length"));
                    req.setCategoryId(parseLongOrNull(getSafe(row, "categoryId", "Category ID")));
                    req.setDiscountPercent(parseIntegerOrNull(getSafe(row, "discountPercent", "Discount %")));

                    String specialOfferValue = getSafe(row, "specialOffer", "Special Offer");
                    if (!specialOfferValue.isBlank()) {
                        req.setSpecialOffer(Boolean.parseBoolean(specialOfferValue));
                    }

                    String activeValue = getSafe(row, "active", "Active");
                    if (!activeValue.isBlank()) {
                        req.setActive(Boolean.parseBoolean(activeValue));
                    }

                    requests.add(req);
                } catch (Exception e) {
                    log.error("Fabric row {} skip error: {}", row.getRecordNumber(), e.getMessage());
                }
            }
        }
        return requests;
    }

    private static CSVParser createParser(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader);
    }

    private static List<String> parseList(String val) {
        if (val == null || val.isBlank()) return new ArrayList<>();
        return Arrays.stream(val.split(",")).map(String::trim).collect(Collectors.toList());
    }

    private static Long parseLong(String val) {
        return (val == null || val.isBlank()) ? 0L : Long.parseLong(val.replaceAll("[^\\d]", ""));
    }

    private static Integer parseInteger(String val) {
        return (val == null || val.isBlank()) ? 0 : Integer.parseInt(val.replaceAll("[^\\d]", ""));
    }

    private static Integer parseIntegerOrNull(String val) {
        return (val == null || val.isBlank()) ? null : Integer.parseInt(val.replaceAll("[^\\d]", ""));
    }

    private static Long parseLongOrNull(String val) {
        return (val == null || val.isBlank()) ? null : Long.parseLong(val.replaceAll("[^\\d]", ""));
    }

    private static Double parseDouble(String val) {
        if (val == null || val.isBlank()) return null;
        String cleaned = val.replaceAll("[^\\d.]", "");
        return cleaned.isBlank() ? null : Double.parseDouble(cleaned);
    }

    private static String normalizeValue(String val) {
        if (val == null) return null;
        String cleaned = val.trim();
        if (cleaned.isBlank()) return null;
        return cleaned.toUpperCase();
    }

    private static String getSafe(CSVRecord row, String key1, String key2) {
        if (row.isMapped(key1)) return row.get(key1);
        if (row.isMapped(key2)) return row.get(key2);
        return "";
    }
}
