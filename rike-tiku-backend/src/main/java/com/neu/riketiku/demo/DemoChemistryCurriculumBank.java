package com.neu.riketiku.demo;

import static com.neu.riketiku.demo.DemoCurriculumQuestionBank.fill;
import static com.neu.riketiku.demo.DemoCurriculumQuestionBank.multi;
import static com.neu.riketiku.demo.DemoCurriculumQuestionBank.single;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 化学新增81道原创验收题，覆盖16个通用课程模块。 */
final class DemoChemistryCurriculumBank {
    private DemoChemistryCurriculumBank() {
    }

    static List<DemoDataService.Question> questions() {
        List<DemoDataService.Question> q = new ArrayList<>();
        classification(q);
        ionicReaction(q);
        redox(q);
        chemicalAmount(q);
        metals(q);
        nonmetals(q);
        periodicLaw(q);
        chemicalBond(q);
        reactionEnergy(q);
        reactionRate(q);
        equilibrium(q);
        aqueousSolution(q);
        electrochemistry(q);
        organicChemistry(q);
        experiments(q);
        materialsAndLife(q);
        return List.copyOf(q);
    }

    private static void classification(List<DemoDataService.Question> q) {
        String p = "化学基本概念>物质分类>分散系";
        q.add(single("CC-001", "CHEMISTRY", "下列分散系中，分散质粒子尺度通常属于胶体范围的是哪一项？", p, 1,
                List.of("氢氧化铁胶体", "氯化钠溶液", "泥水悬浊液", "纯水"), "A", "胶体分散质粒子直径通常约为1～100 nm，氢氧化铁胶体符合该尺度"));
        q.add(multi("CC-002", "CHEMISTRY", "关于物质分类，下列说法正确的是哪些？", p, 2,
                List.of("空气属于混合物", "液氯属于纯净物", "Na₂CO₃属于碱", "胶体属于分散系"), Set.of("A", "B", "D"),
                "空气由多种气体组成；液氯是氯单质；Na₂CO₃是盐，胶体是一类分散系"));
        q.add(fill("CC-003", "CHEMISTRY", "分散质粒子直径通常在1～100 nm之间的分散系称为____。", p, 2, "胶体",
                "胶体按分散质粒子尺度界定，其粒子直径通常约为1～100 nm"));
        q.add(single("CC-004", "CHEMISTRY", "用一束可见光照射氢氧化铁胶体时可观察到明亮光路，这一现象称为什么？", p, 1,
                List.of("丁达尔效应", "焰色反应", "盐析", "电离"), "A", "胶体粒子对光散射形成可见光路，称为丁达尔效应"));
        q.add(multi("CC-005", "CHEMISTRY", "下列操作可以用于区分或处理分散系的是哪些？", p, 2,
                List.of("用丁达尔效应区分胶体和溶液", "用过滤法除去水中的不溶性泥沙", "用普通滤纸分离胶体粒子与水", "用渗析法除去胶体中的小离子"), Set.of("A", "B", "D"),
                "胶体粒子可通过普通滤纸，不能用普通过滤分开；渗析可利用半透膜除去小离子"));
        q.add(fill("CC-006", "CHEMISTRY", "氢氧化铁胶体与盐酸混合后，胶体先聚沉再溶解，最终铁元素主要以____离子存在。", p, 3, "Fe³⁺",
                "盐酸电解质使胶体聚沉，过量H⁺又与Fe(OH)₃反应，生成溶液中的Fe³⁺"));
    }

