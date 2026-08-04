import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const [inputJson, outputXlsx, previewPng] = process.argv.slice(2);
if (!inputJson || !outputXlsx || !previewPng) {
  throw new Error("用法：node build_seed_workbook.mjs <input.json> <output.xlsx> <preview.png>");
}

const questions = JSON.parse(await fs.readFile(inputJson, "utf8"));
if (!Array.isArray(questions) || questions.length === 0) {
  throw new Error("输入 JSON 必须是非空题目数组");
}

const fields = [
  "subject",
  "year",
  "region",
  "paperName",
  "questionNumber",
  "questionType",
  "content",
  "options",
  "questionImages",
  "correctAnswer",
  "standardAnalysis",
  "analysisImages",
  "analysisReviewStatus",
  "knowledgePoints",
  "difficultyLevel",
  "difficultyReason",
  "sourceFile",
  "reviewStatus",
];

function displayValue(field, value) {
  if (value === null || value === undefined) return null;
  if (field === "options") {
    return value.map((item) => `${item.label}. ${item.content}`).join("\n");
  }
  if (field === "knowledgePoints") return value.join(" > ");
  if (field === "questionImages") {
    if (!value.length) return null;
    return value
      .map((item) => `第${item.sourcePage}页：${item.imagePath}\n${item.description}`)
      .join("\n");
  }
  if (field === "analysisImages") {
    if (!value.length) return null;
    return value
      .map((item) => `第${item.sourcePage}页：${item.imagePath}\n${item.description}`)
      .join("\n");
  }
  if (field === "sourceFile") {
    return `题干：${value.paper}\n答案解析：${value.answerAnalysis}`;
  }
  return value;
}

const rows = questions.map((question) =>
  fields.map((field) => displayValue(field, question[field])),
);

const workbook = Workbook.create();
const sheet = workbook.worksheets.add("精选题库");
sheet.showGridLines = false;
sheet.mergeCells("A1:R1");
sheet.getRange("A1").values = [[`${questions[0].subject}精选种子题库（待审核）`]];
sheet.getRange("A2:R2").values = [fields];
sheet.getRangeByIndexes(2, 0, rows.length, fields.length).values = rows;

sheet.getRange("A1:R1").format = {
  fill: "#1F4E78",
  font: { bold: true, color: "#FFFFFF", size: 16 },
  horizontalAlignment: "center",
  verticalAlignment: "center",
};
sheet.getRange("A1:R1").format.rowHeight = 30;
sheet.getRange("A2:R2").format = {
  fill: "#D9EAF7",
  font: { bold: true, color: "#17365D" },
  horizontalAlignment: "center",
  verticalAlignment: "center",
  wrapText: true,
  borders: { preset: "outside", style: "medium", color: "#7F8C8D" },
};
sheet.getRange("A2:R2").format.rowHeight = 34;

const lastRow = rows.length + 2;
const body = sheet.getRange(`A3:R${lastRow}`);
body.format = {
  font: { color: "#222222", size: 10 },
  verticalAlignment: "top",
  wrapText: true,
  borders: {
    insideHorizontal: { style: "thin", color: "#D9E2F3" },
    bottom: { style: "thin", color: "#B4C6E7" },
  },
};
body.format.rowHeight = 112;
sheet.getRange(`A3:F${lastRow}`).format.horizontalAlignment = "center";
sheet.getRange(`M3:M${lastRow}`).format.horizontalAlignment = "center";
sheet.getRange(`O3:O${lastRow}`).format.horizontalAlignment = "center";
sheet.getRange(`R3:R${lastRow}`).format.horizontalAlignment = "center";

const widths = [8, 8, 9, 28, 10, 12, 46, 36, 42, 34, 58, 18, 14, 26, 12, 38, 46, 12];
widths.forEach((width, index) => {
  sheet.getRangeByIndexes(0, index, lastRow, 1).format.columnWidth = width;
});

sheet.getRange(`F3:F${lastRow}`).dataValidation = {
  rule: { type: "list", values: ["单选题", "多选题", "填空题", "实验填空题", "解答题"] },
};
sheet.getRange(`M3:M${lastRow}`).dataValidation = {
  rule: { type: "list", values: ["待审核", "审核通过", "退回修改"] },
};
sheet.getRange(`O3:O${lastRow}`).dataValidation = {
  rule: { type: "list", values: ["easy", "medium", "hard"] },
};
sheet.getRange(`R3:R${lastRow}`).dataValidation = {
  rule: { type: "list", values: ["待审核", "审核通过", "退回修改"] },
};
sheet.getRange(`M3:M${lastRow}`).conditionalFormats.add("containsText", {
  text: "待审核",
  format: { fill: "#FFF2CC", font: { color: "#9C6500", bold: true } },
});
sheet.getRange(`R3:R${lastRow}`).conditionalFormats.add("containsText", {
  text: "待审核",
  format: { fill: "#FFF2CC", font: { color: "#9C6500", bold: true } },
});
sheet.getRange(`O3:O${lastRow}`).conditionalFormats.add("containsText", {
  text: "hard",
  format: { fill: "#FCE4D6", font: { color: "#C00000", bold: true } },
});

sheet.tables.add(`A2:R${lastRow}`, true, "SeedQuestionTable");
sheet.freezePanes.freezeRows(2);
sheet.freezePanes.freezeColumns(6);

const inspection = await workbook.inspect({
  kind: "table",
  range: "精选题库!A1:F5",
  include: "values,formulas",
  tableMaxRows: 5,
  tableMaxCols: 6,
  maxChars: 4000,
});
console.log(inspection.ndjson);
const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 100 },
  summary: "final formula error scan",
});
console.log(errors.ndjson);

const preview = await workbook.render({
  sheetName: "精选题库",
  range: `A1:R${lastRow}`,
  scale: 0.8,
  format: "png",
});
await fs.mkdir(path.dirname(previewPng), { recursive: true });
await fs.writeFile(previewPng, new Uint8Array(await preview.arrayBuffer()));

await fs.mkdir(path.dirname(outputXlsx), { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputXlsx);
const savedXlsx = await fs.readFile(outputXlsx);
const importedWorkbook = await SpreadsheetFile.importXlsx(
  savedXlsx.buffer.slice(
    savedXlsx.byteOffset,
    savedXlsx.byteOffset + savedXlsx.byteLength,
  ),
);
const reimportInspection = await importedWorkbook.inspect({
  kind: "table",
  range: `精选题库!A${lastRow - 2}:R${lastRow}`,
  include: "values,formulas",
  tableMaxRows: 3,
  tableMaxCols: 18,
  maxChars: 12000,
});
console.log(reimportInspection.ndjson);
console.log(JSON.stringify({ outputXlsx, previewPng, rows: rows.length }));
