package com.example.todaydeparture.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDataServiceWeatherTest {

    @Test
    void convertsSeoulCoordinatesToKmaGrid() {
        assertThat(PublicDataService.convertToKmaGrid(37.5665, 126.9780))
                .containsExactly(60, 127);
    }

    @Test
    void convertsBusanCoordinatesToKmaGrid() {
        assertThat(PublicDataService.convertToKmaGrid(35.1796, 129.0756))
                .containsExactly(98, 76);
    }
}
