package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.*;
import cn.aiedge.erp.printing.entity.Printer;
import cn.aiedge.erp.printing.entity.PrinterGroup;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface PrinterService {

    Printer createPrinter(Printer printer);

    Printer updatePrinter(Long id, Printer printer);

    Printer getPrinterById(Long id);

    Page<Printer> listPrinters(Integer page, Integer size, String printerType, String status);

    void deletePrinter(Long id);

    PrinterStatusDTO getPrinterStatus(Long id);

    void updatePrinterStatus(Long id, String status, Boolean isOnline);

    PrinterGroup createGroup(PrinterGroup group);

    PrinterGroup updateGroup(Long id, PrinterGroup group);

    void deleteGroup(Long id);

    Page<PrinterGroup> listGroups(Integer page, Integer size);

    void assignPrintersToGroup(Long groupId, List<Long> printerIds);

    List<Printer> listPrintersByGroup(Long groupId);

    Printer getDefaultPrinter(Long groupId);
}