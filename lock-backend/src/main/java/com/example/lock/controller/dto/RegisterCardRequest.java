package com.example.lock.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class RegisterCardRequest {
    @NotBlank
    private String uid;
    @NotNull
    private Long userId;
    private LocalDate expiresAt;

    /**
     * SỬA GỐC RỄ: trước đây đăng ký thẻ và cấp quyền phòng là 2 API tách rời
     * — rất dễ quên bước cấp quyền (nguyên nhân trực tiếp gây whitelist offline
     * rỗng dù thẻ đã đăng ký đầy đủ, từng xảy ra thật). Giờ gộp làm 1: truyền
     * sẵn danh sách phòng muốn cấp NGAY LÚC đăng ký thẻ. Để trống nếu chưa
     * muốn cấp quyền phòng nào (vẫn có thể cấp sau qua API riêng).
     */
    private List<Long> roomIds;
}
