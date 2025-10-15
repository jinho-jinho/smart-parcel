package com.capstone.smart_parcel.dto.sorting;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChuteCreateRequest {

    @NotBlank(message = "슈트 이름을 입력해 주세요.")
    @Size(max = 50, message = "슈트 이름은 50자 이내여야 합니다.")
    private String chuteName;

    @NotNull(message = "서보 각도를 입력해 주세요.")
    @Min(value = 0, message = "서보 각도는 0 이상이어야 합니다.")
    @Max(value = 180, message = "서보 각도는 180 이하이어야 합니다.")
    private Short servoDeg;
}
