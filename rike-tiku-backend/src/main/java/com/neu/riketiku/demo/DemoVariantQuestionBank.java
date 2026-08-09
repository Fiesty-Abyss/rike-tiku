package com.neu.riketiku.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 人工复核通过、仅由显式本地 Demo seed 使用的原创变式题。 */
final class DemoVariantQuestionBank {
    static final int CANDIDATE_COUNT = 54;
    static final int ACCEPTED_COUNT = 30;
    static final int REJECTED_COUNT = CANDIDATE_COUNT - ACCEPTED_COUNT;

    private DemoVariantQuestionBank() {
    }

    static List<DemoDataService.Question> acceptedQuestions() {
        List<DemoDataService.Question> items = new ArrayList<>();
        addPhysics(items);
        addChemistry(items);
        addBiology(items);
        return List.copyOf(items);
    }

    private static void addPhysics(List<DemoDataService.Question> items) {
        String newton = "力学>运动和力>牛顿运动定律";
        String electric = "电磁学>电场>电场强度";
        String thermal = "热学>分子动理论>温度和内能";

        items.add(DemoDataService.choice("PV-01", "PHYSICS", "SINGLE_CHOICE",
                "变式：质量为60 kg的人站在竖直向下加速度为2 m/s²的电梯中，取g=10 m/s²，电梯地板对人的支持力是多少？",
                newton, 2, List.of("480 N", "600 N", "720 N", "120 N"), Set.of("A"),
                "人随电梯向下加速，mg-N=ma，因此N=m(g-a)=480 N"));
        items.add(DemoDataService.choice("PV-02", "PHYSICS", "MULTIPLE_CHOICE",
                "变式：物体当前速度水平向右，而所受合力水平向左。下列判断正确的是哪些？",
                newton, 2, List.of("物体加速度向左", "物体速度大小会先减小", "物体此刻一定向左运动", "合力方向与加速度方向相反"), Set.of("A", "B"),
                "加速度与合力同向，均向左；由于当前速度向右，物体会先减速，但此刻仍向右运动"));
        items.add(fill("PV-03", "PHYSICS",
                "变式：质量为2 kg的物体在水平面上受到向右10 N拉力和向左4 N摩擦力，其加速度为____m/s²。",
                newton, 2, "3", "物体所受合力为10 N-4 N=6 N，由a=F/m得加速度为3 m/s²。"));
        items.add(DemoDataService.choice("PV-04", "PHYSICS", "SINGLE_CHOICE",
                "变式：质量分别为2 kg和3 kg的两个物体在光滑水平面上相连，受到10 N水平外力，整体加速度是多少？",
                newton, 3, List.of("2 m/s²", "5 m/s²", "3.3 m/s²", "10 m/s²"), Set.of("A"),
                "把两个物体视为整体，总质量为5 kg，由a=F/m得2 m/s²"));

        items.add(DemoDataService.choice("PV-05", "PHYSICS", "MULTIPLE_CHOICE",
                "变式：某点电场强度竖直向上，大小为5×10³ N/C。将电荷量为-2×10⁻⁶ C的电荷放在该点，下列判断正确的是哪些？",
                electric, 2, List.of("电场力方向竖直向下", "电场力大小为0.01 N", "电场力方向竖直向上", "该点电场强度因电荷放入而变为零"), Set.of("A", "B"),
                "负电荷受力方向与电场方向相反；力的大小为|q|E=0.01 N，检验电荷不改变原电场定义"));
        items.add(fill("PV-06", "PHYSICS",
                "变式：电荷量为2×10⁻⁶ C的正电荷处在3×10⁴ N/C的匀强电场中，所受电场力为____N。",
                electric, 2, "0.06", "由F=qE得F=2×10⁻⁶ C×3×10⁴ N/C=0.06 N。"));
        items.add(DemoDataService.choice("PV-07", "PHYSICS", "SINGLE_CHOICE",
                "变式：等量异种点电荷分别固定在同一直线两端。在两电荷连线的中点，电场方向如何？",
                electric, 3, List.of("由正电荷指向负电荷", "由负电荷指向正电荷", "垂直于两电荷连线", "电场强度必为零"), Set.of("A"),
                "中点处正电荷产生的电场背离正电荷，负电荷产生的电场指向负电荷，两者方向相同，均由正电荷指向负电荷"));

        items.add(DemoDataService.choice("PV-08", "PHYSICS", "MULTIPLE_CHOICE",
                "变式：气体吸收800 J热量，同时外界对气体做功200 J。下列判断正确的是哪些？",
                thermal, 2, List.of("气体内能增加1000 J", "做功和热传递都改变了气体内能", "气体内能只增加600 J", "气体内能保持不变"), Set.of("A", "B"),
                "按热力学第一定律，吸收热量和外界对气体做功都使内能增加，因此ΔU=800 J+200 J=1000 J"));
        items.add(fill("PV-09", "PHYSICS",
                "变式：某物体温度由20℃升高到50℃，温度变化量为____K。",
                thermal, 1, "30", "摄氏温差与开尔文温差数值相同，因此温度变化量为30 K。"));
        items.add(DemoDataService.choice("PV-10", "PHYSICS", "SINGLE_CHOICE",
                "变式：两份不同种类的理想气体处于相同温度时，其分子的平均平动动能有何关系？",
                thermal, 3, List.of("相同", "摩尔质量大的气体更大", "分子数多的气体更大", "无法由温度判断"), Set.of("A"),
                "理想气体分子的平均平动动能只由热力学温度决定，温度相同则平均平动动能相同"));
    }

