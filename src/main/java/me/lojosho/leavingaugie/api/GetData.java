package me.lojosho.leavingaugie.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.lojosho.leavingaugie.LeavingAugieApplication;
import me.lojosho.leavingaugie.cache.Location;
import me.lojosho.leavingaugie.cache.LocationCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GetData {

    @GetMapping("/locations")
    public String getLocations(@RequestParam() String apiKey) throws JsonProcessingException {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        if (!apiKey.equals(LeavingAugieApplication.getAPI_KEY())) {
            return null;
        }

        Map<String, Object> locations = new HashMap<>();
        for (Location location : LocationCache.getLocations()) {
            locations.put(String.valueOf(location.getId()), Map.of(location.getName(), location.isOrigin()));
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(locations);
    }

    @GetMapping("/fulldata")
    public String getData(@RequestParam String apiKey, @RequestParam() String origin, @RequestParam() String destination) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        if (!apiKey.equals(LeavingAugieApplication.getAPI_KEY())) {
            return null;
        }

        int originId = Integer.parseInt(origin);

        int destinationId = Integer.parseInt(destination);

        Location locOrigin = LocationCache.getLocation(originId);
        Location locdestination = LocationCache.getLocation(destinationId);

        if (locOrigin == null || locdestination == null) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(LeavingAugieApplication.getDatabase().getRecords(locOrigin, locdestination));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
