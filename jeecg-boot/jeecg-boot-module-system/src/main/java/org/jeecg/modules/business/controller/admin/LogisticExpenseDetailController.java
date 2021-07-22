package org.jeecg.modules.business.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.service.ILogisticExpenseDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * @Description: 物流开销明细
 * @Author: jeecg-boot
 * @Date:   2021-07-22
 * @Version: V1.0
 */
@Api(tags="物流开销明细")
@RestController
@RequestMapping("/business/logisticExpenseDetail")
@Slf4j
public class LogisticExpenseDetailController extends JeecgController<LogisticExpenseDetail, ILogisticExpenseDetailService> {
	@Autowired
	private ILogisticExpenseDetailService logisticExpenseDetailService;
	
	/**
	 * 分页列表查询
	 *
	 * @param logisticExpenseDetail
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "物流开销明细-分页列表查询")
	@ApiOperation(value="物流开销明细-分页列表查询", notes="物流开销明细-分页列表查询")
	@GetMapping(value = "/list")
	public Result<?> queryPageList(LogisticExpenseDetail logisticExpenseDetail,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<LogisticExpenseDetail> queryWrapper = QueryGenerator.initQueryWrapper(logisticExpenseDetail, req.getParameterMap());
		Page<LogisticExpenseDetail> page = new Page<LogisticExpenseDetail>(pageNo, pageSize);
		IPage<LogisticExpenseDetail> pageList = logisticExpenseDetailService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param logisticExpenseDetail
	 * @return
	 */
	@AutoLog(value = "物流开销明细-添加")
	@ApiOperation(value="物流开销明细-添加", notes="物流开销明细-添加")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody LogisticExpenseDetail logisticExpenseDetail) {
		logisticExpenseDetailService.save(logisticExpenseDetail);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param logisticExpenseDetail
	 * @return
	 */
	@AutoLog(value = "物流开销明细-编辑")
	@ApiOperation(value="物流开销明细-编辑", notes="物流开销明细-编辑")
	@PutMapping(value = "/edit")
	public Result<?> edit(@RequestBody LogisticExpenseDetail logisticExpenseDetail) {
		logisticExpenseDetailService.updateById(logisticExpenseDetail);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "物流开销明细-通过id删除")
	@ApiOperation(value="物流开销明细-通过id删除", notes="物流开销明细-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		logisticExpenseDetailService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "物流开销明细-批量删除")
	@ApiOperation(value="物流开销明细-批量删除", notes="物流开销明细-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.logisticExpenseDetailService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "物流开销明细-通过id查询")
	@ApiOperation(value="物流开销明细-通过id查询", notes="物流开销明细-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		LogisticExpenseDetail logisticExpenseDetail = logisticExpenseDetailService.getById(id);
		if(logisticExpenseDetail==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(logisticExpenseDetail);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param logisticExpenseDetail
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, LogisticExpenseDetail logisticExpenseDetail) {
        return super.exportXls(request, logisticExpenseDetail, LogisticExpenseDetail.class, "物流开销明细");
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
        return super.importExcel(request, response, LogisticExpenseDetail.class);
    }

}
