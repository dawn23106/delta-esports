package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.entity.ServiceItem;
import com.delta.esports.mapper.ServiceItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceItemService {

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    public List<ServiceItem> findAll(String category) {
        LambdaQueryWrapper<ServiceItem> qw = new LambdaQueryWrapper<>();
        qw.eq(ServiceItem::getIsActive, 1);
        if (category != null && !category.isEmpty()) {
            qw.eq(ServiceItem::getCategory, category);
        }
        qw.orderByAsc(ServiceItem::getSortOrder);
        return serviceItemMapper.selectList(qw);
    }

    public ServiceItem findById(Long id) {
        return serviceItemMapper.selectById(id);
    }

    @Transactional
    public void create(ServiceItem item) {
        serviceItemMapper.insert(item);
    }

    @Transactional
    public void update(ServiceItem item) {
        ServiceItem exist = serviceItemMapper.selectById(item.getId());
        if (exist == null) throw new BusinessException("服务项目不存在");
        serviceItemMapper.updateById(item);
    }

    @Transactional
    public void toggleActive(Long id, boolean active) {
        ServiceItem item = new ServiceItem();
        item.setId(id);
        item.setIsActive(active ? 1 : 0);
        serviceItemMapper.updateById(item);
    }
}
