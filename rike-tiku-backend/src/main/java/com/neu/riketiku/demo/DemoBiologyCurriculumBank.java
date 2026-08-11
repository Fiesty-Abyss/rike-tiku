package com.neu.riketiku.demo;

import static com.neu.riketiku.demo.DemoCurriculumQuestionBank.fill;
import static com.neu.riketiku.demo.DemoCurriculumQuestionBank.multi;
import static com.neu.riketiku.demo.DemoCurriculumQuestionBank.single;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 生物新增79道原创验收题，覆盖21个通用课程模块。 */
final class DemoBiologyCurriculumBank {
    private DemoBiologyCurriculumBank() {
    }

    static List<DemoDataService.Question> questions() {
        List<DemoDataService.Question> q = new ArrayList<>();
        cellMolecules(q);
        cellStructure(q);
        membraneTransport(q);
        enzymeAndAtp(q);
        respiration(q);
        photosynthesis(q);
        cellLife(q);
        segregation(q);
        dnaAndExpression(q);
        variation(q);
        evolution(q);
        homeostasis(q);
        nervousRegulation(q);
        hormonalRegulation(q);
        immunity(q);
        plantRegulation(q);
        populationAndCommunity(q);
        ecosystem(q);
        fermentation(q);
        bioengineering(q);
        inquiry(q);
        return List.copyOf(q);
    }

    private static void cellMolecules(List<DemoDataService.Question> q) {
        String p = "分子与细胞>组成细胞的分子>蛋白质与核酸";
        q.add(single("BC-001", "BIOLOGY", "组成蛋白质的基本单位是什么？", p, 1,
                List.of("氨基酸", "核苷酸", "葡萄糖", "脂肪酸"), "A", "蛋白质由氨基酸脱水缩合形成多肽链，氨基酸是其基本组成单位"));
        q.add(multi("BC-002", "BIOLOGY", "关于组成细胞的化合物，下列说法正确的是哪些？", p, 2,
                List.of("水可参与细胞内多种化学反应", "核酸可储存或传递遗传信息", "所有糖类都是细胞的直接能源", "蛋白质可具有催化或运输功能"), Set.of("A", "B", "D"),
                "水是重要反应介质和反应物，核酸承载遗传信息，蛋白质功能多样；核糖、纤维素等糖类并非直接能源"));
        q.add(fill("BC-003", "BIOLOGY", "核酸的基本组成单位是____。", p, 2, "核苷酸",
                "DNA和RNA均由核苷酸连接而成，核苷酸是核酸的基本单位"));
        q.add(single("BC-004", "BIOLOGY", "两个氨基酸脱水缩合形成二肽时，直接形成的化学键是什么？", p, 1,
                List.of("肽键", "氢键", "磷酸二酯键", "糖苷键"), "A", "一个氨基酸的羧基与另一个氨基酸的氨基脱水缩合，形成—CO—NH—形式的肽键"));
    }

    private static void cellStructure(List<DemoDataService.Question> q) {
        String p = "分子与细胞>细胞结构>细胞膜";
        q.add(single("BC-005", "BIOLOGY", "真核细胞中控制细胞代谢和遗传的主要结构是什么？", p, 1,
                List.of("细胞核", "核糖体", "高尔基体", "中心体"), "A", "细胞核含有主要遗传物质并通过基因表达调控细胞代谢，是细胞的控制中心"));
        q.add(multi("BC-006", "BIOLOGY", "下列结构与其主要功能的对应关系正确的是哪些？", p, 2,
                List.of("核糖体—合成蛋白质", "线粒体—有氧呼吸主要场所", "高尔基体—加工和运输分泌蛋白", "溶酶体—进行光合作用"), Set.of("A", "B", "C"),
                "核糖体、线粒体和高尔基体的对应功能正确；光合作用在叶绿体中进行，溶酶体主要参与细胞内消化"));
        q.add(fill("BC-007", "BIOLOGY", "细胞膜主要由磷脂分子和____组成。", p, 2, "蛋白质",
                "流动镶嵌模型认为细胞膜以磷脂双分子层为基本支架，其中镶嵌或附着多种蛋白质"));
        q.add(single("BC-008", "BIOLOGY", "分泌蛋白合成后，通常首先进入哪种细胞器的腔内进行初步加工？", p, 1,
                List.of("内质网", "叶绿体", "中心体", "液泡"), "A", "分泌蛋白在附着于粗面内质网的核糖体上合成，随后进入内质网腔加工和运输"));
    }

