package com.unikorn.campus.booking.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking")
public class BookingPolicyProperties {

    private int academicBufferMinutes = 5;
    private int sportSlotMinutes = 60;
    private int paymentTimeoutMinutes = 15;

    public int getAcademicBufferMinutes() {
        return academicBufferMinutes;
    }

    public void setAcademicBufferMinutes(int academicBufferMinutes) {
        this.academicBufferMinutes = academicBufferMinutes;
    }

    public int getSportSlotMinutes() {
        return sportSlotMinutes;
    }

    public void setSportSlotMinutes(int sportSlotMinutes) {
        this.sportSlotMinutes = sportSlotMinutes;
    }

    public int getPaymentTimeoutMinutes() {
        return paymentTimeoutMinutes;
    }

    public void setPaymentTimeoutMinutes(int paymentTimeoutMinutes) {
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }
}
