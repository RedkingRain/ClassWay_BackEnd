package com.greenproject.classway.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class WishId implements Serializable {
    private Integer userId;
    private Integer classId;
}
