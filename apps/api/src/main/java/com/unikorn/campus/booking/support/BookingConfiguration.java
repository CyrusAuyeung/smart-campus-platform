package com.unikorn.campus.booking.support;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BookingPolicyProperties.class)
public class BookingConfiguration {
}
