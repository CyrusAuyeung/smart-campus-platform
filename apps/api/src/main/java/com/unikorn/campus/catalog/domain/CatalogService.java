package com.unikorn.campus.catalog.domain;

import com.unikorn.campus.catalog.api.ResourceItem;
import com.unikorn.campus.catalog.api.SportUnitItem;
import com.unikorn.campus.catalog.api.UserItem;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<UserItem> listUsers() {
        return catalogRepository.findUsers();
    }

    public List<ResourceItem> listAcademicSpaces() {
        return catalogRepository.findAcademicSpaces();
    }

    public List<ResourceItem> listSportFacilities() {
        return catalogRepository.findSportFacilities();
    }

    public List<SportUnitItem> listSportUnits() {
        return catalogRepository.findSportUnits();
    }
}
