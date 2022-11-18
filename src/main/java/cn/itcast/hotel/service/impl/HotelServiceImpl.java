package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 20609
 */
@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) {
        int page = params.getPage(), pageSize = params.getSize();
        String key = params.getKey(), sortBy =params.getSortBy();

        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");

        // 2. 准备DSL query match 关键字搜索
        if (key == null || "".equals(key)) {
            request.source().query(QueryBuilders.matchAllQuery());
        } else {
            request.source().query(QueryBuilders.matchQuery("all", key));
        }

        // 2.1. sort
        request.source().sort("default".equals(sortBy) ? "price" : sortBy, SortOrder.ASC);

        // 2.2. 分页 from and size
        request.source().from((page - 1) * pageSize).size(pageSize);

        // 3. 发送请求
        SearchResponse response;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 返回结果处理
        return handleResponse(response);
    }

    private static PageResult handleResponse(SearchResponse response) {
        PageResult pageResult = new PageResult();
        List<HotelDoc> hotelDocList = new ArrayList<>();

        // 4. 处理结果
        SearchHits searchHits = response.getHits();

        // 4.1 获取查询到的总条数
        long total = searchHits.getTotalHits().value;
        pageResult.setTotal(total);

        // 4.2 获取查询到的所有hits
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 获取source
            String json = hit.getSourceAsString();
            // 反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            // 获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();

            // 健壮性判断
            if (!CollectionUtils.isEmpty(highlightFields)) {
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    // 取出高亮结果数组中的第1个, 即酒店名称
                    String name = highlightField.getFragments()[0].string();
                    // 替换掉没高光的名字
                    hotelDoc.setName(name);
                }
            }
            hotelDocList.add(hotelDoc);
        }

        pageResult.setHotels(hotelDocList);
        return pageResult;
    }
}
