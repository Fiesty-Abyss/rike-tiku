package com.neu.riketiku.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Additional original questions used only by the explicit local demo seed. */
final class DemoQuestionBank {
    private DemoQuestionBank() {
    }

    static List<DemoDataService.Question> additionalQuestions() {
        List<DemoDataService.Question> items = new ArrayList<>();
        addPhysics(items);
        addChemistry(items);
        addBiology(items);
        return items;
    }

    private static void addPhysics(List<DemoDataService.Question> items) {
        String newton = "力学>运动和力>牛顿运动定律";
        String electric = "电磁学>电场>电场强度";
        String thermal = "热学>分子动理论>温度和内能";

        items.add(choice("PHYSICS-S3", "PHYSICS", "SINGLE_CHOICE", "质量为2 kg的物体加速度为4 m/s²，所受合力为多少？", newton, 1,
                List.of("8 N", "6 N", "2 N", "0.5 N"), Set.of("A"), "由牛顿第二定律F=ma，代入质量2 kg和加速度4 m/s²，可得合力为8 N"));
        items.add(choice("PHYSICS-M3", "PHYSICS", "MULTIPLE_CHOICE", "质量为1 kg的物体同时受到水平向右5 N和向左2 N的力。下列判断正确的是哪些？", newton, 2,
                List.of("合力为3 N，方向向右", "加速度为3 m/s²，方向向右", "物体一定向右运动", "合力为7 N"), Set.of("A", "B"), "合力和加速度均向右，大小分别为3 N和3 m/s²，但速度方向不能由已知条件确定"));
        items.add(fill("PHYSICS-F3", "PHYSICS", "质量为2 kg的物体由静止开始，在4 N恒力作用下运动3 s，末速度为____m/s。", newton, 3, "6"));
        items.add(choice("PHYSICS-S4", "PHYSICS", "SINGLE_CHOICE", "关于一对作用力和反作用力，下列说法正确的是哪一项？", newton, 2,
                List.of("大小相等、方向相反，作用在不同物体上", "大小相等、方向相同", "作用在同一物体上", "只有物体静止时才存在"), Set.of("A"), "作用力和反作用力总是大小相等、方向相反并作用在两个物体上"));
        items.add(choice("PHYSICS-M4", "PHYSICS", "MULTIPLE_CHOICE", "人站在加速上升的电梯中。下列说法正确的是哪些？", newton, 3,
                List.of("支持力大于重力", "人的合力方向向上", "支持力等于重力", "人的加速度方向向下"), Set.of("A", "B"), "加速度向上时合力向上，因此支持力大于重力"));
        items.add(fill("PHYSICS-F4", "PHYSICS", "质量为5 kg的物体受到10 N合力，其加速度为____m/s²。", newton, 1, "2"));

        items.add(choice("PHYSICS-S5", "PHYSICS", "SINGLE_CHOICE", "电荷量为2×10⁻⁶ C的正电荷在某点受电场力0.04 N，该点电场强度为多少？", electric, 3,
                List.of("20000 N/C", "80000 N/C", "0.00008 N/C", "0.002 N/C"), Set.of("A"), "由E=F/q可得电场强度为20000 N/C"));
        items.add(choice("PHYSICS-M5", "PHYSICS", "MULTIPLE_CHOICE", "关于电场强度，下列说法正确的是哪些？", electric, 1,
                List.of("电场强度描述电场的强弱和方向", "正电荷受力方向与该点电场方向相同", "电场强度由检验电荷大小决定", "没有检验电荷时电场一定不存在"), Set.of("A", "B"), "电场强度是电场本身的性质，正电荷受力方向规定为电场方向"));
        items.add(fill("PHYSICS-F5", "PHYSICS", "电场强度为500 N/C，电荷量为0.02 C的正电荷所受电场力为____N。", electric, 2, "10"));
        items.add(choice("PHYSICS-S6", "PHYSICS", "SINGLE_CHOICE", "静电场中电场线的一般方向是怎样的？", electric, 1,
                List.of("从正电荷或无穷远指向负电荷或无穷远", "总是从负电荷指向正电荷", "与正电荷受力方向相反", "可以相交"), Set.of("A"), "电场线方向与正检验电荷受力方向一致，通常从正电荷指向负电荷"));
        items.add(choice("PHYSICS-M6", "PHYSICS", "MULTIPLE_CHOICE", "在方向向右的匀强电场中，下列判断正确的是哪些？", electric, 2,
                List.of("正电荷所受电场力向右", "负电荷所受电场力向左", "所有电荷所受力都向右", "电场强度随检验电荷改变"), Set.of("A", "B"), "正电荷受力沿电场方向，负电荷受力与电场方向相反"));
        items.add(fill("PHYSICS-F6", "PHYSICS", "正电荷0.01 C沿电场方向在200 N/C的匀强电场中移动0.5 m，电场力做功为____J。", electric, 3, "1"));
        items.add(choice("PHYSICS-S7", "PHYSICS", "SINGLE_CHOICE", "同一电场点放入不同电荷量的检验电荷，该点电场强度如何变化？", electric, 2,
                List.of("保持不变", "随电荷量增大", "随电荷量减小", "变为零"), Set.of("A"), "同一点电场强度由场源和位置决定，与检验电荷无关"));
        items.add(choice("PHYSICS-M7", "PHYSICS", "MULTIPLE_CHOICE", "点电荷形成的电场中，下列判断正确的是哪些？", electric, 3,
                List.of("距离变为2倍时电场强度变为原来的1/4", "场源电荷量变为2倍时同一点电场强度变为2倍", "电场强度与距离无关", "电场强度与场源电荷量无关"), Set.of("A", "B"), "点电荷电场强度与场源电荷量成正比，与距离平方成反比"));
        items.add(fill("PHYSICS-F7", "PHYSICS", "1 C正电荷在某点受到3 N电场力，该点电场强度为____N/C。", electric, 1, "3"));

        items.add(choice("PHYSICS-S8", "PHYSICS", "SINGLE_CHOICE", "一定质量的理想气体在体积不变时温度升高，其内能如何变化？", thermal, 3,
                List.of("增加", "减少", "不变", "无法判断"), Set.of("A"), "一定量理想气体的内能只取决于温度；温度升高意味着分子平均动能增大，因此内能增加"));
        items.add(choice("PHYSICS-M8", "PHYSICS", "MULTIPLE_CHOICE", "关于分子热运动，下列说法正确的是哪些？", thermal, 1,
                List.of("温度越高，分子平均动能通常越大", "分子热运动永不停息", "所有分子速率始终相同", "0℃时分子停止运动"), Set.of("A", "B"), "分子持续进行无规则热运动，温度反映平均动能水平"));
        items.add(fill("PHYSICS-F8", "PHYSICS", "温度27℃约等于____K。", thermal, 2, "300"));
        items.add(choice("PHYSICS-S9", "PHYSICS", "SINGLE_CHOICE", "改变物体内能的两种基本方式是什么？", thermal, 1,
                List.of("做功和热传递", "升高和降低", "加速和减速", "熔化和凝固"), Set.of("A"), "做功和热传递是改变内能的两种基本方式"));
        items.add(choice("PHYSICS-M9", "PHYSICS", "MULTIPLE_CHOICE", "下列过程通常会使物体内能增加的是哪些？", thermal, 2,
                List.of("摩擦使物体发热", "快速压缩气体", "物体向外放热且不做功", "气体绝热膨胀并对外做功"), Set.of("A", "B"), "克服摩擦做功和外界压缩气体都把其他形式能量转化为内能；放热或绝热对外做功则使内能减小"));
        items.add(fill("PHYSICS-F9", "PHYSICS", "气体吸收500 J热量，同时对外做功200 J，其内能增加____J。", thermal, 3, "300"));
        items.add(choice("PHYSICS-S10", "PHYSICS", "SINGLE_CHOICE", "从分子动理论看，温度主要反映大量分子的什么量？", thermal, 2,
                List.of("平均动能", "总质量", "平均位置", "分子个数"), Set.of("A"), "温度是大量分子热运动平均动能的标志"));
        items.add(choice("PHYSICS-M10", "PHYSICS", "MULTIPLE_CHOICE", "关于一定量理想气体的内能，下列说法正确的是哪些？", thermal, 3,
                List.of("温度升高时内能增加", "温度相同时内能相同", "只增大体积必然增加内能", "只增大压强必然增加内能"), Set.of("A", "B"), "一定量理想气体的内能只由温度决定，所以升温时内能增加、同温时内能相同；仅知体积或压强变化不能确定内能"));
        items.add(fill("PHYSICS-F10", "PHYSICS", "温度0℃约等于____K。", thermal, 1, "273"));
    }

