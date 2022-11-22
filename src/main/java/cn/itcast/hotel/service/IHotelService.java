package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author 20609
 */
public interface IHotelService extends IService<Hotel> {
    /**
     * 分页查询
     *
     * @param params 前端参数
     * @return 一页hotel
     */
    PageResult search(RequestParams params);

    /**
     * 聚合查询
     *
     * @param params 前端参数
     * @return 聚合结果 格式为: {"城市": [上海, 广州, ...], "品牌": ["如家", ...], ...}
     */
    Map<String, List<String>> filters(RequestParams params);

    /**
     * 获取关联词
     *
     * @param prefix 前缀
     * @return 关联词集合
     */
    List<String> getSuggestion(String prefix);

    /**
     * 根据id插入酒店
     * @param id 酒店id
     */
    void insertById(Long id);

    /**
     * 根据id删除酒店
     *
     * @param id 酒店id
     */
    void deleteById(Long id);
}