    private static void membraneTransport(List<DemoDataService.Question> q) {
        String p = "分子与细胞>细胞的物质输入和输出>葡萄糖运输";
        q.add(single("BC-009", "BIOLOGY", "葡萄糖顺浓度梯度借助载体蛋白进入细胞，但不消耗ATP，这种方式属于什么？", p, 1,
                List.of("协助扩散", "主动运输", "胞吞", "自由扩散"), "A", "顺浓度梯度、需要载体且不消耗能量的跨膜运输是协助扩散"));
        q.add(multi("BC-010", "BIOLOGY", "关于物质跨膜运输，下列说法正确的是哪些？", p, 2,
                List.of("自由扩散不需要载体", "主动运输可逆浓度梯度进行", "胞吞和胞吐依赖膜的流动性", "水分子只能通过主动运输跨膜"), Set.of("A", "B", "C"),
                "水可通过自由扩散或水通道蛋白跨膜，不必依赖主动运输；其余三项符合相应运输方式特点"));
        q.add(fill("BC-011", "BIOLOGY", "植物细胞置于高浓度外界溶液中失水，原生质层与细胞壁逐渐分离的现象称为____。", p, 2, "质壁分离",
                "外界溶液浓度较高时细胞失水，原生质体体积缩小并与细胞壁分离，形成质壁分离"));
        q.add(single("BC-012", "BIOLOGY", "某细胞逆浓度梯度吸收葡萄糖时，低温会明显降低吸收速率，但对O₂自由扩散影响较小，最合理的解释是什么？", p, 3,
                List.of("葡萄糖吸收依赖酶促供能过程", "葡萄糖能直接穿过磷脂层", "O₂运输一定消耗ATP", "低温使细胞膜完全破裂"), "A", "题设中的葡萄糖吸收属于主动运输，需要呼吸供能和载体活动，低温降低酶活性；O₂自由扩散不直接耗能"));
    }

    private static void enzymeAndAtp(List<DemoDataService.Question> q) {
        String p = "分子与细胞>细胞代谢>酶与ATP";
        q.add(single("BC-013", "BIOLOGY", "酶能够提高化学反应速率的直接原因是什么？", p, 1,
                List.of("降低反应所需活化能", "为反应提供全部能量", "改变反应平衡点", "使产物能量升高"), "A", "酶通过降低活化能加快反应，但不改变反应的平衡位置和总能量变化"));
        q.add(multi("BC-014", "BIOLOGY", "关于ATP，下列说法正确的是哪些？", p, 2,
                List.of("ATP可为细胞生命活动直接供能", "ATP与ADP可相互转化", "ATP在细胞中大量长期储存", "ATP水解释放的能量可与吸能反应偶联"), Set.of("A", "B", "D"),
                "ATP是直接能源物质并处于快速周转中，通常含量不高，水解释能可驱动多种吸能过程"));
        q.add(fill("BC-015", "BIOLOGY", "酶具有高效性和专一性，其作用条件通常还具有一定的温度和____范围。", p, 2, "pH",
                "温度和pH会影响酶的空间结构和活性，每种酶通常具有适宜的温度和pH范围"));
        q.add(single("BC-016", "BIOLOGY", "将胃蛋白酶溶液置于强碱性环境后活性明显降低，主要原因是什么？", p, 3,
                List.of("酶的空间结构可能改变", "底物全部转化为ATP", "酶分子数量必然增加", "强碱为反应提供氧气"), "A", "胃蛋白酶适宜酸性环境，强碱可能破坏其空间结构并使活性降低甚至失活"));
    }