    private static void addChemistry(List<DemoDataService.Question> items) {
        String mole = "化学基本概念>物质的量>摩尔计算";
        String redox = "无机化学>元素化合物>氧化还原反应";
        String equilibrium = "化学反应原理>化学平衡>平衡移动";

        items.add(choice("CHEMISTRY-S3", "CHEMISTRY", "SINGLE_CHOICE", "NaCl的摩尔质量约为多少？", mole, 1,
                List.of("58.5 g/mol", "23 g/mol", "35.5 g/mol", "18 g/mol"), Set.of("A"), "Na和Cl的相对原子质量之和约为58.5"));
        items.add(choice("CHEMISTRY-M3", "CHEMISTRY", "MULTIPLE_CHOICE", "0.5 mol CO₂中所含各元素原子的物质的量，下列说法正确的是哪些？", mole, 2,
                List.of("C原子为0.5 mol", "O原子为1 mol", "C原子为1 mol", "O原子为0.5 mol"), Set.of("A", "B"), "每个CO₂分子含1个C原子和2个O原子，因此0.5 mol CO₂含0.5 mol C原子和1 mol O原子"));
        items.add(fill("CHEMISTRY-F3", "CHEMISTRY", "标准状况下11.2 L理想气体约为____mol。", mole, 3, "0.5"));
        items.add(choice("CHEMISTRY-S4", "CHEMISTRY", "SINGLE_CHOICE", "18 g H₂O的物质的量约为多少？", mole, 2,
                List.of("1 mol", "2 mol", "0.5 mol", "18 mol"), Set.of("A"), "H₂O摩尔质量为18 g/mol，因此18 g为1 mol"));
        items.add(choice("CHEMISTRY-M4", "CHEMISTRY", "MULTIPLE_CHOICE", "500 mL、0.1 mol/L的NaCl溶液中，忽略体积变化，下列说法正确的是哪些？", mole, 3,
                List.of("NaCl为0.05 mol", "Na⁺约为0.05 mol", "Cl⁻约为0.05 mol", "NaCl为0.5 mol"), Set.of("A", "B", "C"), "由n=cV可得NaCl为0.05 mol，完全电离后Na⁺和Cl⁻各0.05 mol"));
        items.add(fill("CHEMISTRY-F4", "CHEMISTRY", "2 mol CO₂含有____mol氧原子。", mole, 1, "4"));
        items.add(choice("CHEMISTRY-S5", "CHEMISTRY", "SINGLE_CHOICE", "约含3.01×10²³个分子的物质，其物质的量约为多少？", mole, 3,
                List.of("0.5 mol", "1 mol", "2 mol", "3 mol"), Set.of("A"), "3.01×10²³约为阿伏加德罗常数的一半"));

        items.add(choice("CHEMISTRY-M5", "CHEMISTRY", "MULTIPLE_CHOICE", "关于氧化和还原，下列说法正确的是哪些？", redox, 1,
                List.of("失去电子的过程是氧化", "得到电子的过程是还原", "氧化反应一定得到电子", "还原反应一定失去电子"), Set.of("A", "B"), "氧化还原反应按电子转移判断：失去电子、化合价升高是氧化，得到电子、化合价降低是还原"));
        items.add(fill("CHEMISTRY-F5", "CHEMISTRY", "Fe³⁺转化为Fe²⁺时，每个Fe³⁺得到____个电子。", redox, 2, "1"));
        items.add(choice("CHEMISTRY-S6", "CHEMISTRY", "SINGLE_CHOICE", "反应Zn + CuSO₄ = ZnSO₄ + Cu中，还原剂是哪种物质？", redox, 1,
                List.of("Zn", "CuSO₄", "ZnSO₄", "Cu"), Set.of("A"), "Zn失去电子被氧化，因此Zn是还原剂"));
        items.add(choice("CHEMISTRY-M6", "CHEMISTRY", "MULTIPLE_CHOICE", "反应CuO + H₂ = Cu + H₂O中，下列判断正确的是哪些？", redox, 2,
                List.of("H₂是还原剂", "CuO是氧化剂", "Cu元素被氧化", "H元素被还原"), Set.of("A", "B"), "H₂被氧化为H₂O，CuO中的Cu被还原为单质"));
        items.add(fill("CHEMISTRY-F6", "CHEMISTRY", "KMnO₄中Mn元素的化合价为正____价。", redox, 3, "7"));
        items.add(choice("CHEMISTRY-S7", "CHEMISTRY", "SINGLE_CHOICE", "反应Cl₂ + 2KI = 2KCl + I₂中，被氧化的粒子是哪个？", redox, 2,
                List.of("I⁻", "K⁺", "Cl₂", "Cl⁻"), Set.of("A"), "反应中I⁻失去电子生成I₂，碘元素化合价由-1升至0，因此I⁻发生氧化并作为还原剂"));
        items.add(choice("CHEMISTRY-M7", "CHEMISTRY", "MULTIPLE_CHOICE", "关于歧化反应，下列说法正确的是哪些？", redox, 3,
                List.of("同一元素同时发生氧化和还原", "同一物质可同时作氧化剂和还原剂", "反应中一定没有电子转移", "所有元素化合价都不变"), Set.of("A", "B"), "歧化反应中同一元素的化合价一部分升高、一部分降低"));
        items.add(fill("CHEMISTRY-F7", "CHEMISTRY", "H₂O中O元素的化合价为负____价。", redox, 1, "2"));
        items.add(choice("CHEMISTRY-S8", "CHEMISTRY", "SINGLE_CHOICE", "Fe²⁺转化为Fe³⁺的过程中，每个Fe²⁺发生什么变化？", redox, 3,
                List.of("失去1个电子", "得到1个电子", "失去2个电子", "化合价不变"), Set.of("A"), "Fe元素化合价由+2升至+3，失去1个电子"));

        items.add(choice("CHEMISTRY-M8", "CHEMISTRY", "MULTIPLE_CHOICE", "可逆反应达到化学平衡时，下列说法正确的是哪些？", equilibrium, 1,
                List.of("正反应速率等于逆反应速率", "各组分浓度保持不变", "正、逆反应均停止", "反应物浓度一定等于生成物浓度"), Set.of("A", "B"), "化学平衡是正逆反应速率相等的动态平衡"));
        items.add(fill("CHEMISTRY-F8", "CHEMISTRY", "对于给定可逆反应，化学平衡常数主要随____改变。", equilibrium, 2, "温度"));
        items.add(choice("CHEMISTRY-S9", "CHEMISTRY", "SINGLE_CHOICE", "其他条件不变时，增大反应物浓度，平衡通常向哪个方向移动？", equilibrium, 1,
                List.of("消耗反应物的方向", "生成反应物的方向", "一定不移动", "反应停止"), Set.of("A"), "平衡会向减弱反应物浓度增大影响的方向移动"));
        items.add(choice("CHEMISTRY-M9", "CHEMISTRY", "MULTIPLE_CHOICE", "某正反应为放热反应。下列判断正确的是哪些？", equilibrium, 2,
                List.of("升高温度有利于逆反应", "降低温度有利于正反应", "升高温度一定使平衡正向移动", "温度不影响平衡"), Set.of("A", "B"), "温度升高时平衡向吸热方向移动，温度降低时向放热方向移动；本反应正向放热，因此升温利于逆反应、降温利于正反应"));
        items.add(fill("CHEMISTRY-F9", "CHEMISTRY", "反应N₂ + 3H₂ ⇌ 2NH₃达到平衡后，增大压强时平衡向____移动。", equilibrium, 3, "右"));
        items.add(choice("CHEMISTRY-S10", "CHEMISTRY", "SINGLE_CHOICE", "加入合适催化剂后，化学平衡如何变化？", equilibrium, 2,
                List.of("平衡位置不变，只加快达到平衡", "一定正向移动", "一定逆向移动", "平衡常数增大"), Set.of("A"), "催化剂同等程度加快正逆反应，不改变平衡位置和平衡常数"));
        items.add(choice("CHEMISTRY-M10", "CHEMISTRY", "MULTIPLE_CHOICE", "对于反应前后气体总物质的量相等的可逆反应，下列说法正确的是哪些？", equilibrium, 3,
                List.of("改变压强通常不引起平衡移动", "催化剂不改变平衡位置", "增大压强一定正向移动", "平衡时反应停止"), Set.of("A", "B"), "气体总物质的量不变时压强改变不影响平衡方向，催化剂也不改变平衡位置"));
        items.add(fill("CHEMISTRY-F10", "CHEMISTRY", "可逆反应达到平衡后，在外界条件不变时，各组分浓度随时间保持____。", equilibrium, 1, "不变"));
    }

