import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const [physicsJson, chemistryJson, biologyJson, previewDir] = process.argv.slice(2);
if (!physicsJson || !chemistryJson || !biologyJson || !previewDir) {
  throw new Error(
    "用法：node build_master_workbooks.mjs <physics.json> <chemistry.json> <biology.json> <preview_dir>",
  );
}

const inputs = [physicsJson, chemistryJson, biologyJson];
const colors = {
  物理: { dark: "#17365D", mid: "#D9EAF7", light: "#EEF5FB" },
  化学: { dark: "#375623", mid: "#E2F0D9", light: "#F2F8EE" },
  生物: { dark: "#6B3F1D", mid: "#FCE4D6", light: "#FFF5EE" },
};

const headers = [
  "学科",
  "年份",
  "区域",
  "试卷来源",
  "题号",
  "题型",
  "题干",
  "选项",
  "题干图片",
  "答案",
  "标准解析",
  "解析图片",
  "公式对象数",
  "普通图片对象数",
  "题干图片数",
  "解析图片数",
  "解析审核状态",
  "知识点",
  "难度",
  "难度理由",
  "题干源文件",
  "解析源文件",
  "审核状态",
];

function fitCell(value) {
  if (value === null || value === undefined) return null;
  const text = String(value);
  if (text.length <= 32000) return text;
  return `${text.slice(0, 31920)}\n[单元格内容过长，完整内容请查看 JSON]`;
}

function optionsText(options) {
  if (!options) return null;
  return fitCell(options.map((item) => `${item.label}. ${item.content}`).join("\n"));
}

function imagesText(images) {
  if (!images?.length) return null;
  return fitCell(
    images
      .map((item) => `第${item.sourcePage}页｜${item.imagePath}\n${item.description}`)
      .join("\n"),
  );
}

function rowValues(question) {
  return [
    question.subject,
    question.year,
    question.region,
    question.paperName,
    question.questionNumber,
    question.questionType,
    fitCell(question.content),
    optionsText(question.options),
    imagesText(question.questionImages),
    fitCell(question.correctAnswer),
    fitCell(question.standardAnalysis),
    imagesText(question.analysisImages),
    null,
    null,
    question.questionImages.length,
    question.analysisImages.length,
    question.analysisReviewStatus,
    Array.isArray(question.knowledgePoints)
      ? question.knowledgePoints.join(" > ")
      : question.knowledgePoints,
    question.difficultyLevel,
    question.difficultyReason,
    question.sourceFile.paper,
    question.sourceFile.answerAnalysis,
    question.reviewStatus,
  ];
}

function safeTableName(subject) {
  return { 物理: "PhysicsMasterTable", 化学: "ChemistryMasterTable", 生物: "BiologyMasterTable" }[
    subject
  ];
}