    private static void respiration(List<DemoDataService.Question> q) {
        String p = "分子与细胞>细胞代谢>细胞呼吸";
        q.add(single("BC-017", "BIOLOGY", "有氧呼吸第一阶段发生的主要场所是什么？", p, 1,
                List.of("细胞质基质", "线粒体内膜", "线粒体基质", "叶绿体基质"), "A", "葡萄糖分解为丙酮酸的第一阶段在细胞质基质中进行"));
        q.add(multi("BC-018", "BIOLOGY", "关于细胞呼吸，下列说法正确的是哪些？", p, 2,
                List.of("有氧呼吸能彻底氧化有机物", "无氧呼吸也可产生少量ATP", "氧气直接参与有氧呼吸第一阶段", "呼吸释放的能量只有一部分储存在ATP中"), Set.of("A", "B", "D"),
                "氧气参与有氧呼吸末阶段，不直接参与第一阶段；呼吸能量部分转入ATP，部分以热形式散失"));
        q.add(fill("BC-019", "BIOLOGY", "酵母菌在无氧条件下进行酒精发酵，除酒精外还产生____气体。", p, 2, "二氧化碳",
                "酵母菌无氧呼吸生成酒精和二氧化碳，并释放少量能量"));
        q.add(single("BC-020", "BIOLOGY", "长时间剧烈运动后肌肉出现酸胀，与哪种物质暂时积累有关？", p, 3,
                List.of("乳酸", "酒精", "淀粉", "尿素"), "A", "供氧不足时肌细胞可进行乳酸发酵，乳酸暂时积累与酸胀感有关"));
    }

    private static void photosynthesis(List<DemoDataService.Question> q) {
        String p = "分子与细胞>细胞代谢>光合作用";
        q.add(single("BC-021", "BIOLOGY", "绿色植物光反应阶段的主要场所是什么？", p, 1,
                List.of("叶绿体类囊体薄膜", "叶绿体基质", "细胞质基质", "线粒体内膜"), "A", "光合色素和光反应相关蛋白位于类囊体薄膜，光能在此转化"));
        q.add(multi("BC-022", "BIOLOGY", "光合作用过程中，下列判断正确的是哪些？", p, 2,
                List.of("光反应产生ATP和NADPH", "暗反应固定CO₂", "释放的O₂来自CO₂", "暗反应需要光反应提供能量和还原力"), Set.of("A", "B", "D"),
                "光反应水的光解释放O₂并产生ATP和NADPH，暗反应利用这些产物固定和还原CO₂"));
        q.add(fill("BC-023", "BIOLOGY", "光合作用释放的氧气来源于____的光解。", p, 2, "水",
                "同位素示踪等证据表明，光合作用释放的O₂来自水分子的光解"));
        q.add(single("BC-024", "BIOLOGY", "在光照、温度等条件适宜时，降低环境CO₂浓度会直接限制光合作用的哪个过程？", p, 3,
                List.of("碳的固定", "水的光解", "叶绿素吸收光能", "氧气扩散"), "A", "CO₂是碳固定的底物，浓度降低会首先限制暗反应中的CO₂固定"));
    }

    private static void cellLife(List<DemoDataService.Question> q) {
        String p = "分子与细胞>细胞生命历程>有丝分裂与凋亡";
        q.add(single("BC-025", "BIOLOGY", "有丝分裂过程中，姐妹染色单体分离发生在哪个时期？", p, 1,
                List.of("后期", "前期", "中期", "末期结束后"), "A", "有丝分裂后期着丝粒分裂，姐妹染色单体分开并移向细胞两极"));
        q.add(multi("BC-026", "BIOLOGY", "关于细胞生命历程，下列说法正确的是哪些？", p, 2,
                List.of("细胞分化通常伴随基因选择性表达", "细胞凋亡受基因调控", "细胞衰老意味着所有酶活性都升高", "癌细胞可能具有无限增殖倾向"), Set.of("A", "B", "D"),
                "分化和凋亡均受基因调控；衰老细胞多种酶活性降低，癌细胞常出现增殖失控"));
        q.add(fill("BC-027", "BIOLOGY", "细胞周期中完成DNA复制的时期称为____期。", p, 2, "S",
                "细胞间期分为G₁、S和G₂期，DNA复制主要在S期完成"));
        q.add(single("BC-028", "BIOLOGY", "蝌蚪发育过程中尾部逐渐消失，主要涉及哪种细胞过程？", p, 3,
                List.of("细胞凋亡", "细胞无限增殖", "细胞质壁分离", "细胞受精"), "A", "尾部细胞按发育程序有序死亡，属于受基因调控的细胞凋亡"));
    }

