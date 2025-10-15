package com.capstone.smart_parcel.dto.sorting;

import com.capstone.smart_parcel.domain.enums.InputType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SortingRuleUpdateRequest {

    @Size(max = 50, message = "룰 이름은 50자 이내여야 합니다.")
    private String ruleName;

    private InputType inputType;

    @Size(max = 50, message = "입력 값은 50자 이내여야 합니다.")
    private String inputValue;

    @Size(max = 100, message = "품목명은 100자 이내여야 합니다.")
    private String itemName;

    private List<Long> chuteIds;
}
