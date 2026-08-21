package com.restconf.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class BenchmarkRunnerTest {

    @Test
    @DisplayName("Should execute benchmark runner without exceptions")
    void testBenchmarkExecution() {
        assertThatNoException().isThrownBy(() -> {
            BenchmarkRunner.main(new String[]{});
        });
    }
}
