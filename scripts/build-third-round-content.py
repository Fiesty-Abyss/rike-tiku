from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "docs" / "content"


def math(value: str) -> str:
    return value if value.startswith((r"\(", r"\[")) else rf"\({value}\)"


def question(title: str, stage: str, topic_type: str, difficulty: int, stem: str, analysis: str, point: int) -> dict:
    # The source strings below are intentionally compact. Repair the few Python
    # escape sequences that would otherwise turn TeX commands into control
    # characters before writing the JSON runtime contract.
    def repair(value: str) -> str:
        return (value.replace("\f", r"\frac")
                .replace("\text", r"\text")
                .replace("\times", r"\times")
                .replace("\varepsilon", r"\varepsilon")
                .replace("\vec", r"\vec")
                .replace("\nu", r"\nu"))

    stem = repair(stem)
    analysis = repair(analysis)
    return {
        "title": title,
        "stage": stage,
        "topicType": topic_type,
        "difficulty": difficulty,
        "knowledgePointId": point,
        "stem": stem,
        "standardAnalysis": analysis,
    }


def unit(subject: str, title: str, introduction: str, difficulty: int, primary: int, questions: list[dict]) -> dict:
    return {
        "subjectCode": subject,
        "title": title,
        "introduction": introduction,
        "difficulty": difficulty,
        "primaryKnowledgePointId": primary,
        "questions": questions,
    }


