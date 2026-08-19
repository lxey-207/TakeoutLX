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
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public void uCancel(Long id) {

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

    @Override
    public void repetition(Long id) {

        // 校验订单存在 + 归属
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!orders.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 1. 查出这个订单的所有菜品明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 2. 把明细转换成购物车对象（id 不复制，让购物车主键重新生成）
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        // 3. 批量插入购物车，用户回首页即可看到并重新下单
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /* admin */

    @Override
    public PageResult<Orders> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        List<Orders> ordersList = orderMapper.list(ordersPageQueryDTO);

        PageInfo<Orders> pageInfo = new PageInfo<>(ordersList);

        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    @Override
    public OrderStatisticsVO statistics() {

        // 待接单(2)、待派送(3)、派送中(4) 各查一次数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    @Override
    public void confirm(Long id) {

        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 只有"待接单"(2) 才能接单
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CONFIRMED)   // 待派送(3)
                .build();
        orderMapper.update(orders);
    }

    @Override
    public void rejection(Long id, String rejectionReason) {

        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 只有"待接单"(2) 才能拒单
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 配置真实微信支付后：已支付订单被拒单时，需要调用 weChatPayUtil.refund() 发起退款
        // 实体没有 rejectTime 字段，暂用 cancelTime 记录拒单时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(rejectionReason)
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);

    }

    @Override
    public void aCancel(Long id, String cancelReason) {
        // 1. 查订单
        Orders ordersDB = orderMapper.getById(id);

        // 2. 校验订单存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 3. 校验状态：已完成(5)和已取消(6)的订单不能再取消
        if (ordersDB.getStatus().equals(Orders.COMPLETED)
                || ordersDB.getStatus().equals(Orders.CANCELLED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)          // 已取消
                .cancelReason(cancelReason)          // 记录原因
                .cancelTime(LocalDateTime.now())   // 记录时间
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {

        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只有"待派送"(3) 才能派送
        if (!Orders.CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(orders);
    }


}
