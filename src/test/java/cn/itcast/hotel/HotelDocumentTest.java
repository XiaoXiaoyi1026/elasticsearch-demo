package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import cn.itcast.hotel.service.impl.HotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

@SpringBootTest
public class HotelDocumentTest {

    @Autowired
    private IHotelService hotelService;
    private RestHighLevelClient client;

    @Test
    void testAddDocument() throws IOException {
        // 获取hotel表中的一条记录
        Hotel hotel = hotelService.getById(56227L);
        // 转换为文档类型
        HotelDoc hotelDoc = new HotelDoc(hotel);
        // 1. 准备Request对象
        IndexRequest request = new IndexRequest("hotel")
                .id(String.valueOf(hotelDoc.getId()));
        // 2. 准备Json文档
        request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
        // 3. 发送请求
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetDocumentById() throws IOException {
        // 1. 准备request
        GetRequest request = new GetRequest("hotel").id("56227");
        // 2. 发起http请求, 获取响应
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 3. 解析响应, 获取source字符串
        String json = response.getSourceAsString();
        // 4. 用fastjson解析source字符串
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    @Test
    void testUpdateDocumentById() throws IOException {
        // 1. 准备request
        UpdateRequest request = new UpdateRequest("hotel", "56227");
        request.doc(
                "score", "50",
                "price", "233"
        );
        // 2. 发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testDeleteDocumentById() throws IOException {
        // 1. 准备request
        DeleteRequest request = new DeleteRequest("hotel", "56227");
        // 2. 发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testInit() {
        System.out.println(client);
    }

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://10.190.67.128:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}