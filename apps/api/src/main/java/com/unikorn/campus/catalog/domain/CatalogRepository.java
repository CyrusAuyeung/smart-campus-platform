package com.unikorn.campus.catalog.domain;

import com.unikorn.campus.catalog.api.ResourceItem;
import com.unikorn.campus.catalog.api.SportUnitItem;
import com.unikorn.campus.catalog.api.UserItem;
import java.util.List;

public interface CatalogRepository {

    List<UserItem> findUsers();

    List<ResourceItem> findAcademicSpaces();

    List<ResourceItem> findSportFacilities();

    List<SportUnitItem> findSportUnits();
}
