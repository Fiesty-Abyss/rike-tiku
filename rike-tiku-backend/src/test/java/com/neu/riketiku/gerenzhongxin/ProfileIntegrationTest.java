package com.neu.riketiku.gerenzhongxin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.demo.DemoDataService;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired private DemoDataService demo;
    @Autowired private JdbcTemplate jdbc;
    @LocalServerPort private int port;

    @Test
    void allRolesReadOnlyTheirOwnBusinessProfiles() throws Exception {
        demo.seed();
        try {
            String student = get("/api/v1/profile", login("demo_student", "STUDENT")).body();
            assertThat(student).contains("demo_student", "STUDENT", "演示学生", "DEMO_S001", "高三理综演示班")
                    .doesNotContain("mi_ma_zhai_yao", "passwordHash", "accessToken");

            String teacher = get("/api/v1/profile", login("demo_teacher", "TEACHER")).body();
            assertThat(teacher).contains("demo_teacher", "TEACHER", "演示教师", "DEMO_T001", "物理", "化学", "生物");
            assertThat(JsonPath.<List<?>>read(teacher, "$.teacherProfile.teachingScopes")).hasSize(3);

            String admin = get("/api/v1/profile", login("demo_admin", "ADMIN")).body();
            assertThat(admin).contains("demo_admin", "ADMIN", "\"studentProfile\":null", "\"teacherProfile\":null");

            String multi = get("/api/v1/profile", login("demo_physics_admin", "ADMIN")).body();
            assertThat(JsonPath.<List<String>>read(multi, "$.account.roles"))
                    .containsExactlyInAnyOrder("ADMIN", "TEACHER");
            assertThat(multi).contains("DEMO_T_PHYSICS", "物理管理员教师", "199班", "200班");

            assertThat(get("/api/v1/profile", null).statusCode()).isEqualTo(401);
        } finally {
            demo.clean();
        }
    }

    @Test
    void introductionCanBeTrimmedClearedAndCannotMutateAccountFacts() throws Exception {
        demo.seed();
        try {
            String token = login("demo_student", "STUDENT");
            HttpResponse<String> updated = put("/api/v1/profile", """
                    {"introduction":"  喜欢用受力图检查思路。  ","username":"changed","roles":["ADMIN"]}
                    """, token);
            assertThat(updated.statusCode()).isEqualTo(200);
            assertThat(updated.body()).contains("喜欢用受力图检查思路。", "demo_student", "STUDENT")
                    .doesNotContain("changed");
            assertThat(jdbc.queryForObject("SELECT ge_ren_jian_jie FROM yong_hu WHERE yong_hu_ming='demo_student'", String.class))
                    .isEqualTo("喜欢用受力图检查思路。");

            HttpResponse<String> cleared = put("/api/v1/profile", "{\"introduction\":\"   \"}", token);
            assertThat(cleared.statusCode()).isEqualTo(200);
            assertThat((Object) JsonPath.read(cleared.body(), "$.personal.introduction")).isNull();

            HttpResponse<String> tooLong = put("/api/v1/profile",
                    "{\"introduction\":\"" + "简".repeat(501) + "\"}", token);
            assertThat(tooLong.statusCode()).isEqualTo(400);
            assertThat(tooLong.body()).contains("个人简介不能超过500字");
        } finally {
            demo.clean();
        }
    }

    @Test
    void pngAndJpegAvatarPersistDeleteAndStayIsolated() throws Exception {
        demo.seed();
        try {
            String studentToken = login("demo_student", "STUDENT");
            String otherToken = login("demo_199_01", "STUDENT");

            byte[] png = image("png");
            HttpResponse<String> pngResponse = upload(png, "avatar.png", "image/png", studentToken);
            assertThat(pngResponse.statusCode()).isEqualTo(200);
            assertThat(pngResponse.body()).contains("data:image/png;base64,");
            assertThat(get("/api/v1/profile", studentToken).body()).contains("data:image/png;base64,");
            assertThat(get("/api/v1/profile", otherToken).body()).contains("\"avatarDataUrl\":null");

            byte[] jpeg = image("jpeg");
            HttpResponse<String> jpegResponse = upload(jpeg, "avatar.jpg", "image/jpeg", studentToken);
            assertThat(jpegResponse.statusCode()).isEqualTo(200);
            assertThat(jpegResponse.body()).contains("data:image/jpeg;base64,");

            HttpResponse<String> deleted = delete("/api/v1/profile/avatar", studentToken);
            assertThat(deleted.statusCode()).isEqualTo(200);
            assertThat(deleted.body()).contains("\"avatarDataUrl\":null");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming='demo_student' AND tou_xiang IS NULL", Integer.class))
                    .isEqualTo(1);
        } finally {
            demo.clean();
        }
    }

    @Test
    void invalidMimeContentAndOversizedAvatarAreRejected() throws Exception {
        demo.seed();
        try {
            String token = login("demo_admin", "ADMIN");
            assertThat(upload(image("png"), "avatar.txt", "text/plain", token).body())
                    .contains("AVATAR_TYPE_INVALID", "头像仅支持PNG或JPEG图片");
            assertThat(upload("not-an-image".getBytes(StandardCharsets.UTF_8), "avatar.png", "image/png", token).body())
                    .contains("AVATAR_CONTENT_INVALID", "不是有效的PNG或JPEG图片");
            assertThat(upload(image("png"), "avatar.jpg", "image/jpeg", token).body())
                    .contains("AVATAR_CONTENT_INVALID");

            byte[] tooLarge = new byte[2 * 1024 * 1024 + 1];
            tooLarge[0] = (byte) 0x89;
            HttpResponse<String> oversized = upload(tooLarge, "avatar.png", "image/png", token);
            assertThat(oversized.statusCode()).isEqualTo(400);
            assertThat(oversized.body()).contains("AVATAR_TOO_LARGE", "头像文件不能超过2MB");
        } finally {
            demo.clean();
        }
    }

    private String login(String username, String role) throws Exception {
        HttpResponse<String> challenge = get("/api/v1/auth/captcha-challenge", null);
        String body = """
                {"username":"%s","password":"a1234567","expectedRole":"%s","challengeId":"%s","captchaCode":"%s"}
                """.formatted(username, role, JsonPath.read(challenge.body(), "$.challengeId"),
                JsonPath.read(challenge.body(), "$.testCode"));
        HttpResponse<String> response = post("/api/v1/auth/login", body, null);
        assertThat(response.statusCode()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.accessToken");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).GET();
        authorize(builder, token);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        authorize(builder, token);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));
        authorize(builder, token);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).DELETE();
        authorize(builder, token);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> upload(byte[] bytes, String filename, String mime, String token) throws Exception {
        String boundary = "----profile" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"\r\nContent-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri("/api/v1/profile/avatar"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        authorize(builder, token);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(31, 95, 159));
        graphics.fillRect(0, 0, 8, 8);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void authorize(HttpRequest.Builder builder, String token) {
        if (token != null) builder.header("Authorization", "Bearer " + token);
    }
}
