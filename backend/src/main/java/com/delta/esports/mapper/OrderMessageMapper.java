package com.delta.esports.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delta.esports.entity.OrderMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMessageMapper extends BaseMapper<OrderMessage> {

    /**
     * 按主键游标读取最近消息。先倒序利用 (order_id, id) 索引取 limit 条，
     * Service 再反转为聊天界面需要的时间正序，避免 OFFSET 深分页。
     */
    @Select("<script>" +
            "SELECT id, order_id, sender_id, content, type, created_at " +
            "FROM t_order_message WHERE order_id = #{orderId} " +
            "<if test='beforeId != null'>AND id &lt; #{beforeId} </if>" +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<OrderMessage> selectRecent(@Param("orderId") Long orderId,
                                    @Param("beforeId") Long beforeId,
                                    @Param("limit") int limit);
}
