package cn.itcast.hotel.pojo;

import lombok.Data;

/**
 * @author 20609
 */
@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String city;
    private String brand;
    private String starName;
    private Integer maxPrice;
    private Integer minPrice;
    private String location;
}
