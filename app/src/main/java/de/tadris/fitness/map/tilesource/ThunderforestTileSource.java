/*
 * Copyright (c) 2020 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.map.tilesource;

import org.mapsforge.core.model.Tile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;

public class ThunderforestTileSource extends FitoTrackTileSource {

    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        Properties properties = new Properties();
        try (InputStream input = ThunderforestTileSource.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing config.properties file. Please add it as per README instructions.");
            }
            properties.load(input);
            return properties.getProperty("THUNDERFOREST_API_KEY");
        } catch (IOException e) {
            throw new RuntimeException("Error loading API key from config file", e);
        }
    }

    public static final ThunderforestTileSource OUTDOORS = new ThunderforestTileSource("outdoors", "Outdoor");
    public static final ThunderforestTileSource CYCLE_MAP = new ThunderforestTileSource("cycle", "Cycle Map");
    private static final int PARALLEL_REQUESTS_LIMIT = 8;
    private static final String PROTOCOL = "https";
    private static final int ZOOM_LEVEL_MAX = 19;
    private static final int ZOOM_LEVEL_MIN = 0;

    private final String mapName;
    private final String name;

    private ThunderforestTileSource(String mapName, String name) {
        super(new String[]{"tile.thunderforest.com"}, 443, (byte) ZOOM_LEVEL_MIN, (byte) ZOOM_LEVEL_MAX, PARALLEL_REQUESTS_LIMIT);
        this.mapName = mapName;
        this.name = name;
    }

    @Override
    public int getParallelRequestsLimit() {
        return PARALLEL_REQUESTS_LIMIT;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected String buildTileUrlPath(Tile tile) {
        return "/" + mapName + "/" + tile.zoomLevel + '/' + tile.tileX + '/' + tile.tileY + ".png?apikey=" + API_KEY;
    }
}