    private static void segregation(List<DemoDataService.Question> q) {
        String p = "遗传与进化>遗传规律>分离定律";
        q.add(single("BC-029", "BIOLOGY", "基因型为Aa的个体在不考虑突变时可产生哪两类配子？", p, 1,
                List.of("A和a", "AA和aa", "只有A", "Aa和aa"), "A", "减数分裂形成配子时成对等位基因分离，每个配子含A或a中的一个"));
        q.add(multi("BC-030", "BIOLOGY", "在完全显性且后代数量足够多时，Aa×aa测交后代的判断正确的是哪些？", p, 2,
                List.of("Aa与aa约各占一半", "显性与隐性表现型约为1∶1", "后代全部杂合", "可用于判断显性个体的基因型"), Set.of("A", "B", "D"),
                "测交亲本aa只产生a配子，Aa产生A、a两类配子，后代基因型和表现型均约1∶1"));
        q.add(fill("BC-031", "BIOLOGY", "在完全显性条件下，Aa自交后代中隐性表现型所占比例为____。", p, 2, "1/4",
                "Aa自交后代基因型比例为1AA∶2Aa∶1aa，只有aa表现隐性，所以比例为1/4"));
        q.add(single("BC-032", "BIOLOGY", "某显性性状个体与隐性纯合子测交，后代全为显性。在后代数量充分且无致死等干扰时，该亲本最可能是什么基因型？", p, 3,
                List.of("AA", "Aa", "aa", "无法作任何推断"), "A", "AA只产生A配子，与aa测交后代均为Aa并表现显性；Aa测交通常会出现隐性后代"));
    }

    private static void dnaAndExpression(List<DemoDataService.Question> q) {
        String p = "遗传与进化>基因的本质与表达>DNA复制与表达";
        q.add(single("BC-033", "BIOLOGY", "以DNA的一条链为模板合成RNA的过程称为什么？", p, 1,
                List.of("转录", "翻译", "复制", "逆转录"), "A", "转录以DNA一条链为模板，按碱基互补配对原则合成RNA"));
        q.add(multi("BC-034", "BIOLOGY", "关于DNA复制和基因表达，下列说法正确的是哪些？", p, 2,
                List.of("DNA复制具有半保留特点", "翻译在核糖体上进行", "密码子位于mRNA上", "一个密码子同时编码多种氨基酸"), Set.of("A", "B", "C"),
                "DNA复制半保留，mRNA密码子在核糖体上被读取；遗传密码具有确定性，一个密码子通常只对应一种氨基酸或终止信号"));
        q.add(fill("BC-035", "BIOLOGY", "mRNA上决定一个氨基酸的三个相邻碱基称为一个____。", p, 2, "密码子",
                "翻译时核糖体按mRNA上连续三个碱基为一组读取，这一组三联体称为密码子"));
        q.add(single("BC-036", "BIOLOGY", "DNA一条模板链局部碱基序列为3′-TAC-5′，转录得到的mRNA对应序列是哪一项？", p, 3,
                List.of("5′-AUG-3′", "5′-TAC-3′", "3′-AUG-5′", "5′-UAC-3′"), "A", "RNA与模板链反向互补，T-A、A-U、C-G，因此得到5′-AUG-3′"));
    }

