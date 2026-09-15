package cn.aiedge.dms.task.mapper;

import cn.aiedge.dms.task.dto.TaskFilterOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配送查询-查询条件下拉选项 Mapper（只读）
 *
 * <p>数据源：配送司机/送货员 = dms_rider 档案，配送车辆 = dms_vehicle 档案，
 * 制单人 = 既有配送任务的制单人快照去重。全部只读，不重复建表。</p>
 */
@Mapper
public interface TaskFilterOptionMapper {

    /** 配送司机 / 送货员选项（配送员档案，企业员工与外部平台共用一张档案表） */
    @Select("SELECT id, real_name AS name, phone AS extra FROM dms_rider "
            + "WHERE deleted = 0 ORDER BY id")
    List<TaskFilterOptionVO> selectRiders();

    /** 配送车辆选项（车牌号 + 车辆编号） */
    @Select("SELECT id, plate_no AS name, vehicle_code AS extra FROM dms_vehicle "
            + "WHERE deleted = 0 ORDER BY id")
    List<TaskFilterOptionVO> selectVehicles();

    /** 制单人选项（历史任务制单人快照去重，避免引用已删除用户） */
    @Select("SELECT DISTINCT creator_name AS name FROM dms_task "
            + "WHERE deleted = 0 AND creator_name IS NOT NULL AND creator_name <> '' "
            + "ORDER BY creator_name")
    List<String> selectCreatorNames();
}
