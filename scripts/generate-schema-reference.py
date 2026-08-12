import csv
import os
import subprocess
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "DATABASE_SCHEMA_REFERENCE.md"
SCHEMA = os.getenv("RIKE_SCHEMA_REFERENCE_DATABASE", "rike_tiku_demo")
LATEST = 19

GROUPS = {
    "Authentication": {"yong_hu", "jiao_se", "yong_hu_jiao_se"},
    "Organization": {"ban_ji", "ban_ji_xue_sheng", "jiao_shi_dang_an", "xue_sheng_dang_an", "ren_ke_guan_xi"},
    "Question bank": {"ke_mu", "zhi_shi_dian", "ti_mu", "ti_mu_xuan_xiang", "ti_mu_jie_xi", "ti_mu_zhi_shi_dian", "ti_mu_lai_yuan", "ti_mu_fu_jian", "ti_mu_shen_he_ji_lu", "dao_ru_pi_ci"},
    "Practice": {"lian_xi_hui_hua", "lian_xi_ti_mu", "xue_sheng_da_ti", "xue_xi_jie_guo"},
    "Wrong / mastery": {"cuo_ti_ji_lu", "gao_pin_kao_dian"},
    "Communication": {"si_xin_hui_hua", "si_xin_xiao_xi"},
    "Audit": {"guan_li_cao_zuo_ri_zhi"},
    "AI Provider": {"ai_diao_yong_ri_zhi", "ai_mo_xing_pei_zhi"},
    "Student AI": {"ai_cuo_ti_fen_xi", "ai_hui_hua", "ai_xiao_xi"},
    "Student AI variants": {"ai_xue_sheng_bian_shi_shi_li"},
    "AI generation / vision": {"ai_sheng_cheng_ren_wu", "ai_hou_xuan_ti_zhi_liang_ping_jia", "ai_shi_jue_shang_xia_wen"},
    "Account recovery": {"mi_ma_chong_zhi_shen_qing"},
    "Paper": {"shi_juan", "shi_juan_ti_mu"},
}

PURPOSE = {
    "yong_hu": "登录账号与密码摘要", "jiao_se": "角色字典", "yong_hu_jiao_se": "账号角色关系",
    "ban_ji": "班级", "ban_ji_xue_sheng": "学生班级归属", "jiao_shi_dang_an": "教师档案",
    "xue_sheng_dang_an": "学生档案", "ren_ke_guan_xi": "教师—班级—科目授权",
    "ke_mu": "物理/化学/生物科目", "zhi_shi_dian": "层级知识点", "ti_mu": "题目及权威答案事实",
    "ti_mu_xuan_xiang": "选择题选项", "ti_mu_jie_xi": "STANDARD 解析", "ti_mu_zhi_shi_dian": "题目知识点",
    "ti_mu_lai_yuan": "题目来源与权利", "ti_mu_fu_jian": "图片/公式附件", "ti_mu_shen_he_ji_lu": "审核状态轨迹",
    "dao_ru_pi_ci": "题目导入批次", "lian_xi_hui_hua": "练习会话", "lian_xi_ti_mu": "冻结练习题",
    "xue_sheng_da_ti": "正式答题事实", "xue_xi_jie_guo": "练习最终结果", "cuo_ti_ji_lu": "错题生命周期",
    "gao_pin_kao_dian": "班级科目高频考点", "si_xin_hui_hua": "师生私信会话", "si_xin_xiao_xi": "师生私信消息",
    "guan_li_cao_zuo_ri_zhi": "管理员操作审计", "ai_diao_yong_ri_zhi": "AI 调用安全元数据",
    "ai_mo_xing_pei_zhi": "本地 AI Provider/模型配置", "ai_cuo_ti_fen_xi": "错题结构化 AI 分析",
    "ai_hui_hua": "当前题 AI 会话", "ai_xiao_xi": "当前题 AI 消息", "ai_sheng_cheng_ren_wu": "候选变式题生成任务",
    "ai_hou_xuan_ti_zhi_liang_ping_jia": "候选题人工质量评价", "ai_shi_jue_shang_xia_wen": "受控视觉上下文缓存",
    "ai_xue_sheng_bian_shi_shi_li": "绑定答题事实的学生结构化变式", "mi_ma_chong_zhi_shen_qing": "匿名密码恢复请求与处理事实",
    "shi_juan": "教师冻结试卷", "shi_juan_ti_mu": "试卷题目顺序与分值",
}

