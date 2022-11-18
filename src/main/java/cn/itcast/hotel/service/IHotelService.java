package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 20609
 */
public interface IHotelService extends IService<Hotel> {
    /**
     * 分页查询
     * @return 一页hotel
     */
    PageResult search(RequestParams params);
}
