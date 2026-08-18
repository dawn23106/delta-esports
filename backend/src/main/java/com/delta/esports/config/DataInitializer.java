package com.delta.esports.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.entity.*;
import com.delta.esports.mapper.*;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;

/**
 * 开发环境数据初始化
 * 仅当数据库为空时插入默认数据
 * 所有用户密码均为 123456
 */
@Component
@Profile("dev")
@Order(1)
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserMapper userMapper;
    @Autowired private ServiceItemMapper serviceItemMapper;
    @Autowired private AnnouncementMapper announcementMapper;

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) return;

        String pwd = BCrypt.hashpw("123456", BCrypt.gensalt());

        // 管理员
        User admin = new User();
        admin.setPhone("13800000001");
        admin.setPassword(pwd);
        admin.setNickname("管理员");
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setBalance(BigDecimal.ZERO);
        admin.setRating(BigDecimal.valueOf(5.0));
        admin.setTotalOrders(0);
        admin.setIntroduction("系统管理员");
        userMapper.insert(admin);

        // 老板
        User boss1 = new User();
        boss1.setPhone("13900000001");
        boss1.setPassword(pwd);
        boss1.setNickname("战神阿瑞斯");
        boss1.setRole("boss");
        boss1.setStatus("active");
        boss1.setBalance(BigDecimal.valueOf(5000));
        boss1.setRating(BigDecimal.valueOf(5.0));
        boss1.setTotalOrders(12);
        boss1.setIntroduction("热爱游戏，寻找给力队友");
        userMapper.insert(boss1);

        User boss2 = new User();
        boss2.setPhone("13900000002");
        boss2.setPassword(pwd);
        boss2.setNickname("暗夜猎手");
        boss2.setRole("boss");
        boss2.setStatus("active");
        boss2.setBalance(BigDecimal.valueOf(3000));
        boss2.setRating(BigDecimal.valueOf(4.8));
        boss2.setTotalOrders(8);
        boss2.setIntroduction("上班族，周末在线");
        userMapper.insert(boss2);

        // 陪陪
        User booster1 = new User();
        booster1.setPhone("13600000001");
        booster1.setPassword(pwd);
        booster1.setNickname("影刃");
        booster1.setRole("booster");
        booster1.setStatus("active");
        booster1.setBoosterStatus("idle");
        booster1.setBalance(BigDecimal.valueOf(15000));
        booster1.setRating(BigDecimal.valueOf(4.95));
        booster1.setTotalOrders(320);
        booster1.setIntroduction("三角洲部队退役，全能型陪陪");
        userMapper.insert(booster1);

        User booster2 = new User();
        booster2.setPhone("13600000002");
        booster2.setPassword(pwd);
        booster2.setNickname("小仙女");
        booster2.setRole("booster");
        booster2.setStatus("active");
        booster2.setBoosterStatus("idle");
        booster2.setBalance(BigDecimal.valueOf(8000));
        booster2.setRating(BigDecimal.valueOf(4.8));
        booster2.setTotalOrders(156);
        booster2.setIntroduction("声音甜美，气氛担当，擅长监狱逃杀");
        userMapper.insert(booster2);

        User booster3 = new User();
        booster3.setPhone("13600000003");
        booster3.setPassword(pwd);
        booster3.setNickname("雷神之锤");
        booster3.setRole("booster");
        booster3.setStatus("active");
        booster3.setBoosterStatus("idle");
        booster3.setBalance(BigDecimal.valueOf(22000));
        booster3.setRating(BigDecimal.valueOf(4.9));
        booster3.setTotalOrders(498);
        booster3.setIntroduction("资深技术陪练，沟通清晰，按订单时长提供服务");
        userMapper.insert(booster3);

        // 服务项目
        insertService("陪玩·标准模式", "专业陪玩，支持零号大坝、长弓溪谷等经典地图", "陪玩专区", 58, "hour", "按小时提供陪玩与组队交流", "未开始服务可申请退款", 1);
        insertService("陪玩·高段位", "高段位技术陪练，支持绝密模式", "陪玩专区", 88, "hour", "按小时提供技术陪练服务", "服务异常按未完成时长处理", 2);
        insertService("撤离护航·机密", "机密模式组队护航与路线协作", "老板护航", 128, "round", "按局提供组队护航服务", "未开始服务可申请退款", 3);
        insertService("撤离护航·绝密", "绝密模式团队护航与战术协作", "老板护航", 198, "round", "按局提供团队协作服务", "服务异常由客服核实处理", 4);
        insertService("监狱·标准突围", "监狱地图组队突围与路线协作", "监狱专区", 68, "round", "按局提供组队陪玩服务", "未开始服务可申请退款", 5);
        insertService("监狱·高效通关", "监狱高效通关，快速撤离，效率优先", "监狱专区", 108, "round", "15分钟内通关", "超时退50%", 6);
        insertService("趣味·刀战模式", "纯刀战趣味模式，娱乐为主", "趣味玩法", 38, "hour", "", "不满意退款", 7);
        insertService("趣味·狙击对决", "狙击枪对决趣味玩法", "趣味玩法", 48, "hour", "", "不满意退款", 8);
        insertService("特殊·虎溪守卫", "虎溪特殊玩法守卫模式", "特殊玩法", 78, "round", "守卫成功", "失败退全款", 9);

        // 公告
        Announcement a1 = new Announcement(); a1.setTitle("平台上线公告"); a1.setContent("沧月电竞平台正式上线！欢迎各位老板和陪陪加入我们的大家庭。"); a1.setStatus("published"); a1.setSortOrder(1); announcementMapper.insert(a1);
        Announcement a2 = new Announcement(); a2.setTitle("新版本更新说明"); a2.setContent("1. 优化了订单匹配算法\n2. 新增礼物系统\n3. 修复了已知问题"); a2.setStatus("published"); a2.setSortOrder(2); announcementMapper.insert(a2);
        Announcement a3 = new Announcement(); a3.setTitle("陪陪招募计划"); a3.setContent("长期招募高水平陪陪，待遇优厚，详情请联系客服。"); a3.setStatus("published"); a3.setSortOrder(3); announcementMapper.insert(a3);

        System.out.println("=== Delta Esports 初始化数据完成 ===");
        System.out.println("管理员: 13800000001 / 123456");
        System.out.println("老板: 13900000001 / 123456");
        System.out.println("陪陪: 13600000001 / 123456");
    }

    private void insertService(String name, String desc, String category, int price, String unit, String guarantee, String refund, int sort) {
        ServiceItem s = new ServiceItem();
        s.setName(name); s.setDescription(desc); s.setCategory(category);
        s.setBasePrice(BigDecimal.valueOf(price)); s.setPriceUnit(unit);
        s.setGuaranteeDesc(guarantee); s.setRefundPolicy(refund);
        s.setIsActive(1); s.setSortOrder(sort);
        serviceItemMapper.insert(s);
    }
}
