package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

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

    /**
     * 测试批量请求
     */
    @Test
    void testBulkRequest() throws IOException {
        // 批量查询所有酒店数据
        List<Hotel> hotelList = hotelService.list();
        // 1. 准备request
        BulkRequest bulkRequest = new BulkRequest();
        // 2. 添加批量request(打包)
        for (Hotel hotel : hotelList) {
            // 转换成文档对象
            HotelDoc hotelDoc = new HotelDoc(hotel);
            // 添加请求到bulk请求中, 批量添加文档
            bulkRequest.add(new IndexRequest("hotel")
                    .id(String.valueOf(hotelDoc.getId()))
                    .source(JSON.toJSONString(hotelDoc), XContentType.JSON));
        }
        // 3. 发送请求(批量执行request)
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    /**
     * 测试client初始化结果
     */
    @Test
    void testInit() {
        System.out.println(client);
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
     * 销毁client
     */
    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
