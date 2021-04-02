package org.jeecg.modules.business.vo;

import java.util.List;

import org.jeecg.modules.business.entity.ClientSku;
import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @Description: 客户
 * @Author: jeecg-boot
 * @Date:   2021-04-01
 * @Version: V1.0
 */
@Data
@ApiModel(value="clientPage对象", description="客户")
public class ClientPage {

	/**主键*/
	@ApiModelProperty(value = "主键")
    private String id;
	/**创建人*/
	@ApiModelProperty(value = "创建人")
    private String createBy;
	/**创建日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "创建日期")
    private Date createTime;
	/**更新人*/
	@ApiModelProperty(value = "更新人")
    private String updateBy;
	/**更新日期*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "更新日期")
    private Date updateTime;
	/**姓*/
	@Excel(name = "姓", width = 15)
	@ApiModelProperty(value = "姓")
    private String surname;
	/**名*/
	@Excel(name = "名", width = 15)
	@ApiModelProperty(value = "名")
    private String firstName;
	/**简称*/
	@Excel(name = "简称", width = 15)
	@ApiModelProperty(value = "简称")
    private String internalCode;
	/**发票实体*/
	@Excel(name = "发票实体", width = 15)
	@ApiModelProperty(value = "发票实体")
    private String invoiceEntity;
	/**邮箱*/
	@Excel(name = "邮箱", width = 15)
	@ApiModelProperty(value = "邮箱")
    private String email;
	/**电话*/
	@Excel(name = "电话", width = 15)
	@ApiModelProperty(value = "电话")
    private String phone;
	/**门牌号码*/
	@Excel(name = "门牌号码", width = 15)
	@ApiModelProperty(value = "门牌号码")
    private String streetNumber;
	/**街道名*/
	@Excel(name = "街道名", width = 15)
	@ApiModelProperty(value = "街道名")
    private String streetName;
	/**备用地址*/
	@Excel(name = "备用地址", width = 15)
	@ApiModelProperty(value = "备用地址")
    private String additionalAddress;
	/**邮编*/
	@Excel(name = "邮编", width = 15)
	@ApiModelProperty(value = "邮编")
    private String postcode;
	/**城市*/
	@Excel(name = "城市", width = 15)
	@ApiModelProperty(value = "城市")
    private String city;
	/**国家*/
	@Excel(name = "国家", width = 15)
	@ApiModelProperty(value = "国家")
    private String country;
	/**货币*/
	@Excel(name = "货币", width = 15)
	@ApiModelProperty(value = "货币")
    private String currency;
	/**运费折扣*/
	@Excel(name = "运费折扣", width = 15)
	@ApiModelProperty(value = "运费折扣")
    private java.math.BigDecimal shippingDiscount;
	/**公司识别码类型*/
	@Excel(name = "公司识别码类型", width = 15)
	@ApiModelProperty(value = "公司识别码类型")
    private String companyIdType;
	/**公司识别码数值*/
	@Excel(name = "公司识别码数值", width = 15)
	@ApiModelProperty(value = "公司识别码数值")
    private String companyIdValue;
	/**账户余额*/
	@Excel(name = "账户余额", width = 15)
	@ApiModelProperty(value = "账户余额")
    private java.math.BigDecimal balance;

	@ExcelCollection(name="客户名下SKU")
	@ApiModelProperty(value = "客户名下SKU")
	private List<ClientSku> clientSkuList;

}