    private static void variation(List<DemoDataService.Question> q) {
        String p = "遗传与进化>变异>基因突变与染色体变异";
        q.add(single("BC-037", "BIOLOGY", "DNA分子中碱基对发生替换、增添或缺失，可能形成哪类变异？", p, 1,
                List.of("基因突变", "基因重组", "染色体组加倍", "环境适应"), "A", "基因内部碱基序列改变属于基因突变，可表现为替换、增添或缺失"));
        q.add(multi("BC-038", "BIOLOGY", "下列属于可遗传变异来源的是哪些？", p, 2,
                List.of("基因突变", "基因重组", "染色体变异", "单纯由营养差异导致且遗传物质不变的体重变化"), Set.of("A", "B", "C"),
                "遗传物质发生改变的基因突变、重组和染色体变异可遗传；仅由环境导致且遗传物质不变的变化通常不遗传"));
        q.add(fill("BC-039", "BIOLOGY", "某二倍体生物体细胞中多了一条特定染色体，这种变异称为____体。", p, 2, "三",
                "正常二倍体某对染色体多一条时，染色体组成表示为2n+1，称为三体"));
        q.add(multi("BC-040", "BIOLOGY", "关于基因突变，下列判断正确的是哪些？", p, 3,
                List.of("具有不定向性", "可以产生新的等位基因", "发生后一定改变生物表现型", "是生物变异的根本来源之一"), Set.of("A", "B", "D"),
                "基因突变不定向并能产生新等位基因，是变异的重要根源；由于密码简并、隐性等原因，不一定改变表现型"));
    }

    private static void evolution(List<DemoDataService.Question> q) {
        String p = "遗传与进化>生物进化>自然选择";
        q.add(single("BC-041", "BIOLOGY", "达尔文自然选择学说中，环境的作用主要是什么？", p, 1,
                List.of("对已有可遗传变异进行选择", "定向诱导所有个体产生需要的变异", "使个体主动改变基因", "消除种群内全部差异"), "A", "变异先于选择出现，环境使具有不同可遗传性状的个体繁殖成功率不同"));
        q.add(multi("BC-042", "BIOLOGY", "关于现代生物进化理论，下列说法正确的是哪些？", p, 2,
                List.of("种群是生物进化的基本单位", "突变和基因重组产生进化原材料", "自然选择可改变种群基因频率", "个体在一生中获得的适应性改变一定遗传"), Set.of("A", "B", "C"),
                "进化体现在种群基因频率改变；遗传变异提供材料，自然选择定向改变频率，个体后天获得性状不一定可遗传"));
        q.add(fill("BC-043", "BIOLOGY", "同一物种不同种群长期地理隔离后，若形成____隔离，可标志新物种形成。", p, 2, "生殖",
                "物种形成的关键标志是种群间出现生殖隔离，即不能自由交配或不能产生可育后代"));
        q.add(multi("BC-044", "BIOLOGY", "某昆虫种群原有绿色和褐色体色，环境变暗后褐色个体比例逐代升高。合理解释包括哪些？", p, 3,
                List.of("种群原有体色变异", "褐色个体在该环境中可能更易存活繁殖", "环境直接把所有绿色个体基因改成褐色", "自然选择改变了相关基因频率"), Set.of("A", "B", "D"),
                "自然选择作用于种群已有变异，适应性较高的褐色个体留下更多后代，从而使相关基因频率上升"));
    }

    private static void homeostasis(List<DemoDataService.Question> q) {
        String p = "稳态与调节>内环境>稳态";
        q.add(single("BC-045", "BIOLOGY", "人体内环境主要由血浆、组织液和什么组成？", p, 1,
                List.of("淋巴液", "细胞内液", "消化液", "尿液"), "A", "内环境是细胞直接生活的细胞外液，主要包括血浆、组织液和淋巴液"));
        q.add(multi("BC-046", "BIOLOGY", "关于内环境稳态，下列说法正确的是哪些？", p, 2,
                List.of("稳态是相对稳定而非恒定不变", "神经、体液和免疫调节共同参与", "内环境变化超过调节能力可能导致疾病", "稳态只包括体温恒定"), Set.of("A", "B", "C"),
                "稳态涉及温度、pH、渗透压等多项指标的动态平衡，由多种调节网络共同维持"));
        q.add(fill("BC-047", "BIOLOGY", "人体细胞外液渗透压的90%以上来源于Na⁺和____。", p, 2, "Cl⁻",
                "细胞外液中Na⁺和Cl⁻含量较高，对渗透压的贡献占主要部分"));
        q.add(multi("BC-048", "BIOLOGY", "剧烈运动时机体仍能维持内环境相对稳定，可能发生的调节有哪一些？", p, 3,
                List.of("呼吸加深加快以排出更多CO₂", "皮肤血流和出汗变化参与散热", "缓冲物质参与维持pH", "血糖调节完全停止"), Set.of("A", "B", "C"),
                "运动时呼吸、循环、体温和酸碱调节协同工作，血糖调节也会继续进行以满足能量需求"));
    }

