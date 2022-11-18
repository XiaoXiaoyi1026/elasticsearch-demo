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
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
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
