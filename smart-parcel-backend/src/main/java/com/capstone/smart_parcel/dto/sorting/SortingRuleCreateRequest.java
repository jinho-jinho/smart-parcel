package com.capstone.smart_parcel.dto.sorting;

import com.capstone.smart_parcel.domain.enums.InputType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SortingRuleCreateRequest {

    @NotBlank(message = "룰 이름을 입력해 주세요.")
    @Size(max = 50, message = "룰 이름은 50자 이내여야 합니다.")
    private String ruleName;

    @NotNull(message = "입력 타입을 선택해 주세요.")
    private InputType inputType;

    @NotBlank(message = "입력 값을 입력해 주세요.")
    @Size(max = 50, message = "입력 값은 50자 이내여야 합니다.")
    private String inputValue;

    @NotBlank(message = "품목명을 입력해 주세요.")
    @Size(max = 100, message = "품목명은 100자 이내여야 합니다.")
    private String itemName;

    private List<@NotNull(message = "슈트 ID는 null일 수 없습니다.") Long> chuteIds;
}
