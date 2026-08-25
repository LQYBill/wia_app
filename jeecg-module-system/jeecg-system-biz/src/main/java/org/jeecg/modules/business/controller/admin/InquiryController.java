package org.jeecg.modules.business.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.Inquiry;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IInquiryService;
import org.jeecg.modules.business.service.ISecurityService;
import org.jeecg.modules.business.vo.InquiryImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "Inquiry")
@RestController
@RequestMapping("/inquiry")
@Slf4j
public class InquiryController extends JeecgController<Inquiry, IInquiryService> {
    @Autowired
    private IInquiryService inquiryService;
    @Autowired
    private ISecurityService securityService;
    @Autowired
    private IClientService clientService;

    @ApiOperation(value = "Inquiry list", notes = "Inquiry list")
    @GetMapping(value = "/list")
    public Result<IPage<Inquiry>> queryPageList(Inquiry inquiry,
                                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                HttpServletRequest req) {
        String inquiryCountryFilter = inquiry == null ? null : inquiry.getCountryId();
        if (inquiry != null && StringUtils.isNotBlank(inquiryCountryFilter)) {
            inquiry.setCountryId(null);
        }
        if (!securityService.checkIsEmployee()) {
            Client client = clientService.getCurrentClient();
            if (client == null || StringUtils.isBlank(client.getId())) {
                return Result.error(403, "Access denied");
            }
            inquiry.setClientId(client.getId());
        }
        Map<String, String[]> queryParams = new HashMap<>(req.getParameterMap());
        queryParams.remove("countryId");
        queryParams.remove("countryIds");
        QueryWrapper<Inquiry> queryWrapper = QueryGenerator.initQueryWrapper(inquiry, queryParams);
        if (StringUtils.isNotBlank(inquiryCountryFilter)) {
            String normalizedCountry = inquiryService.normalizeCountryValue(inquiryCountryFilter);
            queryWrapper.and(wrapper -> wrapper
                    .apply("FIND_IN_SET({0}, REPLACE(REPLACE(country_id, ', ', ','), ' ,', ','))", normalizedCountry)
                    .or().apply("FIND_IN_SET((SELECT name_en FROM country WHERE id = {0}), REPLACE(REPLACE(country_id, ', ', ','), ' ,', ','))", normalizedCountry)
                    .or().apply("FIND_IN_SET((SELECT special_name FROM country WHERE id = {0}), REPLACE(REPLACE(country_id, ', ', ','), ' ,', ','))", normalizedCountry)
                    .or().apply("FIND_IN_SET((SELECT name_zh FROM country WHERE id = {0}), REPLACE(REPLACE(country_id, ', ', ','), ' ,', ','))", normalizedCountry)
                    .or().apply("FIND_IN_SET((SELECT code FROM country WHERE id = {0}), REPLACE(REPLACE(country_id, ', ', ','), ' ,', ','))", normalizedCountry));
        }
        queryWrapper.orderByDesc("create_time");
        Page<Inquiry> page = new Page<>(pageNo, pageSize);
        IPage<Inquiry> pageList = inquiryService.page(page, queryWrapper);
        if (pageList != null && pageList.getRecords() != null) {
            pageList.getRecords().forEach(inquiryService::prepareInquiryForView);
        }
        return Result.OK(pageList);
    }