MIGRATION = {
    1: {"ke_mu", "zhi_shi_dian"},
    2: {"dao_ru_pi_ci", "ti_mu", "ti_mu_xuan_xiang", "ti_mu_jie_xi", "ti_mu_zhi_shi_dian", "ti_mu_lai_yuan", "ti_mu_fu_jian", "ti_mu_shen_he_ji_lu"},
    5: {"yong_hu", "jiao_se", "yong_hu_jiao_se", "jiao_shi_dang_an", "xue_sheng_dang_an"},
    6: {"ban_ji", "ban_ji_xue_sheng", "ren_ke_guan_xi"},
    7: {"lian_xi_hui_hua", "lian_xi_ti_mu", "xue_sheng_da_ti", "xue_xi_jie_guo", "cuo_ti_ji_lu"},
    8: {"gao_pin_kao_dian"}, 9: {"si_xin_hui_hua", "si_xin_xiao_xi"}, 11: {"guan_li_cao_zuo_ri_zhi"},
    12: {"ai_diao_yong_ri_zhi"}, 13: {"ai_cuo_ti_fen_xi", "ai_hui_hua", "ai_xiao_xi"},
    14: {"ai_mo_xing_pei_zhi", "ai_sheng_cheng_ren_wu", "ai_hou_xuan_ti_zhi_liang_ping_jia", "ai_shi_jue_shang_xia_wen"},
    17: {"mi_ma_chong_zhi_shen_qing"}, 18: {"shi_juan", "shi_juan_ti_mu"}, 19: {"ai_xue_sheng_bian_shi_shi_li"},
}


def migration(table):
    for version, tables in MIGRATION.items():
        if table in tables:
            return f"V{version}"
    return f"V1–V{LATEST} 演进"


def escape(value):
    return str(value or "—").replace("|", "\\|").replace("\n", " ")


def mysql_rows(query, fieldnames):
    command = [
        "mysql", "-h", os.getenv("RIKE_TIKU_DB_HOST", "localhost"),
        "-P", os.getenv("RIKE_TIKU_DB_PORT", "3306"),
        "-u", os.getenv("RIKE_TIKU_DB_USERNAME", "root"), "--default-character-set=utf8mb4",
        "--batch", "--raw", "--skip-column-names", "-e", query,
    ]
    environment = os.environ.copy()
    environment["MYSQL_PWD"] = environment["RIKE_TIKU_DB_PASSWORD"]
    result = subprocess.run(command, env=environment, text=True, encoding="utf-8", capture_output=True, check=True)
    return [dict(zip(fieldnames, row)) for row in csv.reader(result.stdout.splitlines(), delimiter="\t")]


columns = defaultdict(list)
for row in mysql_rows(f"""
    SELECT table_name,column_name,column_type,is_nullable,column_key,extra,
           REPLACE(REPLACE(COALESCE(column_comment,''),'\\t',' '),'\\n',' '),ordinal_position
    FROM information_schema.columns WHERE table_schema='{SCHEMA}' AND table_name<>'flyway_schema_history'
    ORDER BY table_name,ordinal_position
""", ["table_name", "column_name", "column_type", "is_nullable", "column_key", "extra", "column_comment", "ordinal_position"]):
    columns[row["table_name"]].append(row)

constraints = defaultdict(list)
for row in mysql_rows(f"""
    SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints
    WHERE table_schema='{SCHEMA}' AND table_name<>'flyway_schema_history' ORDER BY table_name,constraint_name
""", ["table_name", "constraint_name", "constraint_type"]):
    constraints[row["table_name"]].append(f'{row["constraint_type"]}:{row["constraint_name"]}')

lines = [
    f"# RIKE V{LATEST} 数据库结构参考", "",
    f"> 本文由 `information_schema` 只读生成，校验对象为隔离库 `{SCHEMA}` 的 Flyway V1–V{LATEST} 业务表。字段与约束以迁移脚本为准；`database/schema_snapshot_v{LATEST}.sql` 仅是便于查阅的纯结构快照，不能替代 Flyway。", "",
    "## 总体约定", "",
    "- MySQL 8.4，默认 `utf8mb4`。", "- 业务主键均为 `BIGINT` 自增标识；关系约束和状态枚举由外键、唯一索引、Check 与服务层共同维护。",
    "- `yi_shan_chu` 为软删除标识时，查询必须同时考虑状态字段。AI Key 只存在本地配置表，API/日志不得回显。", "",
]

for group, tables in GROUPS.items():
    lines += [f"## {group}", ""]
    for table in sorted(tables):
        lines += [f"### `{table}`", "", f"用途：{PURPOSE[table]}。创建/演进：{migration(table)}。", "",
                  "| 字段 | SQL 类型 | 可空 | 键/附加 | 说明 |", "|---|---|---:|---|---|"]
        for col in columns[table]:
            key = ", ".join(value for value in [col["column_key"], col["extra"]] if value) or "—"
            lines.append(f'| `{col["column_name"]}` | `{escape(col["column_type"])}` | {"是" if col["is_nullable"] == "YES" else "否"} | {escape(key)} | {escape(col["column_comment"])} |')
        lines += ["", "约束：" + ("；".join(f"`{escape(c)}`" for c in constraints[table]) or "无命名约束") + "。",
                  f"生命周期：由{PURPOSE[table]}对应服务创建和更新；归档/删除遵循表内状态、外键和业务审计规则。", ""]

OUTPUT.write_text("\n".join(lines), encoding="utf-8")
print(f"tables={len(columns)}")
print(f"output={OUTPUT.name}")
