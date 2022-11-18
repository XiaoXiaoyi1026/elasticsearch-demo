package cn.itcast.hotel.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author 20609
 */
@Data
public class PageResult {
    private long total;
    private List<HotelDoc> hotels;

    public PageResult() {
    }

    public PageResult(long total, List<HotelDoc> hotels) {
        this.total = total;
        this.hotels = hotels;
    }
}
