package com.example.bookmark.dish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void login() throws Exception {
        adminToken = loginToken("admin", "admin123");
        userToken = loginToken("user", "user123");
    }

    @Test
    void createListRandomUpdateDelete() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "黄焖鸡",
                "note", "微辣",
                "tags", "快餐,鸡肉",
                "favorite", true
        ));

        String created = mockMvc.perform(post("/api/dishes/create")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("黄焖鸡"))
                .andExpect(jsonPath("$.favorite").value(true))
                .andExpect(jsonPath("$.spice").isNumber())
                .andExpect(jsonPath("$.salt").isNumber())
                .andExpect(jsonPath("$.light").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/dishes/list")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(id.intValue())))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/dishes/list?q=黄焖&page=0&size=5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.size").value(5));

        mockMvc.perform(get("/api/dishes/list?favoriteOnly=true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/dishes/random")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isString());

        mockMvc.perform(get("/api/dishes/detail/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").value("快餐,鸡肉"));

        String update = objectMapper.writeValueAsString(Map.of(
                "name", "黄焖鸡米饭",
                "note", "中辣",
                "tags", "快餐",
                "favorite", false
        ));
        mockMvc.perform(put("/api/dishes/update/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("黄焖鸡米饭"))
                .andExpect(jsonPath("$.favorite").value(false));

        mockMvc.perform(delete("/api/dishes/delete/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dishes/detail/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotWriteDishes() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "用户偷偷加菜",
                "note", "",
                "tags", "",
                "favorite", false
        ));

        mockMvc.perform(post("/api/dishes/create")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dishes/import-template")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dishes/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importExcelSkipsDuplicates() throws Exception {
        byte[] xlsx = excelBytes(
                new String[]{"名称", "标签", "备注", "收藏"},
                new String[]{"烤鱼", "川菜", "香辣", "是"},
                new String[]{"烤鱼", "川菜", "", ""},
                new String[]{"酸菜鱼", "川菜", "", ""}
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dishes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsx
        );

        mockMvc.perform(multipart("/api/dishes/import").file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.dishes", hasSize(2)));

        mockMvc.perform(get("/api/dishes/import-template")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void openApiDocsIncludeDishPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("开吃 API"))
                .andExpect(jsonPath("$.paths['/api/dishes/list']").exists())
                .andExpect(jsonPath("$.paths['/api/dishes/random']").exists())
                .andExpect(jsonPath("$.paths['/api/dishes/import']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }

    private String loginToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
        ));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private byte[] excelBytes(String[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("菜单");
            for (int r = 0; r < rows.length; r++) {
                XSSFRow row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
