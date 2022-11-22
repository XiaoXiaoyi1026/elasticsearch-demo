package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class HotelSearchTest {

    private RestHighLevelClient client;

    @Test
    void testInit() {
        System.out.println(client);
    }

    @Test
    void testMatchAll() throws IOException {
        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 处理结果
        handleResponse(response);
    }

    @Test
    void testMatch() throws IOException {
        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    @Test
    void testBool() throws IOException {
        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL
        // 2.1. 创建boolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.2. 添加term
        boolQuery.must(QueryBuilders.termQuery("city", "上海"));
        // 2.3. 添加filter
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(250));
        request.source().query(boolQuery);
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    @Test
    void testPageAndSort() throws IOException {
        int page = 3, pageSize = 5;
        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL match_all
        request.source().query(QueryBuilders.matchAllQuery());
        // 2.1. sort
        request.source().sort("price", SortOrder.ASC);
        // 2.2. 分页 from and size
        request.source().from((page - 1) * pageSize).size(pageSize);
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    @Test
    void testHighlight() throws IOException {
        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL match_all
        // 2.1. query
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        // 2.2. high_light
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    private static void handleResponse(SearchResponse response) {
        // 4. 处理结果
        SearchHits searchHits = response.getHits();
        // 4.1 获取查询到的总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共查询到" + total + "条数据");
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
            System.out.println(hotelDoc);
        }
    }

    @Test
    void testAggregation() throws IOException {
        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL
        // 2.1. 去掉文档数据
        request.source().size(0);
        // 2.2. 聚合
        request.source().aggregation(
                AggregationBuilders.terms("brandAgg")
                        .field("brand")
                        .size(20)
        );
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 处理结果
        Aggregations aggregations = response.getAggregations();
        // 4.1. 获取terms
        Terms brandTerms = aggregations.get("brandAgg");
        // 4.2. 获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        // 4.3. 遍历buckets
        for (Terms.Bucket bucket : buckets) {
            // 4.4. 从bucket中拿到想要的字段值
            String key = bucket.getKeyAsString();
            System.out.println(key);
        }
    }

    @Test
    void testSuggestion() throws IOException {
        // 1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        // 2. 准备DSL
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "suggestions",
                SuggestBuilders.completionSuggestion("suggestion")
                        .size(10)
                        .prefix("sz")
                        .skipDuplicates(true)
        ));
        // 3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4. 处理结果
        Suggest suggest = response.getSuggest();
        // 4.1. 根据名称获取对应的suggestion
        CompletionSuggestion suggestion = suggest.getSuggestion("suggestions");
        // 4.2. 获取options
        List<CompletionSuggestion.Entry.Option> options = suggestion.getOptions();
        // 4.3. 遍历取值
        for (CompletionSuggestion.Entry.Option option : options) {
            String text = String.valueOf(option.getText());
            System.out.println(text);
        }
    }

    /**
     * 初始化client
     */
    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://10.190.67.128:9200")
        ));
    }

    /**
     * 关闭client
     */
    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