    private static void ionicReaction(List<DemoDataService.Question> q) {
        String p = "化学基本概念>离子反应>离子方程式";
        q.add(single("CC-007", "CHEMISTRY", "稀盐酸与氢氧化钠溶液反应的离子方程式是哪一项？", p, 1,
                List.of("H⁺+OH⁻=H₂O", "Na⁺+Cl⁻=NaCl", "2H⁺+O²⁻=H₂O", "HCl+NaOH=NaCl+H₂O"), "A", "强酸强碱在水中完全电离，旁观离子Na⁺和Cl⁻约去，净反应为H⁺+OH⁻=H₂O"));
        q.add(multi("CC-008", "CHEMISTRY", "下列离子在无色强酸性溶液中能够大量共存的是哪些？", p, 2,
                List.of("Na⁺", "K⁺", "NO₃⁻", "CO₃²⁻"), Set.of("A", "B", "C"),
                "Na⁺、K⁺和NO₃⁻在该条件下不反应；CO₃²⁻会与H⁺反应生成CO₂和水"));
        q.add(fill("CC-009", "CHEMISTRY", "书写BaCl₂溶液与Na₂SO₄溶液反应的净离子方程式时，Ba²⁺与SO₄²⁻的化学计量数之比为____。", p, 2, "1∶1",
                "净离子方程式为Ba²⁺+SO₄²⁻=BaSO₄↓，两种离子的系数比为1∶1"));
        q.add(single("CC-010", "CHEMISTRY", "向含有少量HCO₃⁻的溶液中加入足量强酸，主要气体产物是什么？", p, 1,
                List.of("CO₂", "H₂", "O₂", "NH₃"), "A", "HCO₃⁻+H⁺=CO₂↑+H₂O，因此产生二氧化碳"));
        q.add(multi("CC-011", "CHEMISTRY", "下列反应可用同一离子方程式H⁺+OH⁻=H₂O表示的是哪些？", p, 2,
                List.of("稀盐酸与NaOH溶液", "稀硝酸与KOH溶液", "醋酸与NaOH溶液", "稀硫酸与Ba(OH)₂溶液"), Set.of("A", "B"),
                "强酸与可溶性强碱且无其他沉淀反应时可约为该式；醋酸不能拆写，硫酸与氢氧化钡还会生成BaSO₄沉淀"));
    }

    private static void redox(List<DemoDataService.Question> q) {
        String p = "无机化学>元素化合物>氧化还原反应";
        q.add(single("CC-012", "CHEMISTRY", "反应Zn+Cu²⁺=Zn²⁺+Cu中，发生还原反应的微粒是哪一种？", p, 1,
                List.of("Cu²⁺", "Zn", "Zn²⁺", "Cu"), "A", "Cu²⁺得到电子生成Cu，化合价降低，发生还原反应"));
        q.add(multi("CC-013", "CHEMISTRY", "关于氧化还原反应，下列判断正确的是哪些？", p, 2,
                List.of("氧化剂得到电子", "还原剂发生氧化反应", "有元素化合价变化", "氧化剂和还原剂得到电子数可以不相等"), Set.of("A", "B", "C"),
                "电子转移总数守恒，氧化剂得电子被还原，还原剂失电子被氧化，并伴随元素化合价变化"));
        q.add(fill("CC-014", "CHEMISTRY", "反应2Al+3Cu²⁺=2Al³⁺+3Cu中，每2 mol Al反应时转移电子____mol。", p, 2, "6",
                "每个Al原子由0价升到+3价失去3个电子，2 mol Al共失去6 mol电子"));
        q.add(single("CC-015", "CHEMISTRY", "在Cl₂+2Br⁻=2Cl⁻+Br₂反应中，氧化剂是什么？", p, 1,
                List.of("Cl₂", "Br⁻", "Cl⁻", "Br₂"), "A", "Cl₂中的氯由0价降为-1价，Cl₂得到电子并作氧化剂"));
        q.add(multi("CC-016", "CHEMISTRY", "配平酸性条件下MnO₄⁻氧化Fe²⁺的反应时，下列守恒关系必须满足的是哪些？", p, 2,
                List.of("原子守恒", "电荷守恒", "得失电子守恒", "反应前后各物质的物质的量相等"), Set.of("A", "B", "C"),
                "氧化还原离子方程式必须同时满足原子、电荷和电子转移守恒，各物质物质的量并不要求相等"));
    }

