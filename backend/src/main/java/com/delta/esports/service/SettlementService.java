package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.PageResult;
import com.delta.esports.entity.Settlement;
import com.delta.esports.mapper.SettlementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    @Autowired
    private SettlementMapper settlementMapper;

    public PageResult<Settlement> page(int page, int size) {
        LambdaQueryWrapper<Settlement> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Settlement::getCreatedAt);
        return PageResult.of(settlementMapper.selectPage(new Page<>(page, size), qw));
    }

    @Transactional
    public void updateStatus(Long id, String status, String remark) {
        Settlement s = new Settlement();
        s.setId(id);
        s.setStatus(status);
        s.setRemark(remark);
        settlementMapper.updateById(s);
    }
}
