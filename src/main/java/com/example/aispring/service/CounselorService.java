package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.command.CounselorApplyCommandDTO;
import com.example.aispring.DTO.command.CounselorAuditCommandDTO;
import com.example.aispring.DTO.response.CounselorResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.Counselor;
import com.example.aispring.entity.User;
import com.example.aispring.enumClass.CounselorAuditStatus;
import com.example.aispring.enumClass.NotificationType;
import com.example.aispring.enumClass.UserType;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CounselorMapper;
import com.example.aispring.mapper.UserMapper;
import com.example.aispring.service.convert.CounselingConvert;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CounselorService {

    @Resource
    private CounselorMapper counselorMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SystemNotificationService notificationService;

    /**
     * 提交/重新提交咨询师申请（被拒绝后可修改并重新提交）
     */
    @Transactional
    public void apply(Long userId, CounselorApplyCommandDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        Counselor existing = counselorMapper.selectOne(new LambdaQueryWrapper<Counselor>()
                .eq(Counselor::getUserId, userId));

        if (existing != null) {
            if (CounselorAuditStatus.APPROVED.getCode().equals(existing.getAuditStatus())) {
                throw new BusinessException("您已经是咨询师");
            }
            if (CounselorAuditStatus.PENDING.getCode().equals(existing.getAuditStatus())) {
                throw new BusinessException("您的申请正在审核中，请勿重复提交");
            }
            counselorMapper.update(null, new LambdaUpdateWrapper<Counselor>()
                    .eq(Counselor::getId, existing.getId())
                    .set(Counselor::getRealName, dto.getRealName())
                    .set(Counselor::getGender, dto.getGender())
                    .set(Counselor::getPhone, dto.getPhone())
                    .set(Counselor::getEmail, dto.getEmail())
                    .set(Counselor::getQualificationNo, dto.getQualificationNo())
                    .set(Counselor::getCertificationFileUrl, dto.getCertificationFileUrl())
                    .set(Counselor::getSpecialty, dto.getSpecialty())
                    .set(Counselor::getIntroduction, dto.getIntroduction())
                    .set(Counselor::getYearsOfExperience, dto.getYearsOfExperience())
                    .set(Counselor::getAuditStatus, CounselorAuditStatus.PENDING.getCode())
                    .set(Counselor::getAuditRemark, null)
                    .set(Counselor::getAuditedBy, null)
                    .set(Counselor::getAuditedAt, null)
                    .set(Counselor::getStatus, 1)
                    .set(Counselor::getUpdatedAt, now));
            return;
        }

        counselorMapper.insert(Counselor.builder()
                .userId(userId)
                .realName(dto.getRealName())
                .gender(dto.getGender())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .qualificationNo(dto.getQualificationNo())
                .certificationFileUrl(dto.getCertificationFileUrl())
                .specialty(dto.getSpecialty())
                .introduction(dto.getIntroduction())
                .yearsOfExperience(dto.getYearsOfExperience())
                .auditStatus(CounselorAuditStatus.PENDING.getCode())
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    /**
     * 管理员审核：通过时同步把 user_type 置为咨询师
     */
    @Transactional
    public void audit(Long adminId, CounselorAuditCommandDTO dto) {
        Counselor counselor = counselorMapper.selectById(dto.getCounselorId());
        if (counselor == null) {
            throw new BusinessException("咨询师资料不存在");
        }
        if (!CounselorAuditStatus.PENDING.getCode().equals(counselor.getAuditStatus())) {
            throw new BusinessException("该申请已审核，请勿重复操作");
        }
        boolean approve = CounselorAuditStatus.APPROVED.getCode().equals(dto.getAuditStatus());
        if (!approve && !CounselorAuditStatus.REJECTED.getCode().equals(dto.getAuditStatus())) {
            throw new BusinessException("审核结果无效");
        }

        LocalDateTime now = LocalDateTime.now();
        int rows = counselorMapper.update(null, new LambdaUpdateWrapper<Counselor>()
                .eq(Counselor::getId, counselor.getId())
                .eq(Counselor::getAuditStatus, CounselorAuditStatus.PENDING.getCode())
                .set(Counselor::getAuditStatus, dto.getAuditStatus())
                .set(Counselor::getAuditRemark, dto.getAuditRemark())
                .set(Counselor::getAuditedBy, adminId)
                .set(Counselor::getAuditedAt, now)
                .set(Counselor::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("该申请已被其他管理员处理");
        }

        if (approve) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, counselor.getUserId())
                    .set(User::getUserType, UserType.COUNSELOR.getCode())
                    .set(User::getUpdatedAt, now));
            notificationService.create(NotificationType.AUDIT.getCode(), counselor.getUserId(),
                    "咨询师申请已通过",
                    "您的咨询师申请已通过审核，请重新登录以获取咨询师权限。", null);
        } else {
            String remark = dto.getAuditRemark() == null ? "未提供" : dto.getAuditRemark();
            notificationService.create(NotificationType.AUDIT.getCode(), counselor.getUserId(),
                    "咨询师申请未通过",
                    "您的咨询师申请未通过审核，原因：" + remark + "。可修改资料后重新提交。", null);
        }
    }

    public CounselorResponseDTO getByUserId(Long userId) {
        Counselor counselor = counselorMapper.selectOne(new LambdaQueryWrapper<Counselor>()
                .eq(Counselor::getUserId, userId));
        return CounselingConvert.toCounselorResponse(counselor);
    }

    public PageResult<CounselorResponseDTO> pageList(int page, int size, Integer auditStatus) {
        Page<Counselor> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Counselor> wrapper = new LambdaQueryWrapper<Counselor>()
                .eq(auditStatus != null, Counselor::getAuditStatus, auditStatus)
                .orderByDesc(Counselor::getCreatedAt);
        Page<Counselor> result = counselorMapper.selectPage(pageParam, wrapper);
        List<CounselorResponseDTO> records = result.getRecords().stream()
                .map(CounselingConvert::toCounselorResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    /**
     * 用户可预约的咨询师列表（已通过审核且启用）
     */
    public List<CounselorResponseDTO> enabledList() {
        return counselorMapper.selectList(new LambdaQueryWrapper<Counselor>()
                        .eq(Counselor::getAuditStatus, CounselorAuditStatus.APPROVED.getCode())
                        .eq(Counselor::getStatus, 1)
                        .orderByDesc(Counselor::getCreatedAt))
                .stream()
                .map(CounselingConvert::toCounselorResponse)
                .toList();
    }

    public Counselor getApprovedByUserId(Long userId) {
        return counselorMapper.selectOne(new LambdaQueryWrapper<Counselor>()
                .eq(Counselor::getUserId, userId)
                .eq(Counselor::getAuditStatus, CounselorAuditStatus.APPROVED.getCode())
                .eq(Counselor::getStatus, 1));
    }

    public Counselor getById(Long counselorId) {
        return counselorMapper.selectById(counselorId);
    }
}