    private static void addChemistry(List<DemoDataService.Question> items) {
        String mole = "化学基本概念>物质的量>摩尔计算";
        String redox = "无机化学>元素化合物>氧化还原反应";
        String equilibrium = "化学反应原理>化学平衡>平衡移动";

        items.add(DemoDataService.choice("CV-01", "CHEMISTRY", "SINGLE_CHOICE",
                "变式：4.4 g CO₂的物质的量是多少？",
                mole, 1, List.of("0.1 mol", "0.2 mol", "1 mol", "10 mol"), Set.of("A"),
                "CO₂的摩尔质量为44 g/mol，由n=m/M得4.4 g CO₂为0.1 mol"));
        items.add(DemoDataService.choice("CV-02", "CHEMISTRY", "MULTIPLE_CHOICE",
                "变式：0.2 mol H₂SO₄中各元素原子的物质的量，下列说法正确的是哪些？",
                mole, 2, List.of("H原子为0.4 mol", "S原子为0.2 mol", "O原子为0.8 mol", "全部原子合计为0.8 mol"), Set.of("A", "B", "C"),
                "每个H₂SO₄分子含2个H、1个S和4个O，因此三种原子分别为0.4 mol、0.2 mol和0.8 mol；全部原子合计1.4 mol"));
        items.add(fill("CV-03", "CHEMISTRY",
                "变式：250 mL、0.4 mol/L的NaCl溶液中含NaCl____mol。",
                mole, 3, "0.1", "将250 mL换算为0.250 L，由n=cV得n=0.4 mol/L×0.250 L=0.1 mol。"));

        items.add(DemoDataService.choice("CV-04", "CHEMISTRY", "SINGLE_CHOICE",
                "变式：反应2FeCl₂ + Cl₂ = 2FeCl₃中，还原剂是哪种物质？",
                redox, 1, List.of("FeCl₂", "Cl₂", "FeCl₃", "Cl⁻"), Set.of("A"),
                "FeCl₂中的Fe²⁺失去电子变为Fe³⁺，FeCl₂被氧化并作为还原剂"));
        items.add(DemoDataService.choice("CV-05", "CHEMISTRY", "MULTIPLE_CHOICE",
                "变式：反应SO₂ + Br₂ + 2H₂O = H₂SO₄ + 2HBr中，下列判断正确的是哪些？",
                redox, 2, List.of("SO₂是还原剂", "Br₂是氧化剂", "S元素化合价由+6降为+4", "Br元素被氧化"), Set.of("A", "B"),
                "S元素由+4升至+6，SO₂被氧化并作还原剂；Br元素由0降至-1，Br₂被还原并作氧化剂"));
        items.add(fill("CV-06", "CHEMISTRY",
                "变式：Cr₂O₇²⁻中Cr元素的化合价为正____价。",
                redox, 3, "6", "设Cr为+x价，根据离子总电荷有2x+7×(-2)=-2，解得x=+6。"));

        items.add(DemoDataService.choice("CV-07", "CHEMISTRY", "SINGLE_CHOICE",
                "变式：反应N₂ + 3H₂ ⇌ 2NH₃达到平衡后，其他条件不变，移走部分NH₃，平衡如何移动？",
                equilibrium, 1, List.of("向右移动", "向左移动", "不移动", "反应立即停止"), Set.of("A"),
                "降低生成物NH₃浓度后，平衡向生成NH₃的方向移动，即向右移动"));
        items.add(DemoDataService.choice("CV-08", "CHEMISTRY", "MULTIPLE_CHOICE",
                "变式：对放热反应N₂ + 3H₂ ⇌ 2NH₃，下列措施及平衡移动判断正确的是哪些？",
                equilibrium, 2, List.of("恒温恒容加入惰性气体，平衡不移动", "恒温减小容器体积，平衡向右移动", "加入催化剂会增大平衡常数", "升高温度，平衡向右移动"), Set.of("A", "B"),
                "恒温恒容加入惰性气体不改变各组分浓度；减小体积增大压强，平衡向气体物质的量较少的右侧移动。催化剂不改变平衡常数，升温使放热反应平衡左移"));
        items.add(fill("CV-09", "CHEMISTRY",
                "变式：反应2SO₂ + O₂ ⇌ 2SO₃达到平衡后，其他条件不变，减小压强时平衡向____移动。",
                equilibrium, 3, "左", "减小压强时平衡向气体物质的量较多的一侧移动；左侧为3 mol气体，右侧为2 mol气体，因此向左移动。"));
    }

