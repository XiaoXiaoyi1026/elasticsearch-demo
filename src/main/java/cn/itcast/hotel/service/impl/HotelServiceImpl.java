package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
        try {
            int page = params.getPage(), pageSize = params.getSize();

            // 1. 准备request
            SearchRequest request = new SearchRequest("hotel");

            // 2. 构建原始Query
            buildBasicQuery(params, request);

            // 2.5. 分页 from and size
            request.source().from((page - 1) * pageSize).size(pageSize);

            // 3. 发送请求
            SearchResponse response;
            response = client.search(request, RequestOptions.DEFAULT);
            // 返回结果处理
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
            Map<String, List<String>> res = new HashMap<>(3);
            // 1. 准备request
            SearchRequest request = new SearchRequest("hotel");
            // 2. 准备聚合查询
            // 2.1. 准备query
            buildBasicQuery(params, request);
            // 2.2. 设置size
            request.source().size(0);
            // 2.3. 准备聚合
            buildAggregation(request, "cityAgg", "city");
            buildAggregation(request, "starNameAgg", "starName");
            buildAggregation(request, "brandAgg", "brand");
            // 3. 发送请求
            SearchResponse response;
            response = client.search(request, RequestOptions.DEFAULT);
            // 4. 处理结果
            // 4.1. 获取aggregations
            Aggregations aggregations = response.getAggregations();

            // 4.2. 根据名称, 获取城市聚合结果
            List<String> values = getAggregationByName(aggregations, "cityAgg");
            res.put("city", values);

            // 4.3. 获取星级聚合结果
            values = getAggregationByName(aggregations, "starNameAgg");
            res.put("starName", values);

            // 4.3. 获取品牌聚合结果
            values = getAggregationByName(aggregations, "brandAgg");
            res.put("brand", values);

            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getSuggestion(String prefix) {
        try {
            // 1. 准备请求
            SearchRequest request = new SearchRequest("hotel");
            // 2. 准备DSL
            request.source().suggest(
                    new SuggestBuilder().addSuggestion("suggestions",
                            SuggestBuilders.completionSuggestion("suggestion")
                                    .skipDuplicates(true)
                                    .size(10)
                                    .prefix(prefix)
                    ));
            // 3. 发送请求, 获得相应
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4. 处理结果
            Suggest suggest = response.getSuggest();
            // 4.1. 根据名称获取对应的suggestions
            CompletionSuggestion suggestion = suggest.getSuggestion("suggestions");
            // 4.2. 获取suggestions中的options
            List<CompletionSuggestion.Entry.Option> options = suggestion.getOptions();
            // 4.3. 遍历封装结果
            List<String> res = new ArrayList<>();
            for (CompletionSuggestion.Entry.Option option : options) {
                res.add(String.valueOf(option.getText()));
            }
            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertById(Long id) {
        try {
            // 0. 根据id从mysql中查询酒店数据
            Hotel hotel = getById(id);
            // 转换为文档类型
            HotelDoc hotelDoc = new HotelDoc(hotel);
            // 1. 准备request
            IndexRequest request = new IndexRequest("hotel").id(String.valueOf(id));
            // 2. 准备DSL(java对象转JSON)
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            // 3. 发送请求
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            // 1. 准备请求
            DeleteRequest request = new DeleteRequest("hotel").id(String.valueOf(id));
            // 2. 发送请求
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> getAggregationByName(Aggregations aggregations, String aggregationName) {
        // 4.2. 获取terms
        Terms terms = aggregations.get(aggregationName);
        // 4.3. 获取buckets
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        // 4.4. 循环遍历
        List<String> values = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            values.add(bucket.getKeyAsString());
        }
        return values;
    }

    private static void buildAggregation(SearchRequest request, String aggregationName, String field) {
        request.source().aggregation(
                AggregationBuilders.terms(aggregationName)
                        .field(field)
                        .size(100)
        );
    }

    private static void buildBasicQuery(RequestParams params, SearchRequest request) {
        // 2. 准备DSL
        String key = params.getKey(), sortBy = params.getSortBy();
        // 2.1. 构建boolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        request.source().query(boolQuery);

        // 2.2. 设置must, 关键字搜索
        if (key == null || "".equals(key)) {
            boolQuery.must(
                    QueryBuilders.matchAllQuery()
            );
        } else {
            boolQuery.must(
                    QueryBuilders.matchQuery("all", key)
            );
        }

        // 2.3. 条件过滤(城市、品牌、星级、价格)
        // 2.3.1. 城市过滤
        if (params.getCity() != null && !"".equals(params.getCity())) {
            boolQuery.must(
                    QueryBuilders.termQuery("city", params.getCity())
            );
        }

        // 2.3.2. 品牌过滤
        if (params.getBrand() != null && !"".equals(params.getBrand())) {
            boolQuery.must(
                    QueryBuilders.termQuery("brand", params.getBrand())
            );
        }

        // 2.3.3. 星级过滤
        if (params.getStarName() != null && !"".equals(params.getStarName())) {
            boolQuery.must(
                    QueryBuilders.termQuery("starName", params.getStarName())
            );
        }

        // 2.3.4. 价格过滤
        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(
                    QueryBuilders.rangeQuery("price")
                            .lte(params.getMaxPrice())
                            .gte(params.getMinPrice())
            );
        }

        // 2.4.1. 根据地理坐标(距离由近及远)排序
        String location = params.getLocation();
        if (location != null && !"".equals(location)) {
            request.source()
                    .sort(
                            SortBuilders.geoDistanceSort("location", new GeoPoint(location))
                                    .order(SortOrder.ASC)
                                    .unit(DistanceUnit.KILOMETERS)
                    );
        }

        // 2.4.2. sort
        if (!"default".equals(sortBy)) {
            request.source().sort(sortBy, SortOrder.ASC);
        }

        // 3. 权重分析(为符合条件的查询结果增加权重)
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        // 原始查询, 做相关性算分
                        boolQuery,
                        // function score 的数据
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // 1个function score 元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        // 过滤条件
                                        QueryBuilders.termQuery("isAD", true),
                                        // 算分函数 weight *10
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });
        request.source().query(functionScoreQuery);
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

            // 获取sort排序值
            Object[] sortValues = hit.getSortValues();
            // 健壮性判断
            if (sortValues.length > 0) {
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }

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