    private static void nervousRegulation(List<DemoDataService.Question> q) {
        String p = "稳态与调节>生命活动调节>神经调节";
        q.add(single("BC-049", "BIOLOGY", "完成反射活动的结构基础是什么？", p, 1,
                List.of("反射弧", "突触小泡", "大脑皮层单个神经元", "效应器单独结构"), "A", "反射活动依赖完整反射弧，包括感受器、传入神经、神经中枢、传出神经和效应器"));
        q.add(multi("BC-050", "BIOLOGY", "关于兴奋在神经系统中的传导和传递，下列说法正确的是哪些？", p, 2,
                List.of("神经纤维上的兴奋以电信号形式传导", "化学突触处通常单向传递", "神经递质由突触后膜释放", "突触存在使传递发生时间延搁"), Set.of("A", "B", "D"),
                "神经递质通常由突触前膜释放并作用于突触后膜，化学突触结构决定其单向和相对较慢的传递"));
        q.add(fill("BC-051", "BIOLOGY", "神经元之间传递兴奋时，由突触前膜释放并作用于突触后膜的化学物质称为____。", p, 2, "神经递质",
                "突触小泡释放神经递质，递质跨越突触间隙并与突触后膜受体结合"));
        q.add(fill("BC-052", "BIOLOGY", "反射弧中将神经中枢发出的兴奋传到效应器的结构是____神经。", p, 3, "传出",
                "传出神经连接神经中枢与效应器，将中枢发出的兴奋传向效应器"));
    }

    private static void hormonalRegulation(List<DemoDataService.Question> q) {
        String p = "稳态与调节>生命活动调节>激素调节";
        q.add(single("BC-053", "BIOLOGY", "健康人血糖升高时，分泌量通常增加的激素是什么？", p, 1,
                List.of("胰岛素", "胰高血糖素", "抗利尿激素", "甲状腺激素"), "A", "血糖升高刺激胰岛B细胞分泌胰岛素，促进组织摄取、利用和储存葡萄糖"));
        q.add(multi("BC-054", "BIOLOGY", "关于激素调节，下列说法正确的是哪些？", p, 2,
                List.of("激素通过体液运输", "激素只作用于具有相应受体的靶细胞", "激素含量虽少但作用显著", "一种激素只运输到一个器官"), Set.of("A", "B", "C"),
                "激素随体液广泛运输，但只有具有相应受体的靶细胞响应；其特点包括微量和高效"));
        q.add(fill("BC-055", "BIOLOGY", "甲状腺激素浓度升高后抑制下丘脑和垂体相关激素分泌，这种调节称为____反馈。", p, 3, "负",
                "系统输出升高后反过来抑制上游，使输出回落，这种维持稳定的机制是负反馈"));
        q.add(fill("BC-056", "BIOLOGY", "人体缺水时，促进肾小管和集合管重吸收水的激素是____。", p, 3, "抗利尿激素",
                "缺水使细胞外液渗透压升高，抗利尿激素释放增加，促进水重吸收并减少尿量"));
    }

