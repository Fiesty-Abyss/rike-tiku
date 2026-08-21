package com.neu.riketiku.sixin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.demo.DemoDataService;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SiXinIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired private DemoDataService demo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @LocalServerPort private int port;

    @Test
    void twoWayMessagingUnreadReadAndParticipantIsolationWork() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long physics199 = scope("DEMO_T_PHYSICS", "DEMO_CLASS_199", 1);
            long physics200 = scope("DEMO_T_PHYSICS", "DEMO_CLASS_200", 1);
            long student199 = student("DEMO_199_02");
            long student200 = student("DEMO_200_01");
            String studentToken = login("demo_199_02", "STUDENT");
            String otherStudentToken = login("demo_200_01", "STUDENT");
            String teacherToken = login("demo_physics_admin", "TEACHER");
            String adminToken = login("demo_admin", "ADMIN");

            HttpResponse<String> studentContacts = get("/api/v1/messages/contacts", studentToken);
            assertThat(studentContacts.statusCode()).isEqualTo(200);
            assertThat(studentContacts.body()).contains("物理管理员教师", "生物教师", "化学教师", "199班")
                    .doesNotContain("200班");
            HttpResponse<String> teacherContacts = get("/api/v1/messages/contacts", teacherToken);
            assertThat(teacherContacts.body()).contains("199班学生01", "200班学生01");

            HttpResponse<String> created = post("/api/v1/messages/conversations", studentToken,
                    "{\"teachingAssignmentId\":" + physics199 + "}");
            assertThat(created.statusCode()).isEqualTo(200);
            long conversationId = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
            assertThat(((Number) JsonPath.read(post("/api/v1/messages/conversations", studentToken,
                    "{\"teachingAssignmentId\":" + physics199 + "}").body(), "$.id")).longValue())
                    .isEqualTo(conversationId);

            assertThat(post("/api/v1/messages/conversations", studentToken,
                    "{\"teachingAssignmentId\":" + physics200 + "}").statusCode()).isEqualTo(403);
            assertThat(post("/api/v1/messages/conversations", teacherToken,
                    "{\"teachingAssignmentId\":" + physics199 + ",\"studentId\":" + student200 + "}").statusCode())
                    .isEqualTo(403);

            HttpResponse<String> sent = post("/api/v1/messages/conversations/" + conversationId + "/messages", studentToken,
                    "{\"content\":\"老师，这道题我想再确认一下。\",\"senderUserId\":-1}");
            assertThat(sent.statusCode()).isEqualTo(200);
            assertThat(sent.body()).contains("老师，这道题我想再确认一下。", "\"mine\":true");
            assertThat(get("/api/v1/messages/conversations", teacherToken).body()).contains("\"unreadCount\":1");
            assertThat(get("/api/v1/messages/conversations/" + conversationId + "/messages", otherStudentToken).statusCode())
                    .isEqualTo(403);
            assertThat(get("/api/v1/messages/conversations/" + conversationId + "/messages", adminToken).statusCode())
                    .isEqualTo(403);

            assertThat(post("/api/v1/messages/conversations/" + conversationId + "/read", teacherToken, "{}").body())
                    .contains("\"readCount\":1");
            assertThat(get("/api/v1/messages/conversations", teacherToken).body()).contains("\"unreadCount\":0");
            assertThat(post("/api/v1/messages/conversations/" + conversationId + "/messages", teacherToken,
                    "{\"content\":\"可以，请把受力对象和方向再列一遍。\"}").statusCode()).isEqualTo(200);
            String history = get("/api/v1/messages/conversations/" + conversationId + "/messages", studentToken).body();
            assertThat(history).containsSubsequence("老师，这道题我想再确认一下。", "可以，请把受力对象和方向再列一遍。");
            assertThat(history).contains("\"unreadCount\":1");

            assertThat(post("/api/v1/messages/conversations/" + conversationId + "/messages", studentToken,
                    "{\"content\":\"   \"}").statusCode()).isEqualTo(400);
            assertThat(post("/api/v1/messages/conversations/" + conversationId + "/messages", studentToken,
                    "{\"content\":\"" + "长".repeat(1001) + "\"}").statusCode()).isEqualTo(400);
            assertThat(student199).isPositive();
        } finally {
            demo.clean();
        }
    }

    @Test
    void inactiveTeachingAndStudentTransferKeepHistoryButBlockSending() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long physics199 = scope("DEMO_T_PHYSICS", "DEMO_CLASS_199", 1);
            long chemistry200 = scope("DEMO_T_CHEMISTRY", "DEMO_CLASS_200", 2);
            long studentId = student("DEMO_199_02");
            String studentToken = login("demo_199_02", "STUDENT");
            String teacherToken = login("demo_physics_admin", "TEACHER");
            long conversationId = ((Number) JsonPath.read(post("/api/v1/messages/conversations", studentToken,
                    "{\"teachingAssignmentId\":" + physics199 + "}").body(), "$.id")).longValue();
            assertThat(post("/api/v1/messages/conversations/" + conversationId + "/messages", studentToken,
                    "{\"content\":\"历史消息\"}").statusCode()).isEqualTo(200);

            jdbc.update("UPDATE ren_ke_guan_xi SET zhuang_tai='DISABLED' WHERE id=?", physics199);
            assertThat(post("/api/v1/messages/conversations/" + conversationId + "/messages", teacherToken,
                    "{\"content\":\"停课后消息\"}").statusCode()).isEqualTo(409);
            assertThat(get("/api/v1/messages/conversations/" + conversationId + "/messages", teacherToken).body())
                    .contains("历史消息", "\"canSend\":false");
            jdbc.update("UPDATE ren_ke_guan_xi SET zhuang_tai='ACTIVE' WHERE id=?", physics199);

            jdbc.update("""
                    UPDATE ban_ji_xue_sheng SET zhuang_tai='EXITED',tui_chu_shi_jian=GREATEST(CURRENT_TIMESTAMP(3),jia_ru_shi_jian)
                    WHERE xue_sheng_id=? AND zhuang_tai='ACTIVE' AND shi_fou_zhu_ban_ji=1
                    """, studentId);
            long class200 = jdbc.queryForObject("SELECT id FROM ban_ji WHERE ban_ji_bian_ma='DEMO_CLASS_200'", Long.class);
            jdbc.update("INSERT INTO ban_ji_xue_sheng(ban_ji_id,xue_sheng_id,shi_fou_zhu_ban_ji,zhuang_tai) VALUES (?,?,1,'ACTIVE')",
                    class200, studentId);
            assertThat(post("/api/v1/messages/conversations/" + conversationId + "/messages", studentToken,
                    "{\"content\":\"调班后旧会话\"}").statusCode()).isEqualTo(409);
            assertThat(get("/api/v1/messages/conversations/" + conversationId + "/messages", studentToken).body())
                    .contains("历史消息");
            assertThat(post("/api/v1/messages/conversations", studentToken,
                    "{\"teachingAssignmentId\":" + chemistry200 + "}").statusCode()).isEqualTo(200);
        } finally {
            demo.clean();
        }
    }

    @Test
    void senderCanRecallWithinFiveMinutesAndEachParticipantCanHideOnlyForSelf() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long physics199=scope("DEMO_T_PHYSICS","DEMO_CLASS_199",1);
            String studentToken=login("demo_199_01","STUDENT");String teacherToken=login("demo_physics_admin","TEACHER");
            long conversationId=((Number)JsonPath.read(post("/api/v1/messages/conversations",studentToken,"{\"teachingAssignmentId\":"+physics199+"}").body(),"$.id")).longValue();
            long first=((Number)JsonPath.read(post("/api/v1/messages/conversations/"+conversationId+"/messages",studentToken,"{\"content\":\"需要撤回的消息\"}").body(),"$.id")).longValue();
            assertThat(post("/api/v1/messages/"+conversationId+"/messages/"+first+"/recall",studentToken,"{}").body()).contains("消息已撤回","\"recalled\":true");
            assertThat(post("/api/v1/messages/"+conversationId+"/messages/"+first+"/recall",studentToken,"{}").statusCode()).isEqualTo(409);
            assertThat(get("/api/v1/messages/conversations/"+conversationId+"/messages",teacherToken).body()).contains("消息已撤回").doesNotContain("需要撤回的消息");
            long second=((Number)JsonPath.read(post("/api/v1/messages/conversations/"+conversationId+"/messages",studentToken,"{\"content\":\"只对老师隐藏\"}").body(),"$.id")).longValue();
            assertThat(delete("/api/v1/messages/"+conversationId+"/messages/"+second,teacherToken).statusCode()).isEqualTo(200);
            assertThat(get("/api/v1/messages/conversations/"+conversationId+"/messages",teacherToken).body()).doesNotContain("只对老师隐藏");
            assertThat(get("/api/v1/messages/conversations/"+conversationId+"/messages",studentToken).body()).contains("只对老师隐藏");
            long expired=((Number)JsonPath.read(post("/api/v1/messages/conversations/"+conversationId+"/messages",studentToken,"{\"content\":\"过期消息\"}").body(),"$.id")).longValue();
            jdbc.update("UPDATE si_xin_xiao_xi SET fa_song_shi_jian=DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 6 MINUTE) WHERE id=?",expired);
            assertThat(post("/api/v1/messages/"+conversationId+"/messages/"+expired+"/recall",studentToken,"{}").statusCode()).isEqualTo(409);
        } finally { demo.clean(); }
    }

    private long scope(String teacherNumber, String classCode, long subjectId) {
        return jdbc.queryForObject("""
                SELECT r.id FROM ren_ke_guan_xi r
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id JOIN ban_ji b ON b.id=r.ban_ji_id
                WHERE t.gong_hao=? AND b.ban_ji_bian_ma=? AND r.ke_mu_id=?
                """, Long.class, teacherNumber, classCode, subjectId);
    }

    private long student(String studentNumber) {
        return jdbc.queryForObject("SELECT id FROM xue_sheng_dang_an WHERE xue_hao=?", Long.class, studentNumber);
    }

    private String login(String username, String role) throws Exception {
        HttpResponse<String> challenge = get("/api/v1/auth/captcha-challenge", null);
        String challengeId = JsonPath.read(challenge.body(), "$.challengeId");
        String captchaCode = JsonPath.read(challenge.body(), "$.testCode");
        HttpResponse<String> response = post("/api/v1/auth/login", null, "{\"username\":\"" + username
                + "\",\"password\":\"MessagesPass1\",\"expectedRole\":\"" + role + "\",\"challengeId\":\""
                + challengeId + "\",\"captchaCode\":\"" + captchaCode + "\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.accessToken");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private void useNonDefaultPasswords(){ for(String username: new String[]{"demo_199_01","demo_199_02","demo_200_01","demo_physics_admin","demo_admin"}) jdbc.update("UPDATE yong_hu SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=0 WHERE yong_hu_ming=?",passwordEncoder.encode("MessagesPass1"),username); }
    private HttpResponse<String> delete(String path,String token)throws Exception{HttpRequest.Builder request=HttpRequest.newBuilder().uri(URI.create("http://localhost:"+port+path)).DELETE();if(token!=null)request.header("Authorization","Bearer "+token);return http.send(request.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));}
}
