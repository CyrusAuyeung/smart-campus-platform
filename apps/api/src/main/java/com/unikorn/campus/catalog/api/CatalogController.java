package com.unikorn.campus.catalog.api;

import com.unikorn.campus.catalog.domain.CatalogService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/bootstrap")
    public Map<String, List<?>> bootstrap() {
        return Map.of(
                "users", catalogService.listUsers(),
                "academicSpaces", catalogService.listAcademicSpaces(),
                "sportFacilities", catalogService.listSportFacilities(),
                "sportUnits", catalogService.listSportUnits());
    }
}
