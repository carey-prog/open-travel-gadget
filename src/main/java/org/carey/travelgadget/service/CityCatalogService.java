package org.carey.travelgadget.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.domain.model.DepartureCity;
import org.carey.travelgadget.domain.model.Destination;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CityCatalogService {

    @Getter
    private final List<DepartureCity> departureCities = new ArrayList<>();
    @Getter
    private final Map<String, Destination> destinationsById = new LinkedHashMap<>();

    @PostConstruct
    public void loadCatalog() {
        try (InputStream in = new ClassPathResource("cities.yml").getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            if (root == null) {
                return;
            }
            loadDepartureCities(root.get("departure-cities"));
            loadDestinations(root.get("destinations"));
            log.info("城市目录已加载：{} 个目的地，{} 个出发地", destinationsById.size(), departureCities.size());
        } catch (Exception e) {
            log.error("加载 cities.yml 失败: {}", e.getMessage(), e);
        }
    }

    public Optional<Destination> findDestination(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(destinationsById.get(id.trim()));
    }

    public Optional<DepartureCity> findDepartureCity(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }
        return departureCities.stream()
                .filter(c -> c.getId().equals(id.trim()))
                .findFirst();
    }

    public Destination requireDestination(String id) {
        return findDestination(id)
                .orElseThrow(() -> new IllegalArgumentException("未知目的地: " + id));
    }

    public DepartureCity requireDepartureCity(String id) {
        return findDepartureCity(id)
                .orElseThrow(() -> new IllegalArgumentException("未知出发地: " + id));
    }

    @SuppressWarnings("unchecked")
    private void loadDepartureCities(Object raw) {
        departureCities.clear();
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                DepartureCity city = new DepartureCity();
                city.setId(stringVal(map.get("id")));
                city.setName(stringVal(map.get("name")));
                if (StringUtils.hasText(city.getId())) {
                    departureCities.add(city);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDestinations(Object raw) {
        destinationsById.clear();
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Destination d = new Destination();
                d.setId(stringVal(map.get("id")));
                d.setName(stringVal(map.get("name")));
                d.setCities(stringVal(map.get("cities")));
                d.setArrivalHub(stringVal(map.get("arrival-hub")));
                d.setArrivalHubLabel(stringVal(map.get("arrival-hub-label")));
                d.setTransportMode(stringVal(map.get("transport-mode")));
                d.setLocalTransportNote(stringVal(map.get("local-transport-note")));
                Object themes = map.get("theme-options");
                if (themes instanceof String s) {
                    d.setThemeOptions(List.of(s.split(",")));
                } else if (themes instanceof List<?> themeList) {
                    d.setThemeOptions(themeList.stream().map(String::valueOf).toList());
                }
                if (StringUtils.hasText(d.getId())) {
                    destinationsById.put(d.getId(), d);
                }
            }
        }
    }

    private String stringVal(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }
}
