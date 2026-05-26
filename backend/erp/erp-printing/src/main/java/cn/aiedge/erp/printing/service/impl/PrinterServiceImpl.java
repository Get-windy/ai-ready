package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.erp.printing.dto.PrinterStatusDTO;
import cn.aiedge.erp.printing.entity.Printer;
import cn.aiedge.erp.printing.entity.PrinterGroup;
import cn.aiedge.erp.printing.mapper.PrinterMapper;
import cn.aiedge.erp.printing.mapper.PrinterGroupMapper;
import cn.aiedge.erp.printing.mapper.PrintTaskMapper;
import cn.aiedge.erp.printing.service.PrinterService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrinterServiceImpl implements PrinterService {

    private final PrinterMapper printerMapper;
    private final PrinterGroupMapper groupMapper;
    private final PrintTaskMapper taskMapper;

    @Override
    @Transactional
    public Printer createPrinter(Printer printer) {
        printer.setPrinterCode("PTR" + IdUtil.fastSimpleUUID().substring(0, 8));
        printer.setStatus(1);
        printer.setIsOnline(true);
        printer.setIsDefault(false);
        printer.setLastOnlineTime(LocalDateTime.now());
        printerMapper.insert(printer);
        return printer;
    }

    @Override
    @Transactional
    public Printer updatePrinter(Long id, Printer printer) {
        Printer existing = printerMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("打印机不存在: " + id);
        }
        printer.setId(id);
        printerMapper.updateById(printer);
        return printerMapper.selectById(id);
    }

    @Override
    public Printer getPrinterById(Long id) {
        return printerMapper.selectById(id);
    }

    @Override
    public Page<Printer> listPrinters(Integer page, Integer size, String printerType, String status) {
        Page<Printer> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Printer> wrapper = new LambdaQueryWrapper<>();
        if (printerType != null && !printerType.isEmpty()) {
            wrapper.eq(Printer::getPrinterType, printerType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Printer::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByAsc(Printer::getPrinterName);
        return printerMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void deletePrinter(Long id) {
        printerMapper.deleteById(id);
    }

    @Override
    public PrinterStatusDTO getPrinterStatus(Long id) {
        Printer printer = printerMapper.selectById(id);
        if (printer == null) {
            throw new RuntimeException("打印机不存在: " + id);
        }
        PrinterStatusDTO dto = new PrinterStatusDTO();
        dto.setPrinterId(id);
        dto.setPrinterName(printer.getPrinterName());
        dto.setStatus(printer.getStatus() == 1 ? "ONLINE" : "OFFLINE");
        dto.setIsOnline(printer.getIsOnline());
        dto.setActiveTasks(taskMapper.countActiveTasksByPrinter(id));
        dto.setPaperStatus(printer.getPaperStatus());
        dto.setInkStatus(printer.getInkStatus());
        return dto;
    }

    @Override
    @Transactional
    public void updatePrinterStatus(Long id, String status, Boolean isOnline) {
        Printer printer = printerMapper.selectById(id);
        if (printer == null) {
            throw new RuntimeException("打印机不存在: " + id);
        }
        printer.setStatus(status.equals("ONLINE") ? 1 : 0);
        printer.setIsOnline(isOnline);
        if (isOnline) {
            printer.setLastOnlineTime(LocalDateTime.now());
        }
        printerMapper.updateById(printer);
    }

    @Override
    @Transactional
    public PrinterGroup createGroup(PrinterGroup group) {
        group.setGroupCode("GRP" + IdUtil.fastSimpleUUID().substring(0, 8));
        group.setStatus(1);
        groupMapper.insert(group);
        return group;
    }

    @Override
    @Transactional
    public PrinterGroup updateGroup(Long id, PrinterGroup group) {
        PrinterGroup existing = groupMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("打印机分组不存在: " + id);
        }
        group.setId(id);
        groupMapper.updateById(group);
        return groupMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        groupMapper.deleteById(id);
    }

    @Override
    public Page<PrinterGroup> listGroups(Integer page, Integer size) {
        Page<PrinterGroup> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<PrinterGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PrinterGroup::getSortOrder);
        return groupMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void assignPrintersToGroup(Long groupId, List<Long> printerIds) {
        for (Long printerId : printerIds) {
            Printer printer = printerMapper.selectById(printerId);
            if (printer != null) {
                printer.setGroupId(groupId);
                printerMapper.updateById(printer);
            }
        }
    }

    @Override
    public List<Printer> listPrintersByGroup(Long groupId) {
        LambdaQueryWrapper<Printer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Printer::getGroupId, groupId)
               .eq(Printer::getStatus, 1)
               .orderByAsc(Printer::getPrinterName);
        return printerMapper.selectList(wrapper);
    }

    @Override
    public Printer getDefaultPrinter(Long groupId) {
        LambdaQueryWrapper<Printer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Printer::getGroupId, groupId)
               .eq(Printer::getIsDefault, true)
               .eq(Printer::getStatus, 1);
        return printerMapper.selectOne(wrapper);
    }
}