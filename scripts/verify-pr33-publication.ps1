param(
    [string]$Repository = "Fiesty-Abyss/rike-tiku",
    [string]$Ref = "feat/final-product-completion"
)

$ErrorActionPreference = "Stop"
$required = @(
    "README.md",
    "docs/evidence/thesis-final/README.md",
    (1..24 | ForEach-Object { $extensions = if ($_ -ge 22) { "svg" } else { "png" }; $names = @(
        "01-portal-desktop", "02-login", "03-student-dashboard", "04-practice", "05-result-standard", "06-wrong-questions",
        "07-student-ai-analysis", "08-student-ai-chat", "09-student-ai-variant", "10-student-ai-variant-result",
        "11-teacher-workspace", "12-teacher-ai-review", "13-teacher-paper-builder", "14-paper-student-preview",
        "15-paper-answer-preview", "16-admin-dashboard", "17-admin-ai-models", "18-admin-password-notifications",
        "19-admin-ai-generation", "20-portal-mobile", "21-student-mobile", "22-system-architecture",
        "23-ai-controlled-flow", "24-database-modules"
    ); "docs/evidence/thesis-final/$($names[$_ - 1]).$extensions" }),
    "docs/FEATURE_SCREENSHOT_CODE_INDEX.md",
    "docs/EXCEL_IMPORT_GUIDE.md",
    "docs/DATABASE_SCHEMA_REFERENCE.md",
    "docs/SQL_EXAMPLES.md",
    "docs/THESIS_WRITING_HUB.md",
    "docs/THESIS_REFERENCES.md",
    "docs/references/references.bib",
    "database/schema_snapshot_v19.sql",
    "database/diagrams/rike_tiku_er.md",
    "docs/templates/student-import-template.xlsx",
    "docs/templates/question-import-template.xlsx",
    "docs/thesis/RIKE_THESIS_DRAFT.md",
    "docs/thesis/RIKE_THESIS_FACT_CHECK.md",
    "docs/thesis/RIKE_DEFENSE_OUTLINE.md"
) | ForEach-Object { $_ } | Where-Object { $_ }

$tree = @(git ls-tree -r --name-only "origin/$Ref")
$apiTreeResponse = gh api "repos/$Repository/git/trees/$([uri]::EscapeDataString($Ref))?recursive=1" | ConvertFrom-Json
$apiTree = @($apiTreeResponse.tree | ForEach-Object { $_.path })
$results = foreach ($path in $required) {
    $treeOk = $tree -contains $path
    $apiOk = $apiTree -contains $path
    [pscustomobject]@{ Path = $path; GitTree = $treeOk; GitHubApi = $apiOk }
}

$readme = Get-Content -Raw README.md
$targets = @()
$targets += [regex]::Matches($readme, '!\[[^\]]*\]\(([^)]+)\)') | ForEach-Object { $_.Groups[1].Value }
$targets += [regex]::Matches($readme, '<img\s+[^>]*src="([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
$targets += [regex]::Matches($readme, '(?<!\!)\[[^\]]+\]\(([^)]+)\)') | ForEach-Object { $_.Groups[1].Value }
$localTargets = $targets | Where-Object { $_ -notmatch '^(https?:|mailto:|#)' } | ForEach-Object { ($_ -split '#')[0] } | Where-Object { $_ } | Sort-Object -Unique
$missingReadme = @($localTargets | Where-Object { -not (Test-Path -LiteralPath $_) })

$finalNames = @(Get-ChildItem docs/evidence/thesis-final -File | Select-Object -ExpandProperty Name)
$expectedNames = @($required | Where-Object { $_ -match '^docs/evidence/thesis-final/(?:\d{2}-.+\.(?:png|svg))$' } | ForEach-Object { Split-Path $_ -Leaf })
$missingFinal = @($expectedNames | Where-Object { $finalNames -notcontains $_ })
$emptyFinal = @(Get-ChildItem docs/evidence/thesis-final -File | Where-Object { $_.Length -eq 0 } | Select-Object -ExpandProperty Name)

$results | Format-Table -AutoSize
"REMOTE_REQUIRED_TOTAL=$($results.Count)"
"REMOTE_TREE_MISSING=$(@($results | Where-Object { -not $_.GitTree }).Count)"
"REMOTE_API_MISSING=$(@($results | Where-Object { -not $_.GitHubApi }).Count)"
"README_LOCAL_TARGETS=$($localTargets.Count)"
"README_MISSING=$($missingReadme.Count)"
"FINAL_NUMBERED_MISSING=$($missingFinal.Count)"
"FINAL_EMPTY=$($emptyFinal.Count)"

if (($results | Where-Object { -not $_.GitTree -or -not $_.GitHubApi }) -or $missingReadme -or $missingFinal -or $emptyFinal) {
    exit 1
}
