package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.command.PackageCommandDTO;
import com.example.aispring.DTO.response.PackageResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.CounselingOrder;
import com.example.aispring.entity.CounselingPackage;
import com.example.aispring.enumClass.PackageStatus;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CounselingOrderMapper;
import com.example.aispring.mapper.CounselingPackageMapper;
import com.example.aispring.service.convert.CounselingConvert;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CounselingPackageService {

    @Resource
    private CounselingPackageMapper packageMapper;

    @Resource
    private CounselingOrderMapper orderMapper;

    public void create(PackageCommandDTO dto, Long adminId) {
        LocalDateTime now = LocalDateTime.now();
        packageMapper.insert(CounselingPackage.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .durationMinutes(dto.getDurationMinutes())
                .price(dto.getPrice())
                .status(dto.getStatus() == null ? PackageStatus.ON.getCode() : dto.getStatus())
                .createdBy(adminId)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    public void update(Long id, PackageCommandDTO dto) {
        CounselingPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException("套餐不存在");
        }
        packageMapper.update(null, new LambdaUpdateWrapper<CounselingPackage>()
                .eq(CounselingPackage::getId, id)
                .set(CounselingPackage::getName, dto.getName())
                .set(CounselingPackage::getDescription, dto.getDescription())
                .set(CounselingPackage::getDurationMinutes, dto.getDurationMinutes())
                .set(CounselingPackage::getPrice, dto.getPrice())
                .set(dto.getStatus() != null, CounselingPackage::getStatus, dto.getStatus())
                .set(CounselingPackage::getUpdatedAt, LocalDateTime.now()));
    }

    public void delete(Long id) {
        CounselingPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException("套餐不存在");
        }
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getPackageId, id));
        if (orderCount != null && orderCount > 0) {
            throw new BusinessException("该套餐已产生订单，不能删除，可改为下架");
        }
        packageMapper.deleteById(id);
    }

    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != PackageStatus.ON.getCode() && status != PackageStatus.OFF.getCode())) {
            throw new BusinessException("套餐状态无效");
        }
        CounselingPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException("套餐不存在");
        }
        packageMapper.update(null, new LambdaUpdateWrapper<CounselingPackage>()
                .eq(CounselingPackage::getId, id)
                .set(CounselingPackage::getStatus, status)
                .set(CounselingPackage::getUpdatedAt, LocalDateTime.now()));
    }

    public PageResult<PackageResponseDTO> pageList(int page, int size, String name, Integer status) {
        Page<CounselingPackage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CounselingPackage> wrapper = new LambdaQueryWrapper<CounselingPackage>()
                .like(name != null && !name.isBlank(), CounselingPackage::getName, name)
                .eq(status != null, CounselingPackage::getStatus, status)
                .orderByDesc(CounselingPackage::getCreatedAt);
        Page<CounselingPackage> result = packageMapper.selectPage(pageParam, wrapper);
        List<PackageResponseDTO> records = result.getRecords().stream()
                .map(CounselingConvert::toPackageResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    public List<PackageResponseDTO> onSaleList() {
        return packageMapper.selectList(new LambdaQueryWrapper<CounselingPackage>()
                        .eq(CounselingPackage::getStatus, PackageStatus.ON.getCode())
                        .orderByAsc(CounselingPackage::getId))
                .stream()
                .map(CounselingConvert::toPackageResponse)
                .toList();
    }

    public CounselingPackage getOnSale(Long id) {
        CounselingPackage pkg = packageMapper.selectById(id);
        if (pkg == null || !PackageStatus.ON.getCode().equals(pkg.getStatus())) {
            throw new BusinessException("套餐不存在或已下架");
        }
        return pkg;
    }

    public CounselingPackage getById(Long id) {
        return packageMapper.selectById(id);
    }
}
