package com.example.bookmark.zhihu;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/zhihu")
@Tag(name = "知乎热榜", description = "代理聚合源，返回知乎实时热榜")
public class ZhihuController {

    private static final String UPSTREAM_URL = "https://hotpush.dawenzaist.de5.net/api/hot/zhihu";

    private final RestTemplate restTemplate;

    private static final ParameterizedTypeReference<Map<String, Object>> HOT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    public ZhihuController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    @GetMapping("/hot")
    @Operation(summary = "知乎热榜", description = "返回当前知乎热榜列表，按热度排序。")
    public ZhihuHotListResponse hotList() {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    UPSTREAM_URL,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    HOT_LIST_TYPE);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "上游返回为空");
            }
            return normalize(body);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "拉取知乎热榜失败：" + ex.getMessage(),
                    ex);
        }
    }

    @SuppressWarnings("unchecked")
    private ZhihuHotListResponse normalize(Map<String, Object> body) {
        String source = Optional.ofNullable(body.get("source")).map(Object::toString).orElse("zhihu");
        String sourceName = Optional.ofNullable(body.get("source_name")).map(Object::toString).orElse("知乎热榜");
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.getOrDefault("items", Collections.emptyList());

        List<ZhihuHotItem> items = rawItems.stream()
                .map(this::toItem)
                .limit(50)
                .toList();

        return new ZhihuHotListResponse(source, sourceName, Instant.now(), items);
    }

    private ZhihuHotItem toItem(Map<String, Object> raw) {
        String id = stringify(raw.get("id"));
        String title = stringify(raw.get("title"));
        String url = stringify(raw.get("url"));
        Long heat = parseLong(raw.get("hot_score"));
        Instant published = parseInstant(raw.get("published"));
        String description = stringify(raw.get("description"));
        String image = stringify(raw.get("image"));
        return new ZhihuHotItem(id, title, url, heat, published, description, image);
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
