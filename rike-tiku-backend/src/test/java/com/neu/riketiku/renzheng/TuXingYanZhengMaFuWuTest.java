package com.neu.riketiku.renzheng;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TuXingYanZhengMaFuWuTest {
    @Test
    void formalModeShouldNotExposeCaptchaCode() {
        TuXingYanZhengMaFuWu service = new TuXingYanZhengMaFuWu(false);

        var challenge = service.create(null);

        assertThat(challenge.challengeId()).isNotBlank();
        assertThat(challenge.image()).startsWith("data:image/png;base64,");
        assertThat(challenge.testCode()).isNull();
    }
}
