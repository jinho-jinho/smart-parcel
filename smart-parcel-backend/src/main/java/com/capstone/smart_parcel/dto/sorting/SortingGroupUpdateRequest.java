package com.capstone.smart_parcel.dto.sorting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortingGroupUpdateRequest {

    @NotBlank(message = "그룹명을 입력해 주세요.")
    @Size(max = 50, message = "그룹명은 50자 이내여야 합니다.")
    private String groupName;
}
