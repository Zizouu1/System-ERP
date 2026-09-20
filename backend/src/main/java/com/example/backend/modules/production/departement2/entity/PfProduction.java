package com.example.backend.modules.production.departement2.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.backend.modules.production.shared.entity.Production;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "pf_production")
public class PfProduction extends Production {

    protected PfProduction() {
        super();
    }

    @Transient
    @JsonProperty("startDateTime")
    public LocalDateTime getStartDateTime() {
        if (getStartTime() == null) {
            return null;
        }
        LocalDateTime base = getCreatedAt() != null ? getCreatedAt() : getLastModifiedAt();
        return base != null ? base.toLocalDate().atTime(getStartTime()) : null;
    }

    @Transient
    @JsonProperty("endDateTime")
    public LocalDateTime getEndDateTime() {
        if (getEndTime() == null) {
            return null;
        }
        LocalDateTime base = getCreatedAt() != null ? getCreatedAt() : getLastModifiedAt();
        return base != null ? base.toLocalDate().atTime(getEndTime()) : null;
    }
}