    private static void chemicalAmount(List<DemoDataService.Question> q) {
        String p = "化学基本概念>物质的量>摩尔计算";
        q.add(single("CC-017", "CHEMISTRY", "18 g H₂O的物质的量是多少？", p, 1,
                List.of("1 mol", "0.5 mol", "2 mol", "18 mol"), "A", "水的摩尔质量为18 g/mol，由n=m/M得1 mol"));
        q.add(multi("CC-018", "CHEMISTRY", "关于1 mol CO₂，下列说法正确的是哪些？", p, 2,
                List.of("含1 mol碳原子", "含2 mol氧原子", "质量为44 g", "任何条件下体积都为22.4 L"), Set.of("A", "B", "C"),
                "1 mol CO₂含1 mol C和2 mol O，质量为44 g；22.4 L只适用于标准状况下近似理想气体"));
        q.add(fill("CC-019", "CHEMISTRY", "500 mL、0.20 mol/L的NaOH溶液中NaOH的物质的量为____mol。", p, 2, "0.10",
                "体积换算为0.500 L，由n=cV=0.20×0.500=0.10 mol"));
        q.add(single("CC-020", "CHEMISTRY", "相同物质的量的O₂和N₂所含分子数之比是多少？", p, 1,
                List.of("1∶1", "16∶14", "32∶28", "2∶1"), "A", "分子数N=nN_A，相同物质的量对应相同分子数"));
        q.add(multi("CC-021", "CHEMISTRY", "配制一定物质的量浓度溶液时，下列操作会使最终浓度偏低的是哪些？", p, 2,
                List.of("转移时有少量溶液洒出", "定容时俯视刻度线", "洗涤液未转入容量瓶", "摇匀后液面下降再加水至刻度"), Set.of("A", "C", "D"),
                "溶质损失或摇匀后额外加水会使浓度偏低；俯视定容使实际体积偏小，浓度偏高"));
    }

    private static void metals(List<DemoDataService.Question> q) {
        String p = "无机化学>金属及其化合物>铁及其化合物";
        q.add(single("CC-022", "CHEMISTRY", "向FeCl₃溶液中加入足量铁粉，充分反应后铁元素主要以哪种离子存在于溶液？", p, 1,
                List.of("Fe²⁺", "Fe³⁺", "Fe⁴⁺", "不含铁离子"), "A", "铁单质将Fe³⁺还原为Fe²⁺，反应为Fe+2Fe³⁺=3Fe²⁺"));
        q.add(multi("CC-023", "CHEMISTRY", "关于铁及其化合物，下列说法正确的是哪些？", p, 2,
                List.of("Fe²⁺具有一定还原性", "Fe³⁺可用KSCN溶液检验", "铁锈的主要成分是纯FeO", "铁在潮湿空气中更易腐蚀"), Set.of("A", "B", "D"),
                "Fe²⁺可被氧化为Fe³⁺；Fe³⁺与SCN⁻形成血红色络合物；铁锈是复杂的水合氧化物，不是纯FeO"));
        q.add(fill("CC-024", "CHEMISTRY", "用KSCN溶液检验Fe³⁺时，溶液通常显____色。", p, 2, "血红",
                "Fe³⁺与SCN⁻形成血红色络合物，这是常用的Fe³⁺检验反应"));
        q.add(single("CC-025", "CHEMISTRY", "常温下铝制品较耐腐蚀的主要原因是什么？", p, 1,
                List.of("表面形成致密氧化膜", "铝完全不与氧气反应", "铝的密度较小", "铝不导电"), "A", "铝表面迅速形成致密Al₂O₃薄膜，阻止内部铝继续被氧化"));
        q.add(multi("CC-026", "CHEMISTRY", "下列反应能够体现Al(OH)₃两性的是哪些？", p, 2,
                List.of("与盐酸反应", "与NaOH溶液反应", "受热分解", "与水不明显反应"), Set.of("A", "B"),
                "两性氢氧化物既能与酸反应，也能与强碱反应；受热分解不能单独说明两性"));
    }

