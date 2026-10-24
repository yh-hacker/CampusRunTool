package com.campsruntool.yh.map;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;

/**
 * Key-free raster tile layers. A single template-aware source handles XYZ
 * query strings, Esri's {z}/{y}/{x} order and optional {s} subdomains.
 */
public final class TileSources {
    private static final String GAODE_URL =
            "https://webrd0{s}.is.autonavi.com/appmaptile"
                    + "?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}";
    private static final String OSM_URL =
            "https://tile.openstreetmap.de/{z}/{x}/{y}.png";
    private static final String ESRI_IMAGERY_URL =
            "https://server.arcgisonline.com/ArcGIS/rest/services/"
                    + "World_Imagery/MapServer/tile/{z}/{y}/{x}";

    private TileSources() {
    }

    public static MapLayer gaode() {
        return new MapLayer(
                MapLayer.ID_GAODE,
                "高德地图",
                "© 高德地图",
                MapLayer.CoordSystem.GCJ02,
                new TemplateTileSource("routego-gaode", 3, 18, GAODE_URL,
                        new String[]{"1", "2", "3", "4"}));
    }

    public static MapLayer osm() {
        return new MapLayer(
                MapLayer.ID_OSM,
                "OpenStreetMap",
                "© OpenStreetMap contributors",
                MapLayer.CoordSystem.WGS84,
                new TemplateTileSource("routego-osm", 3, 19, OSM_URL, null));
    }

    public static MapLayer esriImagery() {
        return new MapLayer(
                MapLayer.ID_ESRI,
                "Esri 卫星",
                "© Esri, Maxar, Earthstar Geographics",
                MapLayer.CoordSystem.WGS84,
                new TemplateTileSource("routego-esri", 2, 19, ESRI_IMAGERY_URL, null));
    }

    public static MapLayer custom(String url, MapLayer.CoordSystem coordSystem) {
        if (!isValidTemplate(url)) {
            throw new IllegalArgumentException("custom tile URL must contain {z}, {x} and {y}");
        }
        String name = "routego-custom-" + Integer.toHexString(url.hashCode());
        return new MapLayer(
                MapLayer.ID_CUSTOM,
                "自定义图层",
                "自定义图层",
                coordSystem,
                new TemplateTileSource(name, 0, 19, url, null));
    }

    public static boolean isValidTemplate(String url) {
        return url != null
                && url.contains("{z}")
                && url.contains("{x}")
                && url.contains("{y}");
    }

    public static MapLayer builtIn(String id) {
        if (MapLayer.ID_OSM.equals(id)) {
            return osm();
        }
        if (MapLayer.ID_ESRI.equals(id)) {
            return esriImagery();
        }
        return gaode();
    }

    public static final class TemplateTileSource extends OnlineTileSourceBase {
        private final String template;
        private final String[] servers;

        public TemplateTileSource(String name, int minZoom, int maxZoom,
                                  String template, String[] servers) {
            super(name, minZoom, maxZoom, 256, ".png", new String[]{template});
            this.template = template;
            this.servers = servers;
        }

        @Override
        public String getTileURLString(long tileIndex) {
            int zoom = MapTileIndex.getZoom(tileIndex);
            int x = MapTileIndex.getX(tileIndex);
            int y = MapTileIndex.getY(tileIndex);

            String url = template
                    .replace("{z}", Integer.toString(zoom))
                    .replace("{x}", Integer.toString(x))
                    .replace("{y}", Integer.toString(y));

            if (servers != null && servers.length > 0) {
                int index = (int) (tileIndex % servers.length);
                url = url.replace("{s}", servers[index]);
            } else {
                url = url.replace("{s}", "a");
            }
            return url;
        }
    }
}