    private static void addBiology(List<DemoDataService.Question> items) {
        String membrane = "分子与细胞>细胞结构>细胞膜";
        String segregation = "遗传与进化>遗传规律>分离定律";
        String hormone = "稳态与调节>生命活动调节>激素调节";

        items.add(DemoDataService.choice("BV-01", "BIOLOGY", "SINGLE_CHOICE",
                "变式：将哺乳动物成熟红细胞放入低于细胞质浓度的溶液中，短时间内最可能发生什么变化？",
                membrane, 1, List.of("细胞吸水膨胀", "细胞失水皱缩", "水分子停止跨膜运动", "细胞主动排出全部水分"), Set.of("A"),
                "外界溶液浓度较低时，水分子总体进入细胞，红细胞吸水膨胀；水分子并未停止运动"));
        items.add(DemoDataService.choice("BV-02", "BIOLOGY", "MULTIPLE_CHOICE",
                "变式：下列跨膜运输方式中，不直接消耗细胞代谢能量的是哪些？",
                membrane, 2, List.of("自由扩散", "协助扩散", "主动运输", "胞吞"), Set.of("A", "B"),
                "自由扩散和协助扩散都属于被动运输，顺浓度梯度进行；主动运输和胞吞需要细胞提供能量"));
        items.add(fill("BV-03", "BIOLOGY",
                "变式：O₂分子顺浓度梯度直接穿过细胞膜的运输方式称为____。",
                membrane, 1, "自由扩散", "O₂是小分子，可顺浓度梯度直接穿过磷脂双分子层，这种方式是自由扩散。"));
        items.add(DemoDataService.choice("BV-04", "BIOLOGY", "SINGLE_CHOICE",
                "变式：活细胞逆浓度梯度吸收K⁺时，通常需要哪组条件？",
                membrane, 3, List.of("载体蛋白和能量", "只需要较大的浓度差", "只需要自由扩散", "细胞膜完全失去选择性"), Set.of("A"),
                "逆浓度梯度运输属于主动运输，通常需要载体蛋白并消耗能量；自由扩散不能完成逆梯度运输"));

        items.add(DemoDataService.choice("BV-05", "BIOLOGY", "MULTIPLE_CHOICE",
                "变式：在完全显性条件下，用杂合子Aa与隐性纯合子aa测交。后代数量足够多时，下列判断正确的是哪些？",
                segregation, 2, List.of("基因型Aa与aa约为1:1", "显性与隐性表现型约为1:1", "后代全部为显性", "后代不会出现aa"), Set.of("A", "B"),
                "Aa产生A、a两种数量相近的配子，aa只产生a配子，因此后代Aa与aa及相应表现型均约为1:1"));
        items.add(fill("BV-06", "BIOLOGY",
                "变式：在完全显性条件下，AA与Aa杂交，后代为AA的概率是____。",
                segregation, 2, "1/2", "AA亲本只产生A配子，Aa亲本产生A和a两类配子，因此后代AA与Aa各占1/2。"));
        items.add(DemoDataService.choice("BV-07", "BIOLOGY", "SINGLE_CHOICE",
                "变式：不考虑突变和配子选择，基因型Aa的个体产生A、a两类配子的比例通常是多少？",
                segregation, 1, List.of("1:1", "3:1", "1:2", "全部为A"), Set.of("A"),
                "减数分裂时成对等位基因分离，A和a分别进入不同配子，两类配子比例通常接近1:1"));
        items.add(DemoDataService.choice("BV-08", "BIOLOGY", "MULTIPLE_CHOICE",
                "变式：在完全显性条件下，关于一对等位基因控制的性状，下列说法正确的是哪些？",
                segregation, 3, List.of("隐性表现型个体的基因型为aa", "显性表现型个体可能为AA或Aa", "Aa个体只能产生A配子", "AA与aa杂交的F₁会出现隐性表现型"), Set.of("A", "B"),
                "隐性性状只有aa才能表现，显性性状可由AA或Aa表现；Aa产生A、a两类配子，AA与aa杂交的F₁均为Aa并表现显性"));

        items.add(DemoDataService.choice("BV-09", "BIOLOGY", "SINGLE_CHOICE",
                "变式：健康人进食富含糖类的食物后，短时间内胰岛素和胰高血糖素的分泌通常如何变化？",
                hormone, 2, List.of("胰岛素增加，胰高血糖素减少", "两者都增加", "胰岛素减少，胰高血糖素增加", "两者都停止分泌"), Set.of("A"),
                "进食后血糖升高促进胰岛素分泌并抑制胰高血糖素分泌，以促进血糖降低；两种激素不会停止分泌"));
        items.add(DemoDataService.choice("BV-10", "BIOLOGY", "MULTIPLE_CHOICE",
                "变式：血液中甲状腺激素浓度升高时形成负反馈调节。下列判断正确的是哪些？",
                hormone, 3, List.of("可抑制下丘脑相关激素的分泌", "可抑制垂体促甲状腺激素的分泌", "会持续促进甲状腺激素无限增加", "负反馈会破坏内环境稳态"), Set.of("A", "B"),
                "甲状腺激素升高会反馈抑制下丘脑和垂体的相关分泌，从而限制其继续升高并有利于维持稳态"));
        items.add(fill("BV-11", "BIOLOGY",
                "变式：人体缺水时，促进肾小管和集合管重吸收水的激素是____。",
                hormone, 3, "抗利尿激素", "人体缺水时抗利尿激素释放增加，促进肾小管和集合管重吸收水，从而减少尿量。"));
    }

    private static DemoDataService.Question fill(String key, String subject, String stem, String point,
            int difficulty, String accepted, String analysis) {
        DemoDataService.Question base = DemoDataService.fill(key, subject, stem, point, difficulty, accepted);
        return new DemoDataService.Question(base.key(), base.subject(), base.type(), base.stem(), base.knowledgePath(),
                base.difficulty(), base.answer(), base.options(), analysis);
    }
}
