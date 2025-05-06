package me.lojosho.leavingaugie.database;

import me.lojosho.leavingaugie.LeavingAugieApplication;
import me.lojosho.leavingaugie.cache.Location;
import me.lojosho.leavingaugie.util.Logging;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SQLiteDatabase {

    private Connection connection;
    private File dbFile;

    public SQLiteDatabase() {

    }

    public void setup() {
        File dataFolder = LeavingAugieApplication.getDataFolder();
        boolean exists = dataFolder.exists();

        if (!exists) {
            try {
                boolean created = dataFolder.mkdir();
                if (!created) throw new IOException("File didn't exist but now does");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dbFile = new File(dataFolder, "database.db");
        if (!dbFile.exists()) {
            try {
                boolean created = dbFile.createNewFile();
                if (!created) throw new IOException("File didn't exist but now does");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            openConnection();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS 'LOCATION_DB' " +
                    "(LOCATION INTEGER PRIMARY KEY, " +
                    "NAME MEDIUMTEXT NOT NULL, " +
                    "ORIGIN BOOLEAN NOT NULL " +
                    ");").execute();

            connection.prepareStatement("CREATE TABLE IF NOT EXISTS `TRAVELTIME_DB` " +
                    "(ORIGIN INTEGER, " +
                    "DESTINATION INTEGER, " +
                    "TRAVEL_TIME INTEGER," +
                    "LOG_TIME INTEGER, " +
                    "FOREIGN KEY (ORIGIN) REFERENCES LOCATION_DB(LOCATION), " +
                    "FOREIGN KEY (DESTINATION) REFERENCES LOCATION_DB(LOCATION)" +
                    ");").execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveRecord(Location origin, Location destination, long time) throws SQLException {
        long timestamp = Instant.now().toEpochMilli();

        PreparedStatement saveStatement = connection.prepareStatement("INSERT INTO TRAVELTIME_DB VALUES(?, ?, ?, ?)");
        saveStatement.setInt(1, origin.getId());
        saveStatement.setInt(2, destination.getId());
        saveStatement.setLong(3, time);
        saveStatement.setLong(4, timestamp);

        saveStatement.executeUpdate();
    }

    public Map<String, Object> getRecords(Location origin, Location destination) throws SQLException {
        PreparedStatement getStatement = connection.prepareStatement("SELECT * FROM TRAVELTIME_DB WHERE ORIGIN = ? AND DESTINATION = ?");
        getStatement.setInt(1, origin.getId());
        getStatement.setInt(2, destination.getId());

        ResultSet resultSet = getStatement.executeQuery();
        Map<String, Object> record = new HashMap<>();
        while (resultSet.next()) {
            Long time = resultSet.getLong("TRAVEL_TIME");
            Long timestamp = resultSet.getLong("LOG_TIME");
            record.put(timestamp.toString(), time);
        }

        return record;
    }

    private void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) return;

        // Connect to database host
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        } catch (SQLException e) {
            new RuntimeException(e);
        }
    }

    public Collection<Location> getLocations() throws SQLException {
        openConnection();
        ResultSet rs = connection.prepareStatement("SELECT * FROM 'LOCATION_DB'").executeQuery();

        Collection<Location> locations = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("LOCATION");
            String name = rs.getString("NAME");
            boolean origin = rs.getBoolean("ORIGIN");

            Logging.print(id + " " + name + " " + origin);

            Location location = new Location(id, name, origin);
            locations.add(location);
        }
        return locations;
    }
}