    private static void immunity(List<DemoDataService.Question> q) {
        String p = "稳态与调节>生命活动调节>免疫调节";
        q.add(single("BC-057", "BIOLOGY", "体液免疫中能够大量分泌抗体的细胞是什么？", p, 1,
                List.of("浆细胞", "吞噬细胞", "记忆T细胞", "红细胞"), "A", "B细胞受抗原刺激后增殖分化形成浆细胞，浆细胞可大量合成并分泌特异性抗体"));
        q.add(multi("BC-058", "BIOLOGY", "关于人体免疫系统，下列说法正确的是哪些？", p, 2,
                List.of("皮肤和黏膜属于第一道防线", "吞噬细胞参与非特异性免疫", "记忆细胞可使再次免疫反应更快", "过敏反应属于免疫功能完全缺失"), Set.of("A", "B", "C"),
                "过敏反应是免疫系统对特定物质反应过强，而不是免疫功能完全缺失；其余均为正确免疫机制"));
        q.add(fill("BC-059", "BIOLOGY", "能够与相应抗原发生特异性结合的免疫活性物质称为____。", p, 3, "抗体",
                "抗体由浆细胞分泌，能依据结构互补与特定抗原发生特异性结合"));
        q.add(fill("BC-060", "BIOLOGY", "接种疫苗后机体产生相应抗体和____细胞，从而建立较长期的特异性免疫。", p, 3, "记忆",
                "初次免疫反应除产生效应细胞和抗体外，还形成记忆细胞，使再次接触同种抗原时反应更快更强"));
    }

    private static void plantRegulation(List<DemoDataService.Question> q) {
        String p = "稳态与调节>植物生命活动调节>植物激素";
        q.add(single("BC-061", "BIOLOGY", "单侧光照下植物幼苗向光弯曲，主要与哪种激素分布不均有关？", p, 1,
                List.of("生长素", "乙烯", "脱落酸", "细胞分裂素"), "A", "单侧光使生长素向背光侧转移，背光侧细胞伸长较快，幼苗向光弯曲"));
        q.add(multi("BC-062", "BIOLOGY", "关于植物激素，下列说法正确的是哪些？", p, 2,
                List.of("生长素作用具有浓度相关性", "乙烯可促进果实成熟", "脱落酸可促进种子休眠", "植物激素只在产生部位起作用"), Set.of("A", "B", "C"),
                "植物激素可运输到其他部位发挥调节作用，其效应常与浓度、器官类型和发育阶段有关"));
        q.add(fill("BC-063", "BIOLOGY", "促进果实成熟的植物激素是____。", p, 3, "乙烯",
                "乙烯在果实成熟过程中发挥重要促进作用，生产中可利用其类似物调控成熟"));
        q.add(fill("BC-064", "BIOLOGY", "顶芽优先生长并抑制侧芽发育的现象称为____优势。", p, 3, "顶端",
                "顶芽产生的生长素向下运输并影响侧芽生长，形成顶芽优先生长的顶端优势"));
    }

    private static void populationAndCommunity(List<DemoDataService.Question> q) {
        String p = "生物与环境>种群和群落>种群数量与群落演替";
        q.add(single("BC-065", "BIOLOGY", "在资源和空间有限的环境中，种群数量常呈现哪种增长曲线？", p, 1,
                List.of("S形", "J形", "始终直线", "无任何规律"), "A", "环境存在阻力且资源有限时，种群增长逐渐减慢并在环境容纳量附近波动，呈S形"));
        q.add(multi("BC-066", "BIOLOGY", "关于种群和群落，下列说法正确的是哪些？", p, 2,
                List.of("年龄结构可影响种群未来数量变化", "出生率和死亡率直接影响种群密度", "群落演替中物种组成可改变", "演替一定使原有物种全部消失"), Set.of("A", "B", "C"),
                "演替是群落结构和物种组成的有序变化，但原有物种不一定全部消失"));
        q.add(fill("BC-067", "BIOLOGY", "环境条件所能维持的种群最大数量通常称为环境____量。", p, 3, "容纳",
                "环境容纳量又称K值，是特定环境条件下能够长期维持的种群数量上限"));
    }

    private static void ecosystem(List<DemoDataService.Question> q) {
        String p = "生物与环境>生态系统>能量流动与物质循环";
        q.add(single("BC-068", "BIOLOGY", "生态系统中能量流动的总体特点是什么？", p, 1,
                List.of("单向流动、逐级递减", "循环往复、总量不变", "只在消费者间流动", "可逆流动、逐级增加"), "A", "能量从太阳能等来源进入生态系统，沿营养级单向传递并在每一级因呼吸散失等逐级减少"));
        q.add(multi("BC-069", "BIOLOGY", "关于生态系统功能，下列说法正确的是哪些？", p, 2,
                List.of("物质可以循环利用", "能量不能在生态系统中循环", "信息传递可调节生物关系", "分解者不参与物质循环"), Set.of("A", "B", "C"),
                "分解者把有机物转化为无机物，是物质循环的重要环节；能量则最终以热散失，不能循环"));
        q.add(fill("BC-070", "BIOLOGY", "相邻两个营养级之间的能量传递效率通常约为____。", p, 3, "10%～20%",
                "生态系统相邻营养级能量传递效率通常约为10%～20%，其余用于呼吸或未被下一营养级利用"));
    }

