package com.capstone.smart_parcel.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "event_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventImage {
    @Id
    private UUID eventId;

    private String contentType;

    private byte[] content;

}
