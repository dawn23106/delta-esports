package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.common.PageResult;
import com.delta.esports.common.PageSupport;
import com.delta.esports.entity.Announcement;
import com.delta.esports.mapper.AnnouncementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    public PageResult<Announcement> page(int page, int size) {
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Announcement::getSortOrder).orderByDesc(Announcement::getCreatedAt);
        return PageResult.of(announcementMapper.selectPage(PageSupport.of(page, size), qw));
    }

    @Transactional
    public void create(Announcement a) { announcementMapper.insert(a); }

    @Transactional
    public void update(Announcement a) { announcementMapper.updateById(a); }

    @Transactional
    public void delete(Long id) { announcementMapper.deleteById(id); }
}