    private static void nonmetals(List<DemoDataService.Question> q) {
        String p = "无机化学>非金属及其化合物>氯及其化合物";
        q.add(single("CC-027", "CHEMISTRY", "氯气与水反应生成的、具有漂白性的物质是什么？", p, 1,
                List.of("HClO", "HCl", "Cl⁻", "Cl₂O₇"), "A", "氯气与水反应生成盐酸和次氯酸，HClO具有强氧化性并产生漂白作用"));
        q.add(multi("CC-028", "CHEMISTRY", "关于氯及其化合物，下列说法正确的是哪些？", p, 2,
                List.of("干燥氯气不能使干燥有色布条明显褪色", "Cl⁻可用硝酸酸化的AgNO₃检验", "漂白粉有效成分含有次氯酸根", "氯水长期放置有效成分会增加"), Set.of("A", "B", "C"),
                "漂白需要水参与生成HClO；AgCl为白色沉淀；漂白粉含ClO⁻，氯水久置因HClO分解而失效"));
        q.add(fill("CC-029", "CHEMISTRY", "用AgNO₃溶液检验Cl⁻时生成的沉淀化学式为____。", p, 2, "AgCl",
                "Ag⁺与Cl⁻生成难溶的白色AgCl沉淀"));
        q.add(single("CC-030", "CHEMISTRY", "实验室制取氯气时，尾气通常用哪种溶液吸收更合适？", p, 1,
                List.of("NaOH溶液", "饱和食盐水", "稀盐酸", "蒸馏水"), "A", "氯气有毒，可与NaOH反应被吸收；饱和食盐水常用于除HCl或减少氯气溶解，不适合作尾气吸收剂"));
        q.add(multi("CC-031", "CHEMISTRY", "新制氯水中可能存在的微粒包括哪些？", p, 2,
                List.of("Cl₂分子", "HClO分子", "H⁺", "只有Cl⁻一种离子"), Set.of("A", "B", "C"),
                "氯气与水的反应可逆且HClO为弱酸，新制氯水中同时存在Cl₂、HClO、H⁺、Cl⁻等多种微粒"));
    }

    private static void periodicLaw(List<DemoDataService.Question> q) {
        String p = "物质结构与性质>元素周期律>周期性变化";
        q.add(single("CC-032", "CHEMISTRY", "同一周期主族元素从左到右，原子半径总体呈什么趋势？", p, 1,
                List.of("逐渐减小", "逐渐增大", "完全不变", "先减小后增大且无规律"), "A", "同周期核电荷数增加而电子层数不变，有效核吸引增强，原子半径总体减小"));
        q.add(multi("CC-033", "CHEMISTRY", "同一主族元素自上而下，下列总体趋势正确的是哪些？", p, 2,
                List.of("电子层数增加", "原子半径增大", "金属性通常增强", "非金属性一定增强"), Set.of("A", "B", "C"),
                "同主族向下电子层数和半径增加，失电子趋向增强，金属性通常增强、非金属性减弱"));
        q.add(fill("CC-034", "CHEMISTRY", "某主族元素原子最外层有7个电子，该元素通常属于第____族。", p, 2, "ⅦA",
                "主族元素的主族序数通常等于最外层电子数，7个价电子对应第ⅦA族"));
        q.add(single("CC-035", "CHEMISTRY", "比较Na、Mg、Al三种元素的金属性，正确顺序是哪一项？", p, 1,
                List.of("Na>Mg>Al", "Al>Mg>Na", "Mg>Na>Al", "三者相同"), "A", "三者位于同一周期，随原子序数增大金属性逐渐减弱，因此Na>Mg>Al"));
        q.add(multi("CC-036", "CHEMISTRY", "可以作为判断元素非金属性强弱依据的是哪些？", p, 2,
                List.of("最高价氧化物对应水化物的酸性", "气态氢化物的稳定性", "单质与氢气化合的难易", "原子的中子数"), Set.of("A", "B", "C"),
                "非金属性可由最高价含氧酸酸性、氢化物稳定性和与氢气化合能力等反映，中子数不是常规判断依据"));
    }

