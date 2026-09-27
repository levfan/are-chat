package com.smart.chat.couple;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 异地恋助手内置城市库：精选国内外常用城市（手填匹配，不接外部 API，离线可用）。
 * 城市名精确匹配（去空格后全等），匹配不到时城市文本照常保存，只是不计算时差/距离。
 * 坐标 WGS84；中国城市统一使用 Asia/Shanghai（北京时间）。
 */
public final class CoupleCities {

    private CoupleCities() {
    }

    /** 一个城市 = 名称 + 经纬度 + IANA 时区。 */
    public record City(String name, double lat, double lon, String zoneId) {
    }

    private static final List<City> CITIES = List.of(
            // ── 中国（统一北京时间）──
            new City("北京", 39.9042, 116.4074, "Asia/Shanghai"),
            new City("上海", 31.2304, 121.4737, "Asia/Shanghai"),
            new City("广州", 23.1291, 113.2644, "Asia/Shanghai"),
            new City("深圳", 22.5431, 114.0579, "Asia/Shanghai"),
            new City("杭州", 30.2741, 120.1551, "Asia/Shanghai"),
            new City("成都", 30.5728, 104.0668, "Asia/Shanghai"),
            new City("重庆", 29.5630, 106.5516, "Asia/Shanghai"),
            new City("武汉", 30.5928, 114.3055, "Asia/Shanghai"),
            new City("南京", 32.0603, 118.7969, "Asia/Shanghai"),
            new City("西安", 34.3416, 108.9398, "Asia/Shanghai"),
            new City("长沙", 28.2282, 112.9388, "Asia/Shanghai"),
            new City("郑州", 34.7466, 113.6254, "Asia/Shanghai"),
            new City("济南", 36.6512, 117.1201, "Asia/Shanghai"),
            new City("青岛", 36.0671, 120.3826, "Asia/Shanghai"),
            new City("沈阳", 41.8057, 123.4315, "Asia/Shanghai"),
            new City("大连", 38.9140, 121.6147, "Asia/Shanghai"),
            new City("哈尔滨", 45.8038, 126.5349, "Asia/Shanghai"),
            new City("长春", 43.8171, 125.3235, "Asia/Shanghai"),
            new City("天津", 39.3434, 117.3616, "Asia/Shanghai"),
            new City("石家庄", 38.0428, 114.5149, "Asia/Shanghai"),
            new City("太原", 37.8706, 112.5489, "Asia/Shanghai"),
            new City("合肥", 31.8206, 117.2272, "Asia/Shanghai"),
            new City("南昌", 28.6820, 115.8579, "Asia/Shanghai"),
            new City("福州", 26.0745, 119.2965, "Asia/Shanghai"),
            new City("厦门", 24.4798, 118.0894, "Asia/Shanghai"),
            new City("南宁", 22.8170, 108.3665, "Asia/Shanghai"),
            new City("昆明", 25.0389, 102.7183, "Asia/Shanghai"),
            new City("贵阳", 26.6470, 106.6302, "Asia/Shanghai"),
            new City("兰州", 36.0611, 103.8343, "Asia/Shanghai"),
            new City("西宁", 36.6171, 101.7782, "Asia/Shanghai"),
            new City("银川", 38.4872, 106.2309, "Asia/Shanghai"),
            new City("呼和浩特", 40.8424, 111.7490, "Asia/Shanghai"),
            new City("乌鲁木齐", 43.8256, 87.6168, "Asia/Shanghai"),
            new City("拉萨", 29.6520, 91.1721, "Asia/Shanghai"),
            new City("海口", 20.0444, 110.1999, "Asia/Shanghai"),
            new City("三亚", 18.2528, 109.5119, "Asia/Shanghai"),
            new City("香港", 22.3193, 114.1694, "Asia/Hong_Kong"),
            new City("澳门", 22.1987, 113.5439, "Asia/Macau"),
            new City("台北", 25.0330, 121.5654, "Asia/Taipei"),
            // ── 国际 ──
            new City("东京", 35.6762, 139.6503, "Asia/Tokyo"),
            new City("大阪", 34.6937, 135.5023, "Asia/Tokyo"),
            new City("首尔", 37.5665, 126.9780, "Asia/Seoul"),
            new City("新加坡", 1.3521, 103.8198, "Asia/Singapore"),
            new City("曼谷", 13.7563, 100.5018, "Asia/Bangkok"),
            new City("吉隆坡", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
            new City("悉尼", -33.8688, 151.2093, "Australia/Sydney"),
            new City("墨尔本", -37.8136, 144.9631, "Australia/Melbourne"),
            new City("奥克兰", -36.8485, 174.7633, "Pacific/Auckland"),
            new City("迪拜", 25.2048, 55.2708, "Asia/Dubai"),
            new City("伊斯坦布尔", 41.0082, 28.9784, "Europe/Istanbul"),
            new City("莫斯科", 55.7558, 37.6173, "Europe/Moscow"),
            new City("伦敦", 51.5074, -0.1278, "Europe/London"),
            new City("巴黎", 48.8566, 2.3522, "Europe/Paris"),
            new City("柏林", 52.5200, 13.4050, "Europe/Berlin"),
            new City("罗马", 41.9028, 12.4964, "Europe/Rome"),
            new City("马德里", 40.4168, -3.7038, "Europe/Madrid"),
            new City("阿姆斯特丹", 52.3676, 4.9041, "Europe/Amsterdam"),
            new City("纽约", 40.7128, -74.0060, "America/New_York"),
            new City("洛杉矶", 34.0522, -118.2437, "America/Los_Angeles"),
            new City("旧金山", 37.7749, -122.4194, "America/Los_Angeles"),
            new City("西雅图", 47.6062, -122.3321, "America/Los_Angeles"),
            new City("芝加哥", 41.8781, -87.6298, "America/Chicago"),
            new City("多伦多", 43.6532, -79.3832, "America/Toronto"),
            new City("温哥华", 49.2827, -123.1207, "America/Vancouver")
    );

    private static final Map<String, City> INDEX = CITIES.stream()
            .collect(Collectors.toMap(City::name, Function.identity(), (a, b) -> a));

    /** 按名称精确匹配城市（trim 后全等）。 */
    public static Optional<City> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(INDEX.get(name.trim()));
    }

    /** 城市当前 UTC 偏移秒数（含夏令时）。 */
    public static int offsetSeconds(City city) {
        return ZoneId.of(city.zoneId()).getRules().getOffset(java.time.Instant.now()).getTotalSeconds();
    }

    /** Haversine 球面距离（km，四舍五入取整）。 */
    public static long distanceKm(City a, City b) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double km = 2 * earthRadius * Math.asin(Math.sqrt(h));
        return Math.round(km);
    }
}
