package com.delta.esports.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delta.esports.entity.ServiceItem;
import com.delta.esports.entity.User;
import com.delta.esports.mapper.ServiceItemMapper;
import com.delta.esports.mapper.UserMapper;
import com.delta.esports.mapper.OrderMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile({"prod", "local"})
@Order(2)
public class ProductionDataInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final OrderMapper orderMapper;

    @Value("${BOOTSTRAP_ADMIN_PHONE:}")
    private String adminPhone;

    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}")
    private String adminPassword;

    public ProductionDataInitializer(UserMapper userMapper, ServiceItemMapper serviceItemMapper, OrderMapper orderMapper) {
        this.userMapper = userMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        long serviceCount = serviceItemMapper.selectCount(null);
        if (serviceCount > 0 && !(serviceCount == 9 && orderMapper.selectCount(null) == 0)) return;
        if (serviceCount == 9) serviceItemMapper.selectList(null).forEach(item -> serviceItemMapper.deleteById(item.getId()));
        seedCatalog();
    }

    private void seedAdmin() {
        if (adminPhone == null || adminPhone.isBlank() || adminPassword == null || adminPassword.length() < 12) return;
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, adminPhone));
        if (count > 0) return;
        User admin = new User();
        admin.setPhone(adminPhone);
        admin.setPassword(BCrypt.hashpw(adminPassword, BCrypt.gensalt()));
        admin.setNickname("平台管理员");
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setBalance(BigDecimal.ZERO);
        admin.setRating(BigDecimal.valueOf(5));
        admin.setTotalOrders(0);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(admin);
    }

    private void seedCatalog() {
        int sort = 1;
        insertService("娱乐男陪玩", "以情绪价值和组队交流为主", "小时陪玩", 35, "hour", "普通绝密；进监狱绝密另加 15 元/小时", sort++);
        insertService("娱乐女陪玩", "以情绪价值和组队交流为主", "小时陪玩", 40, "hour", "普通绝密；进监狱绝密另加 15 元/小时", sort++);
        insertService("技术男陪玩", "技术陪练与路线协作", "小时陪玩", 85, "hour", "单陪每小时保底 4 人头；未达标按规则退费", sort++);
        insertService("技术女陪玩", "技术陪练与路线协作", "小时陪玩", 90, "hour", "单陪每小时保底 4 人头；未达标按规则退费", sort++);
        insertService("顶尖男陪玩", "高强度技术陪练", "小时陪玩", 110, "hour", "单陪每小时保底 6 人头；未达标按规则退费", sort++);
        insertService("顶尖女陪玩", "高强度技术陪练", "小时陪玩", 115, "hour", "单陪每小时保底 6 人头；未达标按规则退费", sort++);

        insertService("首单护航·机密", "首单二选一，机密地图", "护航保值", 68, "round", "保底 488 万", sort++);
        insertService("首单护航·绝密", "首单二选一，绝密地图", "护航保值", 98, "round", "保底 588 万", sort++);
        int[][] basic = {{68,328},{98,488},{158,800},{198,1200},{248,1580},{328,2200}};
        for (int[] p : basic) insertService("基础护航·保底" + p[1] + "万", "打手选图，按局护航", "护航保值", p[0], "round", "保底 " + p[1] + " 万", sort++);
        int[][] secret = {{98,400},{138,688},{198,1000},{258,1400},{328,1980}};
        for (int[] p : secret) insertService("绝密护航·保底" + p[1] + "万", "任意绝密地图", "绝密护航", p[0], "round", "保底 " + p[1] + " 万", sort++);
        insertService("清图·机密", "全图无人即完成", "清图专区", 128, "round", "机密地图清图", sort++);
        insertService("清图·绝密", "全图无人即完成", "清图专区", 238, "round", "绝密地图清图", sort++);

        int[][] red = {{98,388},{268,588},{348,988},{428,1480},{648,2100}};
        String[] redNames = {"1格小红","2格小红","4格红","6格大红","9格及以上大红"};
        insertService("火箭燃料", "大红专区，不出一直打", "大红专区", 888, "round", "保底 3000 万", sort++);
        for (int i = 0; i < red.length; i++) insertService(redNames[i], "大红专区，不出一直打", "大红专区", red[i][0], "round", "保底 " + red[i][1] + " 万", sort++);

        int[][] prison = {{168,588},{256,900},{358,1300},{648,2500},{888,3500},{1288,5200}};
        for (int[] p : prison) insertService("监狱护航·保底" + p[1] + "万", "监狱地图按局护航", "监狱专区", p[0], "round", "炸单一局加 80 万保底", sort++);
        insertService("海洋之心", "包含监狱全卡", "监狱专区", 12888, "round", "不出保底 4 个亿", sort++);

        insertService("相同2个红", "指定趣味掉落玩法", "趣味玩法", 488, "round", null, sort++);
        insertService("相同4个红", "指定趣味掉落玩法", "趣味玩法", 2888, "round", null, sort++);
        insertService("相同2个大红", "指定趣味掉落玩法", "趣味玩法", 1688, "round", null, sort++);
        insertService("相同3个大红", "指定趣味掉落玩法", "趣味玩法", 3888, "round", null, sort++);
        insertService("指定2格红", "指定趣味掉落玩法", "趣味玩法", 298, "round", null, sort++);
        insertService("指定大红", "指定趣味掉落玩法", "趣味玩法", 1388, "round", null, sort++);
        insertService("纵横·天圆地方", "趣味地图玩法", "趣味玩法", 1888, "round", null, sort++);
        insertService("盾盾奶", "双打手配合，基础保底 500 万", "趣味玩法", 268, "round", "按击杀、救援和助攻增加保底", sort++);
        insertService("自力更生单", "打手为老板起 5 级头甲与子弹", "趣味玩法", 248, "round", "基础保底 600 万", sort++);
        insertService("自力更生单·加强", "打手为老板起 5 级头甲与子弹", "趣味玩法", 488, "round", "基础保底 1400 万", sort++);
        insertService("堵桥 CS 流", "仅限绝密航天，打手全程保护近点", "趣味玩法", 268, "round", "基础保底 1000 万", sort++);
    }

    private void insertService(String name, String description, String category, int price, String unit, String guarantee, int sort) {
        ServiceItem item = new ServiceItem();
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        item.setBasePrice(BigDecimal.valueOf(price));
        item.setPriceUnit(unit);
        item.setGuaranteeDesc(guarantee);
        item.setRefundPolicy("具体保底与结算以订单说明和客服确认记录为准");
        item.setIsActive(1);
        item.setSortOrder(sort);
        serviceItemMapper.insert(item);
    }
}
