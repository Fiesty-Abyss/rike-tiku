[CmdletBinding()]
param(
    [string]$Database = 'rike_tiku',
    [string]$ContentPath = (Join-Path $PSScriptRoot '..\docs\content\high-frequency-points.v2.json')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Database -ne 'rike_tiku') {
    throw 'This controlled content command only permits the exact formal database name rike_tiku.'
}
if ($Database -in @('mysql', 'information_schema', 'performance_schema', 'sys', 'rike_tiku_demo')) {
    throw 'Protected or demo database rejected.'
}
if (-not $env:RIKE_TIKU_DB_PASSWORD) {
    throw 'RIKE_TIKU_DB_PASSWORD is absent.'
}
if (-not (Test-Path -LiteralPath $ContentPath)) {
    throw "Content file not found: $ContentPath"
}

$oldMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $env:RIKE_TIKU_DB_PASSWORD
$mysqlArgs = @('--default-character-set=utf8mb4', '-uroot', '-h', ($env:RIKE_TIKU_DB_HOST ?? 'localhost'), '-P', ($env:RIKE_TIKU_DB_PORT ?? '3306'), $Database, '--batch', '--skip-column-names', '--raw')

function Invoke-FormalSql([string]$Sql) {
    $result = & mysql @mysqlArgs -e $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "Formal database command failed with exit code $LASTEXITCODE."
    }
    if ($null -ne $result -and @($result).Count -gt 0) { return (@($result) -join "`n") }
    return
}

function Sql([AllowNull()][object]$Value) {
    if ($null -eq $Value) { return 'NULL' }
    $text = [string]$Value
    $text = $text.Replace([string][char]92, [string][char]92 + [char]92)
    $text = $text.Replace([string][char]39, [string][char]39 + [char]39)
    return [string]::Concat([char]39, $text, [char]39)
}

function One([string]$SqlText) {
    $value = (Invoke-FormalSql $SqlText).Trim()
    if ([string]::IsNullOrWhiteSpace($value)) { return $null }
    return $value.Split("`n")[0].Trim()
}

