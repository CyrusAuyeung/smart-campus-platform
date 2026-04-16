package com.unikorn.campus;

import static org.assertj.core.api.Assertions.assertThat;

import com.unikorn.campus.common.SystemController;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SystemControllerTest {

    @Test
    void shouldReturnHealthyPayload() {
        SystemController controller = new SystemController();
        Map<String, Object> payload = controller.health();

        assertThat(payload.get("status")).isEqualTo("ok");
        assertThat(payload.get("service")).isEqualTo("campus-platform-api");
        assertThat(payload).containsKey("time");
    }
}
