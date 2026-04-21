package com.dormpower.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 账单生成消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房间 ID
     */
    private String roomId;

    /**
     * 房间号
     */
    private String roomNumber;

    /**
     * 楼栋号
     */
    private String building;

    /**
     * 账单日期
     */
    private LocalDate billDate;

    /**
     * 用电量 (kWh)
     */
    private double energyKwh;

    /**
     * 电费单价 (元/kWh)
     */
    private double pricePerKwh;

    /**
     * 总金额 (元)
     */
    private double totalAmount;

    /**
     * 用户 ID
     */
    private Long userId;
}