    private static void fermentation(List<DemoDataService.Question> q) {
        String p = "生物技术>传统生物技术>发酵工程";
        q.add(single("BC-071", "BIOLOGY", "制作果酒时主要利用哪类微生物进行发酵？", p, 1,
                List.of("酵母菌", "乳酸菌", "醋酸菌", "蓝细菌"), "A", "酵母菌在缺氧条件下可将糖分解为酒精和二氧化碳，是果酒发酵的主要微生物"));
        q.add(multi("BC-072", "BIOLOGY", "关于传统发酵操作，下列做法合理的是哪些？", p, 2,
                List.of("控制适宜温度", "尽量避免杂菌污染", "根据目标产物调节氧气条件", "发酵装置完全不需要排气或压力管理"), Set.of("A", "B", "C"),
                "温度、无菌程度和氧气条件会影响目标微生物；产气发酵还需合理排气，避免压力过高"));
        q.add(fill("BC-073", "BIOLOGY", "乳酸菌制作酸奶时进行的是____呼吸。", p, 3, "无氧",
                "乳酸菌在无氧条件下把糖转化为乳酸并获得少量能量，属于无氧呼吸"));
    }

    private static void bioengineering(List<DemoDataService.Question> q) {
        String p = "生物技术>现代生物工程>基因工程与细胞工程";
        q.add(single("BC-074", "BIOLOGY", "基因工程中用于识别特定序列并切割DNA的工具酶是什么？", p, 1,
                List.of("限制性内切核酸酶", "DNA连接酶", "淀粉酶", "ATP合酶"), "A", "限制性内切核酸酶能识别特定核苷酸序列并在相应位置切割DNA"));
        q.add(multi("BC-075", "BIOLOGY", "关于现代生物工程技术，下列说法正确的是哪些？", p, 2,
                List.of("质粒可作为基因工程载体", "DNA连接酶可连接DNA片段", "植物组织培养利用细胞全能性", "PCR扩增DNA不需要模板"), Set.of("A", "B", "C"),
                "PCR必须有模板、引物、原料和耐高温DNA聚合酶等；质粒、连接酶和细胞全能性分别用于相应技术"));
        q.add(fill("BC-076", "BIOLOGY", "利用少量DNA在体外快速获得大量特定片段的技术简称为____。", p, 3, "PCR",
                "聚合酶链式反应可通过变性、退火和延伸循环，指数式扩增目标DNA片段"));
    }

    private static void inquiry(List<DemoDataService.Question> q) {
        String p = "生物实验>科学探究>变量控制与数据分析";
        q.add(single("BC-077", "BIOLOGY", "探究光照强度对光合速率影响时，研究者主动改变的量属于什么变量？", p, 1,
                List.of("自变量", "因变量", "无关变量", "随机误差"), "A", "实验者主动设置和改变的光照强度是自变量，光合速率是响应它的因变量"));
        q.add(multi("BC-078", "BIOLOGY", "设计生物学对照实验时，下列做法合理的是哪些？", p, 2,
                List.of("除自变量外尽量保持其他条件一致", "设置适当对照组", "进行足够重复以降低偶然性", "只选择符合预期的数据记录"), Set.of("A", "B", "C"),
                "单一变量、适当对照和重复实验有助于可靠归因；选择性记录会造成偏倚并破坏证据完整性"));
        q.add(fill("BC-079", "BIOLOGY", "同一处理设置多个平行样本并重复实验，主要是为了减小实验结果的____性。", p, 3, "偶然",
                "平行样本和重复实验可以削弱个体差异和随机波动，使结果更稳定并减少偶然性"));
    }
}
