/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author brianxiadong
 */
package com.xs.ai.mcp.sse.server;

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 利用OpenMeteo的免费天气API提供天气服务
 * 该API无需API密钥，可以直接使用
 */
@Service
public class OpenMeteoService {

    // OpenMeteo免费天气API基础URL
    private static final String BASE_URL = "https://api.open-meteo.com/v1";

    private final RestClient restClient;

    public OpenMeteoService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "OpenMeteoClient/1.0")
                .build();
    }

    // OpenMeteo天气数据模型
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherData(
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("current") CurrentWeather current,
            @JsonProperty("daily") DailyForecast daily,
            @JsonProperty("current_units") CurrentUnits currentUnits) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record CurrentWeather(
                @JsonProperty("time") String time,
                @JsonProperty("temperature_2m") Double temperature,
                @JsonProperty("apparent_temperature") Double feelsLike,
                @JsonProperty("relative_humidity_2m") Integer humidity,
                @JsonProperty("precipitation") Double precipitation,
                @JsonProperty("weather_code") Integer weatherCode,
                @JsonProperty("wind_speed_10m") Double windSpeed,
                @JsonProperty("wind_direction_10m") Integer windDirection) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record CurrentUnits(
                @JsonProperty("time") String timeUnit,
                @JsonProperty("temperature_2m") String temperatureUnit,
                @JsonProperty("relative_humidity_2m") String humidityUnit,
                @JsonProperty("wind_speed_10m") String windSpeedUnit) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record DailyForecast(
                @JsonProperty("time") List<String> time,
                @JsonProperty("temperature_2m_max") List<Double> tempMax,
                @JsonProperty("temperature_2m_min") List<Double> tempMin,
                @JsonProperty("precipitation_sum") List<Double> precipitationSum,
                @JsonProperty("weather_code") List<Integer> weatherCode,
                @JsonProperty("wind_speed_10m_max") List<Double> windSpeedMax,
                @JsonProperty("wind_direction_10m_dominant") List<Integer> windDirection) {
        }
    }

    /**
     * 获取天气代码对应的描述
     */
    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "晴朗";
            case 1, 2, 3 -> "多云";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻雨";
            case 61, 63, 65 -> "雨";
            case 66, 67 -> "冻雨";
            case 71, 73, 75 -> "雪";
            case 77 -> "雪粒";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷暴";
            case 96, 99 -> "雷暴伴有冰雹";
            default -> "未知天气";
        };
    }

    /**
     * 获取风向描述
     */
    private String getWindDirection(int degrees) {
        if (degrees >= 337.5 || degrees < 22.5)
            return "北风";
        if (degrees >= 22.5 && degrees < 67.5)
            return "东北风";
        if (degrees >= 67.5 && degrees < 112.5)
            return "东风";
        if (degrees >= 112.5 && degrees < 157.5)
            return "东南风";
        if (degrees >= 157.5 && degrees < 202.5)
            return "南风";
        if (degrees >= 202.5 && degrees < 247.5)
            return "西南风";
        if (degrees >= 247.5 && degrees < 292.5)
            return "西风";
        return "西北风";
    }

    /**
     * 获取指定经纬度的天气预报
     * 
     * @param latitude  纬度
     * @param longitude 经度
     * @return 指定位置的天气预报
     * @throws RestClientException 如果请求失败
     */
    @Tool(description = "获取指定经纬度的天气预报,根据位置自动推算经纬度")
    public String getWeatherForecastByLocation(
              double latitude,
              double longitude) {
        // 获取天气数据（当前和未来7天）
        var weatherData = restClient.get()
                .uri("/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code,wind_speed_10m_max,wind_direction_10m_dominant&timezone=auto&forecast_days=7",
                        latitude, longitude)
                .retrieve()
                .body(WeatherData.class);

        // 拼接天气信息
        StringBuilder weatherInfo = new StringBuilder();

        // 添加当前天气信息
        WeatherData.CurrentWeather current = weatherData.current();
        String temperatureUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().temperatureUnit()
                : "°C";
        String windSpeedUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().windSpeedUnit() : "km/h";
        String humidityUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().humidityUnit() : "%";

        weatherInfo.append(String.format("""
                当前天气:
                温度: %.1f%s (体感温度: %.1f%s)
                天气: %s
                风向: %s (%.1f %s)
                湿度: %d%s
                降水量: %.1f 毫米

                """,
                current.temperature(),
                temperatureUnit,
                current.feelsLike(),
                temperatureUnit,
                getWeatherDescription(current.weatherCode()),
                getWindDirection(current.windDirection()),
                current.windSpeed(),
                windSpeedUnit,
                current.humidity(),
                humidityUnit,
                current.precipitation()));

        // 添加未来天气预报
        weatherInfo.append("未来天气预报:\n");
        WeatherData.DailyForecast daily = weatherData.daily();

        for (int i = 0; i < daily.time().size(); i++) {
            String date = daily.time().get(i);
            double tempMin = daily.tempMin().get(i);
            double tempMax = daily.tempMax().get(i);
            int weatherCode = daily.weatherCode().get(i);
            double windSpeed = daily.windSpeedMax().get(i);
            int windDir = daily.windDirection().get(i);
            double precip = daily.precipitationSum().get(i);

            // 格式化日期
            LocalDate localDate = LocalDate.parse(date);
            String formattedDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)"));

            weatherInfo.append(String.format("""
                    %s:
                    温度: %.1f%s ~ %.1f%s
                    天气: %s
                    风向: %s (%.1f %s)
                    降水量: %.1f 毫米

                    """,
                    formattedDate,
                    tempMin, temperatureUnit,
                    tempMax, temperatureUnit,
                    getWeatherDescription(weatherCode),
                    getWindDirection(windDir),
                    windSpeed, windSpeedUnit,
                    precip));
        }

        return weatherInfo.toString();
    }


}