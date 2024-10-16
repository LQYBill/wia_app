package org.jeecg.modules.business.controller.admin;

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.Credit;
import org.jeecg.modules.business.service.IBalanceService;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.ICreditService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;

import static org.jeecg.modules.business.entity.Balance.OperationType;
 /**
 * @Description: credit
 * @Author: jeecg-boot
 * @Date:   2023-08-23
 * @Version: V1.0
 */
@Api(tags="credit")
@RestController
@RequestMapping("/credit")
@Slf4j
public class CreditController extends JeecgController<Credit, ICreditService> {
	@Autowired
	private ICreditService creditService;
	@Autowired
	private IBalanceService balanceService;
	@Autowired
	private IClientService clientService;
	
	/**
	 * 分页列表查询
	 *
	 * @param credit
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@ApiOperation(value="credit-分页列表查询", notes="credit-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<Credit>> queryPageList(Credit credit,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<Credit> queryWrapper = QueryGenerator.initQueryWrapper(credit, req.getParameterMap());
		Page<Credit> page = new Page<Credit>(pageNo, pageSize);
		IPage<Credit> pageList = creditService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param credit
	 * @return
	 */
	@AutoLog(value = "credit-添加")
	@ApiOperation(value="credit-添加", notes="credit-添加")
	@Transactional
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody Credit credit) {
		creditService.save(credit);
		try {
			balanceService.updateBalance(credit.getClientId(), credit.getId(), credit.getAmount(), credit.getCurrencyId());
		}
		catch (RuntimeException e) {
			Client client = clientService.getById(credit.getClientId());
			return Result.error("Error while updating " + client.fullName() + "'s balance  : " + e.getMessage());
		}
		return Result.OK("sys.api.entryAddSuccess");
	}
	
	/**
	 *  编辑
	 *
	 * @param credit
	 * @return
	 */
	@AutoLog(value = "credit-编辑")
	@ApiOperation(value="credit-编辑", notes="credit-编辑")
	@Transactional
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody Credit credit) {
		log.info("editing credit");
		Credit lastCredit = creditService.getLastCredit(credit.getCurrencyId());
		boolean isAmountUpdated = true;
		// if the last credit is not the one being edited, then the amount cannot be edited
		if(!lastCredit.getId().equals(credit.getId())) {
			isAmountUpdated = false;
			Credit creditToEdit = creditService.getById(credit.getId());
			if(creditToEdit.getAmount().compareTo(credit.getAmount()) != 0) {
				log.error("Credit amount cannot be edited ! Only the last record can be edited !");
				return Result.error("Credit amount cannot be edited ! Only the last record can be edited !");
			}
		}
		creditService.updateById(credit);
		log.info("credit edited successfully !");
		if(isAmountUpdated) {
			try {
				balanceService.editBalance(credit.getId(), OperationType.Credit.name(), credit.getClientId(), credit.getAmount(), credit.getCurrencyId());

			}
			catch (Exception e) {
				log.error("Error while editing balance : " + e.getMessage());
				return Result.error("Error while editing balance : " + e.getMessage());
			}
		}
		return Result.OK("sys.api.entryEditSuccess");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "credit-通过id删除")
	@ApiOperation(value="credit-通过id删除", notes="credit-通过id删除")
	@Transactional
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id") String id) {
		creditService.removeById(id);
		balanceService.deleteBalance(id, OperationType.Credit.name());
		return Result.OK("sys.api.entryDeleteSuccess");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "credit-批量删除")
	@ApiOperation(value="credit-批量删除", notes="credit-批量删除")
	@Transactional
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids") String ids) {
		List<String> idList = Arrays.asList(ids.split(","));
		creditService.removeByIds(idList);
		balanceService.deleteBatchBalance(idList,OperationType.Credit.name());
		return Result.OK("sys.api.entryBatchDeleteSuccess");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@ApiOperation(value="credit-通过id查询", notes="credit-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<Credit> queryById(@RequestParam(name="id") String id) {
		Credit credit = creditService.getById(id);
		if(credit==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(credit);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param credit
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Credit credit) {
        return super.exportXls(request, credit, Credit.class, "credit");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, Credit.class);
    }
}
