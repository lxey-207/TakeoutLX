package com.sky.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Override
    @Transactional
    public OrderSubmitVO sub(OrdersSubmitDTO ordersSubmitDTO) {

        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setCancelReason(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);

        List<OrderDetail> details = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            details.add(orderDetail);
        }

        orderDetailMapper.insertBatch(details);

        shoppingCartMapper.deleteById(userId);

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //=========== 临时绕过微信支付（未配置商户号/证书）开始 ===========
        //  申请到微信支付商户号、配置好 application.yml 中 sky.wechat 的支付参数后，
        //  删除下面 paySuccess 和 mock VO 两段，再恢复文件末尾注释掉的真实调用即可。

        // 1、直接模拟微信支付回调：订单变为"待接单"、已支付
        paySuccess(ordersPaymentDTO.getOrderNumber());

        // 2、返回假的调起支付参数（开发者工具会弹"模拟支付"框，点确认即可）
        OrderPaymentVO vo = OrderPaymentVO.builder()
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .nonceStr("mock")
                .packageStr("prepay_id=mock")
                .signType("RSA")
                .paySign("mock")
                .build();
        return vo;
        //=========== 临时绕过微信支付 结束 ===========

        // //调用微信支付接口，生成预支付交易单
        // JSONObject jsonObject = weChatPayUtil.pay(
        //         ordersPaymentDTO.getOrderNumber(), //商户订单号
        //         new BigDecimal(0.01), //支付金额，单位 元
        //         "苍穹外卖订单", //商品描述
        //         user.getOpenid() //微信用户的openid
        // );
        //
        // if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
        //     throw new OrderBusinessException("该订单已支付");
        // }
        //
        // OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        // vo.setPackageStr(jsonObject.getString("package"));
        //
        // return vo;
    }

    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public PageResult<OrdersDTO> page(OrdersPageQueryDTO ordersPageQueryDTO) {

        // 只查询当前登录用户的订单
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        List<Orders> orders = orderMapper.list(ordersPageQueryDTO);
        List<OrdersDTO> ordersDTOS = new ArrayList<>();
        for (Orders order : orders) {
            OrdersDTO ordersDTO = new OrdersDTO();
            BeanUtils.copyProperties(order, ordersDTO);
            // 查询当前订单的所有菜品明细
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());
            ordersDTO.setOrderDetailList(orderDetailList);
            ordersDTOS.add(ordersDTO);
        }

        // 注意：PageInfo 要传 PageHelper 分页出的 Page 对象，才能拿到真实 total
        PageInfo<Orders> pageInfo = new PageInfo<>(orders);

        return new PageResult<>(pageInfo.getTotal(), ordersDTOS);
    }

    @Override
    public OrderVO selectById(Long id) {

        OrderVO orderVO = new OrderVO();

        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    @Override
    public void cancel(Long id) {

        // 1. 查订单
        Orders ordersDB = orderMapper.getById(id);

        // 2. 校验订单存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 3. 校验状态：只有"待付款"(1)和"待接单"(2)能取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 4. 校验归属：必须是当前登录用户的订单
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 5. 只更新需要的字段（配合 XML 的 <if> 动态 SQL）
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)          // 已取消
                .cancelReason("用户取消")          // 记录原因
                .cancelTime(LocalDateTime.now())   // 记录时间
                .build();

        orderMapper.update(orders);
    }
}
