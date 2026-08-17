package com.neu.riketiku.ai.vision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class XaiVisionProviderTest {
    private final ObjectMapper mapper=new ObjectMapper();private HttpServer server;private JsonNode request;
    @AfterEach void stop(){if(server!=null)server.stop(0);}

    @Test void usesOfficialResponsesShapeWithoutProviderStorageAndParsesUnifiedSchema()throws Exception{
        start(200,"{\"model\":\"grok-4.5\",\"output_text\":\"{\\\"diagramType\\\":\\\"FORCE\\\",\\\"summary\\\":\\\"受力三角形\\\",\\\"visibleText\\\":[],\\\"relations\\\":[],\\\"uncertainty\\\":[]}\",\"usage\":{\"input_tokens\":11,\"output_tokens\":7,\"total_tokens\":18}}");
        var result=provider("grok-4.5").analyze(new AiVisionRequest(9,List.of(new AiVisionRequest.Image("sha","image/png",new byte[]{1,2,3})),"TEST"));
        assertThat(result.context().diagramType()).isEqualTo("FORCE");assertThat(result.usage().totalTokens()).isEqualTo(18);
        assertThat(request.path("store").asBoolean()).isFalse();assertThat(request.path("input").path(0).path("content").path(0).path("type").asText()).isEqualTo("input_image");
        assertThat(request.path("input").path(0).path("content").path(0).path("image_url").asText()).startsWith("data:image/png;base64,");
    }

    @Test void separatesAuthenticationRateLimitAndInvalidResponse()throws Exception{
        start(401,"{}");assertFailure(AiProviderErrorType.AUTHENTICATION_ERROR);stop();
        start(429,"{}");assertFailure(AiProviderErrorType.RATE_LIMITED);stop();
        start(200,"{\"output_text\":\"not-json\"}");assertFailure(AiProviderErrorType.INVALID_RESPONSE);
    }
    @Test void rejectsNonGrokModelBeforeNetwork(){assertThatThrownBy(()->provider("vision-latest").analyze(new AiVisionRequest(1,List.of(new AiVisionRequest.Image("h","image/png",new byte[]{1})),"TEST"))).isInstanceOfSatisfying(AiVisionException.class,e->assertThat(e.errorType()).isEqualTo(AiProviderErrorType.CONFIGURATION_ERROR));}
    private void assertFailure(AiProviderErrorType type){assertThatThrownBy(()->provider("grok-4.5").analyze(new AiVisionRequest(1,List.of(new AiVisionRequest.Image("h","image/png",new byte[]{1})),"TEST"))).isInstanceOfSatisfying(AiVisionException.class,e->assertThat(e.errorType()).isEqualTo(type));}
    private XaiVisionProvider provider(String model){int port=server==null?1:server.getAddress().getPort();return new XaiVisionProvider(new AiRuntimeConfig(null,"xai",model,"http://127.0.0.1:"+port,"test-key","VISION",true,512,Duration.ofSeconds(2),0,false),HttpClient.newHttpClient(),mapper);}
    private void start(int status,String body)throws Exception{server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/responses",exchange->reply(exchange,status,body));server.start();}
    private void reply(HttpExchange exchange,int status,String body){try{request=mapper.readTree(exchange.getRequestBody());byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}catch(Exception ignored){}finally{exchange.close();}}
}