    private static void chemicalBond(List<DemoDataService.Question> q) {
        String p = "物质结构与性质>化学键>离子键与共价键";
        q.add(single("CC-037", "CHEMISTRY", "NaCl晶体中Na⁺与Cl⁻之间主要存在什么作用？", p, 1,
                List.of("离子键", "非极性共价键", "金属键", "氢键"), "A", "Na⁺和Cl⁻之间依靠静电作用形成离子键"));
        q.add(multi("CC-038", "CHEMISTRY", "下列物质中含有共价键的是哪些？", p, 2,
                List.of("H₂O", "NH₃", "NaOH", "Ne"), Set.of("A", "B", "C"),
                "H₂O和NH₃分子内有共价键；NaOH的OH⁻内部也含O—H共价键；Ne为单原子分子"));
        q.add(fill("CC-039", "CHEMISTRY", "两个氯原子形成Cl₂时，共用电子对的数目为____对。", p, 2, "1",
                "每个氯原子缺一个电子达到稳定结构，两者共用一对电子形成单键"));
        q.add(single("CC-040", "CHEMISTRY", "熔融NaCl能够导电而固态NaCl不能导电，主要原因是什么？", p, 3,
                List.of("熔融时离子可以自由移动", "熔融时产生自由电子", "固态中不存在离子", "固态NaCl不含化学键"), "A", "固态离子被固定在晶格中，熔融后离子可定向移动形成电流"));
        q.add(multi("CC-041", "CHEMISTRY", "关于化学键和物质性质，下列说法正确的是哪些？", p, 2,
                List.of("化学反应通常伴随旧键断裂和新键形成", "分子间作用力不属于化学键", "所有含金属元素的物质都只有金属键", "共价键具有方向性"), Set.of("A", "B", "D"),
                "化学反应本质涉及化学键变化；分子间作用力不同于化学键，含金属元素的盐类可含离子键和共价键"));
    }

    private static void reactionEnergy(List<DemoDataService.Question> q) {
        String p = "化学反应原理>反应热>盖斯定律";
        q.add(single("CC-042", "CHEMISTRY", "某反应的ΔH=-100 kJ·mol⁻¹，该反应属于哪一类？", p, 1,
                List.of("放热反应", "吸热反应", "无热量变化", "无法由ΔH判断"), "A", "ΔH小于零表示反应体系焓降低并向环境放热"));
        q.add(multi("CC-043", "CHEMISTRY", "关于反应热，下列说法正确的是哪些？", p, 2,
                List.of("断裂化学键需要吸收能量", "形成化学键通常释放能量", "反应热只与反应途径有关", "盖斯定律可由分步反应焓变求总焓变"), Set.of("A", "B", "D"),
                "焓变是状态函数，只与始末状态有关；断键吸能、成键放能，分步焓变可按盖斯定律相加"));
        q.add(fill("CC-044", "CHEMISTRY", "已知A→B的ΔH为+20 kJ·mol⁻¹，B→C的ΔH为-50 kJ·mol⁻¹，则A→C的ΔH为____kJ·mol⁻¹。", p, 3, "-30",
                "按盖斯定律总焓变等于各步焓变之和，20+(-50)=-30 kJ·mol⁻¹"));
        q.add(single("CC-045", "CHEMISTRY", "同一反应的热化学方程式各化学计量数同时扩大2倍，ΔH如何变化？", p, 3,
                List.of("扩大2倍", "减小为一半", "不变", "变为0"), "A", "反应焓变与反应进度成正比，方程式系数整体加倍时ΔH也加倍"));
        q.add(fill("CC-046", "CHEMISTRY", "1 mol CH₄完全燃烧放出890 kJ热量，0.5 mol CH₄完全燃烧放出____kJ。", p, 2, "445",
                "相同条件下反应热与反应物的物质的量成正比，0.5 mol释放890×0.5=445 kJ"));
    }