    @AutoLog(value = "Inquiry add")
    @ApiOperation(value = "Inquiry add", notes = "Inquiry add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody Inquiry inquiry) {
        if (!securityService.checkIsEmployee()) {
            Client client = clientService.getCurrentClient();
            if (client == null || StringUtils.isBlank(client.getId())) {
                return Result.error(403, "Access denied");
            }
            inquiry.setClientId(client.getId());
        }
        try {
            inquiryService.createInquiry(inquiry);
            return Result.OK("Added successfully");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation(value = "Inquiry export to Excel", notes = "Also serves as the import template: export with no filters to get a blank/example file matching the import column layout. Newest first. Multi-link JSON (title/url pairs) is flattened into readable \"title: url\" entries, one per line in the same cell; a row with exactly one link gets a clickable hyperlink instead (Excel only supports one hyperlink target per cell).")
    @RequestMapping(value = "/exportXls")
    public void exportXls(HttpServletRequest request, HttpServletResponse response, Inquiry inquiry) throws IOException {
        if (!securityService.checkIsEmployee()) {
            Client client = clientService.getCurrentClient();
            if (client != null && StringUtils.isNotBlank(client.getId())) {
                inquiry.setClientId(client.getId());
            }
        }
        QueryWrapper<Inquiry> queryWrapper = QueryGenerator.initQueryWrapper(inquiry, request.getParameterMap());
        String selections = request.getParameter("selections");
        if (StringUtils.isNotBlank(selections)) {
            queryWrapper.in("id", Arrays.asList(selections.split(",")));
        }
        queryWrapper.orderByDesc("create_time");
        List<Inquiry> exportList = inquiryService.list(queryWrapper);

        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        Workbook workbook = inquiryService.buildExportWorkbook(exportList, sysUser.getRealname());

        String exportDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename="
                + URLEncoder.encode("Inquiry_" + exportDate + ".xlsx", "UTF-8"));
        try (ServletOutputStream out = response.getOutputStream()) {
            workbook.write(out);
            out.flush();
        }
    }

    @ApiOperation(value = "Inquiry batch import from Excel", notes = "Inquiry batch import from Excel. Employees can import for any client; customers can import, but every row is force-bound to their own client id. Each row currently supports a single country (comma-separated multi-country is not supported by the generic dict import). Client text that doesn't match an existing client's internal_code is kept as-is (unregistered/prospect client name), matching the frontend's own convention.")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        boolean isEmployee = securityService.checkIsEmployee();
        String forcedClientId = null;
        if (!isEmployee) {
            Client client = clientService.getCurrentClient();
            if (client == null || StringUtils.isBlank(client.getId())) {
                return Result.error(403, "Access denied");
            }
            forcedClientId = client.getId();
        }
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
            MultipartFile file = entry.getValue();
            try {
                InquiryImportResult result = inquiryService.importFromExcel(file.getBytes(), forcedClientId);
                StringBuilder message = new StringBuilder("Import finished, rows imported: " + result.getImported());
                if (result.getSkippedDuplicates() > 0) {
                    message.append("; skipped as duplicates (same Client+Link+Country): ").append(result.getSkippedDuplicates());
                }
                if (!result.getRowErrors().isEmpty()) {
                    message.append("; failed rows: ").append(String.join("; ", result.getRowErrors()));
                }
                return result.getRowErrors().isEmpty() ? Result.OK(message.toString()) : Result.error(message.toString());
            } catch (Exception e) {
                log.error("Inquiry import failed", e);
                return Result.error("Import failed: " + e.getMessage());
            }
        }
        return Result.error("Import failed: no file uploaded");
    }

    @AutoLog(value = "Inquiry edit")
    @ApiOperation(value = "Inquiry edit", notes = "Inquiry edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody Inquiry inquiry) {
        Result<String> accessError = checkInquiryAccess(inquiry.getId());
        if (accessError != null) {
            return accessError;
        }
        if (!securityService.checkIsEmployee()) {
            Client client = clientService.getCurrentClient();
            if (client == null || StringUtils.isBlank(client.getId())) {
                return Result.error(403, "Access denied");
            }
            inquiry.setClientId(client.getId());
        }
        try {
            inquiryService.updateInquiry(inquiry);
            return Result.OK("Edited successfully");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @AutoLog(value = "Inquiry delete")
    @ApiOperation(value = "Inquiry delete", notes = "Inquiry delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        Result<String> accessError = checkInquiryAccess(id);
        if (accessError != null) {
            return accessError;
        }
        inquiryService.removeById(id);
        return Result.OK("Deleted successfully");
    }

    @AutoLog(value = "Inquiry batch delete")
    @ApiOperation(value = "Inquiry batch delete", notes = "Inquiry batch delete")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        for (String id : ids.split(",")) {
            Result<String> accessError = checkInquiryAccess(id);
            if (accessError != null) {
                return accessError;
            }
        }
        inquiryService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("Deleted successfully");
    }

    @ApiOperation(value = "Inquiry query by id", notes = "Inquiry query by id")
    @GetMapping(value = "/queryById")
    public Result<Inquiry> queryById(@RequestParam(name = "id") String id) {
        Result<String> accessError = checkInquiryAccess(id);
        if (accessError != null) {
            return Result.error(accessError.getCode(), accessError.getMessage());
        }
        Inquiry inquiry = inquiryService.getById(id);
        if (inquiry == null) {
            return Result.error("Record not found");
        }
        return Result.OK(inquiryService.prepareInquiryForView(inquiry));
    }

    private Result<String> checkInquiryAccess(String id) {
        if (securityService.checkIsEmployee()) {
            return null;
        }
        Client client = clientService.getCurrentClient();
        if (client == null || StringUtils.isBlank(client.getId())) {
            return Result.error(403, "Access denied");
        }
        Inquiry inquiry = inquiryService.getById(id);
        if (inquiry == null) {
            return Result.error("Record not found");
        }
        if (!client.getId().equals(inquiry.getClientId())) {
            return Result.error(403, "Access denied");
        }
        return null;
    }
}