    private static void addBiology(List<DemoDataService.Question> items) {
        String membrane = "分子与细胞>细胞结构>细胞膜";
        String segregation = "遗传与进化>遗传规律>分离定律";
        String hormone = "稳态与调节>生命活动调节>激素调节";

        items.add(choice("BIOLOGY-S3", "BIOLOGY", "SINGLE_CHOICE", "细胞膜磷脂分子的亲水头部通常朝向哪里？", membrane, 1,
                List.of("膜两侧的水环境", "膜内部的疏水区域", "细胞核内部", "染色体表面"), Set.of("A"), "磷脂亲水头部朝向膜内外的水环境，疏水尾部相对排列"));
        items.add(choice("BIOLOGY-M3", "BIOLOGY", "MULTIPLE_CHOICE", "细胞膜蛋白可能承担哪些功能？", membrane, 2,
                List.of("物质运输", "信号识别", "构成遗传信息的全部载体", "储存细胞全部能量"), Set.of("A", "B"), "膜蛋白可作为载体、通道或受体参与运输和信息交流"));
        items.add(fill("BIOLOGY-F3", "BIOLOGY", "流动镶嵌模型认为细胞膜的基本支架是____。", membrane, 3, "磷脂双分子层"));
        items.add(choice("BIOLOGY-S4", "BIOLOGY", "SINGLE_CHOICE", "主动运输通常需要什么条件？", membrane, 2,
                List.of("载体蛋白和能量", "只需要浓度差", "不需要膜蛋白", "细胞死亡后仍正常进行"), Set.of("A"), "主动运输通常依赖载体蛋白并消耗细胞提供的能量"));
        items.add(choice("BIOLOGY-M4", "BIOLOGY", "MULTIPLE_CHOICE", "将植物细胞放入高浓度外界溶液时，可能出现哪些现象？", membrane, 3,
                List.of("细胞失水", "原生质层与细胞壁分离", "细胞一定吸水膨胀", "水分子停止运动"), Set.of("A", "B"), "外界溶液浓度较高时细胞失水，可能发生质壁分离"));
        items.add(fill("BIOLOGY-F4", "BIOLOGY", "细胞膜允许某些物质通过而限制另一些物质通过的特性称为____。", membrane, 1, "选择透过性"));
        items.add(choice("BIOLOGY-S5", "BIOLOGY", "SINGLE_CHOICE", "协助扩散速率在较高物质浓度时可能趋于稳定，主要原因是什么？", membrane, 3,
                List.of("膜上载体或通道数量有限", "物质停止运动", "细胞膜完全消失", "浓度差必然变为零"), Set.of("A"), "协助扩散依赖数量有限的转运蛋白，因此可能出现饱和"));

        items.add(choice("BIOLOGY-M5", "BIOLOGY", "MULTIPLE_CHOICE", "基因型为Aa的个体产生配子时，下列说法正确的是哪些？", segregation, 1,
                List.of("可产生含A的配子", "可产生含a的配子", "每个配子同时含A和a", "遗传因子不会分离"), Set.of("A", "B"), "成对遗传因子在形成配子时分离，分别进入不同配子"));
        items.add(fill("BIOLOGY-F5", "BIOLOGY", "完全显性条件下，Aa与Aa杂交，后代aa的概率为____。", segregation, 2, "1/4"));
        items.add(choice("BIOLOGY-S6", "BIOLOGY", "SINGLE_CHOICE", "要测定显性性状个体是否为杂合子，常采用哪种杂交方式？", segregation, 1,
                List.of("测交", "自交一定一次", "与显性纯合子杂交", "随机交配"), Set.of("A"), "测交是待测个体与隐性纯合子杂交，可据后代性状判断基因型"));
        items.add(choice("BIOLOGY-M6", "BIOLOGY", "MULTIPLE_CHOICE", "完全显性且后代数量足够多时，Aa自交后代通常具有哪些比例？", segregation, 2,
                List.of("基因型比例1:2:1", "表现型比例3:1", "基因型全部相同", "隐性表现型占3/4"), Set.of("A", "B"), "Aa自交后代基因型AA:Aa:aa为1:2:1，表现型为3:1"));
        items.add(fill("BIOLOGY-F6", "BIOLOGY", "基因型AA与aa杂交，F1的基因型为____。", segregation, 3, "Aa"));
        items.add(choice("BIOLOGY-S7", "BIOLOGY", "SINGLE_CHOICE", "分离定律的细胞学基础主要发生在什么过程中？", segregation, 2,
                List.of("减数分裂时同源染色体分离", "有丝分裂时姐妹染色单体分离", "DNA复制前", "受精完成后"), Set.of("A"), "等位基因随减数分裂中同源染色体的分离而分开"));
        items.add(choice("BIOLOGY-M7", "BIOLOGY", "MULTIPLE_CHOICE", "完全显性条件下，Aa与Aa杂交的后代中，下列说法正确的是哪些？", segregation, 3,
                List.of("AA约占1/4", "Aa约占1/2", "aa约占1/4", "只有一种基因型"), Set.of("A", "B", "C"), "配子随机结合得到AA、Aa、aa的比例为1:2:1"));
        items.add(fill("BIOLOGY-F7", "BIOLOGY", "显性纯合子的基因型可写为____。", segregation, 1, "AA"));

        items.add(choice("BIOLOGY-S8", "BIOLOGY", "SINGLE_CHOICE", "胰岛素降低血糖的主要作用之一是什么？", hormone, 3,
                List.of("促进组织细胞摄取和利用葡萄糖", "促进肝糖原分解", "抑制葡萄糖进入细胞", "使血糖持续升高"), Set.of("A"), "胰岛素促进细胞摄取利用葡萄糖并促进糖原合成，从而降低血糖"));
        items.add(choice("BIOLOGY-M8", "BIOLOGY", "MULTIPLE_CHOICE", "关于内分泌腺，下列说法正确的是哪些？", hormone, 1,
                List.of("通常没有导管", "分泌物可进入体液", "分泌物只能在腺体内起作用", "所有分泌物都是消化酶"), Set.of("A", "B"), "内分泌腺无导管，激素进入体液并运输到靶细胞"));
        items.add(fill("BIOLOGY-F8", "BIOLOGY", "健康人血糖升高时，胰岛素分泌通常____。", hormone, 2, "增加"));
        items.add(choice("BIOLOGY-S9", "BIOLOGY", "SINGLE_CHOICE", "甲状腺激素的主要作用之一是什么？", hormone, 1,
                List.of("促进代谢和生长发育", "直接消化食物", "构成细胞膜基本支架", "携带遗传信息"), Set.of("A"), "甲状腺激素可促进新陈代谢并影响生长发育"));
        items.add(choice("BIOLOGY-M9", "BIOLOGY", "MULTIPLE_CHOICE", "关于激素调节中的负反馈，下列说法正确的是哪些？", hormone, 2,
                List.of("调节结果可反过来抑制原调节过程", "有助于维持内环境稳定", "会使偏差无限放大", "只存在于消化系统"), Set.of("A", "B"), "负反馈削弱原有变化，是维持稳态的重要机制"));
        items.add(fill("BIOLOGY-F9", "BIOLOGY", "某些糖尿病患者因____分泌不足而出现持续高血糖。", hormone, 3, "胰岛素"));
        items.add(choice("BIOLOGY-S10", "BIOLOGY", "SINGLE_CHOICE", "同一种激素通常只作用于特定靶细胞，主要原因是什么？", hormone, 2,
                List.of("靶细胞具有相应受体", "其他细胞没有细胞膜", "激素不能随体液运输", "靶细胞没有遗传物质"), Set.of("A"), "只有具有相应受体的细胞才能识别并响应特定激素"));
        items.add(choice("BIOLOGY-M10", "BIOLOGY", "MULTIPLE_CHOICE", "人在紧急状态下肾上腺素分泌增加，可能出现哪些变化？", hormone, 3,
                List.of("心率加快", "血糖升高", "所有代谢立即停止", "体液运输停止"), Set.of("A", "B"), "肾上腺素有助于提高心率和血糖，为应急活动提供条件"));
        items.add(fill("BIOLOGY-F10", "BIOLOGY", "激素由内分泌腺分泌后主要通过____运输到全身。", hormone, 1, "体液"));
    }

    private static DemoDataService.Question choice(String key, String subject, String type, String stem, String point,
            int difficulty, List<String> contents, Set<String> correct, String explanation) {
        return DemoDataService.choice(key, subject, type, stem, point, difficulty, contents, correct, explanation);
    }

    private static DemoDataService.Question fill(String key, String subject, String stem, String point,
            int difficulty, String accepted) {
        return DemoDataService.fill(key, subject, stem, point, difficulty, accepted);
    }
}