    private static void reactionRate(List<DemoDataService.Question> q) {
        String p = "化学反应原理>反应速率>影响因素";
        q.add(single("CC-047", "CHEMISTRY", "其他条件相同时，将块状大理石研成粉末与盐酸反应，速率通常如何变化？", p, 1,
                List.of("加快", "减慢", "不变", "先停止后加快"), "A", "研成粉末增大了固体表面积，使有效碰撞机会增多，反应速率加快"));
        q.add(multi("CC-048", "CHEMISTRY", "下列措施通常能够加快化学反应速率的是哪些？", p, 2,
                List.of("升高温度", "增大反应物浓度", "加入合适催化剂", "对所有反应都降低压强"), Set.of("A", "B", "C"),
                "升温、增大浓度和催化剂通常增加有效碰撞；降低压强只会使有气体参与的部分反应减慢"));
        q.add(fill("CC-049", "CHEMISTRY", "反应物浓度在20 s内由0.80 mol/L降至0.40 mol/L，用该反应物表示的平均消耗速率为____mol/(L·s)。", p, 3, "0.020",
                "平均消耗速率为(0.80-0.40)/20=0.020 mol/(L·s)"));
        q.add(single("CC-050", "CHEMISTRY", "催化剂能加快反应速率的主要原因是什么？", p, 3,
                List.of("降低反应的活化能", "增大反应焓变", "改变反应物总能量", "使平衡常数增大"), "A", "催化剂提供活化能较低的反应途径，但不改变反应焓变和平衡常数"));
        q.add(fill("CC-051", "CHEMISTRY", "对于有气体参加的反应，在恒温下压缩容器通常会增大气体的____，从而可能加快反应。", p, 2, "浓度",
                "恒温压缩使单位体积内气体粒子数增加，即浓度增大，有效碰撞频率通常随之提高"));
    }

    private static void equilibrium(List<DemoDataService.Question> q) {
        String p = "化学反应原理>化学平衡>平衡移动";
        q.add(single("CC-052", "CHEMISTRY", "可逆反应达到化学平衡时，正、逆反应速率的关系是什么？", p, 1,
                List.of("相等且均不为零", "均为零", "正反应速率更大", "逆反应速率更大"), "A", "化学平衡是动态平衡，正逆反应持续进行且速率相等"));
        q.add(multi("CC-053", "CHEMISTRY", "对反应2SO₂(g)+O₂(g)⇌2SO₃(g)，下列措施能使平衡向右移动的是哪些？", p, 2,
                List.of("增加O₂浓度", "移走SO₃", "恒温增大压强", "加入催化剂"), Set.of("A", "B", "C"),
                "增大反应物浓度、减小生成物浓度或增压都会使该平衡右移；催化剂只缩短达到平衡的时间"));
        q.add(fill("CC-054", "CHEMISTRY", "在一定温度下，反应达到平衡后加入催化剂，平衡常数____。", p, 3, "不变",
                "平衡常数只随温度改变；催化剂同等程度加快正逆反应，不改变平衡组成"));
        q.add(single("CC-055", "CHEMISTRY", "对于放热可逆反应，升高温度时平衡通常向哪个方向移动？", p, 3,
                List.of("吸热的逆反应方向", "放热的正反应方向", "一定不移动", "无法根据热效应判断"), "A", "升温相当于增加热量，平衡向吸收热量的方向，即放热反应的逆向移动"));
        q.add(fill("CC-056", "CHEMISTRY", "反应N₂(g)+3H₂(g)⇌2NH₃(g)达到平衡后，恒温减小容器体积，平衡向____移动。", p, 2, "右",
                "减小体积使压强增大，平衡向气体物质的量较少的右侧移动"));
    }

    private static void aqueousSolution(List<DemoDataService.Question> q) {
        String p = "化学反应原理>水溶液>酸碱与pH";
        q.add(single("CC-057", "CHEMISTRY", "25℃时，pH=3的强酸溶液中c(H⁺)是多少？", p, 1,
                List.of("1×10⁻³ mol/L", "3 mol/L", "1×10⁻¹¹ mol/L", "1×10³ mol/L"), "A", "由pH=-lg c(H⁺)，pH=3时c(H⁺)=10⁻³ mol/L"));
        q.add(multi("CC-058", "CHEMISTRY", "关于弱电解质在水中的电离，下列说法正确的是哪些？", p, 2,
                List.of("电离过程可建立动态平衡", "加水稀释通常促进电离", "弱电解质完全不产生离子", "同温度下电离常数只由电解质本性决定"), Set.of("A", "B", "D"),
                "弱电解质部分电离并建立动态平衡，稀释促进电离；电离常数在给定温度下由物质本性决定"));
        q.add(fill("CC-059", "CHEMISTRY", "25℃时，pH=5的水溶液中c(H⁺)为____mol/L。", p, 3, "1×10⁻⁵",
                "根据pH定义，c(H⁺)=10^(-pH)=1×10⁻⁵ mol/L"));
        q.add(single("CC-060", "CHEMISTRY", "向醋酸溶液中加入少量醋酸钠固体，醋酸的电离程度通常如何变化？", p, 3,
                List.of("减小", "增大", "不变", "先增大后减小"), "A", "醋酸钠提供CH₃COO⁻，同离子效应使醋酸电离平衡向左移动，电离程度减小"));
        q.add(fill("CC-061", "CHEMISTRY", "25℃时某溶液c(H⁺)=1×10⁻⁹ mol/L，则其pH为____。", p, 3, "9",
                "pH=-lg(1×10⁻⁹)=9，该溶液呈碱性"));
    }

