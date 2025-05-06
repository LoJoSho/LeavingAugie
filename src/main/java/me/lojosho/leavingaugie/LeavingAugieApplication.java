package me.lojosho.leavingaugie;

import me.lojosho.leavingaugie.cache.Location;
import me.lojosho.leavingaugie.cache.LocationCache;
import me.lojosho.leavingaugie.database.SQLiteDatabase;
import me.lojosho.leavingaugie.util.Logging;
import me.lojosho.leavingaugie.web.GoogleMapsRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
public class LeavingAugieApplication {

    private static SQLiteDatabase database;
    private static String API_KEY;
    private static String GOOGLE_API_KEY;

    public static void main(String[] args) {
        SpringApplication.run(LeavingAugieApplication.class, args);

        try {
            API_KEY = Files.readString(Path.of(getDataFolder() + "/APIKEY.txt"));
            GOOGLE_API_KEY = Files.readString(Path.of(getDataFolder() + "/GOOGLE_API_KEY.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        database = new SQLiteDatabase();
        database.setup();
    }

    @Scheduled(fixedRate = 60, timeUnit = TimeUnit.MINUTES)
    public static void runCacheCleanup() {
        LocationCache.refreshCache();
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public static void runTest() {
        Logging.print("Beginning Checking of Durations");

        Collection<Location> rawLocations = LocationCache.getLocations();
        List<Location> destinations = rawLocations.stream().filter(Location::isDestination).toList();
        List<Location> origins = rawLocations.stream().filter(Location::isOrigin).toList();

        for (Location origin : origins) {
            for (Location destination : destinations) {
                try {
                    long duration = getDuration(origin, destination);
                    if (duration < 0) {
                        Logging.print("Skipping " + origin + " to " + destination + " (duration = " + duration + ")");
                        continue;
                    }
                    database.saveRecord(origin, destination, duration);
                    Logging.print("Saved " + origin + " to " + destination + " (duration = " + duration + ")");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Finished Checking of Durations with " + rawLocations.size() + " locations");
    }

    public static Integer getDuration(Location origin, Location destination) {
        GoogleMapsRequest request = new GoogleMapsRequest(origin.getName(), destination.getName());
        try {
            var response = request.fetchJson();

            // Value is the seconds to go from each location
            String seconds = response
                    .path("rows")
                    .path(0)
                    .path("elements")
                    .path(0)
                    .path("duration")
                    .path("value")
                    .toString();

            return Integer.parseInt(seconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static File getDataFolder() {
        File file = new File(System.getenv("PWD") + "/run/");
        return file;
    }

    public static SQLiteDatabase getDatabase() {
        return database;
    }

    public static String getAPI_KEY() {
        return API_KEY;
    }

    public static String getGoogleApiKey() {
        return GOOGLE_API_KEY;
    }
}
