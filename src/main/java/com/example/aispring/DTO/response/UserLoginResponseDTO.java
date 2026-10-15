package com.example.aispring.DTO.response;

import com.example.aispring.DTO.command.UserLoginCommandDTO;
import lombok.Data;

@Data
public class UserLoginResponseDTO {
    private String token;
    private String roletype;
    private UserDetailResponseDTO userinfo;
//    "id": 71,
//            "username": "string",
//            "email": "string",
//            "nickname": "string",
//            "phone": "string",
//            "gender": 0,
//            "genderDisplayName": "未知",
//            "userType": 1,
//            "userTypeDisplayName": "普通用户",
//            "status": 1,
//            "statusDisplayName": "正常",
//            "displayName": "string",
//            "createdAt": "2026-03-06 21:17:50",
//            "updatedAt": "2026-03-06 21:17:50"
    @Data
    public static class UserDetailResponseDTO {
        private long id;
        private String username;
        private String email;
        private String nickname;
        private String phone;
        private Integer gender;
        private String genderDisplayName;
        private Integer userType;
        private String userTypeDisplayName;
        private Integer status;
        private String statusDisplayName;
        private String displayName;
        private String createdAt;
        private String updatedAt;
    }
}
