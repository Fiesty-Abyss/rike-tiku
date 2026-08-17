from __future__ import annotations

import hashlib
import json
import os
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATABASE = "rike_tiku"
SOURCE_NAME = "用户提供的物化生高频考点提纲与项目整理"


def sql(value: object) -> str:
    if value is None:
        return "NULL"
    text = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{text}'"


def mysql(query: str, capture: bool = False) -> str:
    password = os.environ.get("RIKE_TIKU_DB_PASSWORD")
    if not password:
        raise SystemExit("FORMAL_SYNC_BLOCKED: RIKE_TIKU_DB_PASSWORD is absent")
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    command = ["mysql", "--default-character-set=utf8mb4", "-uroot", "-h",
               env.get("RIKE_TIKU_DB_HOST", "localhost"), "-P", env.get("RIKE_TIKU_DB_PORT", "3306"),
               "--batch", "--skip-column-names", "--raw", DATABASE]
    if capture:
        command.extend(["-e", query])
        input_text = None
    else:
        input_text = query
    result = subprocess.run(command, input=input_text, env=env, capture_output=True,
                            text=True, encoding="utf-8", check=False)
    if result.returncode:
        raise SystemExit(f"FORMAL_SYNC_FAILED={result.returncode}")
    return result.stdout.strip()


def rows(query: str) -> list[tuple[str, ...]]:
    output = mysql(query, capture=True)
    return [tuple(line.split("\t")) for line in output.splitlines() if line]


def one(query: str) -> str:
    result = rows(query)
    return result[0][0] if result else ""


