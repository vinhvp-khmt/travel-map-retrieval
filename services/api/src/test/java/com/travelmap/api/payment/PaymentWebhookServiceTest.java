package com.travelmap.api.payment;

import com.travelmap.api.auth.model.*;
import com.travelmap.api.booking.model.*;
import com.travelmap.api.payment.dto.PaymentWebhookRequest;
import com.travelmap.api.payment.model.*;
import com.travelmap.api.payment.repository.*;
import com.travelmap.api.payment.service.PaymentWebhookService;
import com.travelmap.api.poi.model.*;
import com.travelmap.api.common.ApiException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentWebhookServiceTest {
    @Test void validPaidWebhookConfirmsBookingExactlyOnce() {
        var payments=mock(PaymentRepository.class); var events=mock(PaymentWebhookEventRepository.class);
        PaymentEntity payment=payment(); var request=new PaymentWebhookRequest("evt-1",payment.getId(),"PAID");
        var service=new PaymentWebhookService(payments,events,"test-webhook-secret");
        when(events.existsByGatewayAndEventId("MOCK","evt-1")).thenReturn(false,true);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        String signature=service.signForTesting(request.canonicalPayload());
        assertTrue(service.process("mock",request,signature));
        assertEquals(PaymentStatus.PAID,payment.getStatus());
        assertEquals(BookingStatus.CONFIRMED,payment.getBooking().getStatus());
        assertFalse(service.process("mock",request,signature));
        verify(payments,times(1)).findById(payment.getId()); verify(events,times(1)).save(any());
    }
    @Test void invalidHmacDoesNotChangePayment() {
        var payments=mock(PaymentRepository.class); var events=mock(PaymentWebhookEventRepository.class);
        PaymentEntity payment=payment(); var service=new PaymentWebhookService(payments,events,"secret");
        var request=new PaymentWebhookRequest("evt-2",payment.getId(),"PAID");
        assertThrows(ApiException.class,()->service.process("mock",request,"bad"));
        assertEquals(PaymentStatus.CREATED,payment.getStatus()); verifyNoInteractions(payments,events);
    }
    private static PaymentEntity payment() {
        Instant now=Instant.now(); var user=new UserEntity("user@example.com","hash",UserRole.USER);
        var poi=new PoiEntity(new UserEntity("owner@example.com","hash",UserRole.OWNER),
                new CategoryEntity(UUID.randomUUID(),"Cafe","cafe"),"Cafe","cafe",null,10.77,106.70,"Q1",2,10,true); poi.approve();
        var booking=new BookingEntity(user,poi,now.plusSeconds(3600),2,null,new BigDecimal("40000"),now);
        return new PaymentEntity(booking,"MOCK","idem-123456",now);
    }
}
