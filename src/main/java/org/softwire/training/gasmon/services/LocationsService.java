package org.softwire.training.gasmon.services;

import com.google.gson.Gson;
import org.softwire.training.gasmon.model.Location;
import org.softwire.training.gasmon.repository.S3Repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class LocationsService {

    private final S3Repository s3Repository;
    private final String locationsFileName;
    private final Gson gson = new Gson();

    private List<Location> validLocations;

    public LocationsService(S3Repository s3Repository, String locationsFileName) {
        this.s3Repository = s3Repository;
        this.locationsFileName = locationsFileName;
    }

    public List<Location> getValidLocations() throws IOException {
        if (validLocations == null) {
            String locationsJson = s3Repository.getObjectAtKey(locationsFileName);
            validLocations = Arrays.asList(gson.fromJson(locationsJson, Location[].class));
        }
        return validLocations;
    }

    public boolean isValidLocation(String locationId) throws IOException {
        getValidLocations();
        for (Location location : validLocations) {
            if (location.getId().equals(locationId)) {
                return true;
            }
        }
        return false;
    }
}