def build_units() -> list[dict]:
    return [
        unit("PHYSICS", "物理专题单元·力学与实验综合", "按测量、受力、动量与能量逐步完成力学专题训练。", 3, 25, [
            question("力学综合计算", "FOUNDATION", "CALCULATION", 3, "质量为2 kg的物块由静止开始做匀加速运动，4 s内位移16 m。求加速度和合力，并说明如何检查单位。", "先由位移关系确定加速度，再用\(F=ma\)求合力；代入前统一质量、时间和位移单位。", 25),
            question("动量与能量综合", "TRANSFER", "CALCULATION", 3, "质量为1 kg的滑块以6 m/s运动，与静止的2 kg滑块发生完全非弹性碰撞。求共同速度，并说明碰撞前后机械能是否守恒。", "碰撞阶段先用\(m_1v_1+m_2v_2=(m_1+m_2)v\)；碰撞损失的机械能转化为内能，不能直接用机械能守恒。", 18),
            question("纸带实验误差", "ADVANCED", "EXPERIMENT", 3, "用纸带研究小车的匀加速运动，连续相等时间内的位移依次增大。说明判断依据、加速度求法和减小随机误差的方法。", "相等时间间隔内位移差近似相等是匀加速的证据；可用\(a=\frac{\Delta v}{\Delta t}\)或逐差法处理，并增加测量次数取平均值。", 33),
        ]),
        unit("PHYSICS", "物理专题单元·电学、电磁与光学", "围绕电路故障、电磁感应和光学实验完成跨情境分析。", 3, 38, [
            question("电路故障分析", "FOUNDATION", "MATERIAL_ANALYSIS", 3, "小灯泡串联电路闭合后不亮，电流表示数为零，电压表示数接近电源电压。判断最可能的故障并说明理由。", "电流为零说明回路断开，灯泡两端仍承受近似电源电压，优先判断灯丝断路或灯座接触不良，再用短接检查定位。", 38),
            question("电磁感应综合", "TRANSFER", "CALCULATION", 3, "导体棒长0.50 m，在0.80 T匀强磁场中以4.0 m/s垂直切割磁感线，回路电阻0.40 Ω。求感应电动势和电流，并说明安培力的方向。", "用\(E=Blv\)求电动势、用\(I=\frac{E}{R}\)求电流；安培力阻碍导体棒相对磁通量的变化。", 41),
            question("折射率实验设计", "ADVANCED", "EXPERIMENT", 3, "用半圆形玻璃砖和量角器测定玻璃折射率。说明光路、记录量和计算关系，并解释光线从圆心射向弧面的原因。", "记录入射角和折射角，依据\(n=\frac{\sin i}{\sin r}\)计算；从圆心入射时光线垂直弧面，减少弧面折射带来的干扰。", 12),
        ]),
        unit("CHEMISTRY", "化学专题单元·实验与定量分析", "从装置安全、物质分离到滴定定量，完成化学实验综合分析。", 3, 67, [
            question("无机流程分析", "FOUNDATION", "PROCESS", 3, "含\(\ce{Fe^{2+}}\)和\(\ce{Cu^{2+}}\)的酸性废液中加入铁粉回收铜，再氧化并调节pH制备\(\ce{Fe2O3}\)。说明各步骤目的。", "铁粉置换铜的离子反应为\(\ce{Fe + Cu^{2+} -> Fe^{2+} + Cu}\)；调pH要使铁离子沉淀而避免目标组分损失。", 67),
            question("滴定计算与误差", "TRANSFER", "CALCULATION", 3, "用0.1000 mol/L的\(\ce{NaOH}\)滴定20.00 mL盐酸，三次有效体积为18.42、18.38、18.40 mL。计算盐酸浓度并判断滴定管尖嘴气泡消失的误差方向。", "平均体积为18.40 mL，由\(c(\ce{HCl})V(\ce{HCl})=c(\ce{NaOH})V(\ce{NaOH})\)求浓度；起初气泡占据尖嘴会使读数偏大，气泡消失导致实际进入溶液体积偏小，计算浓度偏低。", 62),
            question("实验安全与分离", "ADVANCED", "EXPERIMENT", 3, "设计从含有固体杂质的混合物中获得较纯晶体的方案，说明溶解、过滤、洗涤和干燥的目的，并指出检验洗涤是否充分的方法。", "按溶解度差异选择溶剂，趁热过滤除去不溶物，少量冷溶剂洗涤后干燥；可检验最后一次洗液中的特征离子判断杂质是否洗尽。", 67),
        ]),
        unit("CHEMISTRY", "化学专题单元·平衡、电化学与有机", "综合运用平衡移动、电极反应和有机路线解释实验现象。", 3, 50, [
            question("化学平衡移动", "FOUNDATION", "MATERIAL_ANALYSIS", 3, "反应\(\ce{N2O4(g) <=> 2NO2(g)}\)达到平衡后缩小容器体积，颜色先加深后变浅但仍比原来深。分别解释三个阶段。", "缩小体积使\(\ce{NO2}\)浓度瞬时增大而颜色加深；随后平衡向气体物质的量较少的一侧移动，最终浓度仍受体积变化影响而可能高于原平衡。", 49),
            question("电化学综合", "TRANSFER", "CALCULATION", 3, "惰性电极电解\(\ce{CuSO4}\)溶液，阴极析出3.2 g铜。计算转移电子的物质的量，并比较铜作阳极时的变化。", "阴极反应为\(\ce{Cu^{2+}+2e^- -> Cu}\)，由\(n(\ce{Cu})=\frac{m}{M}\)和电子计量关系求\(n(e^-)\)；铜阳极优先溶解，溶液中\(\ce{Cu^{2+}}\)浓度变化不同。", 58),
            question("有机合成路线", "ADVANCED", "PROCESS", 3, "以乙烯为主要原料制备乙酸乙酯，写出关键反应类型、条件和酯化方程式，并说明提高产率的方法。", "先由加成反应制备乙醇，再氧化得到乙酸，最后发生酯化：\(\ce{CH3COOH + C2H5OH <=> CH3COOC2H5 + H2O}\)；可移去水或使用过量反应物促进平衡移动。", 77),
        ]),
        unit("BIOLOGY", "生物专题单元·遗传与细胞代谢", "结合遗传概率、光合曲线和酶实验完成材料分析。", 3, 123, [
            question("遗传概率分析", "FOUNDATION", "CALCULATION", 3, "豌豆高茎D对矮茎d显性，圆粒R对皱粒r显性，两对基因独立遗传。DdRr自交，求矮茎圆粒概率。", "两对性状分别按分离定律计算，矮茎概率为\(\frac14\)，圆粒概率为\(\frac34\)，独立遗传时相乘得到\(\frac{3}{16}\)。", 123),
            question("光合作用材料分析", "TRANSFER", "MATERIAL_ANALYSIS", 3, "逐步提高光照强度时净光合速率先上升后趋于平台。解释两个阶段的限制因素，并设计实验判断平台期是否受二氧化碳浓度限制。", "低光阶段主要受光能限制，平台期需改变二氧化碳浓度并保持其他条件适宜；若平台随二氧化碳增加而上移，说明二氧化碳是限制因素。", 86),
            question("酶实验设计", "ADVANCED", "EXPERIMENT", 3, "设计实验探究pH对唾液淀粉酶活性的影响，说明自变量、因变量、控制变量和检测方法。", "设置不同pH梯度，控制温度、酶量、底物量和反应时间，以淀粉剩余量或产物生成量表示活性；检测方法应在反应终止后进行，避免连续加热影响酶活性。", 88),
        ]),
        unit("BIOLOGY", "生物专题单元·稳态、生态与生物技术", "从调节机制、生态系统到基因工程梳理生命系统综合问题。", 3, 114, [
            question("神经—体液调节", "FOUNDATION", "MATERIAL_ANALYSIS", 3, "人从温暖室内进入寒冷环境后，按感受、调节中枢、传出途径和效应器说明体温调节过程。", "冷觉感受器将信息传至下丘脑等中枢，通过神经和体液途径使皮肤血管收缩、立毛肌收缩并促进代谢产热，形成负反馈。", 114),
            question("生态系统材料题", "TRANSFER", "MATERIAL_ANALYSIS", 3, "湖泊因生活污水输入出现藻类暴发，随后鱼类大量死亡。请用物质循环、能量流动和溶解氧变化解释过程，并提出治理措施。", "含氮、含磷物质增加促进藻类繁殖，藻类死亡分解消耗氧气，导致鱼类缺氧；应减少源头营养盐输入并配合生态修复，不能把能量循环表述为循环。", 95),
            question("基因工程实验", "ADVANCED", "PROCESS", 3, "将抗虫基因导入植物细胞并筛选稳定表达植株。按目的基因获得、载体构建、导入、筛选和鉴定说明流程，并区分PCR与性状检测的证据。", "PCR可证明目标序列是否存在，表达检测和抗虫性状实验才能支持功能表达；筛选标记只能作为初步筛选依据，不能替代最终鉴定。", 105),
        ]),
        unit("PHYSICS", "物理专题单元·运动学图像与误差", "从图像读取运动状态，结合模型和误差分析完成运动学推理。", 3, 27, [
            question("速度时间图像", "FOUNDATION", "MATERIAL_ANALYSIS", 2, "物体的v-t图像在一段时间内为倾斜直线，随后变为水平线。分别说明两段运动状态，并指出图像面积的物理意义。", "v-t图像斜率表示加速度，水平段表示速度不变；图像与时间轴围成的有向面积表示位移。", 27),
            question("追及问题建模", "TRANSFER", "CALCULATION", 3, "两辆车从同一直线上不同位置出发，已知各自初速度和加速度。说明用何种方程判断相遇时刻，并指出解的物理取舍。", "分别写出\(x=x_0+v_0t+\frac12at^2\)，令两车位置相等得到关于t的方程；舍去负时间和不满足运动阶段条件的根。", 26),
            question("测量不确定度", "ADVANCED", "EXPERIMENT", 3, "用刻度尺测量物体长度并多次重复。说明如何报告测量结果、如何区分系统误差和随机误差。", "结果应写为平均值加不确定度，随机误差可通过多次测量取平均减小，刻度尺零点偏差等系统误差不能仅靠增加次数消除。", 33),
        ]),
        unit("PHYSICS", "物理专题单元·功能关系与动量", "在碰撞、弹簧和摩擦情境中区分动量守恒与能量转化。", 3, 15, [
            question("功与动能", "FOUNDATION", "CALCULATION", 2, "物体在恒力作用下沿力方向移动一段距离，说明恒力做功与动能变化的关系。", "恒力做功为\(W=Fs\)，合外力做功等于动能变化\(W_{合}=\Delta E_k\)，要区分单个力做功和合力做功。", 15),
            question("弹簧储能", "TRANSFER", "CALCULATION", 3, "光滑水平面上的物块压缩轻弹簧后由静止释放。说明分析过程中可使用的能量关系，并指出何时不能把机械能守恒用于碰撞阶段。", "无摩擦阶段可用\(\frac12kx^2=\frac12mv^2\)；若存在非弹性碰撞，机械能转化为内能，应先用动量守恒处理碰撞。", 16),
            question("冲量与受力时间", "ADVANCED", "MATERIAL_ANALYSIS", 3, "安全气囊延长碰撞作用时间，说明在动量变化相同的情况下为什么能减小平均作用力。", "由冲量关系\(I=F\Delta t=\Delta p\)，在\(\Delta p\)一定时延长\(\Delta t\)会减小平均力；这不表示动量变化消失。", 17),
        ]),
        unit("PHYSICS", "物理专题单元·电场与电路实验", "把电场关系、电容器和伏安法测量放在同一实验链路中分析。", 3, 39, [
            question("电场强度判断", "FOUNDATION", "MATERIAL_ANALYSIS", 2, "正电荷在电场中受到电场力，说明电场强度的定义、方向和单位。", "电场强度定义为\(E=\frac{F}{q}\)，方向与正电荷受力方向一致，单位可写作N/C或V/m。", 39),
            question("电容器变化", "TRANSFER", "MATERIAL_ANALYSIS", 3, "平行板电容器保持与电源连接，增大两板间距，判断电容、电压和电荷量的变化。", "由\(C=\frac{\varepsilon S}{d}\)可知间距增大使电容减小；电源保持电压不变，故\(Q=CU\)中的电荷量也减小。", 40),
            question("伏安法测电阻", "ADVANCED", "EXPERIMENT", 3, "用电流表和电压表测小灯泡电阻，说明为什么不能只用一个固定电阻值描述整个实验过程。", "灯丝温度随电流变化，电阻会变化，应通过多组\(U-I\)数据计算对应工作点电阻并绘制图像，而不能默认欧姆元件。", 37),
        ]),
        unit("PHYSICS", "物理专题单元·磁场与光电效应", "结合带电粒子偏转、感应现象和光电效应建立模型边界。", 3, 43, [
            question("洛伦兹力方向", "FOUNDATION", "MATERIAL_ANALYSIS", 2, "带正电粒子垂直进入匀强磁场，说明洛伦兹力方向与速度方向、磁场方向的关系。", "洛伦兹力同时垂直于速度和磁场，速率不变而速度方向改变；方向可用左手定则或\(\vec F=q\vec v\times\vec B\)判断。", 44),
            question("带电粒子圆周运动", "TRANSFER", "CALCULATION", 3, "带电粒子以速度v垂直进入匀强磁场，说明轨道半径和周期与速度、质量、电荷量、磁感应强度的关系。", "由\(qvB=\frac{mv^2}{r}\)得\(r=\frac{mv}{qB}\)，周期为\(T=\frac{2\pi m}{qB}\)；周期与速率无关但依赖质量、电荷量和磁场。", 43),
            question("光电效应", "ADVANCED", "MATERIAL_ANALYSIS", 3, "用不同频率的单色光照射金属，说明遏止电压与频率的关系，并指出提高光强能否改变逸出功。", "由爱因斯坦方程\(E_{k,\max}=h\nu-W_0\)可知遏止电压反映最大初动能；逸出功由金属材料决定，提高光强不能改变逸出功。", 45),
        ]),
        unit("CHEMISTRY", "化学专题单元·物质的量与离子反应", "用单位、守恒和反应限量关系处理溶液与离子反应。", 3, 61, [
            question("物质的量换算", "FOUNDATION", "CALCULATION", 2, "说明质量、摩尔质量和物质的量之间的换算关系，并指出气体体积换算前需要确认的条件。", "使用\(n=\frac{m}{M}\)换算时质量和摩尔质量单位要匹配；气体摩尔体积还需要题目给出的温度、压强和气体状态条件。", 61),
            question("离子方程式", "TRANSFER", "PROCESS", 3, "写离子方程式前应检查哪些信息？以沉淀反应为例说明如何做到元素守恒和电荷守恒。", "先确认反应介质和实际参加反应的离子，再配平原子和电荷，最后检查物态；离子方程式不能把未反应的旁观离子写入。", 64),
            question("限量试剂判断", "ADVANCED", "CALCULATION", 3, "两种溶液混合发生沉淀反应，说明如何根据物质的量和化学计量关系判断限量试剂及剩余离子。", "把各反应物物质的量除以对应化学计量数比较，较小者为限量试剂；再按反应进度计算剩余量并检查电荷守恒。", 63),
        ]),
        unit("CHEMISTRY", "化学专题单元·化学平衡与电化学", "在平衡移动、反应热和电化学中区分瞬时变化与最终变化。", 3, 49, [
            question("反应速率与平衡", "FOUNDATION", "MATERIAL_ANALYSIS", 2, "说明催化剂对反应速率、平衡常数和平衡位置的影响，并指出温度改变为什么会影响平衡常数。", "催化剂降低正、逆反应的活化能，使达到平衡更快，但不改变平衡常数和平衡位置；平衡常数主要由温度决定。", 53),
            question("盖斯定律", "TRANSFER", "CALCULATION", 3, "组合多个热化学方程式求目标反应焓变时，说明方程式系数变化与焓变之间的关系。", "方程式反向时焓变变号，系数乘以k时焓变也乘以k，最后相加得到目标反应；可用\(\Delta H=\sum\Delta H_{生成物}-\sum\Delta H_{反应物}\)复核。", 52),
            question("原电池与电解池", "ADVANCED", "PROCESS", 3, "比较原电池和电解池中电子移动方向、能量转化和电极名称，说明判断电极反应的通用方法。", "两类装置都遵循阳极氧化、阴极还原；原电池把化学能转化为电能，电解池由外电源驱动非自发反应，判断时先看氧化还原变化。", 58),
        ]),
        unit("CHEMISTRY", "化学专题单元·有机化学与实验边界", "从官能团识别、反应路线和实验现象边界分析有机问题。", 3, 77, [
            question("官能团识别", "FOUNDATION", "MATERIAL_ANALYSIS", 2, "说明羟基、羧基、醛基和碳碳双键的典型反应边界，为什么不能仅凭一个现象确定所有官能团。", "要结合试剂、条件和现象共同判断；例如\(\ce{RCOOH + NaHCO3 -> RCOONa + CO2 + H2O}\)可作为羧基的证据，但不能推广到所有含氧官能团。", 77),
            question("有机路线推断", "TRANSFER", "PROCESS", 3, "有机合成路线中出现氧化、还原、加成和取代，说明如何根据官能团变化确定反应类型和试剂。", "先对比反应物和生成物的结构，识别官能团增减，再根据反应条件选择试剂；路线推断应同时检查碳骨架和原子守恒。", 76),
            question("银镜与溴水边界", "ADVANCED", "EXPERIMENT", 3, "说明银镜反应和溴水褪色分别能提供什么证据，并解释为什么溴水颜色变化不能一律归因于加成反应。", "银镜反应需在适宜碱性条件下由醛基还原银离子；溴水褪色还可能涉及取代、萃取或颜色转移，必须结合结构和实验现象判断。", 77),
        ]),
        unit("BIOLOGY", "生物专题单元·细胞结构与代谢调节", "从细胞结构、跨膜运输和酶促反应分析生命活动基础。", 3, 85, [
            question("细胞结构与功能", "FOUNDATION", "MATERIAL_ANALYSIS", 2, "比较原核细胞和真核细胞在细胞核、细胞器和遗传物质组织方式上的差异，并说明结构差异如何影响功能。", "原核细胞没有核膜包围的细胞核，真核细胞有；比较必须限定细胞类型，不能把原核细胞理解成没有任何膜结构。", 91),
            question("跨膜运输", "TRANSFER", "MATERIAL_ANALYSIS", 3, "根据浓度梯度、载体和能量消耗区分自由扩散、协助扩散、主动运输以及胞吞胞吐。", "自由扩散和协助扩散通常顺浓度梯度且不耗能，主动运输逆梯度并需要能量；胞吞胞吐依赖膜泡和能量，不能只看是否有蛋白质。", 92),
            question("呼吸与光合测量", "ADVANCED", "CALCULATION", 3, "解释净光合速率、总光合速率和呼吸速率的关系，并说明净光合速率为零是否意味着没有光合作用。", "在同一口径下有\(P_{净}=P_{总}-R\)；净值为零可能是光合作用强度与呼吸作用强度相等，并不等于光合作用停止。", 86),
        ]),
        unit("BIOLOGY", "生物专题单元·生态系统与现代生物技术", "围绕能量流动、种群变化和基因工程建立证据链。", 3, 94, [
            question("种群数量变化", "FOUNDATION", "MATERIAL_ANALYSIS", 2, "解释种群数量受出生率、死亡率、迁入率和迁出率共同影响时，如何判断种群数量的变化趋势。", "种群数量变化可由出生和迁入增加、死亡和迁出减少共同决定；材料题要结合时间尺度和环境容纳量，不能只看某一个率。", 97),
            question("能量流动与物质循环", "TRANSFER", "MATERIAL_ANALYSIS", 3, "比较生态系统中的能量流动和物质循环，说明为什么提高资源利用率不等同于提高营养级能量传递效率。", "能量沿食物链单向流动并逐级损耗，物质可在生态系统内循环；传递效率是\(\frac{下一营养级同化量}{本营养级同化量}\times100\%\)，口径不能混用。", 95),
            question("现代生物技术证据", "ADVANCED", "PROCESS", 3, "比较PCR、抗原检测和性状观察能支持的结论层级，并说明基因工程筛选为什么需要多步证据。", "PCR主要证明序列存在，表达检测证明转录或翻译，性状实验支持功能结果；不同证据不能互相替代，应按目标逐层验证。", 105),
        ]),
    ]


def main() -> None:
    source = json.loads((CONTENT / "high-frequency-points.v1.json").read_text(encoding="utf-8"))
    for item in source:
        if item.get("latex"):
            item["latex"] = math(item["latex"])
    (CONTENT / "high-frequency-points.v2.json").write_text(
        json.dumps(source, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (CONTENT / "topic-units.v2.json").write_text(
        json.dumps({"version": 2, "units": build_units()}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    units = build_units()
    print(f"HIGH_FREQUENCY_POINTS_V2={len(source)}")
    print(f"TOPIC_UNITS_V2={len(units)}")
    print(f"TOPIC_QUESTIONS_V2={sum(len(item['questions']) for item in units)}")


if __name__ == "__main__":
    main()