async function buildOne(jsonPath) {
  const questions = JSON.parse(await fs.readFile(jsonPath, "utf8"));
  if (!Array.isArray(questions) || questions.length === 0) {
    throw new Error(`${jsonPath} 不是非空题目数组`);
  }
  const subject = questions[0].subject;
  if (!questions.every((item) => item.subject === subject)) {
    throw new Error(`${jsonPath} 混入其他学科`);
  }
  const palette = colors[subject];
  const rows = questions.map(rowValues);
  const lastRow = rows.length + 2;

  const workbook = Workbook.create();
  const dataSheet = workbook.worksheets.add("题目检查");
  dataSheet.showGridLines = false;
  dataSheet.mergeCells("A1:W1");
  dataSheet.getRange("A1").values = [[`${subject}母题库检查表（首批：2023新课标卷、2023全国甲卷）`]];
  dataSheet.getRange("A2:W2").values = [headers];
  dataSheet.getRangeByIndexes(2, 0, rows.length, headers.length).values = rows;

  dataSheet.getRange("A1:W1").format = {
    fill: palette.dark,
    font: { bold: true, color: "#FFFFFF", size: 16 },
    horizontalAlignment: "center",
    verticalAlignment: "center",
  };
  dataSheet.getRange("A1:W1").format.rowHeight = 31;
  dataSheet.getRange("A2:W2").format = {
    fill: palette.mid,
    font: { bold: true, color: palette.dark },
    horizontalAlignment: "center",
    verticalAlignment: "center",
    wrapText: true,
    borders: { preset: "outside", style: "medium", color: "#7F8C8D" },
  };
  dataSheet.getRange("A2:W2").format.rowHeight = 36;

  const body = dataSheet.getRange(`A3:W${lastRow}`);
  body.format = {
    font: { color: "#222222", size: 10 },
    verticalAlignment: "top",
    wrapText: true,
    borders: {
      insideHorizontal: { style: "thin", color: "#D9E2F3" },
      bottom: { style: "thin", color: "#B4C6E7" },
    },
  };
  body.format.rowHeight = 92;

  for (let row = 3; row <= lastRow; row += 1) {
    const joined = `G${row}&H${row}&J${row}&K${row}`;
    dataSheet.getRange(`M${row}`).formulas = [
      [`=(LEN(${joined})-LEN(SUBSTITUTE(${joined},"〔公式对象","")))/LEN("〔公式对象")`],
    ];
    dataSheet.getRange(`N${row}`).formulas = [
      [`=(LEN(${joined})-LEN(SUBSTITUTE(${joined},"〔图片对象","")))/LEN("〔图片对象")`],
    ];
  }

  const widths = [8, 8, 8, 30, 9, 13, 48, 38, 48, 32, 64, 48, 12, 14, 12, 12, 14, 24, 11, 34, 44, 44, 12];
  widths.forEach((width, index) => {
    dataSheet.getRangeByIndexes(0, index, lastRow, 1).format.columnWidth = width;
  });
  dataSheet.getRange(`A3:F${lastRow}`).format.horizontalAlignment = "center";
  dataSheet.getRange(`M3:Q${lastRow}`).format.horizontalAlignment = "center";
  dataSheet.getRange(`S3:S${lastRow}`).format.horizontalAlignment = "center";
  dataSheet.getRange(`W3:W${lastRow}`).format.horizontalAlignment = "center";

  dataSheet.getRange(`F3:F${lastRow}`).dataValidation = {
    rule: { type: "list", values: ["单选题", "多选题", "填空题", "实验填空题", "解答题"] },
  };
  dataSheet.getRange(`Q3:Q${lastRow}`).dataValidation = {
    rule: { type: "list", values: ["待审核", "审核通过", "退回修改"] },
  };
  dataSheet.getRange(`S3:S${lastRow}`).dataValidation = {
    rule: { type: "list", values: ["easy", "medium", "hard"] },
  };
  dataSheet.getRange(`W3:W${lastRow}`).dataValidation = {
    rule: { type: "list", values: ["待审核", "图片缺失", "审核通过", "退回修改"] },
  };
  for (const column of ["Q", "W"]) {
    dataSheet.getRange(`${column}3:${column}${lastRow}`).conditionalFormats.add("containsText", {
      text: "待审核",
      format: { fill: "#FFF2CC", font: { color: "#9C6500", bold: true } },
    });
  }
  dataSheet.tables.add(`A2:W${lastRow}`, true, safeTableName(subject));
  dataSheet.freezePanes.freezeRows(2);
  dataSheet.freezePanes.freezeColumns(6);

  const stats = workbook.worksheets.add("质量统计");
  stats.showGridLines = false;
  stats.mergeCells("A1:C1");
  stats.getRange("A1").values = [[`${subject}母题库质量统计`]];
  stats.getRange("A3:C3").values = [["检查项", "结果", "说明"]];
  const paper1 = "2023年高考真题——理综（新课标卷）";
  const paper2 = "2023年高考真题——理综（全国甲卷）";
  const metrics = [
    ["题目总数", `=COUNTA('题目检查'!A3:A${lastRow})`, "按学科汇总"],
    ["2023新课标卷", `=COUNTIF('题目检查'!D3:D${lastRow},"${paper1}")`, "题数"],
    ["2023全国甲卷", `=COUNTIF('题目检查'!D3:D${lastRow},"${paper2}")`, "题数"],
    ["单选题", `=COUNTIF('题目检查'!F3:F${lastRow},"单选题")`, "题数"],
    ["多选题", `=COUNTIF('题目检查'!F3:F${lastRow},"多选题")`, "题数"],
    ["实验填空题", `=COUNTIF('题目检查'!F3:F${lastRow},"实验填空题")`, "题数"],
    ["解答题", `=COUNTIF('题目检查'!F3:F${lastRow},"解答题")`, "题数"],
    ["公式对象", `=SUM('题目检查'!M3:M${lastRow})`, "保留对象标记及预览图"],
    ["普通图片对象", `=SUM('题目检查'!N3:N${lastRow})`, "题图、结构式、图表等"],
    ["题干图片", `=SUM('题目检查'!O3:O${lastRow})`, "图片记录数"],
    ["解析图片", `=SUM('题目检查'!P3:P${lastRow})`, "图片记录数"],
    ["缺失答案", `=COUNTBLANK('题目检查'!J3:J${lastRow})`, "应为0"],
    ["缺失解析", `=COUNTBLANK('题目检查'!K3:K${lastRow})`, "应为0"],
    ["待审核", `=COUNTIF('题目检查'!W3:W${lastRow},"待审核")`, "未自动判断知识点和难度"],
  ];
  stats.getRangeByIndexes(3, 0, metrics.length, 1).values = metrics.map((row) => [row[0]]);
  stats.getRangeByIndexes(3, 1, metrics.length, 1).formulas = metrics.map((row) => [row[1]]);
  stats.getRangeByIndexes(3, 2, metrics.length, 1).values = metrics.map((row) => [row[2]]);

  stats.getRange("A1:C1").format = {
    fill: palette.dark,
    font: { bold: true, color: "#FFFFFF", size: 16 },
    horizontalAlignment: "center",
    verticalAlignment: "center",
  };
  stats.getRange("A1:C1").format.rowHeight = 31;
  stats.getRange("A3:C3").format = {
    fill: palette.mid,
    font: { bold: true, color: palette.dark },
    horizontalAlignment: "center",
    verticalAlignment: "center",
  };
  stats.getRange(`A4:C${metrics.length + 3}`).format = {
    fill: palette.light,
    wrapText: true,
    borders: { insideHorizontal: { style: "thin", color: "#D9E2F3" } },
  };
  stats.getRange(`B4:B${metrics.length + 3}`).format = {
    font: { bold: true, color: palette.dark },
    horizontalAlignment: "center",
  };
  stats.getRange("A1:A20").format.columnWidth = 24;
  stats.getRange("B1:B20").format.columnWidth = 16;
  stats.getRange("C1:C20").format.columnWidth = 36;
  stats.freezePanes.freezeRows(3);

  const headInspection = await workbook.inspect({
    kind: "region",
    sheetId: "题目检查",
    range: "A1:W6",
    maxChars: 5000,
    tableMaxRows: 6,
    tableMaxCols: 23,
    tableMaxCellChars: 100,
  });
  console.log(headInspection.ndjson);
  const statsInspection = await workbook.inspect({
    kind: "region",
    sheetId: "质量统计",
    range: `A1:C${metrics.length + 3}`,
    maxChars: 5000,
    tableMaxRows: 20,
    tableMaxCols: 3,
    tableMaxCellChars: 120,
  });
  console.log(statsInspection.ndjson);
  const errors = await workbook.inspect({
    kind: "match",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
    options: { useRegex: true, maxResults: 100 },
    summary: `${subject}公式错误扫描`,
  });
  console.log(errors.ndjson);

  await fs.mkdir(previewDir, { recursive: true });
  for (const [sheetName, range] of [
    ["题目检查", `A1:W${Math.min(lastRow, 12)}`],
    ["质量统计", `A1:C${metrics.length + 3}`],
  ]) {
    const preview = await workbook.render({ sheetName, range, scale: 0.8, format: "png" });
    const previewPath = path.join(previewDir, `${subject}_${sheetName}.png`);
    await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));
  }

  const outputXlsx = jsonPath.replace(/\.json$/i, ".xlsx");
  const xlsx = await SpreadsheetFile.exportXlsx(workbook);
  await xlsx.save(outputXlsx);

  const saved = await fs.readFile(outputXlsx);
  const imported = await SpreadsheetFile.importXlsx(
    saved.buffer.slice(saved.byteOffset, saved.byteOffset + saved.byteLength),
  );
  const reimport = await imported.inspect({
    kind: "region",
    sheetId: "质量统计",
    range: `A1:C${metrics.length + 3}`,
    maxChars: 5000,
    tableMaxRows: 20,
    tableMaxCols: 3,
    tableMaxCellChars: 120,
  });
  console.log(reimport.ndjson);
  return { subject, rows: rows.length, outputXlsx };
}

const outputs = [];
for (const input of inputs) {
  outputs.push(await buildOne(input));
}
console.log(JSON.stringify(outputs));
