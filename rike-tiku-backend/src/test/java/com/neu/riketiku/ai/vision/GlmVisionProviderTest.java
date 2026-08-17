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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GlmVisionProviderTest {
    private HttpServer server; private final ObjectMapper mapper=new ObjectMapper(); private final AtomicInteger calls=new AtomicInteger(); private JsonNode body;
    @AfterEach void stop(){if(server!=null)server.stop(0);}
    @Test void mapsStrictVisionJsonTokensAndRawBase64()throws Exception{
        start(200,success(),0);var result=provider(1,Duration.ofSeconds(1)).analyze(request(2));
        assertThat(result.model()).isEqualTo("glm-4.6v-flash");assertThat(result.context().summary()).isEqualTo("电路图");assertThat(result.usage().totalTokens()).isEqualTo(15);
        assertThat(body.path("thinking").path("type").asText()).isEqualTo("disabled");assertThat(body.path("max_tokens").asInt()).isEqualTo(1000);
        assertThat(body.path("messages").path(0).path("content").path(0).path("image_url").path("url").asText()).isEqualTo("AQID");
    }
    @Test void retries429AndServerFailureOnlyOnce()throws Exception{startSequence();assertThat(provider(99,Duration.ofSeconds(1)).analyze(request(1)).context().diagramType()).isEqualTo("CIRCUIT");assertThat(calls).hasValue(2);}
    @Test void doesNotRetryAuthenticationOrInvalidJson()throws Exception{start(401,"{}",0);assertThatThrownBy(()->provider(1,Duration.ofSeconds(1)).analyze(request(1))).isInstanceOfSatisfying(AiVisionException.class,e->assertThat(e.errorType()).isEqualTo(AiProviderErrorType.AUTHENTICATION_ERROR));assertThat(calls).hasValue(1);stop();calls.set(0);start(200,"{bad",0);assertThatThrownBy(()->provider(1,Duration.ofSeconds(1)).analyze(request(1))).isInstanceOfSatisfying(AiVisionException.class,e->assertThat(e.errorType()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE));assertThat(calls).hasValue(1);}
    @Test void timesOutAtMostOnceAndRejectsMissingKeyWithoutNetwork()throws Exception{start(200,success(),150);assertThatThrownBy(()->provider(1,Duration.ofMillis(30)).analyze(request(1))).isInstanceOfSatisfying(AiVisionException.class,e->assertThat(e.errorType()).isEqualTo(AiProviderErrorType.TIMEOUT));assertThat(calls.get()).isBetween(1,2);AiRuntimeConfig missing=new AiRuntimeConfig(null,"glm","glm-4.6v-flash",base(),"","VISION",true,1000,Duration.ofSeconds(1),1,false);assertThatThrownBy(()->new GlmVisionProvider(missing,HttpClient.newHttpClient(),mapper).analyze(request(1))).isInstanceOfSatisfying(AiVisionException.class,e->assertThat(e.errorType()).isEqualTo(AiProviderErrorType.CONFIGURATION_ERROR));}
    @Test void enforcesImageCountMimeAndSize(){assertThatThrownBy(()->new AiVisionRequest(1,List.of(),"X")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new AiVisionRequest.Image("h","image/gif",new byte[]{1})).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->request(3)).isInstanceOf(IllegalArgumentException.class);}
    private AiVisionRequest request(int count){return new AiVisionRequest(1,java.util.stream.IntStream.range(0,count).mapToObj(i->new AiVisionRequest.Image("h"+i,"image/png",new byte[]{1,2,3})).toList(),"QUESTION_VISION_CONTEXT");}
    private GlmVisionProvider provider(int retry,Duration timeout){return new GlmVisionProvider(new AiRuntimeConfig(null,"glm","glm-4.6v-flash",base(),"test-key","VISION",true,1000,timeout,retry,false),HttpClient.newBuilder().connectTimeout(Duration.ofMillis(100)).build(),mapper);}
    private String base(){return "http://127.0.0.1:"+(server==null?1:server.getAddress().getPort());}
    private void start(int status,String response,long delay)throws Exception{server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(Executors.newCachedThreadPool());server.createContext("/chat/completions",exchange->handle(exchange,status,response,delay));server.start();}
    private void startSequence()throws Exception{server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(Executors.newCachedThreadPool());server.createContext("/chat/completions",exchange->{int n=calls.incrementAndGet();reply(exchange,n==1?429:200,n==1?"{}":success(),0);});server.start();}
    private void handle(HttpExchange exchange,int status,String response,long delay){calls.incrementAndGet();try{body=mapper.readTree(exchange.getRequestBody());reply(exchange,status,response,delay);}catch(Exception e){exchange.close();}}
    private void reply(HttpExchange exchange,int status,String response,long delay){try{if(delay>0)Thread.sleep(delay);byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);}catch(Exception ignored){}finally{exchange.close();}}
    private String success(){return "{\"model\":\"glm-4.6v-flash\",\"choices\":[{\"message\":{\"content\":\"{\\\"diagramType\\\":\\\"CIRCUIT\\\",\\\"summary\\\":\\\"电路图\\\",\\\"visibleText\\\":[],\\\"relations\\\":[],\\\"uncertainty\\\":[]}\"}}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}";}
}
