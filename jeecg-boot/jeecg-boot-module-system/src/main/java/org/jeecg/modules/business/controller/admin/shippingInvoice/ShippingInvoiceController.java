package org.jeecg.modules.business.controller.admin.shippingInvoice;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.ShippingInvoice;
import org.jeecg.modules.business.service.IShippingInvoiceService;
import org.jeecg.modules.business.vo.ShippingInvoicePage;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Description: 物流发票
 * @Author: jeecg-boot
 * @Date: 2022-12-20
 * @Version: V1.0
 */
@Api(tags = "物流发票")
@RestController
@RequestMapping("/generated/shippingInvoice")
@Slf4j
public class ShippingInvoiceController {
    @Autowired
    private IShippingInvoiceService shippingInvoiceService;
    private static final String EXTENSION = ".xlsx";
    @Value("${jeecg.path.shippingInvoiceDir}")
    private String INVOICE_LOCATION;
    @Value("${jeecg.path.shippingInvoiceDetailDir}")
    private String INVOICE_DETAIL_LOCATION;
    @Value("${jeecg.path.shippingInvoicePdfDir}")
    private String INVOICE_PDF_LOCATION;
    @Value("${jeecg.path.shippingInvoiceDetailPdfDir}")
    private String INVOICE_DETAIL_PDF_LOCAION;

