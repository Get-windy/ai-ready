# -*- coding: utf-8 -*-
"""系统模块审计 · DB 实测采集
输出 tools/system-db.json（UTF-8），供报告引用。
"""
import json, io, sys
import psycopg2

DSN = "host=localhost port=5432 dbname=devdb user=devuser password=devuser123"
OUT = r"I:\AI-Ready\tools\system-db.json"

Q = {
    # 系统模块菜单全树（60013 起 4 层）
    "menu_tree": """WITH RECURSIVE t AS (
          SELECT * FROM sys_menu WHERE id=60013
          UNION ALL SELECT m.* FROM sys_menu m JOIN t ON m.parent_id=t.id)
        SELECT id,parent_id,menu_name,menu_code,client_type,path,component,list_path,tag_label,
        menu_type,display_mode,visible,status,tenant_id,sort,icon,is_cache,menu_level
        FROM t ORDER BY sort,id""",
    # 平台侧全部菜单（system-admin），不限于 60013 子树
    "menu_system_admin": """SELECT id,parent_id,menu_name,menu_code,path,component,display_mode,visible,status,tenant_id,menu_level
        FROM sys_menu WHERE client_type='system-admin' AND deleted=0 ORDER BY id""",
    # 全库 menu_code 重复
    "dup_menu_code": """SELECT menu_code,count(*) c,string_agg(id::text,',') ids,string_agg(path,' | ') paths
        FROM sys_menu WHERE menu_code IS NOT NULL AND menu_code<>'' GROUP BY menu_code HAVING count(*)>1 ORDER BY c DESC""",
    # 系统相关权限码（库中）
    "perm_sys": """SELECT id,permission_code,permission_name,permission_type,status FROM sys_permission
        WHERE permission_code LIKE 'system:%' OR permission_code LIKE 'platform:%' OR permission_code LIKE 'monitor:%'
        OR permission_code LIKE 'datasource:%' OR permission_code LIKE 'module:%' OR permission_code LIKE 'tenant:%'
        OR permission_code LIKE 'menu:%' OR permission_code LIKE 'cache:%' OR permission_code LIKE 'scheduler:%'
        ORDER BY permission_code""",
    # 权限总数 / 角色数
    "perm_count": "SELECT count(*) FROM sys_permission",
    # 系统模块相关权限的授权情况（非超管角色）
    "perm_grant": """SELECT p.permission_code,count(rp.role_id) role_cnt,string_agg(r.role_name||'(id'||r.id||')',',') roles
        FROM sys_permission p LEFT JOIN sys_role_permission rp ON rp.permission_id=p.id
        LEFT JOIN sys_role r ON r.id=rp.role_id
        WHERE p.permission_code LIKE 'system:%' GROUP BY p.permission_code ORDER BY role_cnt DESC, p.permission_code""",
    # 全部权限码（用于反查代码引用是否存在）
    "perm_all": "SELECT id,permission_code,status FROM sys_permission ORDER BY permission_code",
    # 角色清单
    "roles": "SELECT id,role_name,role_code,role_type,scope,data_scope,status,tenant_id,deleted FROM sys_role ORDER BY id",
    # 用户角色分布
    "user_role_stat": """SELECT r.id,r.role_name,r.role_code,count(ur.user_id) users FROM sys_role r
        LEFT JOIN sys_user_role ur ON ur.role_id=r.id GROUP BY r.id,r.role_name,r.role_code ORDER BY users DESC""",
    # 系统模块表行数
    "table_rows": """SELECT relname, n_live_tup FROM pg_stat_user_tables
        WHERE relname IN ('sys_backup_record','sys_data_source','sys_slow_query','sys_sync_task','sys_data_cleanup_rule',
        'sys_scheduled_task','sys_task_execute_log','sys_job','sys_job_log','sys_system_log','sys_oper_log','sys_login_log',
        'sys_audit_log','sys_audit_rule','sys_alert_rule','sys_alert_history','sys_module','sys_module_version',
        'sys_module_permission','sys_tenant','sys_tenant_package','sys_tenant_quota','sys_tenant_module','sys_tenant_menu',
        'sys_mail_config','sys_sms_config','sys_storage_config','sys_security_policy','sys_config','sys_dict_type',
        'sys_dict_item','sys_role','sys_permission','sys_user','sys_department','sys_dept','sys_message','sys_webhook',
        'sys_automation_rule','sys_record_rule','sys_data_scope','sys_field_permission','sys_sod_rule','sys_tenant_profile',
        'sys_permission_template','sys_message_template','sys_notification_template','sys_file','sys_project_config')
        ORDER BY n_live_tup DESC""",
    # 系统模块相关表清单（含列数与主键）
    "table_meta": """SELECT t.table_name, count(c.column_name) cols,
        (SELECT string_agg(kcu.column_name,',') FROM information_schema.key_column_usage kcu
          JOIN information_schema.table_constraints tc ON tc.constraint_name=kcu.constraint_name
          WHERE tc.table_name=t.table_name AND tc.constraint_type='PRIMARY KEY') pk,
        (SELECT count(*) FROM information_schema.table_constraints tc WHERE tc.table_name=t.table_name AND tc.constraint_type='UNIQUE') uq
        FROM information_schema.tables t JOIN information_schema.columns c ON c.table_name=t.table_name
        WHERE t.table_schema='public' AND (t.table_name LIKE 'sys\\_%' OR t.table_name LIKE 'sync\\_%')
        GROUP BY t.table_name ORDER BY cols DESC""",
}


def main():
    conn = psycopg2.connect(DSN)
    cur = conn.cursor()
    out = {}
    for k, sql in Q.items():
        try:
            cur.execute(sql)
            cols = [d[0] for d in cur.description]
            out[k] = [dict(zip(cols, [str(v) if v is not None else None for v in r])) for r in cur.fetchall()]
        except Exception as e:
            conn.rollback()
            out[k] = {"error": str(e)}
    cur.close(); conn.close()
    io.open(OUT, "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False, indent=1))
    sys.stdout.reconfigure(encoding="utf-8")
    print("menu_tree:", len(out["menu_tree"]))
    print("perm_sys:", len(out["perm_sys"]), "perm_count:", out["perm_count"])
    print("dup_menu_code:", len(out["dup_menu_code"]))
    print("roles:", len(out["roles"]))


if __name__ == "__main__":
    main()