def main() -> None:
    if DATABASE != "rike_tiku" or DATABASE in {"mysql", "information_schema", "performance_schema", "sys", "rike_tiku_demo"}:
        raise SystemExit("FORMAL_SYNC_GUARD_FAILED")
    content = json.loads((ROOT / "docs/content/high-frequency-points.v2.json").read_text(encoding="utf-8"))
    topic = json.loads((ROOT / "docs/content/topic-units.v2.json").read_text(encoding="utf-8"))
    if len(content) != 30 or len(topic["units"]) != 15 or sum(len(item["questions"]) for item in topic["units"]) != 45:
        raise SystemExit("CONTENT_VERSION_GUARD_FAILED")
    if one("SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1") != "30":
        raise SystemExit("FORMAL_FLYWAY_GUARD_FAILED")
    if one("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history'") != "50":
        raise SystemExit("FORMAL_TABLE_COUNT_GUARD_FAILED")

    scope_rows = rows("""
        SELECT r.id,r.ke_mu_id,k.ke_mu_dai_ma,j.yong_hu_id
        FROM ren_ke_guan_xi r JOIN ke_mu k ON k.id=r.ke_mu_id
        JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id
        WHERE r.zhuang_tai='ACTIVE' AND j.zhuang_tai='ACTIVE' AND j.yi_shan_chu=0
        ORDER BY r.ke_mu_id,r.id
    """)
    scopes: dict[str, list[tuple[int, int, int]]] = {}
    for scope_id, subject_id, subject_code, creator_id in scope_rows:
        scopes.setdefault(subject_code, []).append((int(scope_id), int(subject_id), int(creator_id)))
    if set(scopes) != {"PHYSICS", "CHEMISTRY", "BIOLOGY"} or any(not value for value in scopes.values()):
        raise SystemExit("FORMAL_SCOPE_GUARD_FAILED")

    point_rows = rows("SELECT id,ke_mu_id FROM zhi_shi_dian WHERE zhuang_tai='ACTIVE' AND yi_shan_chu=0")
    points = {int(point_id): int(subject_id) for point_id, subject_id in point_rows}
    card_rows = rows("SELECT id,ren_ke_guan_xi_id,zhi_shi_dian_id,HEX(biao_ti) FROM gao_pin_kao_dian WHERE yi_shan_chu=0")
    cards = {(int(scope_id), int(point_id), bytes.fromhex(title).decode("utf-8")): int(card_id)
             for card_id, scope_id, point_id, title in card_rows}
    statements: list[str] = ["START TRANSACTION"]
    cards_updated = 0
    cards_inserted = 0
    for item in content:
        subject_code = item["subjectCode"]
        for scope_id, subject_id, creator_id in scopes[subject_code]:
            point_id = int(item["knowledgePointId"])
            if points.get(point_id) != subject_id:
                raise SystemExit(f"CONTENT_POINT_SCOPE_GUARD_FAILED={point_id}")
            key = (scope_id, point_id, item["title"])
            fields = ",".join([
                f"zi_liao_lei_xing={sql(item['type'])}", f"biao_ti={sql(item['title'])}",
                f"nei_rong={sql(item['content'])}", f"ke_xue_nei_rong={sql(item['scientificContent'])}",
                f"latex_nei_rong={sql(item['latex'])}", f"shi_yong_tiao_jian={sql(item['applicableConditions'])}",
                f"han_yi_tui_dao={sql(item['derivation'])}", f"chang_jian_wu_qu={sql(item['commonMistake'])}",
                f"li_zi={sql(item['example'])}", f"ji_yi_kou_jue={sql(item['mnemonic'])}",
                f"lai_yuan_ming_cheng={sql(SOURCE_NAME)}", "lai_yuan_di_zhi=NULL",
                f"quan_li_zhuang_tai={sql('USER_PROVIDED')}", f"chuang_jian_ren_yong_hu_id={creator_id}",
                f"pai_xu={int(item['sortOrder'])}", "zhuang_tai='PUBLISHED'", "yi_shan_chu=0",
            ])
            card_id = cards.get(key)
            if card_id:
                statements.append(f"UPDATE gao_pin_kao_dian SET {fields} WHERE id={card_id}")
                cards_updated += 1
            else:
                statements.append(
                    "INSERT INTO gao_pin_kao_dian(ren_ke_guan_xi_id,zhi_shi_dian_id,zi_liao_lei_xing,biao_ti,nei_rong,ke_xue_nei_rong,latex_nei_rong,shi_yong_tiao_jian,han_yi_tui_dao,chang_jian_wu_qu,li_zi,ji_yi_kou_jue,lai_yuan_ming_cheng,lai_yuan_di_zhi,quan_li_zhuang_tai,chuang_jian_ren_yong_hu_id,pai_xu,zhuang_tai) "
                    f"VALUES ({scope_id},{point_id},{sql(item['type'])},{sql(item['title'])},{sql(item['content'])},{sql(item['scientificContent'])},{sql(item['latex'])},{sql(item['applicableConditions'])},{sql(item['derivation'])},{sql(item['commonMistake'])},{sql(item['example'])},{sql(item['mnemonic'])},{sql(SOURCE_NAME)},NULL,{sql('USER_PROVIDED')},{creator_id},{int(item['sortOrder'])},'PUBLISHED')"
                )
                statements.append(
                    "INSERT INTO gao_pin_kao_dian_zhi_shi_dian(gao_pin_kao_dian_id,zhi_shi_dian_id,pai_xu) "
                    f"SELECT id,{point_id},1 FROM gao_pin_kao_dian WHERE ren_ke_guan_xi_id={scope_id} AND zhi_shi_dian_id={point_id} AND biao_ti={sql(item['title'])} AND yi_shan_chu=0 ORDER BY id DESC LIMIT 1"
                )
                cards_inserted += 1

    unit_rows = rows("SELECT id,ke_mu_id,HEX(biao_ti), (SELECT COUNT(*) FROM zhuan_ti_xue_xi_dan_yuan_ti_mu i WHERE i.dan_yuan_id=u.id) FROM zhuan_ti_xue_xi_dan_yuan u WHERE u.yi_shan_chu=0")
    units = {(int(subject_id), bytes.fromhex(title).decode("utf-8")): (int(unit_id), int(question_count))
             for unit_id, subject_id, title, question_count in unit_rows}
    question_rows = rows("SELECT id,ke_mu_id,HEX(ti_gan) FROM ti_mu WHERE yi_shan_chu=0")
    questions = {(int(subject_id), bytes.fromhex(stem).decode("utf-8")): int(question_id)
                 for question_id, subject_id, stem in question_rows}
    units_created = 0
    questions_created = 0
    relation_statements = 0
    for unit_order, definition in enumerate(topic["units"], start=1):
        subject_code = definition["subjectCode"]
        scope_id, subject_id, creator_id = scopes[subject_code][0]
        primary = int(definition["primaryKnowledgePointId"])
        if points.get(primary) != subject_id:
            raise SystemExit(f"UNIT_POINT_SCOPE_GUARD_FAILED={primary}")
        unit_key = (subject_id, definition["title"])
        existing_unit = units.get(unit_key)
        if existing_unit:
            if existing_unit[1] != 3:
                raise SystemExit(f"EXISTING_UNIT_NOT_THREE_QUESTIONS={definition['title']}")
            unit_id = existing_unit[0]
            statements.append(
                "UPDATE zhuan_ti_xue_xi_dan_yuan SET jian_jie=" + sql(definition["introduction"])
                + ",nan_du_ceng_ji=" + str(int(definition["difficulty"]))
                + ",zhu_zhi_shi_dian_id=" + str(primary) + ",pai_xu=" + str(unit_order)
                + ",zhuang_tai='PUBLISHED',yi_shan_chu=0 WHERE id=" + str(unit_id)
            )
            for order, item in enumerate(definition["questions"], start=1):
                stem = f"【专题演示】{item['title']}｜{item['stem']}"
                digest = hashlib.sha256(stem.encode("utf-8")).hexdigest()
                current_question = one(
                    "SELECT i.ti_mu_id FROM zhuan_ti_xue_xi_dan_yuan_ti_mu i "
                    f"WHERE i.dan_yuan_id={unit_id} AND i.xue_xi_jie_duan={sql(item['stage'])}"
                )
                released = current_question and one(
                    "SELECT COUNT(*) FROM shi_juan_fa_bu_ti_mu WHERE ti_mu_id=" + current_question
                ) != "0"
                if current_question and not released:
                    statements.append(
                        "UPDATE ti_mu SET zhuan_ti_lei_xing=" + sql(item["topicType"])
                        + ",ti_gan=" + sql(stem) + ",zheng_que_da_an=CAST(" + sql('{"schemaVersion":1,"type":"SUBJECTIVE"}')
                        + " AS JSON),nan_du=" + str(int(item["difficulty"]))
                        + ",nan_du_shuo_ming=" + sql("专题学习题，学生分步作答；不自动评分")
                        + ",shi_fou_ke_zi_dong_pan_fen=0,zhuang_tai='PUBLISHED',nei_rong_ha_xi=" + sql(digest)
                        + " WHERE id=" + current_question
                    )
                    statements.append("UPDATE ti_mu_jie_xi SET jie_xi_nei_rong=" + sql(item["standardAnalysis"])
                                      + ",zhuang_tai='PUBLISHED' WHERE ti_mu_id=" + current_question
                                      + " AND jie_xi_lei_xing='STANDARD' AND yi_shan_chu=0")
                    statements.append("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=" + current_question)
                    statements.append("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES ("
                                      + current_question + "," + str(int(item["knowledgePointId"])) + ",1,1)")
                    statements.append("UPDATE zhuan_ti_xue_xi_dan_yuan_ti_mu SET pai_xu=" + str(order)
                                      + " WHERE dan_yuan_id=" + str(unit_id) + " AND ti_mu_id=" + current_question)
                else:
                    # A published-paper snapshot must keep its original question.  Create a new
                    # authored topic question and repoint only the teaching arrangement.
                    answer = '{"schemaVersion":1,"type":"SUBJECTIVE"}'
                    statements.append(
                        "INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,zhuan_ti_lei_xing,ke_jian_fan_wei,ren_ke_guan_xi_id,chuang_jian_ren_id,ti_gan,zheng_que_da_an,nan_du,nan_du_shuo_ming,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) "
                        f"VALUES ({subject_id},'SUBJECTIVE','TOPIC_LEARNING',{sql(item['topicType'])},'GLOBAL',NULL,{creator_id},{sql(stem)},CAST({sql(answer)} AS JSON),{int(item['difficulty'])},{sql('专题学习题，学生分步作答；不自动评分')},0,'PUBLISHED',{sql(digest)})"
                    )
                    statements.append("SET @replacement_topic_question_id=LAST_INSERT_ID()")
                    statements.append("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (@replacement_topic_question_id,'STANDARD',"
                                      + sql(item["standardAnalysis"]) + ",1,'PUBLISHED')")
                    statements.append("SET @replacement_topic_standard_analysis_id=LAST_INSERT_ID()")
                    if current_question:
                        # Preserve the controlled files used by the authored topic question while
                        # keeping the released-paper snapshot attached to the old immutable fact.
                        statements.append(
                            "INSERT INTO ti_mu_fu_jian(ti_mu_id,ti_mu_xuan_xiang_id,ti_mu_jie_xi_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,nei_rong_ha_xi,dui_xiang_biao_shi,zheng_wen_zi_fu_wei_zhi,yuan_shi_ye_ma,fu_jian_shuo_ming,pai_xu,zhuang_tai,yi_shan_chu) "
                            "SELECT @replacement_topic_question_id,NULL,CASE WHEN f.guan_lian_wei_zhi='STANDARD_ANALYSIS' THEN @replacement_topic_standard_analysis_id ELSE NULL END,"
                            "f.guan_lian_wei_zhi,f.fu_jian_lei_xing,f.yuan_shi_wen_jian_ming,f.xiang_dui_lu_jing,f.nei_rong_ha_xi,f.dui_xiang_biao_shi,f.zheng_wen_zi_fu_wei_zhi,f.yuan_shi_ye_ma,f.fu_jian_shuo_ming,f.pai_xu,f.zhuang_tai,f.yi_shan_chu "
                            "FROM ti_mu_fu_jian f WHERE f.ti_mu_id=" + current_question
                            + " AND f.guan_lian_wei_zhi IN ('QUESTION','STANDARD_ANALYSIS','ANSWER')"
                        )
                    statements.append("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (@replacement_topic_question_id,"
                                      + str(int(item["knowledgePointId"])) + ",1,1)")
                    statements.append("UPDATE zhuan_ti_xue_xi_dan_yuan_ti_mu SET ti_mu_id=@replacement_topic_question_id,pai_xu="
                                      + str(order) + " WHERE dan_yuan_id=" + str(unit_id) + " AND xue_xi_jie_duan=" + sql(item["stage"]))
            continue
        statements.append(
            "INSERT INTO zhuan_ti_xue_xi_dan_yuan(ke_mu_id,biao_ti,jian_jie,nan_du_ceng_ji,zhu_zhi_shi_dian_id,pai_xu,zhuang_tai,chuang_jian_ren_id,lai_yuan_lei_xing,lai_yuan_ming_cheng,quan_li_zhuang_tai) "
            f"VALUES ({subject_id},{sql(definition['title'])},{sql(definition['introduction'])},{int(definition['difficulty'])},{primary},{unit_order},'PUBLISHED',{creator_id},'PROJECT_AUTHORED',{sql(SOURCE_NAME)},'USER_PROVIDED')"
        )
        statements.append("SET @new_unit_id=LAST_INSERT_ID()")
        units_created += 1
        for order, item in enumerate(definition["questions"], start=1):
            stem = f"【专题演示】{item['title']}｜{item['stem']}"
            question_id = questions.get((subject_id, stem))
            if question_id is None:
                digest = hashlib.sha256(stem.encode("utf-8")).hexdigest()
                answer = '{"schemaVersion":1,"type":"SUBJECTIVE"}'
                statements.append(
                    "INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,zhuan_ti_lei_xing,ke_jian_fan_wei,ren_ke_guan_xi_id,chuang_jian_ren_id,ti_gan,zheng_que_da_an,nan_du,nan_du_shuo_ming,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) "
                    f"VALUES ({subject_id},'SUBJECTIVE','TOPIC_LEARNING',{sql(item['topicType'])},'GLOBAL',NULL,{creator_id},{sql(stem)},{sql(answer)},{int(item['difficulty'])},{sql('专题学习题，学生自拟作答')},0,'PUBLISHED',{sql(digest)})"
                )
                statements.append("SET @new_question_id=LAST_INSERT_ID()")
                statements.append(
                    "INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) "
                    f"VALUES (@new_question_id,'STANDARD',{sql(item['standardAnalysis'])},1,'PUBLISHED')"
                )
                statements.append(
                    "INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) "
                    f"VALUES (@new_question_id,{int(item['knowledgePointId'])},1,1)"
                )
                question_reference = "@new_question_id"
                questions_created += 1
            else:
                question_reference = str(question_id)
            statements.append(
                "INSERT INTO zhuan_ti_xue_xi_dan_yuan_ti_mu(dan_yuan_id,ti_mu_id,xue_xi_jie_duan,pai_xu) "
                f"VALUES (@new_unit_id,{question_reference},{sql(item['stage'])},{order})"
            )
            relation_statements += 1
    statements.append("COMMIT")
    mysql(";\n".join(statements) + ";")

    print("FORMAL_THIRD_ROUND_CONTENT_APPLIED")
    print(f"CARDS_UPDATED={cards_updated}")
    print(f"CARDS_INSERTED={cards_inserted}")
    print(f"UNITS_CREATED={units_created}")
    print(f"QUESTIONS_CREATED={questions_created}")
    print(f"UNIT_RELATIONS_WRITTEN={relation_statements}")
    print("FLYWAY=V30")
    print("BUSINESS_TABLES=50")


if __name__ == "__main__":
    main()