    @Autowired
    Environment env;
    /**
     * 分页列表查询
     *
     * @param shippingInvoice
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "物流发票-分页列表查询")
    @ApiOperation(value = "物流发票-分页列表查询", notes = "物流发票-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(ShippingInvoice shippingInvoice,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<ShippingInvoice> queryWrapper = QueryGenerator.initQueryWrapper(shippingInvoice, req.getParameterMap());
        Page<ShippingInvoice> page = new Page<ShippingInvoice>(pageNo, pageSize);
        IPage<ShippingInvoice> pageList = shippingInvoiceService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     *
     * @param shippingInvoicePage
     * @return
     */
    @AutoLog(value = "物流发票-添加")
    @ApiOperation(value = "物流发票-添加", notes = "物流发票-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody ShippingInvoicePage shippingInvoicePage) {
        ShippingInvoice shippingInvoice = new ShippingInvoice();
        BeanUtils.copyProperties(shippingInvoicePage, shippingInvoice);
        shippingInvoiceService.saveMain(shippingInvoice);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     *
     * @param shippingInvoicePage
     * @return
     */
    @AutoLog(value = "物流发票-编辑")
    @ApiOperation(value = "物流发票-编辑", notes = "物流发票-编辑")
    @PutMapping(value = "/edit")
    public Result<?> edit(@RequestBody ShippingInvoicePage shippingInvoicePage) {
        ShippingInvoice shippingInvoice = new ShippingInvoice();
        BeanUtils.copyProperties(shippingInvoicePage, shippingInvoice);
        ShippingInvoice shippingInvoiceEntity = shippingInvoiceService.getById(shippingInvoice.getId());
        if (shippingInvoiceEntity == null) {
            return Result.error("未找到对应数据");
        }
        shippingInvoiceService.updateMain(shippingInvoice);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "物流发票-通过id删除")
    @ApiOperation(value = "物流发票-通过id删除", notes = "物流发票-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        shippingInvoiceService.delMain(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "物流发票-批量删除")
    @ApiOperation(value = "物流发票-批量删除", notes = "物流发票-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.shippingInvoiceService.delBatchMain(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "物流发票-通过id查询")
    @ApiOperation(value = "物流发票-通过id查询", notes = "物流发票-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        ShippingInvoice shippingInvoice = shippingInvoiceService.getById(id);
        if (shippingInvoice == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(shippingInvoice);

    }


    /**
     * 导出excel
     *
     * @param request
     * @param shippingInvoice
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, ShippingInvoice shippingInvoice) {
        // Step.1 组装查询条件查询数据
        QueryWrapper<ShippingInvoice> queryWrapper = QueryGenerator.initQueryWrapper(shippingInvoice, request.getParameterMap());
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

        //Step.2 获取导出数据
        List<ShippingInvoice> queryList = shippingInvoiceService.list(queryWrapper);
        // 过滤选中数据
        String selections = request.getParameter("selections");
        List<ShippingInvoice> shippingInvoiceList = new ArrayList<ShippingInvoice>();
        if (oConvertUtils.isEmpty(selections)) {
            shippingInvoiceList = queryList;
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            shippingInvoiceList = queryList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
        }

        // Step.3 组装pageList
        List<ShippingInvoicePage> pageList = new ArrayList<ShippingInvoicePage>();
        for (ShippingInvoice main : shippingInvoiceList) {
            ShippingInvoicePage vo = new ShippingInvoicePage();
            BeanUtils.copyProperties(main, vo);
            pageList.add(vo);
        }

        // Step.4 AutoPoi 导出Excel
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.FILE_NAME, "物流发票列表");
        mv.addObject(NormalExcelConstants.CLASS, ShippingInvoicePage.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("物流发票数据", "导出人:" + sysUser.getRealname(), "物流发票"));
        mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        return mv;
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
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();// 获取上传文件对象
            ImportParams params = new ImportParams();
            params.setTitleRows(2);
            params.setHeadRows(1);
            params.setNeedSave(true);
            try {
                List<ShippingInvoicePage> list = ExcelImportUtil.importExcel(file.getInputStream(), ShippingInvoicePage.class, params);
                for (ShippingInvoicePage page : list) {
                    ShippingInvoice po = new ShippingInvoice();
                    BeanUtils.copyProperties(page, po);
                    shippingInvoiceService.saveMain(po);
                }
                return Result.OK("文件导入成功！数据行数:" + list.size());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Result.error("文件导入失败:" + e.getMessage());
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.OK("文件导入失败！");
    }

    /** Finds the absolute path of invoice file by recursivly walking the directory and it's subdirectories
     *
     * @param dirPath
     * @param invoiceNumber
     * @return List of paths for the file but should only find one result
     */
    public List<Path> getPath(String dirPath, String invoiceNumber) {
        List<Path> pathList = new ArrayList<>();
        //Recursively list all files
        //The walk() method returns a Stream by walking the file tree beginning with a given starting file/directory in a depth-first manner.
        System.out.println(dirPath);
        try (Stream<Path> stream = Files.walk(Paths.get(dirPath))) {
            pathList = stream.map(Path::normalize)
                    .filter(Files::isRegularFile) // directories, hidden files and files without extension are not included
                    .filter(path -> path.getFileName().toString().contains(invoiceNumber))
                    .filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                    .collect(Collectors.toList());
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return pathList;
    }

    /**
     *  Finds the absolute path of invoice file and return the path
     * @param invoiceNumber
     * @param filetype if it's an invoice or invoice detail
     * @return String returns the path of the invoice file
     */
    public String getInvoiceList(String invoiceNumber, String filetype) {
        System.out.println("invoice number : " + invoiceNumber);
        List<Path> pathList = new ArrayList<>();
        if(filetype.equals("invoice")) {
            System.out.println("File asked is of type invoice");
            pathList = getPath(INVOICE_LOCATION, invoiceNumber);
        }
        if(filetype.equals("detail")) {
            System.out.println("File asked is of type invoice detail");
            pathList = getPath(INVOICE_DETAIL_LOCATION, invoiceNumber);
        }
        if(pathList.isEmpty()) {
            System.out.println("NO INVOICE FILE FOUND : " + invoiceNumber);
            return "ERROR";
        }
        else {
            pathList.forEach(System.out::println);
            return pathList.get(0).toString();
        }
    }

    /**
     * Downloads the invoice and returns it in form of bytearray
     * @param invoiceNumber the invoice we want to download
     * @param filetype invoice or invoice detail
     * @return ResponseEntity if success then returns bytearray of the file, else return ERROR 404
     * @throws IOException
     */
    @GetMapping(value = "/downloadCompleteInvoiceExcel")
    public ResponseEntity<?> download(@RequestParam("invoiceNumber") String invoiceNumber, @RequestParam("filetype") String filetype) throws IOException {
        String filename = getInvoiceList(invoiceNumber, filetype);
        if(!filename.equals("ERROR")) {
            File file = new File(filename);

            System.out.println("filename : " + file);

            HttpHeaders header = new HttpHeaders();
            header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            header.add("Cache-Control", "no-cache, no-store, must-revalidate");
            header.add("Pragma", "no-cache");
            header.add("Expires", "0");

            Path path = Paths.get(file.getAbsolutePath());

            System.out.println("Absolute Path : " + path + "\nLength : " + file.length());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            return ResponseEntity.ok()
                    .headers(header)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(resource);
        }
        else {
            System.out.println("Couldn't find the invoice file for : " + invoiceNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Couldn't find the invoice file for : " + invoiceNumber);
        }
    }

    public String convertToPdf(String invoiceNumber, String fileType) throws Exception {
        String excelFilePath = getInvoiceList(invoiceNumber, fileType);// (C:\PATH\filename.xlsx)

        if(!excelFilePath.equals("ERROR")) {
            String pdfFilePath= INVOICE_PDF_LOCATION + "/" + invoiceNumber + ".pdf";
            if(fileType.equals("invoice")){
                pdfFilePath = INVOICE_PDF_LOCATION + "/Invoice N°" + invoiceNumber + ".pdf";
            }
            if(fileType.equals("detail")) {
                pdfFilePath = INVOICE_DETAIL_PDF_LOCAION + "/Détail_calcul_de_facture_" + invoiceNumber + ".pdf";
            }

            Pattern p = Pattern.compile("^(.*)[\\/\\\\](.*)(\\.[a-z]+)"); //group(1): "C:\PATH" , group(2) : "filename", group(3): ".xlsx"
            Matcher m = p.matcher(excelFilePath);
            if (m.matches()) {
                pdfFilePath = INVOICE_PDF_LOCATION + "/" + m.group(2) + ".pdf";
            }
            // Créé un classeur pour charger le fichier Excel
            Workbook workbook = new Workbook(excelFilePath);
            // On enregistre le document au format PDF
            workbook.save(pdfFilePath, SaveFormat.PDF);
            return pdfFilePath;
        }
        return "ERROR";
    }
    /**
     *
     * @param invoiceNumber
     * @return
     */
    @GetMapping(value = "/downloadPdf")
    public ResponseEntity<?> downloadPdf(@RequestParam("invoiceNumber") String invoiceNumber) throws Exception {
        String pdfFilePath = convertToPdf(invoiceNumber, "invoice");
        if(!pdfFilePath.equals("ERROR")) {
            File file = new File(pdfFilePath);
            System.out.println("file : " + file);
            HttpHeaders header = new HttpHeaders();
            header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdfFilePath);
            header.add("Cache-Control", "no-cache, no-store, must-revalidate");
            header.add("Pragma", "no-cache");
            header.add("Expires", "0");

            Path path = Paths.get(file.getAbsolutePath());

            System.out.println("Absolute Path : " + path + "\nLength : " + file.length());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            return ResponseEntity.ok()
                    .headers(header)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(resource);
        }
        System.out.println("Couldn't find the invoice file for : " + invoiceNumber);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_PLAIN)
                .body("Couldn't find the invoice file for : " + invoiceNumber);
    }
    @GetMapping(value = "/sendDetailsByEmail")
    public Result<?> sendDetailsByEmail(@RequestParam("invoiceNumber") String invoiceNumber,
                                        @RequestParam("invoiceID") String invoiceID,
                                        @RequestParam("email") String email,
                                        @RequestParam("invoiceEntity") String invoiceEntity) throws Exception {
        String filePath = getInvoiceList(invoiceNumber, "detail");
        System.out.println("Attachment file path : " + filePath);

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", env.getProperty("spring.mail.properties.mail.smtp.auth"));
        prop.put("mail.smtp.starttls.enable", env.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        prop.put("mail.smtp.host", env.getProperty("spring.mail.host"));
        prop.put("mail.smtp.port", "587");
        prop.put("mail.debug", "true");
        System.out.println("Prop : " + prop);
        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(env.getProperty("spring.mail.username"), env.getProperty("spring.mail.password"));
            }
        });
        try {
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(Objects.requireNonNull(env.getProperty("spring.mail.username"))));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));

            message.setSubject("Détails de facture N°" + invoiceNumber);

            BodyPart messageBodyPart = new MimeBodyPart();
            String text = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "    <body>\n" +
                    "        <table align=\"center\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" bgcolor=\"#FFF\" style=\"font-family:Arial,Helvetica,sans-serif;text-align:center;table-layout:fixed;font-size: 16px;border: 1px solid #0B49A6\">\n" +
                    "            <tbody>\n" +
                    "                <tr>\n" +
                    "                    <td width=\"600\" height=\"90\" bgcolor=\"#0B49A6\" valign=\"top\" align=\"center\" style=\"padding:20px 0;table-layout:fixed\">\n" +
                    "                        <a href=\"http://app.wia-sourcing.com/user/login\">\n" +
                    "                            <img src=\"https://wia-sourcing.com/wp-content/uploads/2022/10/Fichier-24Icons.png\" alt=\"logo\" width=\"360\" style=\"width:100%;max-width:360px;\">\n" +
                    "                        </a>\n" +
                    "                    </td>\n" +
                    "                </tr>\n" +
                    "                <tr>\n" +
                    "                    <td align=\"left\">\n" +
                    "                        <table width=\"520\" align=\"center\" style=\"color:#000;\">\n" +
                    "                            <tbody>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:35px 0;\">Cher Client(e),</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:0 0 35px 0;\">Vous trouverez en pièce-jointe le fichier que vous nous avez demandé :</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:10px 0;\"><b>Type de fichier :</b> Détails de facture</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:10px 0;\"><b>Client :</b> " + invoiceEntity + "</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:10px 0;\"><b>Numéro de facture :</b> <a href=\"http://app.wia-sourcing.com/business/admin/shippingInvoice/Invoice?invoiceID=" + invoiceID + "\">" + invoiceNumber + "</a></td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:35px 0 5px 0;\">Merci d’utiliser nos services.</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:5px 0;\">Cordialement</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding:5px 0 35px 0;\">L’équipe WIA Sourcing.</td>\n" +
                    "                                </tr>\n" +
                    "                            </tbody>\n" +
                    "                        </table>\n" +
                    "                    </td>\n" +
                    "                </tr>\n" +
                    "                <tr>\n" +
                    "                    <td align=\"left\" bgcolor=\"#0B49A6\"  width=\"600\">\n" +
                    "                        <table align=\"center\" width=\"520\">\n" +
                    "                            <tbody>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"font-style: italic;padding: 20px 0;\">Ce message a été envoyé automatiquement. Merci de ne pas répondre.Ce message et ainsi que toutes les pièces jointes sont confidentiels.</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"padding: 0 0 20px 0;\">Si vous avec reçu ce message par erreur, merci de nous avertir immédiatement et de détruire ce message.</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td>Service client :</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td>Pour obtenir plus d’informations concernant nos services, veuillez vous nous contacter à l’adresse ci dessous ou en visitant notre site web.</td>\n" +
                    "                                </tr>\n" +
                    "                            </tbody>\n" +
                    "                        </table>\n" +
                    "                        <table align=\"center\" width=\"520\" style=\"padding: 15px 0\">\n" +
                    "                            <tbody>\n" +
                    "                                <tr>\n" +
                    "                                    <td width=\"220\" style=\"text-align:center;border-radius:2em;padding:20px 10px 20px 0;;\" bgcolor=\"#EF5A1A\"><a href=\"info@wia-sourcing.com\" style=\"color:white;text-decoration:none\">Nous contacter</a></td>\n" +
                    "                                    <td width=\"40\"  ></td>\n" +
                    "                                    <td width=\"220\" style=\"text-align:center;border-radius:2em;padding:20px 0 20px 10px;\" bgcolor=\"#EF5A1A\"><a href=\"https://wia-sourcing.com/\" style=\"color:white;text-decoration:none\">Notre site web</a></td>\n" +
                    "                                </tr>\n" +
                    "                            </tbody>\n" +
                    "                        </table>\n" +
                    "                        <table align=\"center\" width=\"520\" style=\"padding: 0 0 35px 0;\">\n" +
                    "                            <tbody>\n" +
                    "                                <tr>\n" +
                    "                                    <td style=\"color:#EF5A1A;\">WIA SOURCING</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td>© 2018/2023 par WIA Sourcing Agency.</td>\n" +
                    "                                </tr>\n" +
                    "                                <tr>\n" +
                    "                                    <td>TOUS DROITS RÉSERVÉS©</td>\n" +
                    "                                </tr>\n" +
                    "                            </tbody>\n" +
                    "                        </table>\n" +
                    "                    </td>\n" +
                    "                </tr>\n" +
                    "            </tbody>\n" +
                    "        </table>\n" +
                    "    </body>\n" +
                    "</html>";
            messageBodyPart.setContent(text, "text/html; charset=utf-8");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(filePath));

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            Transport.send(message);

            return Result.OK("Email sent.");
        }
        catch(Exception e) {
            return Result.error("An error occured while trying to send an email.");
        }
    }

    /**
     * Fetches the shop owner's first name and surname via SQL and return the full name or null
     * @param invoiceNumber
     * @return if fetch successful returns full name of owner, else will return error
     * @throws IOException
     */
    @GetMapping(value = "/getInvoiceShopOwner")
    public Result<?> getShopOwnerNameFromInvoice(@RequestParam("invoiceNumber") String invoiceNumber) {
        System.out.println("Invoice Number : " + invoiceNumber);
        Client shopOwnerName =  shippingInvoiceService.getShopOwnerNameFromInvoiceNumber(invoiceNumber);
        if(shopOwnerName == null) {
            System.out.println("SHOP OWNER FETCH : NULL");
        }
        String fullname = shopOwnerName.fullName();
        System.out.println("client full name : "+fullname);
        return Result.OK(fullname);
    }
}
