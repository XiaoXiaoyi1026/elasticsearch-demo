package cn.itcast.hotel.messagequeue;

import cn.itcast.hotel.constants.MessageQueueConstants;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 20609
 * 消费者, 监听队列获取消息
 */
@Component
public class HotelListener {

    @Autowired
    private IHotelService hotelService;

    /**
     * 监听酒店新增或者修改的业务
     * @param id 酒店id
     */
    @RabbitListener(queues = MessageQueueConstants.HOTEL_INSERT_QUEUE)
    public void listenHotelInsertOrUpdate(Long id) {
        hotelService.insertById(id);
    }

    /**
     * 监听酒店新删除的业务
     * @param id 酒店id
     */
    @RabbitListener(queues = MessageQueueConstants.HOTEL_DELETE_QUEUE)
    public void listenHotelDelete(Long id) {
        hotelService.deleteById(id);
    }

}
