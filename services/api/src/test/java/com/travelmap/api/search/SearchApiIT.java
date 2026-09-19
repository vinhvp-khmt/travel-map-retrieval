package com.travelmap.api.search;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.auth.service.TokenService;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiOpeningHourEntity;
import com.travelmap.api.poi.repository.CategoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 10 (IR search plan) — integration test thật cho tầng controller HTTP, trên PostGIS
 * thật qua Testcontainers (xem {@link AbstractPostgresIntegrationTest}).
 *
 * <p>Đây chính là chỗ đã bắt được 2 bug thật trước đó chỉ lộ ra khi gọi {@code curl} tay:
 * {@code search_log}/{@code search_history} có cột {@code NOT NULL} mà tầng service unboxing
 * {@code null} khi search không có GPS. Test ở đây không mock repository nào — request đi
 * qua {@code MockMvc} → filter chain Spring Security thật → controller thật → service thật →
 * JdbcTemplate/JPA thật → Postgres thật, đúng như một request thật sẽ đi qua.
 *
 * <p>Đăng nhập bằng JWT thật (qua {@link TokenService#issuePair}, không mock) thay vì
 * {@code @WithMockUser} — khớp đúng convention đã có sẵn của project ({@code AuthApiSecurityTest}
 * gửi header {@code Authorization: Bearer ...} thật qua {@code JwtAuthenticationFilter} thật);
 * {@code @WithMockUser} đã thử và không hoạt động đúng với filter chain tuỳ biến của app này
 * (context bị bỏ qua, mọi request vẫn bị coi là anonymous → 403).
 *
 * <p>{@code @Transactional} trên test class: mỗi test chạy trong một transaction riêng, rollback
 * sau khi xong — các test không ảnh hưởng lẫn nhau dù dùng chung một container Postgres.
 *
 * <p><b>Vì sao tên lớp là {@code ...IT} chứ không phải {@code ...Test}:</b> lớp này cần Docker
 * sống (qua Testcontainers) và tải image {@code postgis/postgis:16-3.4} lần đầu — nếu để tên
 * khớp pattern mặc định của Surefire ({@code *Test}/{@code *Tests}/{@code *TestCase}/
 * {@code Test*}), MỌI lần chạy {@code mvn test} kể cả trong CI không có Docker sẽ fail. Hậu tố
 * {@code IT} (chuẩn Maven Failsafe, dù project chưa cấu hình failsafe-plugin) không khớp pattern
 * đó nên {@code mvn test}/{@code mvn clean test} bỏ qua lớp này — giống cách
 * {@code IrDatasetEvaluationRunner} cố tình không có hậu tố {@code Test}. Chạy tường minh bằng:
 * <pre>
 *   colima start   # hoặc Docker Desktop
 *   export DOCKER_HOST=unix:///Users/&lt;user&gt;/.colima/default/docker.sock   # neu dung colima
 *   export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock       # neu dung colima
 *   ./mvnw test -Dtest=SearchApiIT
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchApiIT extends AbstractPostgresIntegrationTest {

    private static final String USER_EMAIL = "integ-user@travelmap.local";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PoiRepository poiRepository;
    @Autowired private TokenService tokenService;

    private PoiEntity poi;
    private String bearerToken;

    @BeforeEach
    void seed() {
        UserEntity user = userRepository.save(new UserEntity(USER_EMAIL, "$2a$10$abcdefghijklmnopqrstuv", UserRole.USER));
        bearerToken = "Bearer " + tokenService.issuePair(user).accessToken();

        UserEntity owner = userRepository.save(new UserEntity("integ-owner@travelmap.local",
                "$2a$10$abcdefghijklmnopqrstuv", UserRole.OWNER));
        CategoryEntity category = categoryRepository.findAll().stream()
                .filter(c -> "ca-phe".equals(c.getSlug()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("V2 migration phai seed category ca-phe"));

        PoiEntity draft = new PoiEntity(owner, category, "Integration Test Cafe", "integration test cafe",
                "Quan test cho integration test", 10.7769, 106.7009, "1 Test Street", 2, 20, false);
        List<PoiOpeningHourEntity> hours = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            hours.add(new PoiOpeningHourEntity(day, LocalTime.MIN, LocalTime.MAX, false));
        }
        draft.replaceOpeningHours(hours);
        draft.approve();
        poi = poiRepository.save(draft);
    }

    @Test
    void searchWithoutGpsDoesNotFail() throws Exception {
        // Regression test cho bug that: thieu GPS tung lam search_log insert that bai
        // (NOT NULL) va tra ve 403/500 an, thay vi 200 + distanceMeters=null nhu mong doi.
        mockMvc.perform(get("/api/v1/search").param("q", "Integration Test").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.results[0].distanceMeters").value(nullValue()))
                .andExpect(jsonPath("$.results[0].scoreDetail.spatial").value(0.5));
    }

    @Test
    void searchWithGpsReturnsRealDistance() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "Integration Test")
                        .param("latitude", "10.7769").param("longitude", "106.7009").param("radiusKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.results[0].distanceMeters").isNumber());
    }

    @Test
    void searchWithHalfGpsIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "Integration Test").param("latitude", "10.7769"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_VALIDATION_ERROR"));
    }

    @Test
    void authenticatedSearchWithoutGpsAlsoSucceedsAndSkipsHistory() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "Integration Test").param("size", "5")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());
        // GPS-less search khong luu duoc vao search_history (cot latitude/longitude NOT NULL
        // o do) - phai bo qua ghi, khong duoc nem loi.
        mockMvc.perform(get("/api/v1/search/history").header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void authenticatedSearchRecordsHistoryListAndDelete() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "Integration Test")
                        .param("latitude", "10.7769").param("longitude", "106.7009").param("radiusKm", "5")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/search/history").header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].query").value("Integration Test"));

        mockMvc.perform(delete("/api/v1/search/history").header("Authorization", bearerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/search/history").header("Authorization", bearerToken))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void viewingPoiRecordsHistoryListAndDelete() throws Exception {
        mockMvc.perform(post("/api/v1/pois/{id}/view", poi.getId()).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/viewed-pois").header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].poiId").value(poi.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Integration Test Cafe"));

        mockMvc.perform(delete("/api/v1/users/me/viewed-pois/{id}", poi.getId()).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/users/me/viewed-pois").header("Authorization", bearerToken))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void viewingPoiWithoutAuthIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/pois/{id}/view", poi.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewingNonExistentPoiReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/pois/{id}/view", UUID.randomUUID()).header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