try {
    $version = One "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1"
    if ($version -ne '29') { throw "Formal database Flyway version is $version, expected 29." }
    $businessTables = [int](One "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name<>'flyway_schema_history'")
    if ($businessTables -ne 50) { throw "Formal database business table count is $businessTables, expected 50." }

    $content = Get-Content -LiteralPath $ContentPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $sourceName = '用户提供的物化生高频考点提纲与项目整理'
    $sourceRights = 'USER_PROVIDED'

    $scopeRows = @()
    $scopeSql = "SELECT r.id,r.ke_mu_id,k.ke_mu_dai_ma,j.yong_hu_id FROM ren_ke_guan_xi r JOIN ke_mu k ON k.id=r.ke_mu_id JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.zhuang_tai='ACTIVE' AND j.zhuang_tai='ACTIVE' AND j.yi_shan_chu=0 ORDER BY r.ke_mu_id,r.id"
    $scopeOutput = Invoke-FormalSql $scopeSql
    foreach ($line in ($scopeOutput -split "`n" | Where-Object { $_.Trim() })) {
        $parts = $line -split "`t"
        $scopeRows += [pscustomobject]@{ Id=[long]$parts[0]; SubjectId=[long]$parts[1]; SubjectCode=$parts[2]; CreatorId=[long]$parts[3] }
    }
    if ($scopeRows.Count -ne 6) { throw "Expected 6 active formal teaching scopes, found $($scopeRows.Count)." }

    $pointRows = @{}
    $pointOutput = Invoke-FormalSql "SELECT id,ke_mu_id FROM zhi_shi_dian WHERE zhuang_tai='ACTIVE' AND yi_shan_chu=0"
    foreach ($line in ($pointOutput -split "`n" | Where-Object { $_.Trim() })) {
        $parts = $line -split "`t"
        $pointRows[[long]$parts[0]] = [long]$parts[1]
    }

    $updated = 0
    $inserted = 0
    foreach ($item in $content) {
        $subject = @($scopeRows | Where-Object { $_.SubjectCode -eq [string]$item.subjectCode })
        if ($subject.Count -ne 2) { throw "Expected two active scopes for $($item.subjectCode), found $($subject.Count)." }
        $pointId = [long]$item.knowledgePointId
        if (-not $pointRows.ContainsKey($pointId) -or $pointRows[$pointId] -ne $subject[0].SubjectId) {
            throw "Knowledge point $pointId does not belong to subject $($item.subjectCode)."
        }

        foreach ($scope in $subject) {
            $existing = One "SELECT h.id FROM gao_pin_kao_dian h WHERE h.ren_ke_guan_xi_id=$($scope.Id) AND h.zhi_shi_dian_id=$pointId AND h.biao_ti=$(Sql $item.title) AND h.yi_shan_chu=0 ORDER BY h.id LIMIT 1"
            $fields = @(
                "zi_liao_lei_xing=$(Sql $item.type)",
                "biao_ti=$(Sql $item.title)",
                "nei_rong=$(Sql $item.content)",
                "ke_xue_nei_rong=$(Sql $item.scientificContent)",
                "latex_nei_rong=$(Sql $item.latex)",
                "shi_yong_tiao_jian=$(Sql $item.applicableConditions)",
                "han_yi_tui_dao=$(Sql $item.derivation)",
                "chang_jian_wu_qu=$(Sql $item.commonMistake)",
                "li_zi=$(Sql $item.example)",
                "ji_yi_kou_jue=$(Sql $item.mnemonic)",
                "lai_yuan_ming_cheng=$(Sql $sourceName)",
                'lai_yuan_di_zhi=NULL',
                "quan_li_zhuang_tai=$(Sql $sourceRights)",
                "chuang_jian_ren_yong_hu_id=$($scope.CreatorId)",
                "pai_xu=$([int]$item.sortOrder)",
                "zhuang_tai='PUBLISHED'",
                'yi_shan_chu=0'
            )
            if ($existing) {
                Invoke-FormalSql "UPDATE gao_pin_kao_dian SET $($fields -join ',') WHERE id=$existing"
                Invoke-FormalSql "INSERT INTO gao_pin_kao_dian_zhi_shi_dian(gao_pin_kao_dian_id,zhi_shi_dian_id,pai_xu) VALUES ($existing,$pointId,1) ON DUPLICATE KEY UPDATE pai_xu=1"
                $updated++
            } else {
                Invoke-FormalSql "INSERT INTO gao_pin_kao_dian(ren_ke_guan_xi_id,zhi_shi_dian_id,zi_liao_lei_xing,biao_ti,nei_rong,ke_xue_nei_rong,latex_nei_rong,shi_yong_tiao_jian,han_yi_tui_dao,chang_jian_wu_qu,li_zi,ji_yi_kou_jue,lai_yuan_ming_cheng,lai_yuan_di_zhi,quan_li_zhuang_tai,chuang_jian_ren_yong_hu_id,pai_xu,zhuang_tai) VALUES ($($scope.Id),$pointId,$(Sql $item.type),$(Sql $item.title),$(Sql $item.content),$(Sql $item.scientificContent),$(Sql $item.latex),$(Sql $item.applicableConditions),$(Sql $item.derivation),$(Sql $item.commonMistake),$(Sql $item.example),$(Sql $item.mnemonic),$(Sql $sourceName),NULL,$(Sql $sourceRights),$($scope.CreatorId),$([int]$item.sortOrder),'PUBLISHED')"
                $newId = One "SELECT id FROM gao_pin_kao_dian WHERE ren_ke_guan_xi_id=$($scope.Id) AND zhi_shi_dian_id=$pointId AND biao_ti=$(Sql $item.title) AND yi_shan_chu=0 ORDER BY id DESC LIMIT 1"
                Invoke-FormalSql "INSERT INTO gao_pin_kao_dian_zhi_shi_dian(gao_pin_kao_dian_id,zhi_shi_dian_id,pai_xu) VALUES ($newId,$pointId,1)"
                $inserted++
            }
        }
    }

    $unitDefinitions = @(
        [pscustomobject]@{ SubjectCode='PHYSICS'; Title='物理专题单元·力学与实验综合'; Intro='按测量、受力、动量与能量逐步完成力学专题训练。'; Difficulty=3; PrimaryPoint=25; Points=@(33,25,18) },
        [pscustomobject]@{ SubjectCode='PHYSICS'; Title='物理专题单元·电学、电磁与光学'; Intro='围绕电路故障、电磁感应和光学实验完成跨情境分析。'; Difficulty=3; PrimaryPoint=38; Points=@(38,42,12) },
        [pscustomobject]@{ SubjectCode='CHEMISTRY'; Title='化学专题单元·实验与定量分析'; Intro='从装置安全、物质分离到滴定定量，完成化学实验综合分析。'; Difficulty=3; PrimaryPoint=67; Points=@(67,72,56) },
        [pscustomobject]@{ SubjectCode='CHEMISTRY'; Title='化学专题单元·平衡、电化学与有机'; Intro='综合运用平衡移动、电极反应和有机路线解释实验现象。'; Difficulty=3; PrimaryPoint=50; Points=@(50,58,77) },
        [pscustomobject]@{ SubjectCode='BIOLOGY'; Title='生物专题单元·遗传与细胞代谢'; Intro='结合遗传概率、光合曲线和酶实验完成材料分析。'; Difficulty=3; PrimaryPoint=123; Points=@(123,86,100) },
        [pscustomobject]@{ SubjectCode='BIOLOGY'; Title='生物专题单元·稳态、生态与生物技术'; Intro='从调节机制、生态系统到基因工程梳理生命系统综合问题。'; Difficulty=3; PrimaryPoint=114; Points=@(114,95,105) }
    )
    $unitCount = 0
    foreach ($definition in $unitDefinitions) {
        $scope = @($scopeRows | Where-Object { $_.SubjectCode -eq $definition.SubjectCode })[0]
        $subjectId = $scope.SubjectId
        $questionIds = @()
        foreach ($pointId in $definition.Points) {
            $question = One "SELECT q.id FROM ti_mu q JOIN ti_mu_zhi_shi_dian qp ON qp.ti_mu_id=q.id AND qp.zhi_shi_dian_id=$pointId AND qp.yi_shan_chu=0 WHERE q.ke_mu_id=$subjectId AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING' AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0 ORDER BY q.id LIMIT 1"
            if (-not $question) { throw "Published topic question missing for $($definition.SubjectCode) point $pointId." }
            $questionIds += [long]$question
        }
        $primaryPoint = [long]$definition.PrimaryPoint
        if (-not $pointRows.ContainsKey($primaryPoint) -or $pointRows[$primaryPoint] -ne $subjectId) { throw "Unit primary point $primaryPoint does not belong to $($definition.SubjectCode)." }
        $unitId = One "SELECT id FROM zhuan_ti_xue_xi_dan_yuan WHERE ke_mu_id=$subjectId AND biao_ti=$(Sql $definition.Title) AND yi_shan_chu=0 ORDER BY id LIMIT 1"
        if ($unitId) {
            Invoke-FormalSql "UPDATE zhuan_ti_xue_xi_dan_yuan SET jian_jie=$(Sql $definition.Intro),nan_du_ceng_ji=$([int]$definition.Difficulty),zhu_zhi_shi_dian_id=$primaryPoint,pai_xu=$([int]$definition.Difficulty),zhuang_tai='PUBLISHED',chuang_jian_ren_id=$($scope.CreatorId),lai_yuan_lei_xing='PROJECT_AUTHORED',lai_yuan_ming_cheng=$(Sql $sourceName),quan_li_zhuang_tai='USER_PROVIDED',yi_shan_chu=0 WHERE id=$unitId"
        } else {
            Invoke-FormalSql "INSERT INTO zhuan_ti_xue_xi_dan_yuan(ke_mu_id,biao_ti,jian_jie,nan_du_ceng_ji,zhu_zhi_shi_dian_id,pai_xu,zhuang_tai,chuang_jian_ren_id,lai_yuan_lei_xing,lai_yuan_ming_cheng,quan_li_zhuang_tai) VALUES ($subjectId,$(Sql $definition.Title),$(Sql $definition.Intro),$([int]$definition.Difficulty),$primaryPoint,$([int]$definition.Difficulty),'PUBLISHED',$($scope.CreatorId),'PROJECT_AUTHORED',$(Sql $sourceName),'USER_PROVIDED')"
            $unitId = One "SELECT id FROM zhuan_ti_xue_xi_dan_yuan WHERE ke_mu_id=$subjectId AND biao_ti=$(Sql $definition.Title) AND yi_shan_chu=0 ORDER BY id DESC LIMIT 1"
        }
        $stages = @('FOUNDATION','TRANSFER','ADVANCED')
        for ($index = 0; $index -lt 3; $index++) {
            Invoke-FormalSql "INSERT INTO zhuan_ti_xue_xi_dan_yuan_ti_mu(dan_yuan_id,ti_mu_id,xue_xi_jie_duan,pai_xu) VALUES ($unitId,$($questionIds[$index]),$(Sql $stages[$index]),$($index + 1)) ON DUPLICATE KEY UPDATE xue_xi_jie_duan=$(Sql $stages[$index]),pai_xu=$($index + 1)"
        }
        $unitCount++
    }

    $cardCount = One "SELECT COUNT(*) FROM gao_pin_kao_dian WHERE zhuang_tai='PUBLISHED' AND yi_shan_chu=0"
    $unitSummary = Invoke-FormalSql "SELECT k.ke_mu_dai_ma,COUNT(u.id),GROUP_CONCAT((SELECT COUNT(*) FROM zhuan_ti_xue_xi_dan_yuan_ti_mu i WHERE i.dan_yuan_id=u.id) ORDER BY u.id SEPARATOR ',') FROM zhuan_ti_xue_xi_dan_yuan u JOIN ke_mu k ON k.id=u.ke_mu_id WHERE u.zhuang_tai='PUBLISHED' AND u.yi_shan_chu=0 GROUP BY k.ke_mu_dai_ma ORDER BY k.ke_mu_dai_ma"
    Write-Output 'FORMAL_STUDENT_CONTENT_APPLIED'
    Write-Output "CARDS_UPDATED=$updated"
    Write-Output "CARDS_INSERTED=$inserted"
    Write-Output "PUBLISHED_CARD_TOTAL=$cardCount"
    Write-Output "UNITS_WRITTEN=$unitCount"
    Write-Output 'UNIT_SUMMARY='
    Write-Output $unitSummary
    Write-Output 'FLYWAY=V29'
    Write-Output 'BUSINESS_TABLES=50'
}
finally {
    if ($null -eq $oldMysqlPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $oldMysqlPassword }
}