    private static void electrochemistry(List<DemoDataService.Question> q) {
        String p = "化学反应原理>电化学>原电池与电解池";
        q.add(single("CC-062", "CHEMISTRY", "在Zn-Cu原电池中，锌电极通常发生什么反应？", p, 1,
                List.of("氧化反应", "还原反应", "中和反应", "水解反应"), "A", "锌较活泼，在负极失去电子生成Zn²⁺，发生氧化反应"));
        q.add(multi("CC-063", "CHEMISTRY", "关于原电池，下列说法正确的是哪些？", p, 2,
                List.of("将化学能转化为电能", "电子由负极经外电路流向正极", "盐桥可维持两侧溶液电荷平衡", "正极一定是金属铜"), Set.of("A", "B", "C"),
                "原电池通过自发氧化还原反应输出电能，电子从负极到正极；正极材料取决于具体装置，不一定是铜"));
        q.add(fill("CC-064", "CHEMISTRY", "电解熔融NaCl时，阴极产物为____。", p, 3, "Na",
                "熔融NaCl中Na⁺在阴极得到电子，Na⁺+e⁻=Na"));
        q.add(single("CC-065", "CHEMISTRY", "钢铁发生吸氧腐蚀时，正极的主要反应物是什么？", p, 3,
                List.of("O₂和H₂O", "Fe", "Fe²⁺", "Cl₂"), "A", "中性或弱碱性潮湿环境中，正极发生O₂+2H₂O+4e⁻=4OH⁻"));
        q.add(fill("CC-066", "CHEMISTRY", "电解CuSO₄溶液并用惰性电极时，阴极析出的金属是____。", p, 3, "Cu",
                "Cu²⁺比水更易在阴极得到电子，反应为Cu²⁺+2e⁻=Cu"));
    }

    private static void organicChemistry(List<DemoDataService.Question> q) {
        String p = "有机化学基础>烃及其衍生物>官能团与反应";
        q.add(single("CC-067", "CHEMISTRY", "乙醇分子中决定其典型化学性质的官能团是什么？", p, 1,
                List.of("羟基", "羧基", "醛基", "碳碳双键"), "A", "乙醇的官能团是羟基—OH，它决定乙醇的多种典型反应"));
        q.add(multi("CC-068", "CHEMISTRY", "下列反应属于有机反应基本类型的是哪些？", p, 2,
                List.of("乙烯与溴的加成反应", "甲烷与氯气的取代反应", "乙醇燃烧的氧化反应", "NaCl溶于水的水合过程"), Set.of("A", "B", "C"),
                "加成、取代和氧化是常见有机反应类型；NaCl溶解是物理过程与离子水合，不是有机反应"));
        q.add(fill("CC-069", "CHEMISTRY", "乙酸分子中的官能团名称为____。", p, 3, "羧基",
                "乙酸结构中含—COOH，该官能团称为羧基"));
        q.add(single("CC-070", "CHEMISTRY", "乙烯使溴水褪色的主要反应类型是什么？", p, 3,
                List.of("加成反应", "取代反应", "酯化反应", "水解反应"), "A", "乙烯的碳碳双键与Br₂发生加成，消耗溴而使溴水褪色"));
        q.add(fill("CC-071", "CHEMISTRY", "乙醇与乙酸在浓硫酸和加热条件下生成的有机产物是____。", p, 3, "乙酸乙酯",
                "醇与羧酸发生酯化反应，乙醇和乙酸生成乙酸乙酯和水"));
    }

