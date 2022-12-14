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

            // 1. ??????request
            SearchRequest request = new SearchRequest("hotel");

            // 2. ????????????Query
            buildBasicQuery(params, request);

            // 2.5. ?????? from and size
            request.source().from((page - 1) * pageSize).size(pageSize);

            // 3. ????????????
            SearchResponse response;
            response = client.search(request, RequestOptions.DEFAULT);
            // ??????????????????
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
            Map<String, List<String>> res = new HashMap<>(3);
            // 1. ??????request
            SearchRequest request = new SearchRequest("hotel");
            // 2. ??????????????????
            // 2.1. ??????query
            buildBasicQuery(params, request);
            // 2.2. ??????size
            request.source().size(0);
            // 2.3. ????????????
            buildAggregation(request, "cityAgg", "city");
            buildAggregation(request, "starNameAgg", "starName");
            buildAggregation(request, "brandAgg", "brand");
            // 3. ????????????
            SearchResponse response;
            response = client.search(request, RequestOptions.DEFAULT);
            // 4. ????????????
            // 4.1. ??????aggregations
            Aggregations aggregations = response.getAggregations();

            // 4.2. ????????????, ????????????????????????
            List<String> values = getAggregationByName(aggregations, "cityAgg");
            res.put("city", values);

            // 4.3. ????????????????????????
            values = getAggregationByName(aggregations, "starNameAgg");
            res.put("starName", values);

            // 4.3. ????????????????????????
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
            // 1. ????????????
            SearchRequest request = new SearchRequest("hotel");
            // 2. ??????DSL
            request.source().suggest(
                    new SuggestBuilder().addSuggestion("suggestions",
                            SuggestBuilders.completionSuggestion("suggestion")
                                    .skipDuplicates(true)
                                    .size(10)
                                    .prefix(prefix)
                    ));
            // 3. ????????????, ????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4. ????????????
            Suggest suggest = response.getSuggest();
            // 4.1. ???????????????????????????suggestions
            CompletionSuggestion suggestion = suggest.getSuggestion("suggestions");
            // 4.2. ??????suggestions??????options
            List<CompletionSuggestion.Entry.Option> options = suggestion.getOptions();
            // 4.3. ??????????????????
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
            // 0. ??????id???mysql?????????????????????
            Hotel hotel = getById(id);
            // ?????????????????????
            HotelDoc hotelDoc = new HotelDoc(hotel);
            // 1. ??????request
            IndexRequest request = new IndexRequest("hotel").id(String.valueOf(id));
            // 2. ??????DSL(java?????????JSON)
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
            // 3. ????????????
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            // 1. ????????????
            DeleteRequest request = new DeleteRequest("hotel").id(String.valueOf(id));
            // 2. ????????????
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> getAggregationByName(Aggregations aggregations, String aggregationName) {
        // 4.2. ??????terms
        Terms terms = aggregations.get(aggregationName);
        // 4.3. ??????buckets
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        // 4.4. ????????????
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
        // 2. ??????DSL
        String key = params.getKey(), sortBy = params.getSortBy();
        // 2.1. ??????boolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        request.source().query(boolQuery);

        // 2.2. ??????must, ???????????????
        if (key == null || "".equals(key)) {
            boolQuery.must(
                    QueryBuilders.matchAllQuery()
            );
        } else {
            boolQuery.must(
                    QueryBuilders.matchQuery("all", key)
            );
        }

        // 2.3. ????????????(?????????????????????????????????)
        // 2.3.1. ????????????
        if (params.getCity() != null && !"".equals(params.getCity())) {
            boolQuery.must(
                    QueryBuilders.termQuery("city", params.getCity())
            );
        }

        // 2.3.2. ????????????
        if (params.getBrand() != null && !"".equals(params.getBrand())) {
            boolQuery.must(
                    QueryBuilders.termQuery("brand", params.getBrand())
            );
        }

        // 2.3.3. ????????????
        if (params.getStarName() != null && !"".equals(params.getStarName())) {
            boolQuery.must(
                    QueryBuilders.termQuery("starName", params.getStarName())
            );
        }

        // 2.3.4. ????????????
        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(
                    QueryBuilders.rangeQuery("price")
                            .lte(params.getMaxPrice())
                            .gte(params.getMinPrice())
            );
        }

        // 2.4.1. ??????????????????(??????????????????)??????
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

        // 3. ????????????(??????????????????????????????????????????)
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        // ????????????, ??????????????????
                        boolQuery,
                        // function score ?????????
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // 1???function score ??????
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        // ????????????
                                        QueryBuilders.termQuery("isAD", true),
                                        // ???????????? weight *10
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });
        request.source().query(functionScoreQuery);
    }

    private static PageResult handleResponse(SearchResponse response) {
        PageResult pageResult = new PageResult();
        List<HotelDoc> hotelDocList = new ArrayList<>();

        // 4. ????????????
        SearchHits searchHits = response.getHits();

        // 4.1 ???????????????????????????
        long total = searchHits.getTotalHits().value;
        pageResult.setTotal(total);

        // 4.2 ????????????????????????hits
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // ??????source
            String json = hit.getSourceAsString();
            // ????????????
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            // ??????sort?????????
            Object[] sortValues = hit.getSortValues();
            // ???????????????
            if (sortValues.length > 0) {
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }

            // ??????????????????
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();

            // ???????????????
            if (!CollectionUtils.isEmpty(highlightFields)) {
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    // ?????????????????????????????????1???, ???????????????
                    String name = highlightField.getFragments()[0].string();
                    // ???????????????????????????
                    hotelDoc.setName(name);
                }
            }
            hotelDocList.add(hotelDoc);
        }

        pageResult.setHotels(hotelDocList);
        return pageResult;
    }
}
