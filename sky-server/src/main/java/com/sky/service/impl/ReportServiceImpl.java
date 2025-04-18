package com.sky.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        // 当前集合用于存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询date日期对应的营业额数据，营业额是指：状态为“已完成”的订单金额总计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select sum(amount) from orders where order_time > beginTime and order_time < endTime and status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 :turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(org.apache.commons.lang3.StringUtils.join(dateList, ","))
                .turnoverList(org.apache.commons.lang3.StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        // 存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天新增用户数量 select count(id) from user where create_time < ? and create_time > ?
        List<Integer> newUserList = new ArrayList<>();

        // 存放每天总用户数量  select count(id) from user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);

            // 总用户数量
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);

            // 新增用户数量
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);

        }

        // 封装结果数据
        return UserReportVO
                .builder()
                .dateList(org.apache.commons.lang3.StringUtils.join(dateList, ","))
                .totalUserList(org.apache.commons.lang3.StringUtils.join(totalUserList, ","))
                .newUserList(org.apache.commons.lang3.StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        // 存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();

        // 存放每天的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();

        Integer totalOrderCount = 0;
        Integer totalValidOrderCount = 0;

        // 遍历dateList集合，查询每天的有效订单数和订单总数
        for (LocalDate date : dateList) {
            // 查询每天的订单总数 select count(id) from orders where order_time > ? and order_time < ?
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            totalOrderCount = totalOrderCount + orderCount;

            // 查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = 5
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            totalValidOrderCount = totalValidOrderCount + validOrderCount;

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 计算时间区间内的订单总数量
//        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
//        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0){
            // 计算订单完成率
            orderCompletionRate = totalValidOrderCount.doubleValue() / totalOrderCount;
        }

        return  OrderReportVO.builder()
                .dateList(org.apache.commons.lang3.StringUtils.join(dateList, ","))
                .orderCountList(org.apache.commons.lang3.StringUtils.join(orderCountList, ","))
                .validOrderCountList(org.apache.commons.lang3.StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据条件统计订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

    /**
     * 统计指定时间区间内的销量排名top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = org.apache.commons.lang3.StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = org.apache.commons.lang3.StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