    private static void experiments(List<DemoDataService.Question> q) {
        String p = "化学实验>实验方法>分离检验与安全";
        q.add(single("CC-072", "CHEMISTRY", "分离互不相溶的水和苯，最合适的仪器是什么？", p, 1,
                List.of("分液漏斗", "容量瓶", "滴定管", "蒸发皿"), "A", "水和苯互不相溶并分层，可用分液漏斗分离"));
        q.add(multi("CC-073", "CHEMISTRY", "下列化学实验操作符合安全规范的是哪些？", p, 2,
                List.of("稀释浓硫酸时将浓硫酸缓慢加入水中", "闻气体时用手轻扇使少量气体飘向鼻孔", "剩余药品一律倒回原试剂瓶", "加热试管时管口不朝向人"), Set.of("A", "B", "D"),
                "浓硫酸应入水并搅拌，闻气体用扇闻法，加热管口避人；剩余药品通常不能倒回原瓶以防污染"));
        q.add(fill("CC-074", "CHEMISTRY", "用蒸馏法分离液体混合物，主要利用各组分____不同。", p, 3, "沸点",
                "蒸馏通过加热汽化和冷凝，利用各组分沸点差异实现分离"));
        q.add(single("CC-075", "CHEMISTRY", "检验某无色气体是否为CO₂，较可靠的方法是什么？", p, 3,
                List.of("通入澄清石灰水观察是否变浑浊", "闻气味", "点燃气体", "观察气体颜色"), "A", "CO₂与石灰水反应生成CaCO₃沉淀，出现浑浊是常用检验现象"));
        q.add(fill("CC-076", "CHEMISTRY", "滴定实验中，读取滴定管体积时视线应与液体凹液面最低点保持____。", p, 3, "水平",
                "视线与凹液面最低点水平可避免仰视或俯视造成的系统读数误差"));
    }

    private static void materialsAndLife(List<DemoDataService.Question> q) {
        String p = "化学基本概念>化学与社会>材料和文物保护";
        q.add(single("CC-077", "CHEMISTRY", "钢筋混凝土中的钢筋若长期接触水和氧气，最需要防止的变化是什么？", p, 1,
                List.of("电化学腐蚀", "升华", "核裂变", "光合作用"), "A", "潮湿并有氧气时钢铁容易形成微小原电池而发生电化学腐蚀"));
        q.add(multi("CC-078", "CHEMISTRY", "下列做法符合材料合理使用和环境保护的是哪些？", p, 2,
                List.of("分类回收废旧电池", "按材料性质选择防腐方法", "将实验废液直接倒入下水道", "减少一次性塑料的无序使用"), Set.of("A", "B", "D"),
                "废旧电池和实验废液需规范处理，材料应按性质防护，减少不必要塑料使用有助于降低环境负担"));
        q.add(fill("CC-079", "CHEMISTRY", "在钢铁表面涂漆防锈，主要是为了隔绝水和____。", p, 3, "氧气",
                "钢铁锈蚀通常需要水和氧气共同参与，涂层通过隔绝二者减缓腐蚀"));
        q.add(multi("CC-080", "CHEMISTRY", "关于高分子材料，下列说法正确的是哪些？", p, 3,
                List.of("聚乙烯由乙烯加聚得到", "天然橡胶和合成橡胶都属于高分子材料", "所有塑料都能在自然环境中迅速降解", "合理回收可减少资源浪费"), Set.of("A", "B", "D"),
                "乙烯可加聚成聚乙烯，橡胶属于高分子材料；普通塑料往往难以快速降解，因此应合理减量和回收"));
        q.add(fill("CC-081", "CHEMISTRY", "为减缓埋地钢管腐蚀，将其与更活泼的镁块相连，这种保护方法称为牺牲____保护法。", p, 3, "阳极",
                "镁作更活泼的负极并优先被氧化，相当于牺牲阳极，使钢管作为正极受到保护"));
    }
}
